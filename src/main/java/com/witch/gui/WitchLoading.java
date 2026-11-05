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

	/** Luz suave (camadas de baixa opacidade) centrada em (cx, cy). */
	public static void glow(GuiGraphicsExtractor g, int cx, int cy, int r, float alpha) {
		for (int i = 0; i < 9; i++) {
			float k = 1f - i / 9f;
			int rr = Math.round(r * k);
			int rh = Math.round(r * k * 0.6f);
			if (rr < 2 || rh < 2) break;
			RenderUtil.roundRect(g, cx - rr, cy - rh, rr * 2, rh * 2, rh, RenderUtil.alpha(0xFFFFFFFF, alpha * 0.018f));
		}
	}

	/** Fundo do Witch: degrade escuro e linhas brilhantes balancando nos cantos de baixo. */
	public static void backdrop(GuiGraphicsExtractor g, int w, int h, double t) {
		// fundo: degrade escuro
		int n = 24;
		for (int i = 0; i < n; i++) {
			int y0 = h * i / n, y1 = h * (i + 1) / n;
			g.fill(0, y0, w, y1, RenderUtil.mix(0xFF0B0D10, 0xFF050607, i / (float) (n - 1)));
		}
	}

	/** progress 0..1, ou negativo = indeterminado (barra que desliza). */
	public static void draw(GuiGraphicsExtractor g, int w, int h, String title, String sub, float progress, String hint) {
		double t = System.nanoTime() / 1_000_000_000.0;
		long nowMs = System.currentTimeMillis();
		if (nowMs - lastMs > 1500L) startMs = nowMs;
		lastMs = nowMs;
		long elapsed = (nowMs - startMs) / 1000L;

		backdrop(g, w, h, t);

		int cx = w / 2;
		int cy = h / 2;

		int lw = Math.min(96, w / 3);
		glow(g, cx, cy - 36, 120, 1f);
		WitchLogo.draw(g, cx - lw / 2, cy - 38 - WitchLogo.height(lw) / 2, lw, 1f);

		int dots = (int) (t * 2.5) % 4;
		FontUtil.draw(g, title, cx - FontUtil.width(title) / 2, cy - 14, 0xFFCFCFCF);
		FontUtil.draw(g, ".".repeat(dots), cx + FontUtil.width(title) / 2, cy - 14, 0xFFCFCFCF);

		// barra
		int bw = Math.min(180, w - 40);
		int bx = cx - bw / 2;
		int by = cy + 4;
		RenderUtil.roundRect(g, bx, by, bw, 3, 1, 0x33FFFFFF);
		if (progress >= 0f) {
			int fw = Math.max(3, Math.round(bw * Math.min(1f, progress)));
			RenderUtil.roundRect(g, bx, by, fw, 3, 1, 0xFFFFFFFF);
			String pct = Math.round(Math.min(1f, progress) * 100) + "%  -  " + elapsed + "s";
			FontUtil.draw(g, pct, cx - FontUtil.width(pct) / 2, by + 8, 0xFF8E8E8E);
		} else {
			int seg = bw / 4;
			double p = (t * 0.9) % 1.0;
			int sx = bx + (int) ((bw + seg) * p) - seg;
			int x0 = Math.max(bx, sx), x1 = Math.min(bx + bw, sx + seg);
			if (x1 - x0 > 2) RenderUtil.roundRect(g, x0, by, x1 - x0, 3, 1, 0xFFFFFFFF);
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
