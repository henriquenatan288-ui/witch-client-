package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Fundo escuro com fitas verticais onduladas em cinza, desenhado por codigo (sem textura). */
public final class WaveBackdrop {
	private WaveBackdrop() {}

	public static void draw(GuiGraphicsExtractor g, int w, int h, double sec) {
		g.fill(0, 0, w, h, 0xFF0A0A0B);
		int n = 7;
		int step = 3;
		for (int i = 0; i < n; i++) {
			double baseX = w * (i + 0.5) / n;
			double rw = w / (n * 1.9);
			double amp = w * 0.035;
			for (int y = 0; y < h; y += step) {
				double off = Math.sin(y * 0.016 + i * 1.7 + sec * 0.35) * amp
						+ Math.sin(y * 0.041 - i * 0.9 - sec * 0.22) * amp * 0.35;
				int x0 = (int) Math.round(baseX + off - rw / 2);
				int x1 = (int) Math.round(baseX + off + rw / 2);
				int mid = (x0 + x1) / 2;
				int q = Math.max(1, (x1 - x0) / 4);
				g.fill(x0, y, x1, y + step, 0x0E8C8C90);
				g.fill(x0 + q, y, x1 - q, y + step, 0x118C8C90);
				g.fill(mid - q / 2, y, mid + q / 2 + 1, y + step, 0x138C8C90);
			}
		}
		g.fill(0, 0, w, h, 0x33000000);
	}
}
