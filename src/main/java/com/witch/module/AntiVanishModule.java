package com.witch.module;

import com.witch.gui.FontUtil;
import com.witch.gui.RenderUtil;
import com.witch.gui.Theme;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Lista cargos da tab. Os cargos que voce marca com "Alert this rank" disparam alerta
 * quando um player daquele cargo entra a < 20 blocos: overlay estilo guardian (fadiga),
 * mas com a cabeca da skin do player + nick pequeno de quem esta te observando.
 */
public class AntiVanishModule extends Module {
	private static final double ALERT_RANGE = 20.0;
	private static final long ALERT_HOLD_MS = 3200L;

	private static AntiVanishModule instance;
	private static volatile Map<String, List<String>> ranks = new LinkedHashMap<>();
	/** nome do player -> cargo (atualizado junto com ranks). */
	private static volatile Map<String, String> playerRank = new LinkedHashMap<>();

	/** Botao (toggle 0/1): ao ligar abre a lista de jogadores e volta a 0. */
	private final IntSetting pick = add(new IntSetting("Select players (0/1)", 0, 1, 0, ""));

	/** Nomes (minusculos) dos jogadores marcados como perigosos. */
	private static final Set<String> DANGEROUS = java.util.Collections.synchronizedSet(new HashSet<>());
	private int cooldown = 0;

	/** Alerta ativo: quem esta perto. */
	private volatile String alertName = null;
	private volatile AbstractClientPlayer alertPlayer = null;
	private volatile long alertUntil = 0L;
	private long alertStarted = 0L;

	public AntiVanishModule() {
		super("Anti Vanish", Category.UTILITY);
		instance = this;
	}

	@Override
	protected void onDisable() {
		ranks = new LinkedHashMap<>();
		playerRank = new LinkedHashMap<>();
		alertName = null;
		alertPlayer = null;
		alertUntil = 0L;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();

		if (pick.get() == 1) {
			pick.set(0);
			if (mc.player != null) mc.gui.setScreen(new com.witch.gui.AntiVanishScreen(mc.gui.screen()));
		}

		if (--cooldown <= 0) {
			cooldown = 10;
			refreshRanks(mc);
		}

		// players com cargo selecionado dentro de 20 blocos
		var level = mc.level;
		var me = mc.player;
		if (level == null || me == null || DANGEROUS.isEmpty()) {
			if (System.currentTimeMillis() > alertUntil) {
				alertName = null;
				alertPlayer = null;
			}
			return;
		}

		AbstractClientPlayer closest = null;
		double best = ALERT_RANGE * ALERT_RANGE;
		String closestName = null;
		for (Player p : level.players()) {
			if (p == me || p.isSpectator() || !(p instanceof AbstractClientPlayer acp)) continue;
			String name = p.getGameProfile().name();
			if (!DANGEROUS.contains(name.toLowerCase(java.util.Locale.ROOT))) continue;
			double dx = p.getX() - me.getX();
			double dy = p.getY() - me.getY();
			double dz = p.getZ() - me.getZ();
			double d2 = dx * dx + dy * dy + dz * dz;
			if (d2 <= best) {
				best = d2;
				closest = acp;
				closestName = name;
			}
		}

		if (closest != null) {
			long now = System.currentTimeMillis();
			if (alertName == null || !alertName.equals(closestName)) {
				alertStarted = now;
			}
			alertName = closestName;
			alertPlayer = closest;
			alertUntil = now + ALERT_HOLD_MS;
		} else if (System.currentTimeMillis() > alertUntil) {
			alertName = null;
			alertPlayer = null;
		}
	}

	private void refreshRanks(Minecraft mc) {
		var conn = mc.getConnection();
		if (conn == null) return;
		Map<String, List<String>> tmp = new TreeMap<>();
		Map<String, String> pr = new LinkedHashMap<>();
		for (var info : conn.getOnlinePlayers()) {
			String name = info.getProfile().name();
			var disp = info.getTabListDisplayName();
			String full = disp != null ? strip(disp.getString()) : name;
			int idx = full.indexOf(name);
			String r = idx > 0 ? full.substring(0, idx).trim() : "Sem cargo";
			if (r.isEmpty()) r = "Sem cargo";
			tmp.computeIfAbsent(r, k -> new ArrayList<>()).add(name);
			pr.put(name, r);
		}
		List<Map.Entry<String, List<String>>> sorted = new ArrayList<>(tmp.entrySet());
		sorted.sort((a, b) -> b.getValue().size() - a.getValue().size());
		Map<String, List<String>> out = new LinkedHashMap<>();
		for (var e : sorted) {
			e.getValue().sort(String.CASE_INSENSITIVE_ORDER);
			out.put(e.getKey(), e.getValue());
		}
		ranks = out;
		playerRank = pr;
	}

	public static boolean isDangerous(String name) {
		return DANGEROUS.contains(name.toLowerCase(java.util.Locale.ROOT));
	}

	public static void toggleDangerous(String name) {
		String k = name.toLowerCase(java.util.Locale.ROOT);
		if (!DANGEROUS.remove(k)) DANGEROUS.add(k);
		com.witch.config.Config.save();
	}

	public static List<String> dangerousNames() {
		synchronized (DANGEROUS) { return new ArrayList<>(DANGEROUS); }
	}

	public static void setDangerous(java.util.Collection<String> names) {
		DANGEROUS.clear();
		for (String n : names) DANGEROUS.add(n.toLowerCase(java.util.Locale.ROOT));
	}

