package com.witch.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Logo "WT" do Witch Client (textura branca com alpha). */
public final class WitchLogo {
	private WitchLogo() {}

	private static final Identifier TEX = Identifier.fromNamespaceAndPath("witch", "textures/gui/logo.png");
	private static final int W = 256;
	private static final int H = 94;

	public static int height(int w) {
		return Math.round(w * H / (float) W);
	}

	public static void draw(GuiGraphicsExtractor g, int x, int y, int w, float alpha) {
		int a = Mth.clamp((int) (alpha * 255f), 0, 255);
		if (a <= 0) return;
		g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y, 0f, 0f, w, height(w), W, H, W, H, (a << 24) | 0xFFFFFF);
	}
}
