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
		com.witch.module.AntiVanishModule.drawHud(g);
		com.witch.module.SusChunkFinderModule.drawHud(g);

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

		int x = 5;
		int y = 5;
		FontUtil.draw(g, "WITCH", x + 1, y, 0xFFFFFFFF);
		FontUtil.draw(g, "CLIENT", x + 6 + FontUtil.width("WITCH"), y, 0xFF9A9A9A);
		FontUtil.draw(g, com.witch.Edition.label(), x + 12 + FontUtil.width("WITCH CLIENT"), y, 0xFF5A5A5A);
		y += FontUtil.height() + 3;
		g.fill(x, y - 2, x + FontUtil.width("WITCH CLIENT") + 12, y - 1, 0x55FFFFFF);

		int rh = FontUtil.height() + 3;
		for (Module m : shown) {
			float a = ANIM.get(m);
			float e = 1f - (1f - a) * (1f - a);
			int w = FontUtil.width(m.name) + 9;
			int ox = Math.round(-(1f - e) * (w + 6));
			g.fill(x + ox, y, x + ox + w, y + rh, RenderUtil.alpha(0xFF000000, 0.62f * a));
			g.fill(x + ox, y, x + ox + 1, y + rh, RenderUtil.alpha(0xFFFFFFFF, a));
			FontUtil.draw(g, m.name, x + ox + 5, y + 2, RenderUtil.alpha(0xFFFFFFFF, a));
			y += Math.round(rh * e);
		}
	}
}
