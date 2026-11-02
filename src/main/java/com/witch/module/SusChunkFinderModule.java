package com.witch.module;

import com.mojang.blaze3d.platform.InputConstants;
import com.witch.gui.FontUtil;
import com.witch.gui.RenderUtil;
import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import com.witch.setting.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Marca chunks com sinais fortes de base de jogador (containers, redstone, decoracao).
 * Cada bloco tem um peso; blocos que nascem naturalmente (tabua, trilho, tocha...) nao contam.
 * Alerta com mensagem no centro + som de level-up quando encontra uma chunk nova.
 * Trial spawner: marca vermelho e nao apita.
 * Range de chunks 1-36. Limite de avisos 1-10. Cada chunk so alerta uma vez.
 * Keybind "Reload chunks" limpa e rescanneia.
 */
public class SusChunkFinderModule extends Module implements EspSource {
	private static final int SCAN_EVERY_TICKS = 3;
	private static final int HALF_HEIGHT = 48;
	private static final long ALERT_DISPLAY_MS = 2800L;
	/** Vermelho fixo pra trial (mesmo tom da palette). */
	private static final int TRIAL_COLOR = 0xFFEF4444;

	private static SusChunkFinderModule instance;

	private final IntSetting radius = add(new IntSetting("Chunk radius", 1, 36, 4, ""));
	private final IntSetting threshold = add(new IntSetting("Min score", 5, 200, 30, ""));
	private final IntSetting maxAlerts = add(new IntSetting("Max alerts", 1, 10, 3, ""));
	private final ColorSetting color = add(new ColorSetting("Color", 4));
	private final KeybindSetting reloadKey = add(new KeybindSetting("Reload chunks"));

	private final Map<Block, Integer> weights = buildWeights();
	private final Map<Long, int[]> found = new HashMap<>();
	/** Chunks que ja avisaram / trial — nao alertam de novo ate reload ou desligar. */
	private final Set<Long> seen = new HashSet<>();
	private volatile List<int[]> view = List.of();

	private int[][] offsets = null;
	private int offsetsRadius = -1;
	private int cursor = 0;
	private int cooldown = 0;
	private int alertCount = 0;

	private volatile String alertText = null;
	private volatile long alertUntil = 0L;

	public SusChunkFinderModule() {
		super("Sus Chunk Finder", Category.VISUALS);
		instance = this;
	}

	private static int weightOf(String n) {
		// quase nunca aparecem sozinhos
		if (n.endsWith("shulker_box") || n.equals("ender_chest") || n.equals("beacon")
				|| n.equals("enchanting_table")) {
			return 10;
		}
		if (n.equals("hopper") || n.equals("dropper") || n.equals("dispenser") || n.equals("observer")
				|| n.equals("piston") || n.equals("sticky_piston") || n.equals("repeater")
				|| n.equals("comparator") || n.equals("redstone_lamp") || n.equals("note_block")
				|| n.equals("daylight_detector")) {
			return 8;
		}
		// containers e estacoes de trabalho
		if (n.equals("chest") || n.equals("trapped_chest") || n.equals("barrel") || n.equals("furnace")
				|| n.equals("blast_furnace") || n.equals("smoker") || n.equals("anvil")
				|| n.equals("chipped_anvil") || n.equals("damaged_anvil") || n.equals("brewing_stand")
				|| n.equals("smithing_table")) {
			return 4;
		}
		// decoracao que nao nasce natural
		if (n.equals("crafting_table") || n.endsWith("_wool") || n.contains("concrete")
				|| n.endsWith("stained_glass") || n.endsWith("stained_glass_pane")
				|| n.endsWith("glazed_terracotta")) {
			return 2;
		}
		return 0;
	}

	private static Map<Block, Integer> buildWeights() {
		Map<Block, Integer> m = new HashMap<>();
		for (Block b : BuiltInRegistries.BLOCK) {
			int w = weightOf(BuiltInRegistries.BLOCK.getKey(b).getPath());
			if (w > 0) m.put(b, w);
		}
		return m;
	}

	private static boolean isTrialSpawner(Block b) {
		String n = BuiltInRegistries.BLOCK.getKey(b).getPath();
		return n.equals("trial_spawner");
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		int normal = color.get();
		for (int[] v : view) {
			int x0 = v[0] * 16;
			int z0 = v[1] * 16;
			int argb = v.length > 5 && v[5] != 0 ? TRIAL_COLOR : normal;
			esp.box(x0, v[2], z0, x0 + 16, v[3] + 1, z0 + 16, argb, 0f);
		}
	}

	@Override
	protected void onEnable() {
		cooldown = 0;
		cursor = 0;
		alertCount = 0;
		seen.clear();
	}

	@Override
	protected void onDisable() {
		found.clear();
		seen.clear();
		view = List.of();
		alertText = null;
		alertUntil = 0L;
		alertCount = 0;
	}

