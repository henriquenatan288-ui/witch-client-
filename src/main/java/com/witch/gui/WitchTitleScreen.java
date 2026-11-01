package com.witch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
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
		walker(g, w, h, sec);
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

	private static long walkStart = 0L;
	private float userYaw = 0f;

	/** Uma face do cubo da skin, desenhada com projecao ortografica (largura = projecao da aresta), com cisalhamento por linha pros membros. */
	private void face(GuiGraphicsExtractor g, net.minecraft.resources.Identifier tex, double ox, int oy, int s,
					  double e0, double e1, int u, int v, int fw, int fh, double shearPerRow) {
		int xL = (int) Math.round(ox + Math.min(e0, e1) * s);
		int xR = (int) Math.round(ox + Math.max(e0, e1) * s);
		int dw = xR - xL;
		if (dw <= 0) return;
		if (Math.abs(shearPerRow) < 1.0E-4) {
			g.blit(RenderPipelines.GUI_TEXTURED, tex, xL, oy, (float) u, (float) v, dw, fh * s, fw, fh, 64, 64);
			return;
		}
		for (int r = 0; r < fh; r++) {
			int off = (int) Math.round(shearPerRow * (r + 0.5) * s);
			g.blit(RenderPipelines.GUI_TEXTURED, tex, xL + off, oy + r * s, (float) u, (float) (v + r), dw, s, fw, 1, 64, 64);
		}
	}

	/** Caixa da skin (base + camada externa) girada em yaw: mostra frente/costas e a lateral que estiver virada pra camera. */
	private void box(GuiGraphicsExtractor g, net.minecraft.resources.Identifier tex, double ox, int oy, int s,
					 double partX, int w, int h, int d, int u0, int v0, int u1, int v1, double yaw, double shearPerRow) {
		double c = Math.cos(yaw);
		double sn = Math.sin(yaw);
		double base = ox + partX * c * s;
		double shear = shearPerRow * sn;
		for (int layer = 0; layer < 2; layer++) {
			int u = layer == 0 ? u0 : u1;
			int v = layer == 0 ? v0 : v1;
			if (c > 0.001) {
				face(g, tex, base, oy, s, -(w / 2.0) * c + (d / 2.0) * sn, (w / 2.0) * c + (d / 2.0) * sn, u + d, v + d, w, h, shear);
			} else if (c < -0.001) {
				face(g, tex, base, oy, s, (w / 2.0) * c - (d / 2.0) * sn, -(w / 2.0) * c - (d / 2.0) * sn, u + 2 * d + w, v + d, w, h, shear);
			}
			if (sn > 0.001) {
				face(g, tex, base, oy, s, -(w / 2.0) * c - (d / 2.0) * sn, -(w / 2.0) * c + (d / 2.0) * sn, u, v + d, d, h, shear);
			} else if (sn < -0.001) {
				face(g, tex, base, oy, s, (w / 2.0) * c + (d / 2.0) * sn, (w / 2.0) * c - (d / 2.0) * sn, u + d + w, v + d, d, h, shear);
			}
		}
	}

	/** Modelo 3D da skin: anda de lado ate a direita, gira pra frente e fica parado girando de leve (arraste pra girar). */
	private void walker(GuiGraphicsExtractor g, int w, int h, double sec) {
		var tex = TitleSkin.texture();
		if (tex == null) return;
		if (walkStart == 0L) walkStart = System.nanoTime();
		double t = (System.nanoTime() - walkStart) / 1_000_000_000.0;

		int s = Math.max(1, Math.min(4, h / 100));
		int groundY = h - 20;
		int stopX = Math.min(w - 14 * s, Math.max(bx + bw + 40 + 8 * s, w - 26 * s));
		double speed = 30.0 * s;
		double dist = speed * t;
		double startX = -8.0 * s;
		boolean walking = startX + dist < stopX;
		double cx = Math.min(stopX, startX + dist);
		double turnT = (stopX - startX) / speed;
		double since = t - turnT;

		g.fill(0, groundY, w, groundY + 1, RenderUtil.alpha(0xFFB57BEA, 0.40f));
		g.fill(0, groundY + 1, w, groundY + 3, RenderUtil.alpha(0xFF2A1055, 0.7f));
		RenderUtil.roundRect(g, (int) cx - 7 * s, groundY - 1, 14 * s, 2, 1, RenderUtil.alpha(0xFF000000, 0.4f));

		double yaw;
		double armPhase = 0, legPhase = 0;
		int bob = 0;
		if (walking) {
			yaw = Math.PI / 2;
			double ph = dist / (7.0 * s) * Math.PI;
			armPhase = legPhase = Math.sin(ph) * 0.6;
			bob = (int) Math.round(Math.abs(Math.sin(ph)) * s * 0.7);
		} else {
			double k = Math.min(1.0, since / 0.7);
			double ease = k * k * (3 - 2 * k);
			double wiggle = (k >= 1.0 ? 0.35 * Math.sin(since * 0.9) : 0.0);
			yaw = (Math.PI / 2) * (1.0 - ease) + wiggle + userYaw;
			bob = (int) Math.round(Math.sin(sec * 2.2) * 0.6 * s);
		}
		int top = groundY - 32 * s - bob;
		int armW = TitleSkin.slim() ? 3 : 4;
		double sn = Math.sin(yaw);

		// partes: {x do centro (unidades), y do topo, w, h, d, u base, v base, u camada, v camada, fase do membro}
		double[][] parts = {
				{-(4 + armW / 2.0), 8, armW, 12, 4, 40, 16, 40, 32, -armPhase},   // braco direito
				{(4 + armW / 2.0), 8, armW, 12, 4, 32, 48, 48, 48, armPhase},     // braco esquerdo
				{-2, 20, 4, 12, 4, 0, 16, 0, 32, armPhase},                       // perna direita
				{2, 20, 4, 12, 4, 16, 48, 0, 48, -armPhase},                      // perna esquerda
				{0, 8, 8, 12, 4, 16, 16, 16, 32, 0},                              // corpo
				{0, 0, 8, 8, 8, 0, 0, 32, 0, 0},                                  // cabeca
		};
		// mais longe primeiro (profundidade z' = -x * sen)
		java.util.Arrays.sort(parts, java.util.Comparator.comparingDouble(a -> -a[0] * sn));
		for (double[] pt : parts) {
			double shear = Math.tan(pt[9]);
			box(g, tex, cx, top + (int) pt[1] * s, s, pt[0], (int) pt[2], (int) pt[3], (int) pt[4],
					(int) pt[5], (int) pt[6], (int) pt[7], (int) pt[8], yaw, shear);
		}

		if (!walking) {
			float na = (float) Math.min(1.0, Math.max(0.0, (since - 0.5) / 0.4));
			if (na > 0f) {
				String nick = TitleSkin.nick();
				int nw = FontUtil.width(nick) + 12;
				int nx = (int) cx - nw / 2;
				int ny = top - 20 + Math.round((1f - na) * 6f);
				RenderUtil.roundRect(g, nx, ny, nw, 14, 4, RenderUtil.alpha(0xFF150B2E, 0.85f * na));
				RenderUtil.roundRect(g, nx, ny + 12, nw, 2, 1, RenderUtil.alpha(0xFFB57BEA, 0.9f * na));
				FontUtil.draw(g, nick, nx + 6, ny + 3, RenderUtil.alpha(0xFFFFFFFF, na));
			}
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (click.x() > this.width * 0.55) {
			userYaw += (float) (offsetX * 0.03);
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
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
