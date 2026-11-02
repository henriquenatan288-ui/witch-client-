package com.witch.gui;

import com.witch.config.Config;
import com.witch.module.Category;
import com.witch.module.Module;
import com.witch.module.ModuleManager;
import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import com.witch.setting.KeybindSetting;
import com.witch.setting.OreSetting;
import com.witch.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** ClickGUI estilo Meteor: uma janela arrastavel por categoria, modulos em linhas, ajustes abrem dentro da janela. Monocromatico. */
public class ClickGuiScreen extends Screen {
	private static final int WIN_W = 100;
	private static final int GAP = 5;
	private static final int HEAD_H = 15;
	private static final int ROW = 13;
	private static final int INT_H = 22;
	private static final int ORE_H = 13;
	private static final int SW = 8;
	private static final int SW_GAP = 3;
	private static final int ARROW_W = 18;

	private static final int BG = 0xFF111111;
	private static final int HEAD = 0xFF1A1A1A;
	private static final int LINE = 0xFF3A3A3A;
	private static final int ROW_ON = 0xFF262626;
	private static final int ROW_HOVER = 0xFF1B1B1B;
	private static final int SUB_BG = 0xFF0B0B0B;
	private static final int TXT = 0xFFFFFFFF;
	private static final int TXT_DIM = 0xFF8E8E8E;

	// posicao e rolagem das janelas lembradas entre aberturas
	private static final Map<Category, int[]> POS = new EnumMap<>(Category.class);
	private static final Map<Category, float[]> SCR = new EnumMap<>(Category.class);
	private static final List<Category> ORDER = new ArrayList<>();
	private static final Set<Module> OPEN = new HashSet<>();

	private Category dragWin = null;
	private int dragOffX, dragOffY;
	private Category scrollWin = null;
	private Category sbWin = null;
	private static final int SB_VIS = 4;
	private static final int SB_HIT = 12;
	private double dragAccum = 0;
	private Runnable pending = null;
	private IntSetting slider = null;
	private int sliderX = 0, sliderW = 1;

	private String search = "";
	private boolean searchFocused = false;
	private float openAnim = 0f;
	private float openT = 0f;
	private int curYOff = 0;
	private int curXOff = 0;
	private float parX = 0f, parY = 0f;
	private static final int BOT = 4;








	private static void sweep(GuiGraphicsExtractor g, int x0, int x1, int y, float a, int seed, boolean rev) {
		float t = (System.nanoTime() / 1_000_000_000f) * 0.4f + seed * 0.23f;
		float f = t - (float) Math.floor(t);
		if (rev) f = 1f - f;
		int segW = 34;
		int cx = x0 + Math.round(f * ((x1 - x0) + segW)) - segW / 2;
		for (int i = 0; i < segW; i++) {
			int px = cx - segW / 2 + i;
			if (px < x0 || px >= x1) continue;
			float al = 1f - Math.abs(i - segW / 2f) / (segW / 2f);
			g.fill(px, y, px + 1, y + 1, RenderUtil.alpha(0xFFFFFFFF, a * al * 0.8f));
		}
	}
	private final Map<Category, int[]> drawOff = new EnumMap<>(Category.class);

	/** posicao onde a janela esta desenhada agora (com o efeito de profundidade e a animacao de entrada). */
	private int[] at(Category c) {
		int[] p = pos(c);
		int[] o = drawOff.get(c);
		return o == null ? new int[] {p[0], p[1]} : new int[] {p[0] + o[0], p[1] + o[1]};
	}
	private long lastNanos = 0L;

	public ClickGuiScreen() {
		this(null);
	}

