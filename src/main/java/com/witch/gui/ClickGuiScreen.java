package com.witch.gui;

import com.witch.config.Config;
import com.witch.module.Category;
import com.witch.module.Module;
import com.witch.module.ModuleManager;
import com.witch.setting.IntSetting;
import com.witch.setting.KeybindSetting;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.witch.gui.ClickGuiMetrics.*;

/**
 * ClickGUI em janela unica: menu lateral de categorias, busca e FPS no topo,
 * modulos em lista com switch e ajustes que abrem embaixo do modulo. Preto e branco, bordas arredondadas.
 */
public class ClickGuiScreen extends Screen {
	private static final int HEAD = 24;
	private static final int SIDE = 78;
	private static final int ROWH = 17;
	private static final int ROWGAP = 2;

	private static final Set<Module> OPEN = new HashSet<>();
	private static Category selected = Category.values()[0];
	private static float scroll = 0f;

	private String search = "";
	private boolean searchFocused = false;
	private float openAnim = 0f;
	private long lastNanos = 0L;
	private IntSetting slider = null;
	private int sliderX = 0, sliderW = 1;
	private Module tipNow = null, tipLast = null;
	private long tipSince = 0L;

	private int wx, wy, ww, wh;

	public ClickGuiScreen() {
		this(null);
	}

