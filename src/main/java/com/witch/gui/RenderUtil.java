package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class RenderUtil {
	private RenderUtil() {}

	public static int alpha(int color, float mul) {
		int a = (int) (((color >>> 24) & 0xFF) * Mth.clamp(mul, 0f, 1f));
		return (a << 24) | (color & 0xFFFFFF);
	}

	public static int mix(int a, int b, float t) {
		t = Mth.clamp(t, 0f, 1f);
		int aa = (int) Mth.lerp(t, (a >>> 24) & 0xFF, (b >>> 24) & 0xFF);
		int ar = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int ag = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int ab = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
		return (aa << 24) | (ar << 16) | (ag << 8) | ab;
	}

	/** Painel padrao do Witch: pilula fina, fundo escuro translucido e contorno claro de 1px. */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, float a) {
		int r = Math.min(h / 2, 6);
		roundRect(g, x, y, w, h, r, alpha(0xFFFFFFFF, 0.14f * a));
		roundRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), alpha(0xFF050505, 0.62f * a));
	}

	private static final net.minecraft.resources.Identifier ROUND =
			net.minecraft.resources.Identifier.fromNamespaceAndPath("witch", "textures/gui/round.png");
	private static final int RT = 64, RC = 28, RM = RT - 2 * RC;

	/**
	 * Retangulo arredondado: 9 blits de uma textura branca pre-suavizada (tintada pela cor).
	 * Antes eram dezenas de fills por retangulo (cada canto linha a linha), o que derrubava o FPS no celular.
	 */
	public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
		if (w <= 0 || h <= 0 || ((color >>> 24) & 0xFF) == 0) return;
		int r = Math.min(radius, Math.min(w, h) / 2);
		if (r <= 0) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		var P = net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;
		int mw = w - 2 * r, mh = h - 2 * r;
		int x2 = x + w - r, y2 = y + h - r;
		g.blit(P, ROUND, x, y, 0f, 0f, r, r, RC, RC, RT, RT, color);
		g.blit(P, ROUND, x2, y, (float) (RT - RC), 0f, r, r, RC, RC, RT, RT, color);
		g.blit(P, ROUND, x, y2, 0f, (float) (RT - RC), r, r, RC, RC, RT, RT, color);
		g.blit(P, ROUND, x2, y2, (float) (RT - RC), (float) (RT - RC), r, r, RC, RC, RT, RT, color);
		if (mw > 0) {
			g.blit(P, ROUND, x + r, y, (float) RC, 0f, mw, r, RM, RC, RT, RT, color);
			g.blit(P, ROUND, x + r, y2, (float) RC, (float) (RT - RC), mw, r, RM, RC, RT, RT, color);
		}
		if (mh > 0) {
			g.blit(P, ROUND, x, y + r, 0f, (float) RC, r, mh, RC, RM, RT, RT, color);
			g.blit(P, ROUND, x2, y + r, (float) (RT - RC), (float) RC, r, mh, RC, RM, RT, RT, color);
			if (mw > 0) g.blit(P, ROUND, x + r, y + r, (float) RC, (float) RC, mw, mh, RM, RM, RT, RT, color);
		}
	}

	/** Nove blits (9-slice) de uma textura propria: b = borda na tela, tb = borda na textura. */
	public static void nine(GuiGraphicsExtractor g, net.minecraft.resources.Identifier id, int x, int y, int w, int h,
			int b, int tw, int th, int tb, int color) {
		if (w <= 0 || h <= 0 || ((color >>> 24) & 0xFF) == 0) return;
		b = Math.min(b, Math.min(w, h) / 2);
		var P = net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;
		int mw = w - 2 * b, mh = h - 2 * b, x2 = x + w - b, y2 = y + h - b;
		int mtw = tw - 2 * tb, mth = th - 2 * tb;
		g.blit(P, id, x, y, 0f, 0f, b, b, tb, tb, tw, th, color);
		g.blit(P, id, x2, y, (float) (tw - tb), 0f, b, b, tb, tb, tw, th, color);
		g.blit(P, id, x, y2, 0f, (float) (th - tb), b, b, tb, tb, tw, th, color);
		g.blit(P, id, x2, y2, (float) (tw - tb), (float) (th - tb), b, b, tb, tb, tw, th, color);
		if (mw > 0) {
			g.blit(P, id, x + b, y, (float) tb, 0f, mw, b, mtw, tb, tw, th, color);
			g.blit(P, id, x + b, y2, (float) tb, (float) (th - tb), mw, b, mtw, tb, tw, th, color);
		}
		if (mh > 0) {
			g.blit(P, id, x, y + b, 0f, (float) tb, b, mh, tb, mth, tw, th, color);
			g.blit(P, id, x2, y + b, (float) (tw - tb), (float) tb, b, mh, tb, mth, tw, th, color);
			if (mw > 0) g.blit(P, id, x + b, y + b, (float) tb, (float) tb, mw, mh, mtw, mth, tw, th, color);
		}
	}

	private static final net.minecraft.resources.Identifier GLOW =
			net.minecraft.resources.Identifier.fromNamespaceAndPath("witch", "textures/gui/glow.png");

	/** Brilho suave (PNG radial pre-suavizado, tintado pela cor) centrado em cx,cy com raio r. */
	public static void glow(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
		if (r <= 0 || ((color >>> 24) & 0xFF) == 0) return;
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, GLOW, cx - r, cy - r, 0f, 0f, r * 2, r * 2, 96, 96, 96, 96, color);
	}

	/** Sombra suave retangular: o brilho radial esticado por tras (tinge pela cor). */
	public static void softShadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int pad, int color) {
		if (((color >>> 24) & 0xFF) == 0) return;
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, GLOW, x - pad, y - pad, 0f, 0f, w + 2 * pad, h + 2 * pad, 96, 96, 96, 96, color);
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}