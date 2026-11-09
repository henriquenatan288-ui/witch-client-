package com.witch.module;

import com.mojang.blaze3d.platform.InputConstants;
import com.witch.gui.FontUtil;
import com.witch.gui.RenderUtil;
import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import com.witch.setting.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sus Solo Finder: feito pra usar voando. Escaneia a faixa perto da superficie de cada chunk (nao depende da sua altura),
 * apita ao achar uma suspeita e desenha uma placa neon no solo da chunk pra voce ver de longe.
 * Cor = "chance de base" estimada pela pontuacao dos blocos: vermelho 25%, amarelo 50%, verde 75%, branco 81%+.
 * A chance e uma heuristica (peso dos blocos), nao uma probabilidade real.
 */
public class SusSoloFinderModule extends Module implements EspSource {
	private static final long SCAN_BUDGET_NS = 3_000_000L;
	private static final int MAX_SCANS_PER_TICK = 8;
	private static final long RESCAN_TICKS = 900L;
	private final java.util.Map<Long, Long> lastScan = new java.util.HashMap<>();
	private static final int BELOW = 24;
	private static final int ABOVE = 40;
	private static final long ALERT_DISPLAY_MS = 2800L;
	/** score/(score+K): K=40 -> 25% ~ 13 pts, 50% = 40, 75% = 120, 81% ~ 170. */
	private static final double K = 40.0;

	private static final int RED = 0xFFFF1F4B;
	private static final int YELLOW = 0xFFFFF01F;
	private static final int GREEN = 0xFF39FF14;
	private static final int WHITE = 0xFFFFFFFF;

	private static SusSoloFinderModule instance;

	private final IntSetting radius = add(new IntSetting("Chunk radius", 1, 40, 8, ""));
	private final IntSetting minChance = add(new IntSetting("Min chance", 25, 81, 25, "%"));
	private final IntSetting maxAlerts = add(new IntSetting("Max alerts", 1, 20, 6, ""));
	private final IntSetting fill = add(new IntSetting("Fill opacity", 5, 60, 25, "%"));
	private final IntSetting autoWp = add(new IntSetting("Auto waypoint (0/1)", 0, 1, 0, ""));
	private final KeybindSetting reloadKey = add(new KeybindSetting("Reload chunks"));

	private final Map<Block, Integer> weights = SusChunkFinderModule.buildWeights();
	private final Set<Block> trialBlocks = SusChunkFinderModule.buildTrialBlocks();
	/** cx, cz, groundY, chance */
	private final Map<Long, int[]> found = new HashMap<>();
	private final Set<Long> seen = new HashSet<>();
	private volatile List<int[]> view = List.of();

	private int[][] offsets = null;
	private int offsetsRadius = -1;
	private int cursor = 0;
	private int cooldown = 0;
	private int alertCount = 0;
	private volatile String alertText = null;
	private volatile long alertUntil = 0L;

	public SusSoloFinderModule() {
		super("Sus Solo Finder", Category.VISUALS);
		instance = this;
	}

	static int colorFor(int chance) {
		if (chance >= 81) return WHITE;
		if (chance >= 75) return GREEN;
		if (chance >= 50) return YELLOW;
		return RED;
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		float fa = fill.get() / 100f;
		for (int[] v : view) {
			double x0 = v[0] * 16.0;
			double z0 = v[1] * 16.0;
			int gy = v[2];
			int argb = colorFor(v[3]);
			// placa no solo cobrindo a chunk
			esp.box(x0, gy, z0, x0 + 16, gy + 0.25, z0 + 16, argb, fa);
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
		reset();
	}

	private void reset() {
		lastScan.clear();
		found.clear();
		seen.clear();
		view = List.of();
		alertText = null;
		alertUntil = 0L;
		alertCount = 0;
		cursor = 0;
		cooldown = 0;
	}

	private void buildOffsets(int r) {
		List<int[]> list = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) list.add(new int[]{dx, dz});
		}
		list.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
		offsets = list.toArray(new int[0][]);
		offsetsRadius = r;
		cursor = 0;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();