	/** Se 'openSettings' nao for nulo, abre na categoria dele com os ajustes expandidos. */
	public ClickGuiScreen(Module openSettings) {
		super(Component.literal("Witch Client"));
		if (openSettings != null) {
			selected = openSettings.category;
			if (!openSettings.settings().isEmpty()) OPEN.add(openSettings);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void layout() {
		ww = Math.min(this.width - 12, 340);
		wh = Math.min(this.height - 12, 220);
		wx = (this.width - ww) / 2;
		wy = (this.height - wh) / 2;
	}

	private int cx() { return wx + SIDE + 6; }
	private int cy() { return wy + HEAD + 2; }
	private int cw() { return ww - SIDE - 12; }
	private int ch() { return wh - HEAD - 8; }

	private List<Module> modules() {
		List<Module> out = new ArrayList<>();
		String q = search.toLowerCase(Locale.ROOT);
		if (q.isEmpty()) {
			out.addAll(ModuleManager.byCategory(selected));
		} else {
			for (Module m : ModuleManager.all()) if (m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
		}
		return out;
	}

	private int settingsW() { return cw() - 14; }

	private int contentH() {
		int h = 0;
		for (Module m : modules()) {
			h += ROWH + ROWGAP;
			if (OPEN.contains(m)) for (Setting s : m.settings()) h += SettingRenderer.height(s, settingsW());
		}
		return h;
	}

	private void clampScroll() {
		scroll = Mth.clamp(scroll, 0f, Math.max(0, contentH() - ch()));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		layout();
		long now = System.nanoTime();
		float speed = lastNanos == 0 ? 1f : Math.min(3f, (now - lastNanos) / 16_666_667f);
		lastNanos = now;
		openAnim = Mth.lerp(Math.min(1f, speed * 0.2f), openAnim, 1f);
		float a = openAnim;
		tipNow = null;
		clampScroll();

		g.fill(0, 0, this.width, this.height, RenderUtil.alpha(0x30000000, a));

		float op = com.witch.module.GuiSettingsModule.opacity();
		RenderUtil.roundRect(g, wx, wy, ww, wh, 9, RenderUtil.alpha(0xFFFFFFFF, a * 0.20f));
		RenderUtil.roundRect(g, wx + 1, wy + 1, ww - 2, wh - 2, 8, RenderUtil.alpha(0xFF0A0A0A, a * Math.min(0.92f, 0.55f + op)));

		// cabecalho
		WitchLogo.draw(g, wx + 9, wy + 5, 26, a * 0.95f);
		int fh = FontUtil.height();
		int sx = cx();
		int sw = Math.min(150, ww - SIDE - 110);
		RenderUtil.roundRect(g, sx, wy + 5, sw, 14, 7, RenderUtil.alpha(0xFFFFFFFF, a * 0.14f));
		RenderUtil.roundRect(g, sx + 1, wy + 6, sw - 2, 12, 6, RenderUtil.alpha(0xFF0C0C0C, a * 0.9f));
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
		FontUtil.draw(g, shown, sx + 7, wy + 5 + (14 - fh) / 2, RenderUtil.alpha(col, a));
		String fps = Minecraft.getInstance().getFps() + " FPS";
		int fx = wx + ww - 10 - FontUtil.width(fps);
		FontUtil.draw(g, fps, fx, wy + 5 + (14 - fh) / 2, RenderUtil.alpha(TXT_DIM, a));
		RenderUtil.roundRect(g, wx + 6, wy + HEAD - 1, ww - 12, 1, 0, RenderUtil.alpha(0xFFFFFFFF, a * 0.10f));

		// menu lateral
		Category[] cats = Category.values();
		for (int i = 0; i < cats.length; i++) {
			Category c = cats[i];
			int ry = wy + HEAD + 6 + i * 19;
			boolean sel = c == selected && search.isEmpty();
			boolean hov = RenderUtil.inside(mx, my, wx + 7, ry, SIDE - 10, 17);
			if (sel || hov) RenderUtil.roundRect(g, wx + 7, ry, SIDE - 10, 17, 6, RenderUtil.alpha(0xFFFFFFFF, a * (sel ? 0.13f : 0.06f)));
			CategoryIcons.draw(g, c, wx + 13, ry + 5, RenderUtil.alpha(sel ? TXT : TXT_DIM, a));
			String nm = c.name().charAt(0) + c.name().substring(1).toLowerCase(Locale.ROOT);
			FontUtil.draw(g, nm, wx + 25, ry + (17 - fh) / 2, RenderUtil.alpha(sel ? TXT : TXT_DIM, a));
		}
		int ey = wy + wh - 24;
		boolean eh = RenderUtil.inside(mx, my, wx + 7, ey, SIDE - 10, 17);
		RenderUtil.roundRect(g, wx + 7, ey, SIDE - 10, 17, 6, RenderUtil.alpha(0xFFFFFFFF, a * (eh ? 0.16f : 0.07f)));
		String et = "Edit HUD";
		FontUtil.draw(g, et, wx + 7 + (SIDE - 10 - FontUtil.width(et)) / 2, ey + (17 - fh) / 2, RenderUtil.alpha(eh ? TXT : TXT_DIM, a));
		RenderUtil.roundRect(g, wx + SIDE, wy + HEAD + 4, 1, wh - HEAD - 10, 0, RenderUtil.alpha(0xFFFFFFFF, a * 0.08f));

		// lista de modulos
		int cx = cx(), cy = cy(), cw = cw(), ch = ch();
		g.enableScissor(cx, cy, cx + cw, cy + ch);
		int ry = cy - Math.round(scroll);
		int rowW = cw - 6;
		for (Module m : modules()) {
			boolean hover = RenderUtil.inside(mx, my, cx, ry, rowW, ROWH) && my >= cy && my < cy + ch;
			m.toggleAnim = Mth.lerp(Math.min(1f, speed * 0.3f), m.toggleAnim, m.isEnabled() ? 1f : 0f);
			m.hoverAnim = Mth.lerp(Math.min(1f, speed * 0.3f), m.hoverAnim, hover ? 1f : 0f);
			RenderUtil.roundRect(g, cx, ry, rowW, ROWH, 6, RenderUtil.alpha(0xFFFFFFFF, a * (0.045f + 0.06f * m.hoverAnim)));
			int tc = RenderUtil.mix(TXT_DIM, TXT, m.toggleAnim);
			boolean has = !m.settings().isEmpty();
			FontUtil.draw(g, ModuleRow.fit(m.name, rowW - 62), cx + 8, ry + (ROWH - fh) / 2, RenderUtil.alpha(tc, a));
			if (has) {
				String ar = OPEN.contains(m) ? "-" : "+";
				FontUtil.draw(g, ar, cx + rowW - 38 - FontUtil.width(ar) / 2, ry + (ROWH - fh) / 2, RenderUtil.alpha(OPEN.contains(m) ? TXT : TXT_DIM, a));
			}
			int swx = cx + rowW - 26, swy = ry + (ROWH - 9) / 2;
			RenderUtil.roundRect(g, swx, swy, 20, 9, 4, RenderUtil.mix(0xFF2C2C2C, 0xFFE8E8E8, m.toggleAnim) & 0xFFFFFF | ((int) (a * 255) << 24));
			int kx = swx + 1 + Math.round(11 * m.toggleAnim);
			RenderUtil.roundRect(g, kx, swy + 1, 7, 7, 3, RenderUtil.alpha(RenderUtil.mix(0xFF9A9A9A, 0xFF111111, m.toggleAnim), a));
			if (hover && my >= cy && my < cy + ch) tipNow = m;
			ry += ROWH + ROWGAP;
			if (OPEN.contains(m)) {
				int sxx = cx + 4, sww = settingsW();
				int top = ry;
				int total = 0;
				for (Setting s : m.settings()) total += SettingRenderer.height(s, sww);
				RenderUtil.roundRect(g, cx + 2, top - 1, rowW - 4, total + 2, 5, RenderUtil.alpha(0xFF000000, a * 0.35f));
				for (Setting s : m.settings()) {
					int sh = SettingRenderer.height(s, sww);
					SettingRenderer.draw(g, s, sxx, ry, sww - 4, sh, a, 0f);
					ry += sh;
				}
			}
		}
		g.disableScissor();
		int total = contentH();
		if (total > ch) {
			int th = Math.max(14, Math.round(ch * (ch / (float) total)));
			float f = scroll / Math.max(1, total - ch);
			RenderUtil.roundRect(g, cx + cw - 3, cy + Math.round((ch - th) * f), 2, th, 1, RenderUtil.alpha(0xFFFFFFFF, a * 0.35f));
		}

		// tooltip
		if (tipNow != tipLast) { tipLast = tipNow; tipSince = System.currentTimeMillis(); }
		if (tipNow != null && System.currentTimeMillis() - tipSince > 450) {
			String d = ModuleDescriptions.of(tipNow.name);
			if (d != null && !d.isEmpty()) {
				int tw = Math.min(this.width - 12, FontUtil.width(d) + 12);
				int th = fh + 8;
				int tx = Mth.clamp(mx + 10, 4, this.width - tw - 4);
				int ty = Math.min(this.height - th - 4, my + 12);
				RenderUtil.roundRect(g, tx - 1, ty - 1, tw + 2, th + 2, 5, RenderUtil.alpha(0xFFFFFFFF, a * 0.2f));
				RenderUtil.roundRect(g, tx, ty, tw, th, 4, RenderUtil.alpha(0xFF0C0C0C, a * 0.96f));
				FontUtil.draw(g, ModuleRow.fit(d, tw - 10), tx + 6, ty + 4, RenderUtil.alpha(TXT, a));
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		layout();
		double mx = click.x(), my = click.y();
		int button = click.button();
		int fh = FontUtil.height();
		int sx = cx();
		int sw = Math.min(150, ww - SIDE - 110);
		searchFocused = RenderUtil.inside(mx, my, sx, wy + 5, sw, 14);
		if (searchFocused) return true;
		Category[] cats = Category.values();
		for (int i = 0; i < cats.length; i++) {
			if (RenderUtil.inside(mx, my, wx + 7, wy + HEAD + 6 + i * 19, SIDE - 10, 17)) {
				selected = cats[i];
				search = "";
				scroll = 0f;
				return true;
			}
		}
		if (RenderUtil.inside(mx, my, wx + 7, wy + wh - 24, SIDE - 10, 17)) {
			Minecraft.getInstance().gui.setScreen(new HudEditScreen());
			return true;
		}
		int cx = cx(), cy = cy(), cw = cw(), ch = ch();
		if (!RenderUtil.inside(mx, my, cx, cy, cw, ch)) return super.mouseClicked(click, doubled);
		int ry = cy - Math.round(scroll);
		int rowW = cw - 6;
		for (Module m : modules()) {
			if (my >= ry && my < ry + ROWH) {
				boolean has = !m.settings().isEmpty();
				boolean onArrow = has && mx >= cx + rowW - 48 && mx < cx + rowW - 28;
				if (has && (button == 1 || onArrow)) {
					if (!OPEN.remove(m)) OPEN.add(m);
				} else if (button == 0) {
					m.toggle();
					Config.save();
				}
				return true;
			}
			ry += ROWH + ROWGAP;
			if (OPEN.contains(m)) {
				int sxx = cx + 4, sww = settingsW();
				for (Setting s : m.settings()) {
					int sh = SettingRenderer.height(s, sww);
					if (my >= ry && my < ry + sh) {
						Runnable[] out = new Runnable[1];
						IntSetting slid = SettingRenderer.click(s, sxx, ry, sww - 4, sh, mx, my, fh, out);
						if (slid != null) {
							slider = slid;
							sliderX = sxx + 7;
							sliderW = Math.max(1, sww - 4 - 14);
						}
						if (out[0] != null) out[0].run();
						return true;
					}
					ry += sh;
				}
			}
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (slider != null) {
			slider.setFraction((float) ((click.x() - sliderX) / sliderW));
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		boolean was = slider != null;
		if (slider != null) Config.save();
		slider = null;
		return was || super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scroll -= (float) (verticalAmount * 16.0);
		clampScroll();
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		int cp = input.codepoint();
		if (searchFocused && cp >= 32 && cp != 127 && search.length() < 24) {
			search += new String(Character.toChars(cp));
			scroll = 0f;
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
					if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) ks.setKey(0);
					else if (key != GLFW.GLFW_KEY_ESCAPE) ks.setKey(key);
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
