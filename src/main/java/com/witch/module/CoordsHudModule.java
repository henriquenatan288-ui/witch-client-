package com.witch.module;

import com.witch.gui.FontUtil;
import com.witch.gui.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Coordenadas, direcao e velocidade no canto inferior esquerdo. */
public class CoordsHudModule extends Module {
	private static CoordsHudModule instance;
	private static final String[] DIR = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	public CoordsHudModule() {
		super("Coords HUD", Category.UTILITY);
		instance = this;
	}

	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled()) return;
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null) return;
		String dir = DIR[Math.floorMod(Math.round(p.getYRot() / 45f), 8)];
		var v = p.getDeltaMovement();
		double speed = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
		String l1 = "XYZ  " + p.getBlockX() + "  " + p.getBlockY() + "  " + p.getBlockZ();
		String l2 = dir + "   " + String.format("%.1f", speed) + " b/s";
		int sh = mc.getWindow().getGuiScaledHeight();
		int lh = FontUtil.height() + 3;
		int bw = Math.max(FontUtil.width(l1), FontUtil.width(l2)) + 12;
		int x0 = 5 + com.witch.gui.HudLayout.dx("coords");
		int y = sh - 4 - lh * 2 - 2 + com.witch.gui.HudLayout.dy("coords");
		RenderUtil.panel(g, x0, y, bw, lh * 2 + 4, 1f);
		FontUtil.draw(g, l1, x0 + 7, y + 3, 0xFFFFFFFF);
		FontUtil.draw(g, l2, x0 + 7, y + 3 + lh, 0xFF8A8A8A);
		com.witch.gui.HudLayout.report("coords", x0, y, bw, lh * 2 + 4);
	}
}