		int rk = reloadKey.key();
		if (rk != 0) {
			boolean down = InputConstants.isKeyDown(mc.getWindow(), rk);
			if (down && !reloadKey.wasDown && mc.gui.screen() == null) reset();
			reloadKey.wasDown = down;
		} else {
			reloadKey.wasDown = false;
		}

		if (--cooldown > 0) return;
		cooldown = 1;

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

		long t0 = System.nanoTime();
		int scanned = 0;
		for (int steps = 0; steps < offsets.length && steps < 600; steps++) {
			int[] off = offsets[cursor];
			cursor = (cursor + 1) % offsets.length;
			int cx = pcx + off[0];
			int cz = pcz + off[1];
			if (!level.getChunkSource().hasChunk(cx, cz)) continue;
			long ck = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
			Long prev = lastScan.get(ck);
			long gt = level.getGameTime();
			if (prev != null && gt - prev < RESCAN_TICKS) continue;
			lastScan.put(ck, gt);
			scanChunk(level, cx, cz, player);
			scanned++;
			if (scanned >= MAX_SCANS_PER_TICK || System.nanoTime() - t0 > SCAN_BUDGET_NS) break;
		}

		if (lastScan.size() > 12000) lastScan.clear();
		found.values().removeIf(v -> Math.abs(v[0] - pcx) > r + 1 || Math.abs(v[1] - pcz) > r + 1);
		view = new ArrayList<>(found.values());
	}

	private void scanChunk(net.minecraft.client.multiplayer.ClientLevel level, int cx, int cz,
			net.minecraft.client.player.LocalPlayer player) {
		int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx * 16 + 8, cz * 16 + 8);
		int minY = Math.max(level.getMinY(), ground - BELOW);
		int maxY = Math.min(level.getMaxY(), ground + ABOVE);
		int score = 0;
		int markers = 0;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					pos.set(cx * 16 + x, y, cz * 16 + z);
					Block block = level.getBlockState(pos).getBlock();
					if (trialBlocks.contains(block)) markers++;
					Integer w = weights.get(block);
					if (w != null) score += w;
				}
			}
		}

		long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		// trial chamber: nao e base
		if (markers >= 6) {
			found.remove(key);
			seen.add(key);
			return;
		}

		int chance = (int) Math.round(score * 100.0 / (score + K));
		if (score > 0 && chance >= minChance.get()) {
			boolean isNew = !found.containsKey(key);
			found.put(key, new int[]{cx, cz, ground, Math.min(chance, 99)});
			if (isNew) {
				tryAlert(key, player, chance);
				if (autoWp.get() == 1) Waypoints.addAuto("Solo " + Math.min(chance, 99) + "%", cx * 16 + 8, ground, cz * 16 + 8);
			}
		} else {
			found.remove(key);
		}
	}

	private void tryAlert(long key, net.minecraft.client.player.LocalPlayer player, int chance) {
		if (seen.contains(key)) return;
		seen.add(key);
		if (alertCount >= maxAlerts.get()) return;
		alertCount++;
		alertText = "Solo suspeito: " + Math.min(chance, 99) + "%";
		alertUntil = System.currentTimeMillis() + ALERT_DISPLAY_MS;
		player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.8f, chance >= 75 ? 2.0f : 1.4f);
	}

	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled()) return;
		String text = instance.alertText;
		long until = instance.alertUntil;
		if (text == null || System.currentTimeMillis() > until) return;

		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int tw = FontUtil.width(text);
		int bw = tw + 26;
		int bh = FontUtil.height() + 10;
		int x = (sw - bw) / 2;
		int y = sh / 2 - 66;
		long left = until - System.currentTimeMillis();
		float a = left > 500 ? 1f : Math.max(0f, left / 500f);

		RenderUtil.panel(g, x, y, bw, bh, a);
		RenderUtil.roundRect(g, x + 8, y + bh / 2 - 1, 3, 3, 1, RenderUtil.alpha(0xFF22C55E, a));
		FontUtil.draw(g, text, x + 16, y + 6, RenderUtil.alpha(0xFFFFFFFF, a));
	}
}
