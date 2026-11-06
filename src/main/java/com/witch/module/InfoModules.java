package com.witch.module;

import com.witch.gui.FontUtil;
import com.witch.gui.HudLayout;
import com.witch.gui.RenderUtil;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Modulos de informacao: linhas desenhadas num unico painel arrastavel (id "info"). */
public final class InfoModules {
	private InfoModules() {}

	private static final Map<String, String> DESC = new HashMap<>();

	public static String desc(String name) {
		return DESC.getOrDefault(name, "");
	}

	/** Base: cada modulo contribui com linhas {rotulo, valor}. */
	public abstract static class Line extends Module {
		protected Line(String name, Category cat, String desc) {
			super(name, cat);
			DESC.put(name, desc);
		}

		protected abstract void collect(Minecraft mc, LocalPlayer p, List<String[]> out);
	}

	/** Painel unico com as linhas de todos os modulos de informacao ligados. */
	public static void drawAll(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) return;
		List<String[]> rows = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			if (m instanceof Line l && l.isEnabled()) l.collect(mc, p, rows);
		}
		if (rows.isEmpty()) return;
		int lh = FontUtil.height() + 3;
		int lw = 0, vw = 0;
		for (String[] r : rows) {
			lw = Math.max(lw, FontUtil.width(r[0]));
			vw = Math.max(vw, FontUtil.width(r[1]));
		}
		int bw = lw + vw + 26;
		int bh = rows.size() * lh + 5;
		int x = 5 + HudLayout.dx("info");
		int y = 40 + HudLayout.dy("info");
		RenderUtil.panel(g, x, y, bw, bh, 1f);
		int ty = y + 4;
		for (String[] r : rows) {
			FontUtil.draw(g, r[0], x + 7, ty, 0xFF8A8A8A);
			FontUtil.draw(g, r[1], x + 7 + lw + 12, ty, 0xFFFFFFFF);
			ty += lh;
		}
		HudLayout.report("info", x, y, bw, bh);
	}

	private static String[] row(String a, String b) {
		return new String[] {a, b};
	}

	// ---------- eventos de combate (combo / alcance / hit marker) ----------
	static long lastHit = 0L;
	static int combo = 0;
	static double lastReach = 0;
	private static boolean hooked = false;

	static void hook() {
		if (hooked) return;
		hooked = true;
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			Minecraft mc = Minecraft.getInstance();
			if (level.isClientSide() && player == mc.player && entity instanceof LivingEntity) {
				long now = System.currentTimeMillis();
				combo = now - lastHit < 2000 ? combo + 1 : 1;
				lastHit = now;
				lastReach = player.distanceTo(entity);
			}
			return InteractionResult.PASS;
		});
	}

	// ---------- HUD ----------
	public static class Fps extends Line {
		public Fps() { super("FPS HUD", Category.HUD, "Mostra os FPS."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) { o.add(row("FPS", String.valueOf(mc.getFps()))); }
	}

	public static class Ping extends Line {
		public Ping() { super("Ping HUD", Category.HUD, "Mostra o ping do servidor."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(p.getUUID());
			o.add(row("Ping", info == null || mc.getCurrentServer() == null ? "-" : info.getLatency() + " ms"));
		}
	}

	public static class Cps extends Line {
		private final ArrayDeque<Long> clicks = new ArrayDeque<>();
		private boolean was = false;
		public Cps() { super("CPS HUD", Category.HUD, "Cliques por segundo (botao de ataque)."); }
		@Override public void onTick() {
			boolean d = Minecraft.getInstance().options.keyAttack.isDown();
			long now = System.currentTimeMillis();
			if (d && !was) clicks.addLast(now);
			was = d;
			while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();
		}
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) { o.add(row("CPS", String.valueOf(clicks.size()))); }
	}

	public static class Clock extends Line {
		private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss");
		public Clock() { super("Clock HUD", Category.HUD, "Hora real do seu computador."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) { o.add(row("Hora", LocalTime.now().format(F))); }
	}

	public static class Speed extends Line {
		public Speed() { super("Speed HUD", Category.HUD, "Velocidade em blocos por segundo."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			var v = p.getDeltaMovement();
			o.add(row("Vel", String.format("%.1f b/s", Math.sqrt(v.x * v.x + v.z * v.z) * 20.0)));
		}
	}

	public static class Direction extends Line {
		private static final String[] DIR = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
		public Direction() { super("Direction HUD", Category.HUD, "Direcao em que voce olha."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			o.add(row("Dir", DIR[Math.floorMod(Math.round(p.getYRot() / 45f), 8)]));
		}
	}

	public static class Day extends Line {
		public Day() { super("Day Counter", Category.HUD, "Quantos dias o mundo ja tem."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) { o.add(row("Dia", String.valueOf(mc.level.getGameTime() / 24000L + 1))); }
	}

	public static class Memory extends Line {
		public Memory() { super("Memory HUD", Category.HUD, "Memoria usada pelo jogo."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			Runtime r = Runtime.getRuntime();
			o.add(row("RAM", ((r.totalMemory() - r.freeMemory()) >> 20) + "/" + (r.maxMemory() >> 20) + " MB"));
		}
	}

	public static class Potions extends Line {
		public Potions() { super("Potion HUD", Category.HUD, "Efeitos ativos e o tempo que falta."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			for (MobEffectInstance e : p.getActiveEffects()) {
				int s = e.getDuration() / 20;
				o.add(row(e.getEffect().value().getDisplayName().getString(), (s / 60) + ":" + String.format("%02d", s % 60)));
			}
		}
	}

	public static class NetherCoords extends Line {
		public NetherCoords() { super("Nether Coords", Category.HUD, "Coordenadas convertidas pro Nether (x/8)."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			o.add(row("Nether", Math.floorDiv(p.getBlockX(), 8) + " " + p.getBlockY() + " " + Math.floorDiv(p.getBlockZ(), 8)));
		}
	}

	public static class ChunkCoords extends Line {
		public ChunkCoords() { super("Chunk Coords", Category.HUD, "Chunk em que voce esta."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			o.add(row("Chunk", (p.getBlockX() >> 4) + " " + (p.getBlockZ() >> 4)));
		}
	}

	public static class PlayerCount extends Line {
		public PlayerCount() { super("Player Count", Category.HUD, "Jogadores online no servidor."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			o.add(row("Online", mc.getConnection() == null ? "-" : String.valueOf(mc.getConnection().getOnlinePlayers().size())));
		}
	}

	public static class HeldItem extends Line {
		public HeldItem() { super("Held Item HUD", Category.HUD, "Item na mao e a durabilidade."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			ItemStack s = p.getMainHandItem();
			if (s.isEmpty()) return;
			String v = s.isDamageableItem() ? (s.getMaxDamage() - s.getDamageValue()) + "/" + s.getMaxDamage() : "x" + s.getCount();
			o.add(row(s.getHoverName().getString(), v));
		}
	}

	public static class ServerIp extends Line {
		public ServerIp() { super("Server IP HUD", Category.HUD, "Endereco do servidor atual."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			var sd = mc.getCurrentServer();
			o.add(row("Server", sd == null ? "Singleplayer" : sd.ip));
		}
	}

	public static class SprintStatus extends Line {
		public SprintStatus() { super("Sprint Status", Category.HUD, "Mostra se voce anda, corre ou esta agachado."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			o.add(row("Modo", p.isShiftKeyDown() ? "Sneak" : p.isSprinting() ? "Sprint" : "Walk"));
		}
	}

	public static class Health extends Line {
		public Health() { super("Health HUD", Category.HUD, "Vida e fome em numeros."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			o.add(row("Vida", String.format("%.1f", p.getHealth())));
			o.add(row("Fome", String.valueOf(p.getFoodData().getFoodLevel())));
		}
	}

	// ---------- COMBAT ----------
	public static class ComboCounter extends Line {
		public ComboCounter() { super("Combo Counter", Category.COMBAT, "Conta golpes seguidos (reseta apos 2s)."); hook(); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			if (System.currentTimeMillis() - lastHit < 2000 && combo > 0) o.add(row("Combo", String.valueOf(combo)));
		}
	}

	public static class ReachDisplay extends Line {
		public ReachDisplay() { super("Reach Display", Category.COMBAT, "Distancia do ultimo golpe que voce deu."); hook(); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			if (System.currentTimeMillis() - lastHit < 5000) o.add(row("Reach", String.format("%.2f", lastReach)));
		}
	}

	public static class TargetHud extends Line {
		public TargetHud() { super("Target HUD", Category.COMBAT, "Nome e vida de quem esta na mira."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			if (mc.crosshairPickEntity instanceof LivingEntity le) {
				o.add(row(le.getName().getString(), String.format("%.1f/%.0f", le.getHealth(), le.getMaxHealth())));
			}
		}
	}

	public static class HitMarker extends Module {
		private static HitMarker instance;
		public HitMarker() { super("Hit Marker", Category.COMBAT); instance = this; DESC.put(name, "Um X rapido na mira quando voce acerta."); hook(); }
		public static void drawHud(GuiGraphicsExtractor g) {
			if (instance == null || !instance.isEnabled()) return;
			long dt = System.currentTimeMillis() - lastHit;
			if (dt > 220) return;
			Minecraft mc = Minecraft.getInstance();
			int cx = mc.getWindow().getGuiScaledWidth() / 2, cy = mc.getWindow().getGuiScaledHeight() / 2;
			int c = RenderUtil.alpha(0xFFFFFFFF, 1f - dt / 220f);
			for (int i = 3; i <= 6; i++) {
				g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, c);
				g.fill(cx - i - 1, cy + i, cx - i, cy + i + 1, c);
				g.fill(cx + i, cy - i - 1, cx + i + 1, cy - i, c);
				g.fill(cx - i - 1, cy - i - 1, cx - i, cy - i, c);
			}
		}
	}

	public static class LowHealthAlert extends Module {
		private static LowHealthAlert instance;
		public LowHealthAlert() { super("Low Health Alert", Category.COMBAT); instance = this; DESC.put(name, "Borda vermelha pulsando quando a vida esta baixa."); }
		public static void drawHud(GuiGraphicsExtractor g) {
			if (instance == null || !instance.isEnabled()) return;
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer p = mc.player;
			if (p == null || p.getHealth() > 6f || p.isDeadOrDying()) return;
			int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
			float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 220.0);
			int depth = Math.min(w, h) / 5;
			for (int l = 0; l < 8; l++) {
				float k = 1f - l / 8f;
				int c = RenderUtil.alpha(0xFFB00018, 0.34f * k * k * pulse);
				int a = depth * l / 8, b = depth * (l + 1) / 8;
				g.fill(a, a, w - a, b, c);
				g.fill(a, h - b, w - a, h - a, c);
				g.fill(a, b, b, h - b, c);
				g.fill(w - b, b, w - a, h - b, c);
			}
		}
	}

	// ---------- WORLD ----------
	public static class NearbyPlayers extends Line {
		public NearbyPlayers() { super("Nearby Players", Category.WORLD, "Lista jogadores perto e a distancia."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			int n = 0;
			for (Entity e : mc.level.entitiesForRendering()) {
				if (e instanceof Player pl && pl != p && n < 5) {
					o.add(row(pl.getName().getString(), String.format("%.0f m", p.distanceTo(pl))));
					n++;
				}
			}
		}
	}

	public static class MobCounter extends Line {
		public MobCounter() { super("Mob Counter", Category.WORLD, "Quantos mobs tem em 32 blocos."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			int n = 0;
			for (Entity e : mc.level.entitiesForRendering()) {
				if (e instanceof LivingEntity && !(e instanceof Player) && p.distanceToSqr(e) < 1024.0) n++;
			}
			o.add(row("Mobs", String.valueOf(n)));
		}
	}

	public static class WeatherHud extends Line {
		public WeatherHud() { super("Weather HUD", Category.WORLD, "Clima atual do mundo."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			o.add(row("Clima", mc.level.isThundering() ? "Tempestade" : mc.level.isRaining() ? "Chuva" : "Limpo"));
		}
	}

	// ---------- UTILITY ----------
	public static class InventoryCounter extends Line {
		public InventoryCounter() { super("Inventory Counter", Category.UTILITY, "Slots livres no inventario."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			int free = 0, total = 0;
			for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
				total++;
				if (s.isEmpty()) free++;
			}
			o.add(row("Slots", free + "/" + total));
		}
	}

	public static class ArmorWarning extends Line {
		public ArmorWarning() { super("Armor Warning", Category.UTILITY, "Avisa quando uma peca da armadura esta quase quebrando."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			for (EquipmentSlot sl : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				ItemStack s = p.getItemBySlot(sl);
				if (s.isDamageableItem() && s.getMaxDamage() > 0 && (s.getMaxDamage() - s.getDamageValue()) * 10 < s.getMaxDamage()) {
					o.add(row("Armadura", s.getHoverName().getString() + " baixa!"));
				}
			}
		}
	}

	public static class ItemCount extends Line {
		public ItemCount() { super("Item Count", Category.UTILITY, "Quantos do item da mao voce tem no total."); }
		protected void collect(Minecraft mc, LocalPlayer p, List<String[]> o) {
			ItemStack h = p.getMainHandItem();
			if (h.isEmpty()) return;
			int n = 0;
			for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (ItemStack.isSameItem(s, h)) n += s.getCount();
			o.add(row("Total", String.valueOf(n)));
		}
	}

	// ---------- MOVEMENT ----------
	public static class AutoWalk extends Module {
		public AutoWalk() { super("Auto Walk", Category.MOVEMENT); DESC.put(name, "Anda pra frente sozinho."); }
		@Override public void onTick() {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.gui.screen() == null) mc.options.keyUp.setDown(true);
		}
		@Override protected void onDisable() { Minecraft.getInstance().options.keyUp.setDown(false); }
	}

	public static class ToggleSneak extends Module {
		public ToggleSneak() { super("Toggle Sneak", Category.MOVEMENT); DESC.put(name, "Fica agachado sem precisar segurar a tecla."); }
		@Override public void onTick() {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.gui.screen() == null) mc.options.keyShift.setDown(true);
		}
		@Override protected void onDisable() { Minecraft.getInstance().options.keyShift.setDown(false); }
	}

	// ---------- teclas ----------
	public static class Keystrokes extends Module {
		private static Keystrokes instance;
		public Keystrokes() { super("Keystrokes", Category.HUD); instance = this; DESC.put(name, "Mostra W A S D e os botoes do mouse na tela."); }
		public static void drawHud(GuiGraphicsExtractor g) {
			if (instance == null || !instance.isEnabled()) return;
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) return;
			int cell = 17, gap = 2;
			int w = cell * 3 + gap * 2, h = cell * 3 + gap * 2;
			int x0 = mc.getWindow().getGuiScaledWidth() - w - 6 + HudLayout.dx("keys");
			int y0 = mc.getWindow().getGuiScaledHeight() / 2 + 20 + HudLayout.dy("keys");
			key(g, x0 + cell + gap, y0, cell, cell, "W", mc.options.keyUp.isDown());
			key(g, x0, y0 + cell + gap, cell, cell, "A", mc.options.keyLeft.isDown());
			key(g, x0 + cell + gap, y0 + cell + gap, cell, cell, "S", mc.options.keyDown.isDown());
			key(g, x0 + 2 * (cell + gap), y0 + cell + gap, cell, cell, "D", mc.options.keyRight.isDown());
			int hw = (w - gap) / 2;
			key(g, x0, y0 + 2 * (cell + gap), hw, cell, "LMB", mc.options.keyAttack.isDown());
			key(g, x0 + hw + gap, y0 + 2 * (cell + gap), w - hw - gap, cell, "RMB", mc.options.keyUse.isDown());
			HudLayout.report("keys", x0, y0, w, h);
		}

		private static void key(GuiGraphicsExtractor g, int x, int y, int w, int h, String t, boolean down) {
			RenderUtil.roundRect(g, x, y, w, h, 4, RenderUtil.alpha(0xFFFFFFFF, down ? 0.55f : 0.14f));
			RenderUtil.roundRect(g, x + 1, y + 1, w - 2, h - 2, 3, RenderUtil.alpha(down ? 0xFFE0E0E0 : 0xFF0A0A0A, down ? 0.85f : 0.5f));
			FontUtil.draw(g, t, x + (w - FontUtil.width(t)) / 2, y + (h - FontUtil.height()) / 2, down ? 0xFF111111 : 0xFFFFFFFF);
		}
	}
}
