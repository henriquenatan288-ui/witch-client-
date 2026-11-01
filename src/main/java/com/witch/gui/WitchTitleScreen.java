package com.witch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerSkinWidget;
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

	private static final String[] LABELS = {"Singleplayer", "Multiplayer", "Options", "Client", "Quit"};
	private static final String[] SUBS = {"Your worlds, offline and private", "Servers and LAN games", "Video, audio and controls", "Witch modules and HUD", "Close the game"};
	private final LeafParticles hats = new LeafParticles();
	private long last = 0L;
	private int menuX, menuY, itemW = 176, itemH = 28;
	private final float[] hoverAnim = new float[5];

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

		// fundo: degrade quase preto com um toque de roxo
		int bands = 32;
		for (int i = 0; i < bands; i++) {
			int c = RenderUtil.mix(0xFF050505, 0xFF1C1C1C, i / (float) (bands - 1));
			g.fill(0, h * i / bands, w, h * (i + 1) / bands + 1, c);
		}
		for (int i = 0; i < STARS; i++) {
			float tw = 0.30f + 0.55f * (float) (0.5 + 0.5 * Math.sin(sec * 1.5 + SP[i]));
			g.fill(Math.round(SX[i] * w), Math.round(SY[i] * h), Math.round(SX[i] * w) + 1, Math.round(SY[i] * h) + 1, RenderUtil.alpha(0xFFFFFFFF, tw));
		}
		waves(g, w, h, sec);
		WitchVisuals.render(g, 0, 0, w, h, 0.8f);
		hats.update(speed, w, h);
		hats.render(g, 0, 0, w, h, 0.8f);

		// marca no canto
		float pulse = 0.55f + 0.45f * (float) Math.sin(sec * 2.0);
		drawWord(g, "WITCH", 38, 8, 2, pulse, sec);
		spaced(g, "CLIENT", 74, 16, 0xFFC8C8C8);

		// area do menu
		menuX = Math.max(18, w / 14);
		menuY = Math.max(78, h / 2 - itemH * 5 / 2 + 8);
		spaced(g, "WELCOME BACK", menuX, menuY - 46, 0xFF8A8A8A);
		String nick = TitleSkin.nick();
		FontUtil.draw(g, nick, menuX + 1, menuY - 33, 0xFF2A2A2A);
		FontUtil.draw(g, nick, menuX, menuY - 34, 0xFFFFFFFF);

		if (!model3d(g, w, h, sec, mx, my)) walker(g, w, h, sec);

		for (int i = 0; i < LABELS.length; i++) {
			int y = menuY + i * itemH;
			boolean hover = RenderUtil.inside(mx, my, menuX - 8, y, itemW + 16, itemH - 4);
			hoverAnim[i] += ((hover ? 1f : 0f) - hoverAnim[i]) * Math.min(1f, 0.25f * speed);
			float ha = hoverAnim[i];
			if (ha > 0.02f) {
				RenderUtil.roundRect(g, menuX - 8, y, itemW + 16, itemH - 4, 8, RenderUtil.alpha(0xFFFFFFFF, 0.07f * ha));
				RenderUtil.roundRect(g, menuX - 8, y + 4, 2, itemH - 12, 1, RenderUtil.alpha(Theme.ACCENT, ha));
			}
			int shift = Math.round(3 * ha);
			diamond(g, menuX + 3 + shift, y + 8, 3, RenderUtil.mix(0xFF8A8A8A, 0xFFFFFFFF, ha));
			FontUtil.draw(g, LABELS[i], menuX + 14 + shift, y + 3, RenderUtil.mix(0xFFD0D0D0, 0xFFFFFFFF, ha));
			FontUtil.draw(g, SUBS[i], menuX + 14 + shift, y + 3 + FontUtil.height() + 1, RenderUtil.mix(0xFF666666, 0xFFB0B0B0, ha));
		}

		spaced(g, "WITCH CLIENT  -  MINECRAFT 26.2", 8, h - 12, 0xFF555555);
		String credit = "WITCH CLIENT MADE BY BISTECK";
		spaced(g, credit, w - spacedWidth(credit) - 8, h - 12, 0xFF9A9A9A);
	}

	/** Tres linhas ondulando devagar no fundo (pontinhos), como o fundo do Real Client. */
	private static void waves(GuiGraphicsExtractor g, int w, int h, double sec) {
		for (int k = 0; k < 3; k++) {
			double base = h * (0.55 + 0.10 * k);
			double amp = h * (0.05 + 0.015 * k);
			double freq = 0.012 + 0.004 * k;
			int c = RenderUtil.alpha(0xFFCCCCCC, 0.10f + 0.05f * k);
			for (int x = 0; x < w; x += 3) {
				double y = base + Math.sin(x * freq + sec * (0.5 + 0.15 * k) + k * 2.0) * amp
						+ Math.sin(x * freq * 2.3 - sec * 0.4) * amp * 0.35;
				int yy = (int) y;
				g.fill(x, yy, x + 2, yy + 1, c);
			}
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

	private static int spacedWidth(String s) {
		int w = 0;
		for (int i = 0; i < s.length(); i++) w += FontUtil.width(String.valueOf(s.charAt(i))) + 1;
		return w;
	}

	private static void diamond(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = r - Math.abs(dy);
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	private static long walkStart = 0L;
	private float userYaw = 0f;

	private java.util.List<net.minecraft.client.model.geom.ModelPart> armParts = null;

	/** Acena com o braco direito por ~2.5s depois que o boneco chega (se o jogo deixar mexer na pose). */
	private void wave(double t, boolean arrived) {
		try {
			if (armParts == null) {
				armParts = new java.util.ArrayList<>();
				for (java.lang.reflect.Field f : PlayerSkinWidget.class.getDeclaredFields()) {
					if (!net.minecraft.client.model.Model.class.isAssignableFrom(f.getType())) continue;
					f.setAccessible(true);
					Object o = f.get(skinWidget);
					if (!(o instanceof net.minecraft.client.model.Model mdl)) continue;
					for (String n : new String[] {"right_arm", "right_sleeve"}) {
						try { armParts.add(mdl.root().getChild(n)); } catch (Throwable ignored) { }
					}
				}
			}
			double wt = t - 1.8;
			boolean on = arrived && wt >= 0 && wt < 2.6;
			for (net.minecraft.client.model.geom.ModelPart part : armParts) {
				if (on) {
					part.xRot = -2.9f;
					part.zRot = (float) (Math.sin(wt * 9.0) * 0.35 + 0.15);
				} else {
					part.resetPose();
				}
			}
		} catch (Throwable ignored) {
			armParts = new java.util.ArrayList<>();
		}
	}

	private PlayerSkinWidget skinWidget;
	private boolean skinWidgetFailed = false;
	private int skinWidgetH = -1;

	/** Modelo 3D de verdade (o mesmo do jogo): desliza da esquerda, para na direita e mostra o nick. */
	private boolean model3d(GuiGraphicsExtractor g, int w, int h, double sec, int mx, int my) {
		if (skinWidgetFailed) return false;
		try {
			int mh = Math.max(90, Math.min(h - 70, (int) (h * 0.62)));
			int mw = (int) (mh * 0.62);
			if (skinWidget == null || skinWidgetH != mh) {
				skinWidget = new PlayerSkinWidget(mw, mh, Minecraft.getInstance().getEntityModels(), TitleSkin::skin);
				skinWidgetH = mh;
			}
			if (walkStart == 0L) walkStart = System.nanoTime();
			double t = (System.nanoTime() - walkStart) / 1_000_000_000.0;
			double k = Math.min(1.0, t / 1.6);
			double ease = 1.0 - Math.pow(1.0 - k, 3);
			int stopX = (int) (w * 0.68) - mw / 2;
			int x = (int) (w + (stopX - w) * ease);
			int y = h - 24 - mh;
			skinWidget.setX(x);
			skinWidget.setY(y);

			int groundY = h - 20;
			g.fill(0, groundY, w, groundY + 1, RenderUtil.alpha(0xFFC8C8C8, 0.40f));
			g.fill(0, groundY + 1, w, groundY + 3, RenderUtil.alpha(0xFF1E1E1E, 0.7f));
			int cx = x + mw / 2;
			RenderUtil.roundRect(g, cx - mw / 2, groundY - 2, mw, 4, 2, RenderUtil.alpha(0xFF000000, 0.45f));
			if (k >= 1.0) {
				for (int r = 6; r >= 1; r--) {
					RenderUtil.roundRect(g, cx - r * 12, y + mh / 8 - r * 3, r * 24, mh * 3 / 4 + r * 6, r * 12,
							RenderUtil.alpha(0xFFFFFFFF, 0.018f));
				}
				for (int q = 0; q < 8; q++) {
					double a = sec * 0.6 + q * 0.785;
					int sx = (int) (cx + Math.cos(a) * (mw * 0.55 + (q % 3) * 6));
					int sy = (int) (y + mh * 0.5 + Math.sin(a * 1.3) * (mh * 0.38 + (q % 2) * 6));
					float tw = 0.4f + 0.6f * (float) (0.5 + 0.5 * Math.sin(sec * 2.0 + q));
					int c = RenderUtil.alpha(0xFFFFFFFF, tw);
					g.fill(sx - 2, sy, sx + 3, sy + 1, c);
					g.fill(sx, sy - 2, sx + 1, sy + 3, c);
				}
			}
			wave(t, k >= 1.0);
			skinWidget.extractRenderState(g, mx, my, 0f);
			if (k >= 1.0) {
				float na = (float) Math.min(1.0, Math.max(0.0, (t - 1.8) / 0.4));
				if (na > 0f) {
					String nick = TitleSkin.nick();
					int nw = FontUtil.width(nick) + 12;
					int nx = cx - nw / 2;
					int ny = y - 14 + Math.round((1f - na) * 6f);
					RenderUtil.roundRect(g, nx, ny, nw, 14, 4, RenderUtil.alpha(0xFF0C0C0C, 0.85f * na));
					RenderUtil.roundRect(g, nx, ny + 12, nw, 2, 1, RenderUtil.alpha(0xFFC8C8C8, 0.9f * na));
					FontUtil.draw(g, nick, nx + 6, ny + 3, RenderUtil.alpha(0xFFFFFFFF, na));
				}
			}
			return true;
		} catch (Throwable e) {
			skinWidgetFailed = true;
			return false;
		}
	}

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
		int stopX = (int) (w * 0.66);
		double speed = 30.0 * s;
		double dist = speed * t;
		double startX = -8.0 * s;
		boolean walking = startX + dist < stopX;
		double cx = Math.min(stopX, startX + dist);
		double turnT = (stopX - startX) / speed;
		double since = t - turnT;

		g.fill(0, groundY, w, groundY + 1, RenderUtil.alpha(0xFFC8C8C8, 0.40f));
		g.fill(0, groundY + 1, w, groundY + 3, RenderUtil.alpha(0xFF1E1E1E, 0.7f));
		RenderUtil.roundRect(g, (int) cx - 7 * s, groundY - 1, 14 * s, 2, 1, RenderUtil.alpha(0xFF000000, 0.4f));

		if (!walking) {
			for (int r = 6; r >= 1; r--) {
				RenderUtil.roundRect(g, (int) cx - r * 5 * s, groundY - 36 * s - r * 3, r * 10 * s, 36 * s + r * 6, r * 5 * s,
						RenderUtil.alpha(0xFFFFFFFF, 0.018f));
			}
			for (int k = 0; k < 8; k++) {
				double a = sec * 0.6 + k * 0.785;
				int sx = (int) (cx + Math.cos(a) * (14 + (k % 3) * 5) * s);
				int sy = (int) (groundY - 18 * s + Math.sin(a * 1.3) * (16 + (k % 2) * 6) * s);
				float tw = 0.4f + 0.6f * (float) (0.5 + 0.5 * Math.sin(sec * 2.0 + k));
				int c = RenderUtil.alpha(0xFFFFFFFF, tw);
				g.fill(sx - 2, sy, sx + 3, sy + 1, c);
				g.fill(sx, sy - 2, sx + 1, sy + 3, c);
			}
		}
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
				RenderUtil.roundRect(g, nx, ny, nw, 14, 4, RenderUtil.alpha(0xFF0C0C0C, 0.85f * na));
				RenderUtil.roundRect(g, nx, ny + 12, nw, 2, 1, RenderUtil.alpha(0xFFC8C8C8, 0.9f * na));
				FontUtil.draw(g, nick, nx + 6, ny + 3, RenderUtil.alpha(0xFFFFFFFF, na));
			}
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (click.x() > this.width * 0.55) {
			userYaw += (float) (offsetX * 0.03);
			if (skinWidget != null) {
				try { skinWidget.mouseDragged(click, offsetX, offsetY); } catch (Throwable ignored) { }
			}
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
						g.fill(px + 1, py + 1, px + s + 1, py + s + 1, RenderUtil.alpha(0xFF3A3A3A, 0.95f));
					}
				}
				for (int row = 0; row < 7; row++) {
					for (int col = 0; col < 5; col++) {
						if (gl[row + 1].charAt(col) != '1') continue;
						int px = x + col * s;
						int py = y + row * s;
						double pos = ((x - (cx - total / 2.0)) + col * s) / (double) total;
						float shine = (float) Math.max(0.0, 1.0 - Math.abs(pos + row * 0.02 - sweep) * 7.0);
						int base = RenderUtil.mix(0xFFFFFFFF, 0xFFFFFFFF, row / 6f);
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
				int y = menuY + i * itemH;
				if (RenderUtil.inside(click.x(), click.y(), menuX - 8, y, itemW + 16, itemH - 4)) {
					Minecraft mc = Minecraft.getInstance();
					switch (i) {
						case 0 -> mc.gui.setScreen(new SelectWorldScreen(this));
						case 1 -> mc.gui.setScreen(new JoinMultiplayerScreen(this));
						case 2 -> mc.gui.setScreen(new OptionsScreen(this, mc.options, false));
						case 3 -> mc.gui.setScreen(new ClickGuiScreen());
						default -> mc.stop();
					}
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}
}
