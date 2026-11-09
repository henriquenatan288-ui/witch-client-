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
			RenderUtil.roundRect(g, cx - rr, cy - rh, rr * 2, rh * 2, rh, RenderUtil.alpha(0xFF8B7FD6, alpha * 0.028f));
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

	/** Estrelas subindo devagar e piscando. */
	private static void stars(GuiGraphicsExtractor g, int w, int h, double t) {
		java.util.Random r = new java.util.Random(11L);
		for (int i = 0; i < 60; i++) {
			float fx = r.nextFloat(), fy = r.nextFloat(), ph = r.nextFloat() * 6.28f, sp = 0.4f + r.nextFloat();
			int x = (int) (fx * w);
			int y = (int) (((fy - t * 0.012 * sp) % 1.0 + 1.0) % 1.0 * h);
			float a = (0.25f + 0.55f * (0.5f + 0.5f * (float) Math.sin(t * 1.4 * sp + ph))) * 0.55f;
			g.fill(x, y, x + 1, y + 1, RenderUtil.alpha(0xFFFFFFFF, a));
		}
	}

	/** 12 pontos girando em volta da logo, com rastro que apaga. */
	private static void spinner(GuiGraphicsExtractor g, int cx, int cy, int r, double t) {
		int n = 12;
		double head = (t * 1.6) % 1.0 * n;
		for (int i = 0; i < n; i++) {
			double d = ((head - i) % n + n) % n;
			float a = (float) Math.max(0.0, 1.0 - d / 7.0);
			if (a <= 0.02f) continue;
			double ang = i * Math.PI * 2 / n - Math.PI / 2;
			int x = cx + (int) Math.round(Math.cos(ang) * r), y = cy + (int) Math.round(Math.sin(ang) * r * 0.62);
			RenderUtil.roundRect(g, x - 1, y - 1, 3, 3, 1, RenderUtil.alpha(0xFF8B7FD6, a * 0.9f));
		}
	}

	/** progress 0..1, ou negativo = indeterminado (barra que desliza). */
	public static void draw(GuiGraphicsExtractor g, int w, int h, String title, String sub, float progress, String hint) {
		double t = System.nanoTime() / 1_000_000_000.0;
		long nowMs = System.currentTimeMillis();
		if (nowMs - lastMs > 1500L) startMs = nowMs;
		lastMs = nowMs;
		long elapsed = (nowMs - startMs) / 1000L;

		UiTex.titleBg(g, w, h);
		g.fill(0, 0, w, h, 0x77000000);

		int cx = w / 2;
		int cy = h / 2;

		float breathe = 1f + 0.035f * (float) Math.sin(t * 2.2);
		int lw = Math.round(Math.min(96, w / 3) * breathe);
		glow(g, cx, cy - 36, 120, 0.8f + 0.2f * (float) Math.sin(t * 2.2));
		WitchLogo.draw(g, cx - lw / 2, cy - 38 - WitchLogo.height(lw) / 2, lw, 1f);
		spinner(g, cx, cy - 38, Math.min(96, w / 3) / 2 + 22, t);

		int dots = (int) (t * 2.5) % 4;
		FontUtil.draw(g, title, cx - FontUtil.width(title) / 2, cy - 14, 0xFFD6F5DC);
		FontUtil.draw(g, ".".repeat(dots), cx + FontUtil.width(title) / 2, cy - 14, 0xFFD6F5DC);

		// barra
		int bw = Math.min(180, w - 40);
		int bx = cx - bw / 2;
		int by = cy + 4;
		RenderUtil.roundRect(g, bx, by, bw, 3, 1, 0x33FFFFFF);
		if (progress >= 0f) {
			int fw = Math.max(3, Math.round(bw * Math.min(1f, progress)));
			RenderUtil.roundRect(g, bx, by, fw, 3, 1, 0xFF8B7FD6);
			int sw = Math.min(28, fw);
			int sx = bx + (int) ((fw - sw) * ((t * 0.8) % 1.0));
			RenderUtil.roundRect(g, sx, by, sw, 3, 1, 0x66FFFFFF);
			String pct = Math.round(Math.min(1f, progress) * 100) + "%  -  " + elapsed + "s";
			FontUtil.draw(g, pct, cx - FontUtil.width(pct) / 2, by + 8, 0xFF9A94B8);
		} else {
			int seg = bw / 4;
			double p = (t * 0.9) % 1.0;
			int sx = bx + (int) ((bw + seg) * p) - seg;
			int x0 = Math.max(bx, sx), x1 = Math.min(bx + bw, sx + seg);
			if (x1 - x0 > 2) RenderUtil.roundRect(g, x0, by, x1 - x0, 3, 1, 0xFF8B7FD6);
		}

		if (sub != null && !sub.isEmpty()) {
			FontUtil.draw(g, sub, cx - FontUtil.width(sub) / 2, by + 22, 0xFF9A94B8);
		}
		if (hint == null || hint.isEmpty()) {
			hint = "Dica: " + TIPS[(int) ((nowMs / 6000L) % TIPS.length)];
		}
		if (hint != null && !hint.isEmpty()) {
			long ph = nowMs % 6000L;
			float fa = Math.min(1f, Math.min(ph, 6000L - ph) / 600f);
			FontUtil.draw(g, hint, cx - FontUtil.width(hint) / 2, h - 28, RenderUtil.alpha(0xFF7E7A99, fa));
		}
		String credit = "WITCH CLIENT MADE BY BISTECK";
		FontUtil.draw(g, credit, w - FontUtil.width(credit) - 8, h - FontUtil.height() - 6, 0xFF3A3656);
	}
}
