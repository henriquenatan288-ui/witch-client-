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

	public static final int MW = 1128, MH = 628;
	public static final int BW = 1224, BH = 198;
	private static final Identifier BTN_T = Identifier.fromNamespaceAndPath("witch", "textures/gui/button_title.png");
	private static final int TW = 800, TH = 168, TCAP = 78;
	private static final Identifier TBG = Identifier.fromNamespaceAndPath("witch", "textures/gui/title_bg.png");
	public static final int TBW = 1280, TBH = 576;
	/** Larguras (em px da textura) das pontas do botao que nao podem esticar (cantos arredondados e brilho). */
	private static final int CAP_L = 248, CAP_R = 45;

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

	/** Botao da tela inicial (textura violeta antiga). */
	public static void titleButton(GuiGraphicsExtractor g, int x, int y, int w, int h, int tint, float alpha) {
		if (alpha <= 0f || w <= 0 || h <= 0) return;
		float sc = h / (float) TH;
		int cap = Math.round(TCAP * sc);
		if (cap * 2 > w) cap = w / 2;
		int c = argb(tint, alpha);
		g.blit(RenderPipelines.GUI_TEXTURED, BTN_T, x, y, 0f, 0f, cap, h, TCAP, TH, TW, TH, c);
		int mw = w - cap * 2;
		if (mw > 0) g.blit(RenderPipelines.GUI_TEXTURED, BTN_T, x + cap, y, (float) TCAP, 0f, mw, h, TW - TCAP * 2, TH, TW, TH, c);
		g.blit(RenderPipelines.GUI_TEXTURED, BTN_T, x + w - cap, y, (float) (TW - TCAP), 0f, cap, h, TCAP, TH, TW, TH, c);
	}

	/** Fundo da tela inicial cobrindo a tela inteira (corta o excesso, sem esticar). */
	public static void titleBg(GuiGraphicsExtractor g, int w, int h) {
		float sc = Math.max(w / (float) TBW, h / (float) TBH);
		int vw = Math.min(TBW, Math.round(w / sc)), vh = Math.min(TBH, Math.round(h / sc));
		int u0 = (TBW - vw) / 2, v0 = (TBH - vh) / 2;
		g.blit(RenderPipelines.GUI_TEXTURED, TBG, 0, 0, (float) u0, (float) v0, w, h, vw, vh, TBW, TBH, 0xFFFFFFFF);
	}
}
