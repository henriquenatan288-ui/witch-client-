package com.witch.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;


/** HUD enxuto: marca d'agua discreta e lista de modulos ativos (mais larga em cima), com entrada/saida animada. */
public final class HudRenderer {
	private HudRenderer() {}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.screen() instanceof ClickGuiScreen) return;
		com.witch.module.MoodFilterModule.drawHud(g);
		com.witch.module.FilterModule.drawHud(g);
		com.witch.module.AntiVanishModule.drawHud(g);
		com.witch.module.SusChunkFinderModule.drawHud(g);
		com.witch.module.SusSoloFinderModule.drawHud(g);
		com.witch.module.CoordsHudModule.drawHud(g);
		com.witch.module.ArmorHudModule.drawHud(g);
		com.witch.module.WaypointsModule.drawHud(g);
		com.witch.module.InfoModules.drawAll(g);
		com.witch.module.InfoModules.Keystrokes.drawHud(g);
		com.witch.module.InfoModules.HitMarker.drawHud(g);
		com.witch.module.InfoModules.LowHealthAlert.drawHud(g);
		Toasts.draw(g);

		int x = 5 + HudLayout.dx("modules");
		int y = 5 + HudLayout.dy("modules");
		int top = y;
		int lw = 40;
		WitchLogo.draw(g, x, y, lw, 0.95f);
		HudLayout.report("modules", x, top, lw, WitchLogo.height(lw));
	}
}
