package com.witch.gui;

import com.witch.module.Module;
import com.witch.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** HUD enxuto: marca d'agua discreta e lista de modulos ativos (mais larga em cima), com entrada/saida animada. */
public final class HudRenderer {
	private HudRenderer() {}

	private static final Map<Module, Float> ANIM = new HashMap<>();
	private static long last = 0L;

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
		Toasts.draw(g);

		long now = System.nanoTime();
		float dt = last == 0 ? 1f : Math.min(3f, (now - last) / 16_666_667f);
		last = now;

		List<Module> shown = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			float cur = ANIM.getOrDefault(m, 0f);
			cur = Mth.lerp(Math.min(1f, 0.22f * dt), cur, m.isEnabled() ? 1f : 0f);
			if (cur < 0.01f && !m.isEnabled()) {
				ANIM.remove(m);
				continue;
			}
			ANIM.put(m, cur);
			shown.add(m);
		}
		shown.sort((a, b) -> Integer.compare(FontUtil.width(b.name), FontUtil.width(a.name)));

		int x = 5 + HudLayout.dx("modules");
		int y = 5 + HudLayout.dy("modules");
		int top = y;
		int lw = 40;
		WitchLogo.draw(g, x, y, lw, 0.95f);
		HudLayout.report("modules", x, top, lw, WitchLogo.height(lw));
	}
}
