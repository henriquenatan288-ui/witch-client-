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

	// rolagem por arrasto (toque) e pela barra; clique em cartao/ajuste so vale ao soltar se nao arrastou
	private Runnable deferred;
	private double pressY;
	private float scrollStart;
	private boolean scrollDrag, barDrag, moved;

	private void startPress(double my) {
		pressY = my;
		scrollStart = scroll;
		scrollDrag = true;
		moved = false;
	}

	private void barTo(double my) {
		int total = contentH(), ch = ch();
		if (total <= ch) return;
		int th = Math.max(14, Math.round(ch * (ch / (float) total)));
		float f = (float) ((my - cy() - th / 2.0) / Math.max(1, ch - th));
		scroll = Mth.clamp(f, 0f, 1f) * (total - ch);
	}

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

	/** A moldura e uma imagem 1452x819: todas as posicoes abaixo sao fracoes dela (em px da imagem original). */
	private static final float IW = 1452f, IH = 819f;
	private static final int PURPLE = 0xFF2BE04A; // verde neon da moldura
	private static final int HH = 11;

	private void layout() {
		ww = Math.min(this.width - 12, 460);
		wh = Math.round(ww * IH / IW);
		if (wh > this.height - 12) {
			wh = this.height - 12;
			ww = Math.round(wh * IW / IH);
		}
		wx = (this.width - ww) / 2;
		wy = (this.height - wh) / 2;
	}

	private int fx(float px) { return wx + Math.round(px / IW * ww); }
	private int fy(float py) { return wy + Math.round(py / IH * wh); }

	private int cx() { return fx(231) + 4; }
	private int cy() { return fy(143) + 4; }
	private int cw() { return fx(1433) - fx(231) - 8; }
	private int ch() { return fy(805) - fy(143) - 8; }

	// 7 vagas da sidebar desenhadas na propria imagem
	private int sideX() { return fx(45); }
	private int sideW() { return fx(217) - fx(45); }
	private int catY(int i) { return fy(157 + 81.7f * i); }
	private int catH() { return fy(227) - fy(157); }
	private int headY() { return fy(112) - HH / 2; }
	private int editW() { return FontUtil.width("Edit HUD") + 12; }
	private int editX() { return searchX() - 6 - editW(); }

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

	private static final int CARD = 18;
	private static final int GAP2 = 4;

	/** Peca do layout em grade: cartao de modulo (settingsBlock=false) ou bloco de ajustes (true). Coordenadas relativas ao conteudo, sem scroll. */
	private record Slot(Module m, boolean block, int x, int y, int w, int h) {}

	private int rowW() { return cw() - 6; }

	private List<Slot> slots() {
		List<Slot> out = new ArrayList<>();
		int full = rowW();
		int colW = (full - GAP2) / 2;
		int y = 0, col = 0;
		for (Module m : modules()) {
			boolean open = OPEN.contains(m) && !m.settings().isEmpty();
			if (open) {
				if (col == 1) { y += CARD + GAP2; col = 0; }
				out.add(new Slot(m, false, 0, y, full, CARD));
				y += CARD + 2;
				int total = 0;
				for (Setting st : m.settings()) total += SettingRenderer.height(st, settingsW());
				out.add(new Slot(m, true, 2, y, full - 4, total));
				y += total + GAP2;
			} else {
				out.add(new Slot(m, false, col == 0 ? 0 : colW + GAP2, y, colW, CARD));
				if (col == 1) { y += CARD + GAP2; col = 0; } else col = 1;
			}
		}
		return out;
	}

	private int contentH() {
		int h = 0;
		for (Slot sl : slots()) h = Math.max(h, sl.y + sl.h + GAP2);
		return h;
	}

	private int searchW() { return Math.min(80, Math.max(44, ww / 7)); }
	private int searchX() { return fx(1340) - searchW(); }

	private static String subtitle(Category c) {
		return switch (c) {
			case MOVEMENT -> "Mobility";
			case COMBAT -> "Targeting";
			case VISUALS -> "Render";
			case WORLD -> "Chunks";
			case UTILITY -> "Tools";
			case HUD -> "Info";
			case EFFECTS -> "Sounds";
		};
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

		UiTex.menu(g, wx, wy, ww, wh, a);

		// cabecalho (faixa fina entre o chapeu e o X)
		int fh = FontUtil.height();
		int hy = headY();
		int sx = searchX();
		int sw = searchW();
		int ex = editX(), ew = editW();
		String fps = Minecraft.getInstance().getFps() + " FPS";
		int fpsX = ex - 8 - FontUtil.width(fps);
		String title = search.isEmpty() ? selected.name().charAt(0) + selected.name().substring(1).toLowerCase(Locale.ROOT) : "Search";
		int tx0 = fx(330);
		CategoryIcons.draw(g, selected, tx0, hy + (HH - 7) / 2, RenderUtil.alpha(PURPLE, a));
		FontUtil.draw(g, title, tx0 + 11, hy + (HH - fh) / 2, RenderUtil.alpha(TXT, a));
		int chipX = tx0 + 11 + FontUtil.width(title) + 7;
		String sub = search.isEmpty() ? subtitle(selected) : "All modules";
		int chipW = FontUtil.width(sub) + 12;
		if (chipX + chipW < fpsX - 4) {
			RenderUtil.roundRect(g, chipX, hy, chipW, HH, 5, RenderUtil.alpha(PURPLE, a * 0.18f));
			FontUtil.draw(g, sub, chipX + 6, hy + (HH - fh) / 2, RenderUtil.alpha(TXT_DIM, a));
		}
		if (fpsX > chipX) FontUtil.draw(g, fps, fpsX, hy + (HH - fh) / 2, RenderUtil.alpha(TXT_DIM, a));
		boolean eh = RenderUtil.inside(mx, my, ex, hy, ew, HH);
		RenderUtil.roundRect(g, ex, hy, ew, HH, 5, RenderUtil.alpha(PURPLE, a * (eh ? 0.55f : 0.30f)));
		FontUtil.draw(g, "Edit HUD", ex + 6, hy + (HH - fh) / 2, RenderUtil.alpha(eh ? TXT : TXT_DIM, a));
		RenderUtil.roundRect(g, sx, hy, sw, HH, 5, RenderUtil.alpha(PURPLE, a * (searchFocused ? 0.85f : 0.35f)));
		RenderUtil.roundRect(g, sx + 1, hy + 1, sw - 2, HH - 2, 4, RenderUtil.alpha(0xFF050805, a * 0.95f));
		String shown;
		int col;
		if (search.isEmpty() && !searchFocused) {
			shown = "Search";
			col = 0xFF667066;
		} else {
			boolean blink = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0;
			shown = search + (blink ? "|" : "");
			col = TXT;
		}
		FontUtil.draw(g, ModuleRow.fit(shown, sw - 10), sx + 5, hy + (HH - fh) / 2, RenderUtil.alpha(col, a));

		// menu lateral: uma categoria por vaga da imagem
		Category[] cats = Category.values();
		int bx = sideX(), bw = sideW(), bh = catH();
		for (int i = 0; i < cats.length; i++) {
			Category c = cats[i];
			int ry = catY(i);
			boolean sel = c == selected && search.isEmpty();
			boolean hov = RenderUtil.inside(mx, my, bx, ry, bw, bh);
			if (sel) {
				RenderUtil.roundRect(g, bx + 1, ry + 1, bw - 2, bh - 2, 6, RenderUtil.alpha(PURPLE, a * 0.30f));
			} else if (hov) {
				RenderUtil.roundRect(g, bx + 1, ry + 1, bw - 2, bh - 2, 6, RenderUtil.alpha(PURPLE, a * 0.12f));
			}
			CategoryIcons.draw(g, c, bx + 6, ry + (bh - 7) / 2, RenderUtil.alpha(sel ? PURPLE : TXT_DIM, a));
			String nm = c == Category.MOVEMENT ? "Move" : c.name().charAt(0) + c.name().substring(1).toLowerCase(Locale.ROOT);
			FontUtil.draw(g, ModuleRow.fit(nm, bw - 21), bx + 19, ry + (bh - fh) / 2, RenderUtil.alpha(sel ? TXT : TXT_DIM, a));
		}

		// lista de modulos (grade de 2 colunas)
		int cx = cx(), cy = cy(), cw = cw(), ch = ch();
		g.enableScissor(cx, cy, cx + cw, cy + ch);
		int base = cy - Math.round(scroll);
		for (Slot sl : slots()) {
			Module m = sl.m;
			int sx0 = cx + sl.x, sy0 = base + sl.y;
			if (sy0 + sl.h < cy || sy0 > cy + ch) continue;
			if (sl.block) {
				RenderUtil.roundRect(g, sx0, sy0 - 1, sl.w, sl.h + 2, 6, RenderUtil.alpha(0xFF000000, a * 0.35f));
				int ry = sy0;
				for (Setting st : m.settings()) {
					int sh = SettingRenderer.height(st, settingsW());
					SettingRenderer.draw(g, st, sx0 + 2, ry, settingsW() - 4, sh, a, 0f);
					ry += sh;
				}
				continue;
			}
			boolean hover = RenderUtil.inside(mx, my, sx0, sy0, sl.w, CARD) && my >= cy && my < cy + ch;
			m.toggleAnim = Mth.lerp(Math.min(1f, speed * 0.3f), m.toggleAnim, m.isEnabled() ? 1f : 0f);
			m.hoverAnim = Mth.lerp(Math.min(1f, speed * 0.3f), m.hoverAnim, hover ? 1f : 0f);
			float t = m.toggleAnim;
			int lv = Math.round(0x70 + (0xFF - 0x70) * t + 0x30 * m.hoverAnim * (1f - t));
			lv = Math.min(255, lv);
			UiTex.button(g, sx0, sy0, sl.w, CARD, (lv << 16) | (lv << 8) | lv, a);
			int tc = RenderUtil.mix(TXT_DIM, TXT, t);
			CategoryIcons.draw(g, m.category, sx0 + 9, sy0 + (CARD - 7) / 2, RenderUtil.alpha(tc, a));
			boolean has = !m.settings().isEmpty();
			FontUtil.draw(g, ModuleRow.fit(m.name, sl.w - 20 - 46), sx0 + 20, sy0 + (CARD - fh) / 2, RenderUtil.alpha(tc, a));
			if (has) {
				String ar = OPEN.contains(m) ? "-" : "+";
				FontUtil.draw(g, ar, sx0 + sl.w - 33 + (10 - FontUtil.width(ar)) / 2, sy0 + (CARD - fh) / 2, RenderUtil.alpha(OPEN.contains(m) ? TXT : TXT_DIM, a));
			}
			int swx = sx0 + sl.w - 22, swy = sy0 + (CARD - 8) / 2;
			RenderUtil.roundRect(g, swx, swy, 16, 8, 4, RenderUtil.alpha(RenderUtil.mix(0xFF1C241C, PURPLE, t), a));
			int kx = swx + 1 + Math.round(8 * t);
			RenderUtil.roundRect(g, kx, swy + 1, 6, 6, 3, RenderUtil.alpha(RenderUtil.mix(0xFF7A867A, 0xFF06100A, t), a));
			if (hover) tipNow = m;
		}
		g.disableScissor();
		int total = contentH();
		if (total > ch) {
			int th = Math.max(14, Math.round(ch * (ch / (float) total)));
			float f = scroll / Math.max(1, total - ch);
			RenderUtil.roundRect(g, cx + cw - 4, cy + Math.round((ch - th) * f), 3, th, 1, RenderUtil.alpha(PURPLE, a * (barDrag ? 0.95f : 0.65f)));
		}

		// tooltip
		if (tipNow != tipLast) { tipLast = tipNow; tipSince = System.currentTimeMillis(); }
		if (tipNow != null && System.currentTimeMillis() - tipSince > 450) {
			String d = ModuleDescriptions.of(tipNow.name);
			if (d != null && !d.isEmpty()) {
				int tw = Math.min(ww - 16, FontUtil.width(d) + 12);
				int th = fh + 8;
				int tx = Mth.clamp(mx + 10, wx + 6, wx + ww - tw - 6);
				int ty = Mth.clamp(my + 12, wy + 6, wy + wh - th - 6);
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
		if (RenderUtil.inside(mx, my, fx(1356), fy(70), fx(1448) - fx(1356), fy(162) - fy(70))) {
			this.onClose();
			return true;
		}
		searchFocused = RenderUtil.inside(mx, my, searchX(), headY(), searchW(), HH);
		if (searchFocused) return true;
		Category[] cats = Category.values();
		for (int i = 0; i < cats.length; i++) {
			if (RenderUtil.inside(mx, my, sideX(), catY(i), sideW(), catH())) {
				selected = cats[i];
				search = "";
				scroll = 0f;
				return true;
			}
		}
		if (RenderUtil.inside(mx, my, editX(), headY(), editW(), HH)) {
			Minecraft.getInstance().gui.setScreen(new HudEditScreen());
			return true;
		}
		int cx = cx(), cy = cy(), cw = cw(), ch = ch();
		if (!RenderUtil.inside(mx, my, cx, cy, cw, ch)) return super.mouseClicked(click, doubled);
		deferred = null;
		scrollDrag = false;
		barDrag = false;
		if (contentH() > ch && mx >= cx + cw - 10) {
			barDrag = true;
			barTo(my);
			return true;
		}
		int base = cy - Math.round(scroll);
		for (Slot sl : slots()) {
			Module m = sl.m;
			int sx0 = cx + sl.x, sy0 = base + sl.y;
			if (sl.block) {
				int ry = sy0;
				int sxx = sx0 + 2, sww = settingsW();
				for (Setting st : m.settings()) {
					int sh = SettingRenderer.height(st, sww);
					if (my >= ry && my < ry + sh && mx >= sx0 && mx < sx0 + sl.w) {
						Runnable[] out = new Runnable[1];
						IntSetting slid = SettingRenderer.click(st, sxx, ry, sww - 4, sh, mx, my, fh, out);
						if (slid != null) {
							slider = slid;
							sliderX = sxx + 7;
							sliderW = Math.max(1, sww - 4 - 14);
						}
						if (slid == null && !SettingRenderer.dragging()) {
							deferred = out[0];
							startPress(my);
						}
						return true;
					}
					ry += sh;
				}
				continue;
			}
			if (my >= sy0 && my < sy0 + CARD && mx >= sx0 && mx < sx0 + sl.w) {
				boolean has = !m.settings().isEmpty();
				boolean onArrow = has && mx >= sx0 + sl.w - 37 && mx < sx0 + sl.w - 23;
				if (has && (button == 1 || onArrow)) {
					deferred = () -> { if (!OPEN.remove(m)) OPEN.add(m); };
				} else if (button == 0) {
					deferred = () -> { m.toggle(); Config.save(); };
				}
				startPress(my);
				return true;
			}
		}
		startPress(my);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (SettingRenderer.dragging()) {
			SettingRenderer.drag(click.x(), click.y());
			return true;
		}
		if (slider != null) {
			slider.setFraction((float) ((click.x() - sliderX) / sliderW));
			return true;
		}
		if (barDrag) {
			barTo(click.y());
			return true;
		}
		if (scrollDrag) {
			double dy = click.y() - pressY;
			if (moved || Math.abs(dy) > 3) {
				moved = true;
				deferred = null;
				scroll = scrollStart - (float) dy;
				clampScroll();
			}
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		boolean was = slider != null || SettingRenderer.dragging() || barDrag || scrollDrag;
		if (deferred != null && !moved) deferred.run();
		deferred = null;
		scrollDrag = false;
		barDrag = false;
		SettingRenderer.release();
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
