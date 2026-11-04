package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Tela de carregamento do Witch Client (mundo / entrando em servidor). Monocromatica, com barra fina e linhas brilhantes. */
public final class WitchLoading {
	private WitchLoading() {}

	private static final String[] TIPS = {
			"F9 liga e desliga a fonte Inter do client.",
			"Passe o mouse num modulo do ClickGUI para ver o que ele faz.",
			"A tecla Home devolve as janelas do ClickGUI ao lugar original.",
			"Clique direito no titulo de uma janela reseta so aquela janela.",
			"Em GUI Settings voce ajusta a transparencia do ClickGUI.",
			"Tracers: a cor da linha acompanha a cor do ESP.",
			"Sus Chunk Finder: ajuste o raio de 1 a 30 chunks.",
	};
	private static long startMs = 0L;
	private static long lastMs = 0L;

	/** progress 0..1, ou negativo = indeterminado (barra que desliza). */
	public static void draw(GuiGraphicsExtractor g, int w, int h, String title, String sub, float progress, String hint) {
		double t = System.nanoTime() / 1_000_000_000.0;
		long nowMs = System.currentTimeMillis();
		if (nowMs - lastMs > 1500L) startMs = nowMs;
		lastMs = nowMs;
		long elapsed = (nowMs - startMs) / 1000L;

		// fundo: degrade escuro
		int n = 24;
		for (int i = 0; i < n; i++) {
			int y0 = h * i / n, y1 = h * (i + 1) / n;
			g.fill(0, y0, w, y1, RenderUtil.mix(0xFF0B0D10, 0xFF050607, i / (float) (n - 1)));
		}

		// linhas brilhantes balancando nos cantos de baixo
		int rise = (int) (h * 0.5f);
		for (int side = 0; side < 2; side++) {
			boolean left = side == 0;
			double ph = left ? 0.0 : 1.9;
			for (int i = 0; i < rise; i += 2) {
				float f = i / (float) rise;
				float fade = (1f - f) * (1f - f);
				double sway = Math.sin(t * 1.3 + i * 0.035 + ph) * (6 + 22 * f);
				int x = (int) Math.round(left ? 3 + f * 26 + sway : w - 3 - f * 26 - sway);
				int y = h - i;
				g.fill(x - 3, y - 2, x + 4, y, RenderUtil.alpha(0xFFFFFFFF, fade * 0.06f));
				g.fill(x - 1, y - 2, x + 2, y, RenderUtil.alpha(0xFFFFFFFF, fade * 0.7f));
			}
		}

		int cx = w / 2;
		int cy = h / 2;

		String brand = "WITCH CLIENT";
		FontUtil.draw(g, brand, cx - FontUtil.width(brand) / 2, cy - 34, 0xFFFFFFFF);

		int dots = (int) (t * 2.5) % 4;
		FontUtil.draw(g, title, cx - FontUtil.width(title) / 2, cy - 14, 0xFFCFCFCF);
		FontUtil.draw(g, ".".repeat(dots), cx + FontUtil.width(title) / 2, cy - 14, 0xFFCFCFCF);

		// barra
		int bw = Math.min(180, w - 40);
		int bx = cx - bw / 2;
		int by = cy + 4;
		g.fill(bx, by, bx + bw, by + 3, 0x33FFFFFF);
		if (progress >= 0f) {
			int fw = Math.round(bw * Math.min(1f, progress));
			g.fill(bx, by, bx + fw, by + 3, 0xFFFFFFFF);
			float pulse = 0.35f + 0.25f * (float) Math.sin(t * 5);
			g.fill(Math.max(bx, bx + fw - 6), by - 1, bx + fw + 1, by + 4, RenderUtil.alpha(0xFFFFFFFF, pulse));
			String pct = Math.round(Math.min(1f, progress) * 100) + "%  -  " + elapsed + "s";
			FontUtil.draw(g, pct, cx - FontUtil.width(pct) / 2, by + 8, 0xFF8E8E8E);
		} else {
			int seg = bw / 4;
			double p = (t * 0.9) % 1.0;
			int sx = bx + (int) ((bw + seg) * p) - seg;
			int x0 = Math.max(bx, sx), x1 = Math.min(bx + bw, sx + seg);
			if (x1 > x0) g.fill(x0, by, x1, by + 3, 0xFFFFFFFF);
		}

		if (sub != null && !sub.isEmpty()) {
			FontUtil.draw(g, sub, cx - FontUtil.width(sub) / 2, by + 22, 0xFF8E8E8E);
		}
		if (hint == null || hint.isEmpty()) {
			hint = "Dica: " + TIPS[(int) ((nowMs / 6000L) % TIPS.length)];
		}
		if (hint != null && !hint.isEmpty()) {
			FontUtil.draw(g, hint, cx - FontUtil.width(hint) / 2, h - 28, 0xFF666666);
		}
		String credit = "WITCH CLIENT MADE BY BISTECK";
		FontUtil.draw(g, credit, w - FontUtil.width(credit) - 8, h - FontUtil.height() - 6, 0xFF555555);
	}
}
