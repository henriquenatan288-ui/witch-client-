package com.witch;

import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/** Edicao do client (beta / light / full), definida no build (-Pedition=...). Full e o padrao. */
public final class Edition {
	private Edition() {}

	private static final String NAME = load();

	/** Beta: so o essencial. */
	private static final Set<String> BETA = Set.of(
			"X-Ray Hologram", "Player ESP", "Storage ESP", "Farm ESP", "Tracers",
			"Fullbright", "Zoom", "FreeLook", "Freecam", "No Hurt Cam", "GUI Settings");

	/** Light: beta + modulos visuais. */
	private static final Set<String> LIGHT_EXTRA = Set.of(
			"Mood Filter", "Ambient Dust", "Witch Particles", "Particles", "Break Wave",
			"Slow Hit Visual", "Hide Name");

	private static String load() {
		try (InputStream in = Edition.class.getResourceAsStream("/witch_edition.properties")) {
			if (in != null) {
				Properties p = new Properties();
				p.load(in);
				String e = p.getProperty("edition", "full").trim().toLowerCase();
				if (e.equals("beta") || e.equals("light") || e.equals("full")) return e;
			}
		} catch (Exception ignored) {
		}
		return "full";
	}

	public static String name() {
		return NAME;
	}

	public static String label() {
		return NAME.toUpperCase();
	}

	/** Tela inicial, tela de carregamento, fonte global e texturas de itens: so Light e Full. */
	public static boolean visuals() {
		return !NAME.equals("beta");
	}

	public static boolean allows(String moduleName) {
		if (NAME.equals("full")) return true;
		if (BETA.contains(moduleName)) return true;
		return NAME.equals("light") && LIGHT_EXTRA.contains(moduleName);
	}
}
