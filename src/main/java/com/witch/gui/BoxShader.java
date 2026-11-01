package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * "Shader" das caixas do ClickGUI (calculado na CPU, so tons de cinza): borda fina clara, miolo, faixa de brilho
 * no topo, sombra embaixo e um reflexo que acende no hover. Usa so retangulos arredondados, entao e barato.
 */
public final class BoxShader {
	private BoxShader() {}

	/** @param base cor do miolo (ARGB); @param glow 0..1 = intensidade do hover/ativo */
	public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int base, float glow, float a) {
		if (w <= 4 || h <= 4) return;
		// borda (anel claro de 1px)
		RenderUtil.roundRect(g, x - 1, y - 1, w + 2, h + 2, r + 1, RenderUtil.alpha(0xFFFFFFFF, a * (0.07f + 0.20f * glow)));
		// miolo
		RenderUtil.roundRect(g, x, y, w, h, r, RenderUtil.alpha(base, a));
		// sombra interna embaixo
		int sh = Math.max(2, h / 3);
		RenderUtil.roundRect(g, x + 1, y + h - sh, w - 2, sh - 1, Math.max(1, r - 2), RenderUtil.alpha(0xFF000000, a * 0.22f));
		// brilho no topo
		int hl = Math.max(2, h / 2 - 1);
		RenderUtil.roundRect(g, x + 1, y + 1, w - 2, hl, Math.max(1, r - 1), RenderUtil.alpha(0xFFFFFFFF, a * (0.05f + 0.09f * glow)));
	}
}
