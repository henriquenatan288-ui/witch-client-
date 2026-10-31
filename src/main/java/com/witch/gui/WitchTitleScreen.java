package com.witch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Random;

/** Tela inicial animada do Witch Client: ceu noturno, estrelas, lua, bolhas, chapeus caindo e titulo pulsando. */
public class WitchTitleScreen extends Screen {
	private static final String[][] GLYPHS = {
			{"W", "10001", "10001", "10001", "10101", "10101", "11011", "10001"},
			{"I", "11111", "00100", "00100", "00100", "00100", "00100", "11111"},
			{"T", "11111", "00100", "00100", "00100", "00100", "00100", "00100"},
			{"C", "01111", "10000", "10000", "10000", "10000", "10000", "01111"},
			{"H", "10001", "10001", "10001", "11111", "10001", "10001", "10001"},
			{"L", "10000", "10000", "10000", "10000", "10000", "10000", "11111"},
			{"E", "11111", "10000", "10000", "11110", "10000", "10000", "11111"},
			{"N", "10001", "11001", "10101", "10101", "10011", "10001", "10001"},
	};
	private static final int STARS = 70;
	private static final float[] SX = new float[STARS];
	private static final float[] SY = new float[STARS];
	private static final float[] SP = new float[STARS];

	static {
		Random r = new Random(5);
		for (int i = 0; i < STARS; i++) {
			SX[i] = r.nextFloat();
			SY[i] = r.nextFloat() * 0.85f;
			SP[i] = r.nextFloat() * 6.28f;
		}
	}

	private static final String[] LABELS = {"Singleplayer", "Multiplayer", "Options", "Quit"};
	private final LeafParticles hats = new LeafParticles();
	private long last = 0L;
	private int bx, by0, bw = 170, bh = 22, gap = 8;

	public WitchTitleScreen() {
		super(Component.literal("Witch Client"));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		long now = System.nanoTime();
		float speed = last == 0 ? 1f : Math.min(3f, (now - last) / 16_666_667f);
		last = now;
		double sec = now / 1_000_000_000.0;
		int w = this.width;
		int h = this.height;

		// ceu: gradiente em faixas
		int bands = 32;
		for (int i = 0; i < bands; i++) {
			int c = RenderUtil.mix(0xFF0A0520, 0xFF3A1670, i / (float) (bands - 1));
			g.fill(0, h * i / bands, w, h * (i + 1) / bands + 1, c);
		}
		for (int i = 0; i < STARS; i++) {
			float tw = 0.35f + 0.65f * (float) (0.5 + 0.5 * Math.sin(sec * 1.5 + SP[i]));
			int x = Math.round(SX[i] * w);
			int y = Math.round(SY[i] * h);
			g.fill(x, y, x + 1, y + 1, RenderUtil.alpha(0xFFFFFFFF, tw));
		}
		WitchVisuals.render(g, 0, 0, w, h, 1f);
		hats.update(speed, w, h);
		hats.render(g, 0, 0, w, h, 1f);

		// titulo
		float pulse = 0.55f + 0.45f * (float) Math.sin(sec * 2.0);
		int s = Math.max(2, Math.min(6, (int) (w * 0.62f / 30f)));
		int ty = Math.max(14, h / 8);
		drawWord(g, "WITCH", w / 2, ty, s, pulse);
		drawWord(g, "CLIENT", w / 2, ty + 8 * s + s, Math.max(1, s / 2), pulse * 0.8f);

		// botoes
		by0 = Math.max(ty + 8 * s + s + 8 * Math.max(1, s / 2) + 18, h / 2);
		bx = (w - bw) / 2;
		for (int i = 0; i < LABELS.length; i++) {
			int y = by0 + i * (bh + gap);
			boolean hover = RenderUtil.inside(mx, my, bx, y, bw, bh);
			RenderUtil.roundRect(g, bx - 2, y - 2, bw + 4, bh + 4, 8, RenderUtil.alpha(Theme.ACCENT, hover ? 0.45f : 0.16f * (0.6f + pulse)));
			RenderUtil.roundRect(g, bx, y, bw, bh, 6, hover ? 0xE03A1670 : 0xD0170D33);
			int tw = FontUtil.width(LABELS[i]);
			FontUtil.draw(g, LABELS[i], bx + (bw - tw) / 2, y + (bh - FontUtil.height()) / 2, hover ? 0xFFFFFFFF : 0xFFD8C8F5);
		}
	}

	private void drawWord(GuiGraphicsExtractor g, String word, int cx, int y, int s, float glow) {
		int total = word.length() * 6 * s - s;
		int x = cx - total / 2;
		for (int i = 0; i < word.length(); i++) {
			String[] gl = glyph(word.charAt(i));
			if (gl == null) {
				x += 6 * s;
				continue;
			}
			for (int row = 0; row < 7; row++) {
				String bits = gl[row + 1];
				for (int col = 0; col < 5; col++) {
					if (bits.charAt(col) != '1') continue;
					int px = x + col * s;
					int py = y + row * s;
					g.fill(px - 1, py - 1, px + s + 1, py + s + 1, RenderUtil.alpha(Theme.ACCENT, 0.25f * glow));
				}
			}
			for (int row = 0; row < 7; row++) {
				String bits = gl[row + 1];
				for (int col = 0; col < 5; col++) {
					if (bits.charAt(col) != '1') continue;
					int px = x + col * s;
					int py = y + row * s;
					g.fill(px, py, px + s, py + s, RenderUtil.mix(0xFFFFFFFF, 0xFFD2B0F5, row / 6f));
				}
			}
			x += 6 * s;
		}
	}

	private static String[] glyph(char ch) {
		for (String[] gl : GLYPHS) {
			if (gl[0].charAt(0) == ch) return gl;
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (click.button() == 0) {
			for (int i = 0; i < LABELS.length; i++) {
				int y = by0 + i * (bh + gap);
				if (RenderUtil.inside(click.x(), click.y(), bx, y, bw, bh)) {
					Minecraft mc = Minecraft.getInstance();
					switch (i) {
						case 0 -> mc.gui.setScreen(new SelectWorldScreen(this));
						case 1 -> mc.gui.setScreen(new JoinMultiplayerScreen(this));
						case 2 -> mc.gui.setScreen(new OptionsScreen(this, mc.options, false));
						default -> mc.stop();
					}
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}
}
