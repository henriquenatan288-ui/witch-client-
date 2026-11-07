package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Texturas PNG do ClickGUI (moldura do menu e botao de modulo). Tamanhos = tamanho real dos PNGs. */
public final class UiTex {
	private UiTex() {}

	private static final Identifier MENU = Identifier.fromNamespaceAndPath("witch", "textures/gui/menu.png");
	private static final Identifier BTN = Identifier.fromNamespaceAndPath("witch", "textures/gui/button.png");

	public static final int MW = 1100, MH = 620;
	public static final int BW = 800, BH = 168;
	/** Larguras (em px da textura) das pontas do botao que nao podem esticar (cantos arredondados e brilho). */
	private static final int CAP_L = 78, CAP_R = 78;

	private static int argb(int rgb, float alpha) {
		return (Mth.clamp((int) (alpha * 255f), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	public static void menu(GuiGraphicsExtractor g, int x, int y, int w, int h, float alpha) {
		if (alpha <= 0f || w <= 0 || h <= 0) return;
		g.blit(RenderPipelines.GUI_TEXTURED, MENU, x, y, 0f, 0f, w, h, MW, MH, MW, MH, argb(0xFFFFFF, alpha));
	}

	/** Botao em 3 partes: pontas com proporcao fixa (pela altura) e meio esticado. 'tint' escurece/clareia (multiplica). */
	public static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, int tint, float alpha) {
		if (alpha <= 0f || w <= 0 || h <= 0) return;
		float sc = h / (float) BH;
		int lc = Math.round(CAP_L * sc), rc = Math.round(CAP_R * sc);
		if (lc + rc > w) { lc = w * lc / (lc + rc); rc = w - lc; }
		int c = argb(tint, alpha) | 0;
		g.blit(RenderPipelines.GUI_TEXTURED, BTN, x, y, 0f, 0f, lc, h, CAP_L, BH, BW, BH, c);
		int mw = w - lc - rc;
		if (mw > 0) g.blit(RenderPipelines.GUI_TEXTURED, BTN, x + lc, y, (float) CAP_L, 0f, mw, h, BW - CAP_L - CAP_R, BH, BW, BH, c);
		g.blit(RenderPipelines.GUI_TEXTURED, BTN, x + w - rc, y, (float) (BW - CAP_R), 0f, rc, h, CAP_R, BH, BW, BH, c);
	}
}