	/** Cargo (prefixo da tab) de um jogador online; "Sem cargo" se nao tiver. Funciona mesmo com o modulo parado. */
	public static String rankOf(net.minecraft.client.multiplayer.PlayerInfo info) {
		String name = info.getProfile().name();
		var disp = info.getTabListDisplayName();
		String full = disp != null ? strip(disp.getString()) : name;
		int idx = full.indexOf(name);
		String r = idx > 0 ? full.substring(0, idx).trim() : "Sem cargo";
		return r.isEmpty() ? "Sem cargo" : r;
	}

	private static String strip(String s) {
		return s.replaceAll("§.", "");
	}

	/** Painel de cargos + overlay estilo guardian. */
	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled()) return;
		drawRankPanel(g);
		drawGuardianAlert(g);
	}

	private static void drawRankPanel(GuiGraphicsExtractor g) {
		if (ranks.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		int focus = 0;
		List<String> keys = new ArrayList<>(ranks.keySet());
		int lh = FontUtil.height() + 2;
		int lines = 1 + keys.size();
		if (focus >= 1 && focus <= keys.size()) {
			lines += 1 + ranks.get(keys.get(focus - 1)).size();
		}
		int w = 130;
		for (String k : keys) w = Math.max(w, FontUtil.width(k) + 50);
		if (focus >= 1 && focus <= keys.size()) {
			for (String n : ranks.get(keys.get(focus - 1))) w = Math.max(w, FontUtil.width(n) + 24);
		}
		int h = lines * lh + 10;
		int x = mc.getWindow().getGuiScaledWidth() - w - 6;
		int y = 6;
		RenderUtil.roundRect(g, x, y, w, h, 5, 0xB0201830);
		RenderUtil.roundRect(g, x, y, 3, h, 1, Theme.ACCENT);
		int ty = y + 5;
		FontUtil.draw(g, "Cargos online", x + 8, ty, Theme.TEXT);
		ty += lh;
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			boolean focused = (i + 1) == focus;
			boolean alert = false;
			for (String n : ranks.get(key)) if (DANGEROUS.contains(n.toLowerCase(java.util.Locale.ROOT))) alert = true;
			String mark = alert ? "[!]	" : "    ";
			String line = mark + (i + 1) + ". " + key + " (" + ranks.get(key).size() + ")";
			int col = alert ? 0xFFFF6B6B : (focused ? 0xFFFFFFFF : 0xFFB9A8D8);
			FontUtil.draw(g, line, x + 8, ty, col);
			ty += lh;
		}
		if (focus >= 1 && focus <= keys.size()) {
			FontUtil.draw(g, "-- " + keys.get(focus - 1), x + 8, ty, 0xFF7CFC5A);
			ty += lh;
			for (String n : ranks.get(keys.get(focus - 1))) {
				FontUtil.draw(g, n, x + 14, ty, 0xFFFFFFFF);
				ty += lh;
			}
		}
	}

	/**
	 * Overlay estilo elder guardian / mining fatigue:
	 * cabeca grande no centro com fade + nick pequeno de quem observa.
	 */
	private static void drawGuardianAlert(GuiGraphicsExtractor g) {
		String name = instance.alertName;
		AbstractClientPlayer player = instance.alertPlayer;
		long until = instance.alertUntil;
		long now = System.currentTimeMillis();
		if (name == null || now > until) return;

		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();

		// alpha: sobe rapido, segura, desce no final (parecido com o efeito do guardian)
		long age = now - instance.alertStarted;
		long left = until - now;
		float a;
		if (age < 400) a = age / 400f;
		else if (left < 600) a = left / 600f;
		else a = 1f;
		a = Mth.clamp(a, 0f, 1f);
		// pulso leve
		float pulse = 0.85f + 0.15f * (float) Math.sin(now / 180.0);
		float alpha = a * pulse;

		// vinheta escura suave (estilo fadiga)
		int dark = RenderUtil.alpha(0xFF000000, 0.35f * alpha);
		g.fill(0, 0, sw, sh, dark);

		// tamanho grande da cabeca (estilo guardian)
		int size = Math.min(sw, sh) / 3;
		int hx = (sw - size) / 2;
		int hy = (sh - size) / 2 - 12;

		int faceColor = RenderUtil.alpha(0xFFFFFFFF, alpha);
		if (player != null) {
			try {
				var skin = player.getSkin();
				PlayerFaceExtractor.extractRenderState(g, skin, hx, hy, size, faceColor);
			} catch (Throwable t) {
				// fallback: tenta pela textura do body
				try {
					var tex = player.getSkin().body().texturePath();
					PlayerFaceExtractor.extractRenderState(g, tex, hx, hy, size, true, false, faceColor);
				} catch (Throwable ignored) {
					RenderUtil.roundRect(g, hx, hy, size, size, 8, RenderUtil.alpha(0xFF4A2060, alpha));
				}
			}
		} else {
			RenderUtil.roundRect(g, hx, hy, size, size, 8, RenderUtil.alpha(0xFF4A2060, alpha));
		}

		// nick pequeno embaixo da cabeca
		String label = name;
		int tw = FontUtil.width(label);
		int tx = (sw - tw) / 2;
		int ty = hy + size + 6;
		FontUtil.draw(g, label, tx + 1, ty + 1, RenderUtil.alpha(0xFF000000, alpha * 0.7f));
		FontUtil.draw(g, label, tx, ty, RenderUtil.alpha(0xFFFFE8A0, alpha));
	}
}
