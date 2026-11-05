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
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Marcadores no mundo: tecla "Add waypoint" salva a posicao; coluna visivel de longe e lista com distancia. */
public class WaypointsModule extends Module implements EspSource {
	private static WaypointsModule instance;

	private final KeybindSetting addKey = add(new KeybindSetting("Add waypoint"));
	private final KeybindSetting removeKey = add(new KeybindSetting("Remove nearest"));
	private final ColorSetting color = add(new ColorSetting("Color", 1));
	private final IntSetting listed = add(new IntSetting("Listed on HUD", 0, 6, 3, ""));

	private volatile List<Waypoints.Wp> view = List.of();
	private int cooldown = 0;

	public WaypointsModule() {
		super("Waypoints", Category.VISUALS);
		instance = this;
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		int c = color.get();
		for (Waypoints.Wp w : view) {
			esp.box(w.x(), w.y(), w.z(), w.x() + 1, w.y() + 1, w.z() + 1, c, 0.3f);
			esp.box(w.x() + 0.4, w.y() + 1, w.z() + 0.4, w.x() + 0.6, w.y() + 90, w.z() + 0.6, c, 0.12f);
		}
	}

	private boolean pressed(KeybindSetting k, Minecraft mc) {
		int key = k.key();
		if (key == 0) {
			k.wasDown = false;
			return false;
		}
		boolean down = InputConstants.isKeyDown(mc.getWindow(), key);
		boolean edge = down && !k.wasDown && mc.gui.screen() == null;
		k.wasDown = down;
		return edge;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || mc.level == null) {
			view = List.of();
			return;
		}
		if (pressed(addKey, mc)) {
			String n = "WP " + (Waypoints.count() + 1);
			Waypoints.add(n, p.getBlockX(), p.getBlockY(), p.getBlockZ());
			p.sendOverlayMessage(Component.literal("Waypoint salvo: " + n));
		}
		if (pressed(removeKey, mc)) {
			String n = Waypoints.removeNearest(p.getX(), p.getZ());
			p.sendOverlayMessage(Component.literal(n == null ? "Nenhum waypoint por perto" : "Removido: " + n));
		}
		if (--cooldown > 0) return;
		cooldown = 10;
		view = Waypoints.here();
	}

	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled()) return;
		var p = Minecraft.getInstance().player;
		int n = instance.listed.get();
		if (p == null || n == 0) return;
		List<Waypoints.Wp> list = new ArrayList<>(instance.view);
		if (list.isEmpty()) return;
		list.sort((a, b) -> Double.compare(d2(a, p.getX(), p.getZ()), d2(b, p.getX(), p.getZ())));
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int lh = FontUtil.height() + 3;
		int shown = Math.min(n, list.size());
		// canto direito, logo acima do Armor HUD (a esquerda fica a lista de modulos)
		int y = sh / 2 - 26 - shown * (lh + 1) + com.witch.gui.HudLayout.dy("waypoints");
		int ox = com.witch.gui.HudLayout.dx("waypoints");
		int top = y;
		int minX = Integer.MAX_VALUE;
		int maxW = 0;
		for (int i = 0; i < shown; i++) {
			var w = list.get(i);
			String t = w.name() + "  " + Math.round(Math.sqrt(d2(w, p.getX(), p.getZ()))) + "m";
			int bw = FontUtil.width(t) + 12;
			int x = sw - 5 - bw - 6 + ox;
			minX = Math.min(minX, x);
			maxW = Math.max(maxW, bw);
			RenderUtil.panel(g, x, y, bw + 6, lh + 2, 1f);
			RenderUtil.roundRect(g, x + 6, y + (lh + 2) / 2 - 1, 3, 3, 1, instance.color.get());
			FontUtil.draw(g, t, x + 13, y + 3, 0xFFFFFFFF);
			y += lh + 4;
		}
		if (maxW > 0) com.witch.gui.HudLayout.report("waypoints", minX, top, maxW, y - top);
	}

	private static double d2(Waypoints.Wp w, double px, double pz) {
		return (w.x() - px) * (w.x() - px) + (w.z() - pz) * (w.z() - pz);
	}
}
