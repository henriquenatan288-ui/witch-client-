package com.witch.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.witch.module.Module;
import com.witch.module.ModuleManager;
import com.witch.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private Config() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("witch.json");
	}

	public static void save() {
		JsonObject root = new JsonObject();
		root.addProperty("_font", com.witch.gui.FontUtil.isCustom());
		com.google.gson.JsonArray dang = new com.google.gson.JsonArray();
		for (String n : com.witch.module.AntiVanishModule.dangerousNames()) dang.add(n);
		root.add("_dangerous", dang);
		for (Module m : ModuleManager.all()) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", m.isEnabled());
			JsonObject s = new JsonObject();
			for (Setting st : m.settings()) {
				s.addProperty(st.name, st.toNumber());
			}
			o.add("settings", s);
			root.add(m.name, o);
		}
		// escreve num arquivo temporario e troca no fim: um crash no meio nunca deixa o witch.json pela metade
		Path target = file();
		Path tmp = target.resolveSibling("witch.json.tmp");
		try {
			try (Writer w = Files.newBufferedWriter(tmp)) {
				GSON.toJson(root, w);
			}
			try {
				Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void load() {
		Path f = file();
		if (!Files.exists(f)) return;
		try (Reader r = Files.newBufferedReader(f)) {
			JsonElement parsed = JsonParser.parseReader(r);
			if (parsed == null || !parsed.isJsonObject()) return;
			if (parsed.getAsJsonObject().has("_font")) {
				com.witch.gui.FontUtil.setCustom(parsed.getAsJsonObject().get("_font").getAsBoolean());
			}
			if (parsed.getAsJsonObject().has("_dangerous") && parsed.getAsJsonObject().get("_dangerous").isJsonArray()) {
				java.util.List<String> l = new java.util.ArrayList<>();
				for (JsonElement d : parsed.getAsJsonObject().getAsJsonArray("_dangerous")) l.add(d.getAsString());
				com.witch.module.AntiVanishModule.setDangerous(l);
			}
			for (Map.Entry<String, JsonElement> e : parsed.getAsJsonObject().entrySet()) {
				if (e.getKey().startsWith("_")) continue;
				Module m = ModuleManager.byName(e.getKey());
				if (m == null) continue;
				try {
					JsonElement el = e.getValue();
					if (el.isJsonPrimitive()) {
						// formato antigo: so true/false
						m.setEnabled(el.getAsBoolean());
					} else if (el.isJsonObject()) {
						JsonObject o = el.getAsJsonObject();
						if (o.has("settings") && o.get("settings").isJsonObject()) {
							JsonObject s = o.getAsJsonObject("settings");
							for (Setting st : m.settings()) {
								try {
									if (s.has(st.name)) st.fromNumber(s.get(st.name).getAsDouble());
								} catch (Exception bad) {
									System.err.println("[Witch] ajuste invalido ignorado: " + m.name + "/" + st.name);
								}
							}
						}
						if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
					}
				} catch (Throwable bad) {
					System.err.println("[Witch] config de '" + m.name + "' ignorada: " + bad);
				}
				m.toggleAnim = m.isEnabled() ? 1f : 0f;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}