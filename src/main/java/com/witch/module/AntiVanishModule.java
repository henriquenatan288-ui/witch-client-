package com.witch.module;

import com.witch.gui.FontUtil;
import com.witch.gui.RenderUtil;
import com.witch.setting.IntSetting;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public class AntiVanishModule extends Module {
	private static AntiVanishModule instance;
	private static volatile Map<String, List<String>> ranks = new LinkedHashMap<>();
	private final IntSetting rank = this.add(new IntSetting("Rank (0 = so cargos)", 0, 30, 0, ""));
	private int cooldown = 0;

	public AntiVanishModule() {
		super("Anti Vanish", Category.UTILITY);
		instance = this;
	}

	@Override
	protected void onDisable() {
		ranks = new LinkedHashMap<>();
	}

	@Override
	public void onTick() {
		if (--this.cooldown > 0) return;
		this.cooldown = 10;
		Minecraft mc = Minecraft.getInstance();
		ClientPacketListener conn = mc.getConnection();
		if (conn == null) return;
		TreeMap<String, List<String>> tmp = new TreeMap<>();
		for (PlayerInfo info : conn.getOnlinePlayers()) {
			String name = info.getProfile().name();
			Component component = info.getTabListDisplayName();
			String full = component != null ? strip(component.getString()) : name;
			int idx = full.indexOf(name);
			String r = idx > 0 ? full.substring(0, idx).trim() : "Sem cargo";
			if (r.isEmpty()) r = "Sem cargo";
			tmp.computeIfAbsent(r, k -> new ArrayList<>()).add(name);
		}
		List<Map.Entry<String, List<String>>> sorted = new ArrayList<>(tmp.entrySet());
		sorted.sort((a, b) -> b.getValue().size() - a.getValue().size());
		LinkedHashMap<String, List<String>> out = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : sorted) {
			entry.getValue().sort(String.CASE_INSENSITIVE_ORDER);
			out.put(entry.getKey(), entry.getValue());
		}
		ranks = out;
	}

	private static String strip(String s) {
		return s.replaceAll("\u00a7.", "");
	}

	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled() || ranks.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		int sel = instance.rank.get();
		List<String> keys = new ArrayList<>(ranks.keySet());
		int lines = 1 + keys.size() + (sel >= 1 && sel <= keys.size() ? 1 + ranks.get(keys.get(sel - 1)).size() : 0);
		int lh = FontUtil.height() + 2;
		int w = 120;
		for (String k : keys) w = Math.max(w, FontUtil.width(k) + 40);
		if (sel >= 1 && sel <= keys.size()) {
			for (String n : ranks.get(keys.get(sel - 1))) w = Math.max(w, FontUtil.width(n) + 20);
		}
		int h = lines * lh + 10;
		int x = mc.getWindow().getGuiScaledWidth() - w - 6;
		int y = 6;
		RenderUtil.roundRect(g, x, y, w, h, 5, 0xB0101010);
		RenderUtil.roundRect(g, x, y, 3, h, 1, 0xFF8A2BE2);
		int ty = y + 5;
		FontUtil.draw(g, "Cargos online", x + 8, ty, 0xFFFFFFFF);
		ty += lh;
		for (int i = 0; i < keys.size(); ++i) {
			boolean on = i + 1 == sel;
			String line = (i + 1) + ". " + keys.get(i) + " (" + ranks.get(keys.get(i)).size() + ")";
			FontUtil.draw(g, line, x + 8, ty, on ? 0xFFFFFFFF : 0xFFB9B9B8);
			ty += lh;
		}
		if (sel >= 1 && sel <= keys.size()) {
			FontUtil.draw(g, "-- " + keys.get(sel - 1), x + 8, ty, 0xFF7C7C7A);
			ty += lh;
			for (String n : ranks.get(keys.get(sel - 1))) {
				FontUtil.draw(g, n, x + 14, ty, 0xFFFFFFFF);
				ty += lh;
			}
		}
	}
}
