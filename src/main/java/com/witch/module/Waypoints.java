package com.witch.module;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Lista de waypoints salva em config/witch-waypoints.txt (nome|x|y|z|dimensao). */
public final class Waypoints {
	private Waypoints() {}

	public record Wp(String name, int x, int y, int z, String dim) {}

	private static final List<Wp> LIST = new ArrayList<>();
	private static boolean loaded = false;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("witch-waypoints.txt");
	}

	private static synchronized void load() {
		if (loaded) return;
		loaded = true;
		try {
			Path f = file();
			if (!Files.exists(f)) return;
			for (String line : Files.readAllLines(f)) {
				String[] p = line.split("\\|");
				if (p.length < 5) continue;
				LIST.add(new Wp(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4]));
			}
		} catch (Exception ignored) {
		}
	}

	private static void save() {
		try {
			StringBuilder sb = new StringBuilder();
			for (Wp w : LIST) {
				sb.append(w.name.replace('|', ' ')).append('|').append(w.x).append('|').append(w.y).append('|')
						.append(w.z).append('|').append(w.dim).append('\n');
			}
			Files.writeString(file(), sb.toString());
		} catch (Exception ignored) {
		}
	}

	public static String dim() {
		var level = Minecraft.getInstance().level;
		return level == null ? "" : level.dimension().toString();
	}

	/** Copia da lista da dimensao atual. */
	public static synchronized List<Wp> here() {
		load();
		String d = dim();
		List<Wp> out = new ArrayList<>();
		for (Wp w : LIST) if (w.dim.equals(d)) out.add(w);
		return out;
	}

	public static synchronized String add(String name, int x, int y, int z) {
		load();
		LIST.add(new Wp(name, x, y, z, dim()));
		save();
		return name;
	}

	/** Adiciona so se nao houver outro a menos de 24 blocos (evita repetir a mesma base). */
	public static synchronized boolean addAuto(String name, int x, int y, int z) {
		load();
		String d = dim();
		for (Wp w : LIST) {
			if (w.dim.equals(d) && Math.abs(w.x - x) < 24 && Math.abs(w.z - z) < 24) return false;
		}
		LIST.add(new Wp(name, x, y, z, d));
		save();
		return true;
	}

	public static synchronized int count() {
		load();
		return LIST.size();
	}

	/** Remove o mais proximo (ate 300 blocos). Devolve o nome ou null. */
	public static synchronized String removeNearest(double px, double pz) {
		load();
		String d = dim();
		Wp best = null;
		double bd = 300.0 * 300.0;
		for (Wp w : LIST) {
			if (!w.dim.equals(d)) continue;
			double dd = (w.x - px) * (w.x - px) + (w.z - pz) * (w.z - pz);
			if (dd < bd) {
				bd = dd;
				best = w;
			}
		}
		if (best == null) return null;
		LIST.remove(best);
		save();
		return best.name;
	}
}
