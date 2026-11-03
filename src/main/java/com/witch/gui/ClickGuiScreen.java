package com.witch.gui;

import com.witch.config.Config;
import com.witch.module.Category;
import com.witch.module.Module;
import com.witch.module.ModuleManager;
import com.witch.setting.IntSetting;
import com.witch.setting.KeybindSetting;
import com.witch.setting.Setting;
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

import static com.witch.gui.ClickGuiMetrics.*;

/**
 * ClickGUI estilo Meteor: uma janela arrastável por categoria, módulos em linhas,
 * ajustes abrem dentro da janela. Monocromático.
 * <p>
 * Render e settings delegados a {@link GuiWindow}, {@link ModuleRow}, {@link SettingRenderer}.
 * Comportamento idêntico à versão monolítica anterior.
 */
public class ClickGuiScreen extends Screen {

	/** Opacidade do fundo das janelas (ajustável em "GUI Settings"); soma e limitada a 1. */
	private static float gl(float add) {
		return Math.min(1f, com.witch.module.GuiSettingsModule.opacity() + add);
	}

	private Module tipNow = null;
	private Module tipLast = null;
	private long tipSince = 0L;

	// posição e rolagem das janelas lembradas entre aberturas
	private static final Map<Category, int[]> POS = new EnumMap<>(Category.class);
	private static final Map<Category, float[]> SCR = new EnumMap<>(Category.class);
	private static final List<Category> ORDER = new ArrayList<>();
	private static final Set<Module> OPEN = new HashSet<>();

	private Category dragWin = null;
	private int dragOffX, dragOffY;
	private Category scrollWin = null;
	private Category sbWin = null;
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

	private final Map<Category, int[]> drawOff = new EnumMap<>(Category.class);
	private long lastNanos = 0L;
	private int winWCache = WIN_W;
	private static int lastW = -1, lastH = -1;

	public ClickGuiScreen() {
		this(null);
	}

	/** Se 'openSettings' não for nulo, a janela já abre com os ajustes desse módulo expandidos. */
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

	private int[] at(Category c) {
		int[] p = pos(c);
		int[] o = drawOff.get(c);
		return o == null ? new int[] {p[0], p[1]} : new int[] {p[0] + o[0], p[1] + o[1]};
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

	private int contentH(Category c, int w) {
		int h = 0;
		for (Module m : modules(c)) {
			h += ROW;
			if (OPEN.contains(m)) {
				for (Setting s : m.settings()) h += SettingRenderer.height(s, w);
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
		tipNow = null;

		g.fill(0, 0, this.width, this.height, RenderUtil.alpha(0x99000000, a));
		glowLines(g, a);

		// busca no topo
		int sw = Math.min(150, this.width - 20);
		int sx = (this.width - sw) / 2;
		g.fill(sx - 1, 4, sx + sw + 1, 19, RenderUtil.alpha(LINE, a));
		g.fill(sx, 5, sx + sw, 18, RenderUtil.alpha(0xFF0C0C0C, a * gl(0.1f)));
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

		Module[] tipSlot = new Module[1];
		for (Category c : ORDER) {
			float k = Mth.clamp((openT - c.ordinal() * 0.09f) / 0.45f, 0f, 1f);
			float u = k - 1f;
			float e = 1f + 2.4f * u * u * u + 1.4f * u * u; // easeOutBack
			curYOff = Math.round((1f - e) * 22f);
			curXOff = 0;

			int[] p = pos(c);
			int w = winW();
			int vh = viewH(c, w);
			int ch = contentH(c, w);
			int x = p[0] + curXOff;
			int y = p[1] + curYOff;
			drawOff.put(c, new int[] {curXOff, curYOff});
			float[] sc = SCR.computeIfAbsent(c, key -> new float[] {0f});
			sc[0] = Mth.clamp(sc[0], 0, Math.max(0, ch - vh));

			tipSlot[0] = null;
			GuiWindow.render(g, c, x, y, w, vh, ch, sc[0], modules(c), OPEN,
					dragWin, sbWin, mx, my, speed, a * Math.min(1f, k * 1.6f), tipSlot);
			if (tipSlot[0] != null) tipNow = tipSlot[0];
		}

		if (tipNow != tipLast) {
			tipLast = tipNow;
			tipSince = System.currentTimeMillis();
		}
		if (tipNow != null && com.witch.module.GuiSettingsModule.tooltips() && System.currentTimeMillis() - tipSince > 350) {
			String d = ModuleDescriptions.of(tipNow.name);
			if (!d.isEmpty()) {
				int tw = FontUtil.width(d) + 8;
				int th = FontUtil.height() + 6;
				int tipX = Math.min(this.width - tw - 2, mx + 10);
				int tipY = Math.min(this.height - th - 2, my + 12);
				g.fill(tipX - 1, tipY - 1, tipX + tw + 1, tipY + th + 1, RenderUtil.alpha(LINE, a));
				g.fill(tipX, tipY, tipX + tw, tipY + th, RenderUtil.alpha(0xFF0C0C0C, a * 0.95f));
				FontUtil.draw(g, d, tipX + 4, tipY + 3, RenderUtil.alpha(TXT, a));
			}
		}
	}

	/** Linhas brilhantes brancas que sobem balançando dos cantos de baixo. */
	private void glowLines(GuiGraphicsExtractor g, float a) {
		double t = System.nanoTime() / 1_000_000_000.0;
		int rise = (int) (this.height * 0.55f);
		for (int side = 0; side < 2; side++) {
			boolean left = side == 0;
			double ph = left ? 0.0 : 1.9;
			for (int i = 0; i < rise; i += 2) {
				float f = i / (float) rise;
				float fade = (1f - f) * (1f - f);
				double sway = Math.sin(t * 1.3 + i * 0.035 + ph) * (6 + 22 * f)
						+ Math.sin(t * 0.7 + i * 0.012 + ph * 2) * (4 + 14 * f);
				double bx = left ? 3 + f * 26 + sway : this.width - 3 - f * 26 - sway;
				int y = this.height - i;
				int x = (int) Math.round(bx);
				g.fill(x - 5, y - 2, x + 6, y, RenderUtil.alpha(0xFFFFFFFF, a * fade * 0.035f));
				g.fill(x - 3, y - 2, x + 4, y, RenderUtil.alpha(0xFFFFFFFF, a * fade * 0.07f));
				g.fill(x - 1, y - 2, x + 2, y, RenderUtil.alpha(0xFFFFFFFF, a * fade * 0.85f));
			}
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
		if (my < y + HEAD_H && button == 1) {
			POS.remove(c);
			return true;
		}
		if (my < y + HEAD_H) {
			dragWin = c;
			dragOffX = (int) mx - x;
			dragOffY = (int) my - y;
			return true;
		}
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
					int sh = SettingRenderer.height(s, w);
					if (my >= ry && my < ry + sh) {
						Runnable[] out = new Runnable[1];
						IntSetting slid = SettingRenderer.click(s, x, ry, w, sh, mx, my, fh, out);
						if (slid != null) {
							slider = slid;
							sliderX = x + 7;
							sliderW = Math.max(1, w - 14);
						}
						if (out[0] != null) pending = out[0];
						return true;
					}
					ry += sh;
				}
			}
		}
		return true;
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
		if (key == GLFW.GLFW_KEY_HOME) {
			POS.clear();
			return true;
		}
		if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			this.onClose();
			return true;
		}
		return super.keyPressed(input);
	}
}
