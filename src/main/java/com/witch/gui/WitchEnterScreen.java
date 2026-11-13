package com.witch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Tela "Press to enter": logo no meio da tela preta. Ao entrar, o logo e arrastado para cima
 * com efeito de esticar (squash and stretch) e para no centro do topo, onde o titulo o recebe.
 */
public class WitchEnterScreen extends Screen {
	private static boolean shown = false;
	/** Quando o logo chegou (ms); o titulo usa para sair do preto. 0 = nao veio da tela de entrada. */
	public static long handoffMs = 0L;

	private static final long TEXT_OUT = 250L, MOVE_DELAY = 120L, MOVE = 950L, HOLD = 100L;
	private static final int END_LW = 58;

	private final long born = System.currentTimeMillis();
	private long startMs = 0L;
	private boolean done = false;

	public WitchEnterScreen() {
		super(Component.literal("Witch Client"));
		shown = true;
	}

	/** So na primeira vez que o titulo aparece em cada sessao. */
	public static boolean pending() {
		return !shown;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void enter() {
		if (startMs == 0L) startMs = System.currentTimeMillis();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		enter();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (input.key() == GLFW.GLFW_KEY_ESCAPE) return true;
		enter();
		return true;
	}

	private static float ease(float t) {
		return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
	}

	/** Velocidade normalizada (0..1) do easing: 0 parado, 1 no meio do arrasto. */
	private static float speed(float t) {
		float d = t < 0.5f ? 12f * t * t : 3f * (2f - 2f * t) * (2f - 2f * t);
		return Mth.clamp(d / 3f, 0f, 1f);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		long now = System.currentTimeMillis();
		int w = this.width, h = this.height;
		int fh = FontUtil.height();
		g.fill(0, 0, w, h, 0xFF000000);

		float fadeIn = Mth.clamp((now - born) / 500f, 0f, 1f);
		int lw0 = Math.min(96, w / 3);
		int cx = w / 2;
		int cy0 = h / 2 - 8;
		int cy1 = 6 + WitchLogo.height(END_LW) / 2;

		float tm = startMs == 0L ? -1f : (now - startMs - MOVE_DELAY) / (float) MOVE;
		float e = tm <= 0f ? 0f : ease(Mth.clamp(tm, 0f, 1f));
		float k = tm <= 0f || tm >= 1f ? 0f : speed(tm);
		float textA = startMs == 0L ? fadeIn : Mth.clamp(1f - (now - startMs) / (float) TEXT_OUT, 0f, 1f);

		double sec = now / 1000.0;
		float pulse = 0.55f + 0.45f * (float) Math.sin(sec * 2.0);
		float breathe = startMs == 0L ? 1f + 0.03f * (float) Math.sin(sec * 2.2) : 1f;
		float lw = Mth.lerp(e, lw0, END_LW) * breathe;
		float cy = Mth.lerp(e, cy0, cy1);

		// logo com squash and stretch: estica na vertical e afina na horizontal enquanto anda
		g.pose().pushMatrix();
		g.pose().translate((float) cx, cy);
		g.pose().scale(1f - 0.22f * k, 1f + 0.45f * k);
		g.pose().translate((float) -cx, -cy);
		int iw = Math.round(lw);
		WitchLoading.glow(g, cx, Math.round(cy), iw * 2, (0.5f + 0.5f * pulse) * fadeIn);
		WitchLogo.draw(g, cx - iw / 2, Math.round(cy) - WitchLogo.height(iw) / 2, iw, fadeIn);
		g.pose().popMatrix();

		if (textA > 0.01f) {
			String name = "Witch Client";
			FontUtil.draw(g, name, cx - FontUtil.width(name) / 2, cy0 + WitchLogo.height(lw0) / 2 + 10, RenderUtil.alpha(0xFFFFFFFF, textA));
			String hint = "Press to enter";
			float blink = 0.45f + 0.35f * (float) Math.sin(sec * 3.0);
			FontUtil.draw(g, hint, cx - FontUtil.width(hint) / 2, cy0 + WitchLogo.height(lw0) / 2 + 10 + fh + 8, RenderUtil.alpha(0xFF8E939B, textA * blink));
		}

		if (!done && startMs != 0L && now - startMs > MOVE_DELAY + MOVE + HOLD) {
			done = true;
			handoffMs = now;
			Minecraft.getInstance().gui.setScreen(new WitchTitleScreen());
		}
	}
}
