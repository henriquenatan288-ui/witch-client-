package com.witch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Avisos pequenos no canto superior direito quando um modulo liga/desliga. */
public final class Toasts {
	private Toasts() {}

	private record Toast(String name, boolean on, long at) {}

	private static final List<Toast> LIST = new ArrayList<>();
	private static final long LIFE = 1900L;
	private static final long IN = 220L;
	private static final long OUT = 350L;

	public static void push(String name, boolean on) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.level == null) return; // carregando config / fora do mundo
		LIST.removeIf(t -> t.name.equals(name));
		LIST.add(new Toast(name, on, System.currentTimeMillis()));
		while (LIST.size() > 4) LIST.remove(0);
	}

	public static void draw(GuiGraphicsExtractor g) {
		if (LIST.isEmpty()) return;
		long now = System.currentTimeMillis();
		LIST.removeIf(t -> now - t.at > LIFE);
		int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int y = 6;
		for (int i = LIST.size() - 1; i >= 0; i--) {
			Toast t = LIST.get(i);
			long age = now - t.at;
			float in = Math.min(1f, age / (float) IN);
			float out = age > LIFE - OUT ? (LIFE - age) / (float) OUT : 1f;
			float e = 1f - (1f - in) * (1f - in);
			String state = t.on ? "ON" : "OFF";
			int tw = FontUtil.width(t.name) + 8 + FontUtil.width(state);
			int bw = tw + 14;
			int bh = FontUtil.height() + 8;
			int x = sw - 6 - bw + Math.round((1f - e) * (bw + 8));
			float a = out;
			int bar = t.on ? Theme.accent() : 0xFF6A6A6A;
			RenderUtil.roundRect(g, x, y, bw, bh, 4, RenderUtil.alpha(0xFF0C0C0C, 0.8f * a));
			g.fill(x, y + 2, x + 2, y + bh - 2, RenderUtil.alpha(bar, a));
			FontUtil.draw(g, t.name, x + 8, y + 4, RenderUtil.alpha(0xFFFFFFFF, a));
			FontUtil.draw(g, state, x + 8 + FontUtil.width(t.name) + 8, y + 4, RenderUtil.alpha(t.on ? Theme.accent() : 0xFF8E8E8E, a));
			y += bh + 3;
		}
	}
}
