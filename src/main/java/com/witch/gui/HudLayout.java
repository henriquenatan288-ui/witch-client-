package com.witch.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Posicao dos elementos do HUD (deslocamento em relacao ao padrao), editavel em {@link HudEditScreen}. Salvo em config/witch-hud.txt. */
public final class HudLayout {
	private HudLayout() {}

	public static final String[] IDS = {"modules", "coords", "armor", "waypoints"};
	public static final String[] LABELS = {"Logo + modulos", "Coordenadas", "Armadura", "Waypoints"};

	private static final Map<String, int[]> OFF = new HashMap<>();
	/** x, y, w, h, dx e dy no momento do desenho. */
	private static final Map<String, int[]> RECT = new HashMap<>();
	private static boolean loaded = false;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("witch-hud.txt");
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		try {
			Path f = file();
			if (!Files.exists(f)) return;
			for (String line : Files.readAllLines(f)) {
				String[] p = line.split("\\|");
				if (p.length < 3) continue;
				OFF.put(p[0], new int[] {Integer.parseInt(p[1]), Integer.parseInt(p[2])});
			}
		} catch (Exception ignored) {
		}
	}

	public static void save() {
		try {
			StringBuilder sb = new StringBuilder();
			for (String id : IDS) {
				int[] o = OFF.get(id);
				if (o != null && (o[0] != 0 || o[1] != 0)) sb.append(id).append('|').append(o[0]).append('|').append(o[1]).append('\n');
			}
			Files.writeString(file(), sb.toString());
		} catch (Exception ignored) {
		}
	}

	public static int dx(String id) {
		load();
		int[] o = OFF.get(id);
		return o == null ? 0 : o[0];
	}

	public static int dy(String id) {
		load();
		int[] o = OFF.get(id);
		return o == null ? 0 : o[1];
	}

	public static void set(String id, int dx, int dy) {
		load();
		OFF.put(id, new int[] {dx, dy});
	}

	public static void reset() {
		load();
		OFF.clear();
		RECT.clear();
		save();
	}

	/** Chamado por quem desenha o elemento: guarda onde ele ficou (ja com o deslocamento). */
	public static void report(String id, int x, int y, int w, int h) {
		RECT.put(id, new int[] {x, y, w, h, dx(id), dy(id)});
	}

	/** Retangulo atual {x, y, w, h}: o ultimo desenhado (acompanhando o deslocamento) ou o padrao. */
	public static int[] rect(String id) {
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int[] r = RECT.get(id);
		if (r != null) return new int[] {r[0] + dx(id) - r[4], r[1] + dy(id) - r[5], r[2], r[3]};
		int lh = FontUtil.height() + 3;
		int x, y, w, h;
		switch (id) {
			case "coords" -> { w = 100; h = lh * 2 + 2; x = 5; y = sh - 4 - lh * 2 - 2; }
			case "armor" -> { w = 80; h = (lh + 1) * 4; x = sw - 85; y = sh / 2 - 20; }
			case "waypoints" -> { w = 95; h = (lh + 1) * 3; x = sw - 100; y = sh / 2 - 26 - h; }
			default -> { w = 110; h = 50; x = 5; y = 5; }
		}
		return new int[] {x + dx(id), y + dy(id), w, h};
	}
}