	/** Se 'openSettings' nao for nulo, a janela ja abre com os ajustes desse modulo expandidos. */
	public ClickGuiScreen(Module openSettings) {
		super(Component.literal("Witch Client"));
		if (openSettings != null && !openSettings.settings().isEmpty()) OPEN.add(openSettings);
		if (ORDER.isEmpty()) {
			for (Category c : Category.values()) ORDER.add(c);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int[] pos(Category c) {
		int[] p = POS.get(c);
		if (p == null) {
			int n = Category.values().length;
			int ww = winWCache;
			int total = ww * n + GAP * (n - 1);
			int x0 = Math.max(4, (this.width - total) / 2);
			p = new int[] {x0 + c.ordinal() * (ww + GAP), 26};
			POS.put(c, p);
		}
		return p;
	}

	private static int lastW = -1, lastH = -1;
	private int winWCache = WIN_W;

	@Override
	protected void init() {
		super.init();
		if (lastW != this.width || lastH != this.height) {
			POS.clear();
			lastW = this.width;
			lastH = this.height;
		}
		updateWinW();
	}

	private int winW() {
		return winWCache;
	}

	private void updateWinW() {
		int n = Category.values().length;
		int mx = 0;
		for (Module m : ModuleManager.all()) mx = Math.max(mx, FontUtil.width(m.name));
		int want = Math.max(WIN_W, mx + 26);
		int cap = (this.width - 4 - GAP * (n - 1)) / n;
		winWCache = Math.max(70, Math.min(want, cap));
	}

	private List<Module> modules(Category c) {
		List<Module> out = new ArrayList<>();
		String q = search.toLowerCase(Locale.ROOT);
		for (Module m : ModuleManager.byCategory(c)) {
			if (q.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
		}
		return out;
	}

	private static int setH(Setting s, int w) {
		if (s instanceof IntSetting) return INT_H;
		if (s instanceof ColorSetting) {
			int per = Math.max(1, (w - 16) / (SW + SW_GAP));
			int rows = (ColorSetting.PALETTE.length + per - 1) / per;
			return 12 + rows * (SW + SW_GAP) + 3;
		}
		return ORE_H;
	}

	private int contentH(Category c, int w) {
		int h = 0;
		for (Module m : modules(c)) {
			h += ROW;
			if (OPEN.contains(m)) {
				for (Setting s : m.settings()) h += setH(s, w);
			}
		}
		return h;
	}

	private int viewH(Category c, int w) {
		int max = Math.max(40, this.height - pos(c)[1] - 6 - HEAD_H);
		return Math.min(contentH(c, w), max);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		long now = System.nanoTime();
		float speed = lastNanos == 0 ? 1f : Math.min(3f, (now - lastNanos) / 16_666_667f);
		lastNanos = now;
		updateWinW();
		openAnim = Mth.lerp(Math.min(1f, speed * 0.2f), openAnim, 1f);
		openT += speed / 60f;
		float a = openAnim;

		g.fill(0, 0, this.width, this.height, RenderUtil.alpha(0x99000000, a));

		// busca no topo
		int sw = Math.min(150, this.width - 20);
		int sx = (this.width - sw) / 2;
		g.fill(sx - 1, 4, sx + sw + 1, 19, RenderUtil.alpha(LINE, a));
		g.fill(sx, 5, sx + sw, 18, RenderUtil.alpha(0xFF0C0C0C, a));
		String shown;
		int col;
		if (search.isEmpty() && !searchFocused) {
			shown = "Search modules...";
			col = 0xFF666666;
		} else {
			boolean blink = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0;
			shown = search + (blink ? "|" : "");
			col = TXT;
		}
		FontUtil.draw(g, shown, sx + 6, 5 + (13 - FontUtil.height()) / 2, RenderUtil.alpha(col, a));

		float tx = (mx - this.width / 2f) / Math.max(1, this.width);
		float ty = (my - this.height / 2f) / Math.max(1, this.height);
		parX = Mth.lerp(Math.min(1f, speed * 0.12f), parX, tx);
		parY = Mth.lerp(Math.min(1f, speed * 0.12f), parY, ty);
		for (Category c : ORDER) {
			float k = Mth.clamp((openT - c.ordinal() * 0.09f) / 0.45f, 0f, 1f);
			float u = k - 1f;
			float e = 1f + 2.4f * u * u * u + 1.4f * u * u; // easeOutBack: passa um pouco e volta
			curYOff = Math.round((1f - e) * 22f);
			float depth = 2f + c.ordinal();
			curXOff = Math.round(parX * depth * 6f);
			curYOff += Math.round(parY * depth * 4f);
			renderWindow(g, c, mx, my, speed, a * Math.min(1f, k * 1.6f));
		}
	}

	private static String fit(String t, int maxW) {
		if (FontUtil.width(t) <= maxW) return t;
		String r = t;
		while (r.length() > 1 && FontUtil.width(r + "..") > maxW) r = r.substring(0, r.length() - 1);
		return r.trim() + "..";
	}


	private static void vgrad(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bot, float a) {
		if (h <= 0) return;
		int n = Math.max(1, Math.min(h, 24));
		for (int i = 0; i < n; i++) {
			int y0 = y + h * i / n;
			int y1 = y + h * (i + 1) / n;
			g.fill(x, y0, x + w, y1, RenderUtil.alpha(RenderUtil.mix(top, bot, i / (float) Math.max(1, n - 1)), a));
		}
	}

	private void renderWindow(GuiGraphicsExtractor g, Category c, int mx, int my, float speed, float a) {
		int[] p = pos(c);
		int w = winW();
		int vh = viewH(c, w);
		int ch = contentH(c, w);
		int x = p[0] + curXOff;
		int y = p[1] + curYOff;
		drawOff.put(c, new int[] {curXOff, curYOff});
		float maxScroll = Math.max(0, ch - vh);
		float[] sc = SCR.computeIfAbsent(c, k -> new float[] {0f});
		sc[0] = Mth.clamp(sc[0], 0, maxScroll);

		int totalH = HEAD_H + vh + BOT;
		boolean lifted = dragWin == c;
		int lift = lifted ? 3 : 0;
		for (int k = 5; k >= 1; k--) {
			RenderUtil.roundRect(g, x - k - curXOff / 2, y - k + 2 + lift + k / 2, w + 2 * k, totalH + 2 * k, 6 + k,
					RenderUtil.alpha(0xFF000000, a * (lifted ? 0.09f : 0.06f)));
		}
		RenderUtil.roundRect(g, x - 2, y - 2, w + 4, totalH + 4, 8, RenderUtil.alpha(0xFFFFFFFF, a * 0.04f));
		RenderUtil.roundRect(g, x - 1, y - 1, w + 2, totalH + 2, 7, RenderUtil.alpha(LINE, a));
		sweep(g, x + 6, x + w - 6, y - 1, a, c.ordinal(), false);
		sweep(g, x + 6, x + w - 6, y + totalH, a, c.ordinal(), true);
		RenderUtil.roundRect(g, x, y, w, totalH, 6, RenderUtil.alpha(0xFF121212, a));
		RenderUtil.roundRect(g, x, y, w, HEAD_H, 6, RenderUtil.alpha(0xFF232323, a));
		g.fill(x, y + HEAD_H - 6, x + w, y + HEAD_H, RenderUtil.alpha(0xFF232323, a));
		g.fill(x + 5, y + 1, x + w - 5, y + 2, RenderUtil.alpha(0xFFFFFFFF, a * 0.16f));
		g.fill(x, y + HEAD_H - 1, x + w, y + HEAD_H, RenderUtil.alpha(0xFF000000, a * 0.6f));
		String title = c.label;
		FontUtil.draw(g, title, x + (w - FontUtil.width(title)) / 2, y + (HEAD_H - 1 - FontUtil.height()) / 2, RenderUtil.alpha(TXT, a));

		int by = y + HEAD_H;
		g.enableScissor(x, by, x + w, by + vh);
		int ry = by - Math.round(sc[0]);
		int fh = FontUtil.height();
		for (Module m : modules(c)) {
			boolean hover = RenderUtil.inside(mx, my, x, ry, w, ROW) && RenderUtil.inside(mx, my, x, by, w, vh);
			m.toggleAnim = Mth.lerp(Math.min(1f, speed * 0.3f), m.toggleAnim, m.isEnabled() ? 1f : 0f);
			if (m.toggleAnim > 0.02f) {
				int sl = 20;
				for (int q = 0; q < sl; q++) {
					int x0 = x + w * q / sl;
					int x1 = x + w * (q + 1) / sl;
					g.fill(x0, ry, x1, ry + ROW, RenderUtil.alpha(0xFFFFFFFF, a * m.toggleAnim * (0.16f - 0.14f * q / (sl - 1f))));
				}
				g.fill(x, ry, x + 2, ry + ROW, RenderUtil.alpha(TXT, a * m.toggleAnim));
			}
			m.hoverAnim = Mth.lerp(Math.min(1f, speed * 0.3f), m.hoverAnim, hover ? 1f : 0f);
			if (m.hoverAnim > 0.02f && m.toggleAnim < 0.5f) {
				g.fill(x, ry, x + w, ry + ROW, RenderUtil.alpha(0xFFFFFFFF, a * 0.08f * m.hoverAnim));
			}
			int tc = RenderUtil.mix(TXT_DIM, TXT, m.toggleAnim);
			FontUtil.draw(g, fit(m.name, w - 7 - 11), x + 7, ry + (ROW - fh) / 2, RenderUtil.alpha(tc, a));
			boolean open = OPEN.contains(m);
			if (!m.settings().isEmpty()) {
				String ar = open ? "-" : "+";
				FontUtil.draw(g, ar, x + w - 11, ry + (ROW - fh) / 2, RenderUtil.alpha(open ? TXT : TXT_DIM, a));
			}
			ry += ROW;
			if (open) {
				for (Setting s : m.settings()) {
					int sh = setH(s, w);
					g.fill(x, ry, x + w, ry + sh, RenderUtil.alpha(SUB_BG, a));
					g.fill(x, ry, x + 1, ry + sh, RenderUtil.alpha(LINE, a));
					drawSetting(g, s, x, ry, w, sh, mx, my, by, vh, a);
					ry += sh;
				}
			}
		}
		g.disableScissor();

		// barra de rolagem: trilho fraco + alca (arrastavel)
		if (maxScroll > 0) {
			int th = Math.max(16, Math.round(vh * (vh / (float) ch)));
			int ty = by + Math.round((vh - th) * (sc[0] / maxScroll));
			int bx = x + w - SB_VIS - 1;
			g.fill(bx, by + 1, bx + SB_VIS, by + vh - 1, RenderUtil.alpha(0xFFFFFFFF, a * 0.06f));
			g.fill(bx, ty, bx + SB_VIS, ty + th, RenderUtil.alpha(sbWin == c ? 0xFFFFFFFF : 0xFFA8A8A8, a * 0.9f));
		}
	}

	private void drawSetting(GuiGraphicsExtractor g, Setting s, int x, int ry, int w, int sh, int mx, int my, int by, int vh, float a) {
		int fh = FontUtil.height();
		if (s instanceof IntSetting is) {
			FontUtil.draw(g, is.name, x + 7, ry + 3, RenderUtil.alpha(TXT_DIM, a));
			String val = is.get() + is.suffix;
			FontUtil.draw(g, val, x + w - 6 - FontUtil.width(val), ry + 3, RenderUtil.alpha(TXT, a));
			int tx = x + 7;
			int tw = w - 14;
			int ty = ry + fh + 8;
			g.fill(tx, ty, tx + tw, ty + 3, RenderUtil.alpha(0xFF2A2A2A, a));
			int fw = Math.round(tw * is.fraction());
			if (fw > 0) g.fill(tx, ty, tx + fw, ty + 3, RenderUtil.alpha(TXT, a));
			g.fill(tx + fw - 1, ty - 2, tx + fw + 1, ty + 5, RenderUtil.alpha(TXT, a));
		} else if (s instanceof ColorSetting cs) {
			FontUtil.draw(g, cs.name, x + 7, ry + 3, RenderUtil.alpha(TXT_DIM, a));
			int per = Math.max(1, (w - 16) / (SW + SW_GAP));
			for (int i = 0; i < ColorSetting.PALETTE.length; i++) {
				int sx = x + 7 + (i % per) * (SW + SW_GAP);
				int sy = ry + 13 + (i / per) * (SW + SW_GAP);
				if (i == cs.index()) g.fill(sx - 1, sy - 1, sx + SW + 1, sy + SW + 1, RenderUtil.alpha(TXT, a));
				g.fill(sx, sy, sx + SW, sy + SW, RenderUtil.alpha(ColorSetting.PALETTE[i], a));
			}
		} else if (s instanceof KeybindSetting ks) {
			FontUtil.draw(g, ks.name, x + 7, ry + (sh - fh) / 2, RenderUtil.alpha(TXT_DIM, a));
			String lab = ks.listening ? "..." : ks.label();
			int cw = Math.max(26, FontUtil.width(lab) + 10);
			int cx = x + w - 6 - cw;
			g.fill(cx, ry + 2, cx + cw, ry + sh - 2, RenderUtil.alpha(ks.listening ? 0xFF4A4A4A : 0xFF222222, a));
			FontUtil.draw(g, lab, cx + (cw - FontUtil.width(lab)) / 2, ry + (sh - fh) / 2, RenderUtil.alpha(TXT, a));
		} else if (s instanceof OreSetting os) {
			int bx = x + 7;
			int bY = ry + (sh - 9) / 2;
			g.fill(bx, bY, bx + 9, bY + 9, RenderUtil.alpha(0xFF3A3A3A, a));
			g.fill(bx + 1, bY + 1, bx + 8, bY + 8, RenderUtil.alpha(0xFF0C0C0C, a));
			if (os.enabled()) g.fill(bx + 2, bY + 2, bx + 7, bY + 7, RenderUtil.alpha(TXT, a));
			FontUtil.draw(g, os.name, bx + 14, ry + (sh - fh) / 2, RenderUtil.alpha(os.enabled() ? TXT : TXT_DIM, a));
			int cx = x + w - 6 - 12;
			g.fill(cx - 1, bY - 1, cx + 13, bY + 10, RenderUtil.alpha(0xFF3A3A3A, a));
			g.fill(cx, bY, cx + 12, bY + 9, RenderUtil.alpha(os.color(), a));
		}
	}

	private Category winAt(double mx, double my) {
		for (int i = ORDER.size() - 1; i >= 0; i--) {
			Category c = ORDER.get(i);
			int[] p = at(c);
			int w = winW();
			if (RenderUtil.inside(mx, my, p[0], p[1], w, HEAD_H + viewH(c, w))) return c;
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mx = click.x();
		double my = click.y();
		int button = click.button();
		dragAccum = 0;
		pending = null;

		int sw = Math.min(150, this.width - 20);
		int sx = (this.width - sw) / 2;
		searchFocused = RenderUtil.inside(mx, my, sx, 5, sw, 13);
		if (searchFocused) return true;
		Category c = winAt(mx, my);
		if (c == null) return super.mouseClicked(click, doubled);
		ORDER.remove(c);
		ORDER.add(c);

		int[] p = at(c);
		int w = winW();
		int x = p[0];
		int y = p[1];
		if (my < y + HEAD_H) {
			dragWin = c;
			dragOffX = (int) mx - x;
			dragOffY = (int) my - y;
			return true;
		}
		float[] sc0 = SCR.computeIfAbsent(c, k -> new float[] {0f});
		if (contentH(c, w) > viewH(c, w) && mx >= x + w - SB_HIT && button == 0) {
			sbWin = c;
			dragSb(c, my);
			return true;
		}
		scrollWin = c;
		float[] sc = SCR.computeIfAbsent(c, k -> new float[] {0f});
		int ry = y + HEAD_H - Math.round(sc[0]);
		int fh = FontUtil.height();
		for (Module m : modules(c)) {
			if (my >= ry && my < ry + ROW) {
				boolean has = !m.settings().isEmpty();
				boolean onArrow = has && mx >= x + w - ARROW_W;
				if (has && (button == 1 || onArrow)) {
					pending = () -> { if (!OPEN.remove(m)) OPEN.add(m); };
				} else if (button == 0) {
					pending = () -> { m.toggle(); Config.save(); };
				}
				return true;
			}
			ry += ROW;
			if (OPEN.contains(m)) {
				for (Setting s : m.settings()) {
					int sh = setH(s, w);
					if (my >= ry && my < ry + sh) {
						clickSetting(s, x, ry, w, sh, mx, my, fh);
						return true;
					}
					ry += sh;
				}
			}
		}
		return true;
	}

	private void clickSetting(Setting s, int x, int ry, int w, int sh, double mx, double my, int fh) {
		if (s instanceof IntSetting is) {
			int tx = x + 7;
			int tw = w - 14;
			int ty = ry + fh + 8;
			if (RenderUtil.inside(mx, my, tx - 4, ty - 7, tw + 8, 17)) {
				slider = is;
				sliderX = tx;
				sliderW = Math.max(1, tw);
				is.setFraction((float) ((mx - tx) / sliderW));
			}
		} else if (s instanceof ColorSetting cs) {
			int per = Math.max(1, (w - 16) / (SW + SW_GAP));
			for (int i = 0; i < ColorSetting.PALETTE.length; i++) {
				int sx = x + 7 + (i % per) * (SW + SW_GAP);
				int sy = ry + 13 + (i / per) * (SW + SW_GAP);
				if (RenderUtil.inside(mx, my, sx - 2, sy - 2, SW + 4, SW + 4)) {
					final int idx = i;
					pending = () -> { cs.setIndex(idx); Config.save(); };
					return;
				}
			}
		} else if (s instanceof KeybindSetting ks) {
			pending = () -> ks.listening = !ks.listening;
		} else if (s instanceof OreSetting os) {
			final boolean onChip = mx >= x + w - 6 - 16;
			pending = () -> {
				if (onChip) os.cycleColor(); else os.toggle();
				Config.save();
			};
		}
	}

	private void dragSb(Category c, double my) {
		int w = winW();
		int vh = viewH(c, w);
		int th = Math.max(16, Math.round(vh * (vh / (float) contentH(c, w))));
		float span = Math.max(1f, vh - th);
		float f = Mth.clamp((float) ((my - (at(c)[1] + HEAD_H) - th / 2.0) / span), 0f, 1f);
		float[] sc = SCR.computeIfAbsent(c, k -> new float[] {0f});
		sc[0] = f * Math.max(0, contentH(c, w) - vh);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (sbWin != null) {
			dragSb(sbWin, click.y());
			return true;
		}
		if (dragWin != null) {
			int[] p = pos(dragWin);
			int[] o = drawOff.getOrDefault(dragWin, new int[] {0, 0});
			p[0] = Mth.clamp((int) click.x() - dragOffX - o[0], -winW() + 20, this.width - 20);
			p[1] = Mth.clamp((int) click.y() - dragOffY - o[1], 0, this.height - HEAD_H);
			dragAccum += 100;
			return true;
		}
		if (slider != null) {
			slider.setFraction((float) ((click.x() - sliderX) / sliderW));
			return true;
		}
		if (scrollWin != null) {
			dragAccum += Math.abs(offsetX) + Math.abs(offsetY);
			float[] sc = SCR.computeIfAbsent(scrollWin, k -> new float[] {0f});
			sc[0] -= (float) offsetY;
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		boolean was = sbWin != null || dragWin != null || slider != null || scrollWin != null || pending != null;
		if (slider != null) Config.save();
		if (pending != null && dragAccum < 6) pending.run();
		pending = null;
		sbWin = null;
		dragWin = null;
		slider = null;
		scrollWin = null;
		dragAccum = 0;
		return was || super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		Category c = winAt(mouseX, mouseY);
		if (c != null) {
			float[] sc = SCR.computeIfAbsent(c, k -> new float[] {0f});
			sc[0] -= (float) (verticalAmount * 14.0);
		}
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		int cp = input.codepoint();
		if (searchFocused && cp >= 32 && cp != 127 && search.length() < 24) {
			search += new String(Character.toChars(cp));
			return true;
		}
		return super.charTyped(input);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		int key = input.key();
		for (Module m : OPEN) {
			for (Setting st : m.settings()) {
				if (st instanceof KeybindSetting ks && ks.listening) {
					ks.listening = false;
					if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
						ks.setKey(0);
					} else if (key != GLFW.GLFW_KEY_ESCAPE) {
						ks.setKey(key);
					}
					Config.save();
					return true;
				}
			}
		}
		if (searchFocused) {
			if (key == GLFW.GLFW_KEY_BACKSPACE) {
				if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
				return true;
			}
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				searchFocused = false;
				return true;
			}
		}
		if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			this.onClose();
			return true;
		}
		return super.keyPressed(input);
	}
}