	/** Limpa chunks expostas, seen e contador de alertas — força rescan. */
	private void reloadExposed() {
		found.clear();
		seen.clear();
		view = List.of();
		alertCount = 0;
		alertText = null;
		alertUntil = 0L;
		cursor = 0;
		cooldown = 0;
	}

	private void buildOffsets(int r) {
		List<int[]> list = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				list.add(new int[]{dx, dz});
			}
		}
		list.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
		offsets = list.toArray(new int[0][]);
		offsetsRadius = r;
		cursor = 0;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();

		// keybind de reload (edge detect, so fora de tela)
		int rk = reloadKey.key();
		if (rk != 0) {
			boolean down = InputConstants.isKeyDown(mc.getWindow(), rk);
			if (down && !reloadKey.wasDown && mc.gui.screen() == null) {
				reloadExposed();
			}
			reloadKey.wasDown = down;
		} else {
			reloadKey.wasDown = false;
		}

		if (--cooldown > 0) return;
		cooldown = SCAN_EVERY_TICKS;

		var level = mc.level;
		var player = mc.player;
		if (player == null || level == null) {
			found.clear();
			view = List.of();
			return;
		}

		int r = radius.get();
		if (offsets == null || offsetsRadius != r) buildOffsets(r);

		BlockPos pp = player.blockPosition();
		int pcx = pp.getX() >> 4;
		int pcz = pp.getZ() >> 4;

		int[] off = offsets[cursor];
		cursor = (cursor + 1) % offsets.length;
		int cx = pcx + off[0];
		int cz = pcz + off[1];
		if (level.getChunkSource().hasChunk(cx, cz)) {
			scanChunk(level, cx, cz, pp.getY(), player);
		}

		found.values().removeIf(v -> Math.abs(v[0] - pcx) > r + 1 || Math.abs(v[1] - pcz) > r + 1);
		view = new ArrayList<>(found.values());
	}

	private void scanChunk(net.minecraft.client.multiplayer.ClientLevel level, int cx, int cz, int py,
			net.minecraft.client.player.LocalPlayer player) {
		int minY = Math.max(level.getMinY(), py - HALF_HEIGHT);
		int maxY = py + HALF_HEIGHT;
		int score = 0;
		int lo = Integer.MAX_VALUE;
		int hi = Integer.MIN_VALUE;
		boolean hasTrial = false;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					pos.set(cx * 16 + x, y, cz * 16 + z);
					Block block = level.getBlockState(pos).getBlock();
					if (isTrialSpawner(block)) {
						hasTrial = true;
						if (y < lo) lo = y;
						if (y > hi) hi = y;
					}
					Integer w = weights.get(block);
					if (w != null) {
						score += w;
						if (y < lo) lo = y;
						if (y > hi) hi = y;
					}
				}
			}
		}

		long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);

		// trial spawner: sempre mostra em vermelho, sem alerta
		if (hasTrial) {
			if (lo == Integer.MAX_VALUE) {
				lo = minY;
				hi = maxY;
			}
			boolean isNew = !found.containsKey(key);
			found.put(key, new int[]{cx, cz, lo, hi, score, 1});
			if (isNew) {
				seen.add(key); // marca vista, sem som/mensagem
			}
			return;
		}

		if (score >= threshold.get()) {
			boolean isNew = !found.containsKey(key);
			found.put(key, new int[]{cx, cz, lo, hi, score, 0});
			if (isNew) {
				tryAlert(key, player);
			}
		} else {
			found.remove(key);
		}
	}

	private void tryAlert(long key, net.minecraft.client.player.LocalPlayer player) {
		if (seen.contains(key)) return;
		if (alertCount >= maxAlerts.get()) {
			seen.add(key);
			return;
		}

		seen.add(key);
		alertCount++;
		alertText = "Chunk suspeita encontrada";
		alertUntil = System.currentTimeMillis() + ALERT_DISPLAY_MS;

		player.playSound(SoundEvents.PLAYER_LEVELUP, 0.7f, 1.0f);
	}

	/** Mensagem pequena no centro da tela (chamado pelo HudRenderer). */
	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled()) return;
		String text = instance.alertText;
		long until = instance.alertUntil;
		if (text == null || System.currentTimeMillis() > until) return;

		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int tw = FontUtil.width(text);
		int th = FontUtil.height();
		int padX = 10;
		int padY = 6;
		int bw = tw + padX * 2;
		int bh = th + padY * 2;
		int x = (sw - bw) / 2;
		int y = sh / 2 - 40;

		long left = until - System.currentTimeMillis();
		float a = left > 500 ? 1f : Math.max(0f, left / 500f);

		RenderUtil.roundRect(g, x, y, bw, bh, 5, RenderUtil.alpha(0xFF1A1028, 0.82f * a));
		RenderUtil.roundRect(g, x, y, 3, bh, 1, RenderUtil.alpha(0xFFE8C56A, a));
		FontUtil.draw(g, text, x + padX, y + padY, RenderUtil.alpha(0xFFFFF0C8, a));
	}
}
