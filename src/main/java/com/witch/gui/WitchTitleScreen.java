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
	private final float[] hoverAnim = new float[4];

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
		drawWord(g, "WITCH", w / 2, ty, s, pulse, sec);
		subtitle(g, w / 2, ty + 8 * s + s + 2, Math.max(1, s / 2), pulse);

		// botoes
		by0 = Math.max(ty + 8 * s + s + 8 * Math.max(1, s / 2) + 22, h / 2);
		bx = (w - bw) / 2;
		for (int i = 0; i < LABELS.length; i++) {
			int y = by0 + i * (bh + gap);
			boolean hover = RenderUtil.inside(mx, my, bx, y, bw, bh);
			hoverAnim[i] += ((hover ? 1f : 0f) - hoverAnim[i]) * Math.min(1f, 0.25f * speed);
			float ha = hoverAnim[i];
			RenderUtil.roundRect(g, bx - 3, y - 3, bw + 6, bh + 6, 9, RenderUtil.alpha(Theme.ACCENT, (0.08f + 0.30f * ha) * (0.7f + 0.3f * pulse)));
			RenderUtil.roundRect(g, bx - 1, y - 1, bw + 2, bh + 2, 7, RenderUtil.alpha(0xFFB57BEA, 0.25f + 0.55f * ha));
			RenderUtil.roundRect(g, bx, y, bw, bh, 6, RenderUtil.mix(0xF0150B2E, 0xF03A1670, ha));
			g.fill(bx + 5, y + 1, bx + bw - 5, y + 2, RenderUtil.alpha(0xFFFFFFFF, 0.10f + 0.12f * ha));
			int bar = Math.round(4 + (bw / 2f - 8) * ha);
			RenderUtil.roundRect(g, bx + bw / 2 - bar, y + bh - 3, bar * 2, 2, 1, RenderUtil.alpha(0xFFD2B0F5, 0.85f * ha));
			String label = LABELS[i].toUpperCase();
			int tw = spacedWidth(label);
			int ty2 = y + (bh - FontUtil.height()) / 2;
			spaced(g, label, bx + (bw - tw) / 2, ty2, RenderUtil.mix(0xFFD8C8F5, 0xFFFFFFFF, ha));
		}
		walker(g, w, h, sec);
	}

	private static int spacedWidth(String s) {
		int t = 0;
		for (int i = 0; i < s.length(); i++) t += FontUtil.width(String.valueOf(s.charAt(i))) + 1;
		return t - 1;
	}

	private static void spaced(GuiGraphicsExtractor g, String s, int x, int y, int color) {
		for (int i = 0; i < s.length(); i++) {
			String c = String.valueOf(s.charAt(i));
			FontUtil.draw(g, c, x, y, color);
			x += FontUtil.width(c) + 1;
		}
	}

	/** "CLIENT" em letras pequenas e espacadas, entre dois losangos (como no logo). */
	private void subtitle(GuiGraphicsExtractor g, int cx, int y, int s, float pulse) {
		String word = "CLIENT";
		int total = word.length() * 6 * s - s;
		int x = cx - total / 2;
		int color = RenderUtil.alpha(0xFFB57BEA, 0.85f);
		for (int i = 0; i < word.length(); i++) {
			String[] gl = glyph(word.charAt(i));
			for (int row = 0; row < 7; row++) {
				for (int col = 0; col < 5; col++) {
					if (gl[row + 1].charAt(col) == '1') {
						g.fill(x + col * s, y + row * s, x + col * s + s, y + row * s + s, color);
					}
				}
			}
			x += 6 * s;
		}
		int d = Math.max(2, s + 1);
		int cy = y + 3 * s + s / 2;
		diamond(g, cx - total / 2 - 14 - d, cy, d, RenderUtil.alpha(0xFFD2B0F5, 0.5f + 0.4f * pulse));
		diamond(g, cx + total / 2 + 14 + d, cy, d, RenderUtil.alpha(0xFFD2B0F5, 0.5f + 0.4f * pulse));
	}

	private static void diamond(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = r - Math.abs(dy);
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	/** Boneco andando da esquerda pra direita na area direita da tela, com o nick em cima. */
	private void walker(GuiGraphicsExtractor g, int w, int h, double sec) {
		int s = Math.max(1, Math.min(3, h / 130));
		int startX = bx + bw + 24;
		int endX = w - 14 * s;
		if (endX - startX < 20 * s) return;
		int groundY = h - 22;
		double cycle = 14.0;
		double p = (sec % cycle) / cycle;
		int x = (int) (startX + (endX - startX) * p);
		float fade = (float) Math.min(1.0, Math.min(p * 10.0, (1.0 - p) * 10.0));
		double ph = sec * 7.0;
		double sw = Math.sin(ph);
		int liftL = (int) Math.round(Math.max(0, sw) * 3 * s);
		int liftR = (int) Math.round(Math.max(0, -sw) * 3 * s);
		int armL = (int) Math.round(sw * 2 * s);
		int armR = -armL;
		int bob = (int) Math.round(Math.abs(sw) * s);

		// chao
		g.fill(startX - 20, groundY, w, groundY + 1, RenderUtil.alpha(0xFFB57BEA, 0.45f * fade));
		g.fill(startX - 20, groundY + 1, w, groundY + 3, RenderUtil.alpha(0xFF2A1055, 0.7f));
		// sombra
		RenderUtil.roundRect(g, x - 5 * s, groundY - 1, 10 * s, 2, 1, RenderUtil.alpha(0xFF000000, 0.35f * fade));

		int legH = 12 * s;
		int bodyH = 12 * s;
		int top = groundY - legH - bodyH - 8 * s - bob;
		int skin = 0xFFE0AC8A, hair = 0xFF3B2A20, shirt = 0xFF7B2FBF, pants = 0xFF2B2D6B, shoe = 0xFF1B1530;
		// pernas
		g.fill(x - 4 * s, top + 20 * s, x, groundY - liftL, RenderUtil.alpha(pants, fade));
		g.fill(x, top + 20 * s, x + 4 * s, groundY - liftR, RenderUtil.alpha(pants, fade));
		g.fill(x - 4 * s, groundY - liftL - s, x, groundY - liftL, RenderUtil.alpha(shoe, fade));
		g.fill(x, groundY - liftR - s, x + 4 * s, groundY - liftR, RenderUtil.alpha(shoe, fade));
		// bracos
		g.fill(x - 8 * s, top + 8 * s + armL, x - 4 * s, top + 20 * s + armL, RenderUtil.alpha(skin, fade));
		g.fill(x + 4 * s, top + 8 * s + armR, x + 8 * s, top + 20 * s + armR, RenderUtil.alpha(skin, fade));
		// corpo
		g.fill(x - 4 * s, top + 8 * s, x + 4 * s, top + 20 * s, RenderUtil.alpha(shirt, fade));
		g.fill(x - 4 * s, top + 8 * s, x + 4 * s, top + 9 * s, RenderUtil.alpha(0xFFB57BEA, fade));
		// cabeca
		g.fill(x - 4 * s, top, x + 4 * s, top + 8 * s, RenderUtil.alpha(skin, fade));
		g.fill(x - 4 * s, top, x + 4 * s, top + 2 * s, RenderUtil.alpha(hair, fade));
		g.fill(x - 3 * s, top + 4 * s, x - s, top + 5 * s, RenderUtil.alpha(0xFF2B2040, fade));
		g.fill(x + s, top + 4 * s, x + 3 * s, top + 5 * s, RenderUtil.alpha(0xFF2B2040, fade));
		// chapeu de bruxa
		g.fill(x - 6 * s, top - s, x + 6 * s, top, RenderUtil.alpha(0xFF3B0F73, fade));
		g.fill(x - 3 * s, top - 4 * s, x + 3 * s, top - s, RenderUtil.alpha(0xFF6A1FB5, fade));
		g.fill(x - s, top - 7 * s, x + 2 * s, top - 4 * s, RenderUtil.alpha(0xFF6A1FB5, fade));

		// nick
		String nick = Minecraft.getInstance().getUser().getName();
		int nw = FontUtil.width(nick) + 10;
		int nx = x - nw / 2;
		int ny = top - 7 * s - 16;
		RenderUtil.roundRect(g, nx, ny, nw, 13, 4, RenderUtil.alpha(0xFF150B2E, 0.8f * fade));
		RenderUtil.roundRect(g, nx, ny + 11, nw, 2, 1, RenderUtil.alpha(0xFFB57BEA, 0.8f * fade));
		FontUtil.draw(g, nick, nx + 5, ny + 2, RenderUtil.alpha(0xFFFFFFFF, fade));
	}

	private void drawWord(GuiGraphicsExtractor g, String word, int cx, int y, int s, float glow, double sec) {
		int total = word.length() * 6 * s - s;
		int x = cx - total / 2;
		double sweep = ((sec * 0.35) % 1.6) - 0.3; // 0..1 atravessa a palavra
		for (int i = 0; i < word.length(); i++) {
			String[] gl = glyph(word.charAt(i));
			if (gl != null) {
				for (int row = 0; row < 7; row++) {
					for (int col = 0; col < 5; col++) {
						if (gl[row + 1].charAt(col) != '1') continue;
						int px = x + col * s;
						int py = y + row * s;
						// brilho suave em volta
						g.fill(px - 2, py - 2, px + s + 2, py + s + 2, RenderUtil.alpha(Theme.ACCENT, 0.16f * glow));
						// extrusao (profundidade)
						g.fill(px + 2, py + 3, px + s + 2, py + s + 3, RenderUtil.alpha(0xFF2A0F55, 0.9f));
						g.fill(px + 1, py + 2, px + s + 1, py + s + 2, RenderUtil.alpha(0xFF4B1A80, 0.9f));
					}
				}
				for (int row = 0; row < 7; row++) {
					for (int col = 0; col < 5; col++) {
						if (gl[row + 1].charAt(col) != '1') continue;
						int px = x + col * s;
						int py = y + row * s;
						double pos = ((x - (cx - total / 2.0)) + col * s) / (double) total;
						float shine = (float) Math.max(0.0, 1.0 - Math.abs(pos + row * 0.02 - sweep) * 7.0);
						int base = RenderUtil.mix(0xFFFFFFFF, 0xFFD2B0F5, row / 6f);
						g.fill(px, py, px + s, py + s, RenderUtil.mix(base, 0xFFFFFFFF, shine));
					}
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
