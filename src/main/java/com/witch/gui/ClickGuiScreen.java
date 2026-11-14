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
	private String catTip = null;
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

	private static final net.minecraft.resources.Identifier TEX_CARD_OFF = net.minecraft.resources.Identifier.fromNamespaceAndPath("witch", "textures/gui/ui_card_off.png");
	private static final net.minecraft.resources.Identifier TEX_CARD_ON = net.minecraft.resources.Identifier.fromNamespaceAndPath("witch", "textures/gui/ui_card_on.png");
	private static final int ACC = 0xFFFFFFFF; // destaque (branco, como a referencia)
	private static final float IW = 1996f, IH = 1196f; // tamanho da imagem da janela

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

	/** px = posicao na imagem original (relativa a janela). */
	private int fx(float px) { return wx + Math.round(px / IW * ww); }
	private int fy(float py) { return wy + Math.round(py / IH * wh); }

	private int cx() { return fx(500); }
	private int cy() { return fy(205); }
	private int cw() { return fx(1936) - fx(500); }
	private int ch() { return fy(1050) - fy(205); }

	private int sideX() { return fx(28); }
	private int sideItemW() { return fx(410) - fx(28); }
	private int catY(int i) { return fy(229 + 96 * i); }
	private int catH() { return fy(88) - fy(0); }
	private int editY() { return fy(1072); }
	private int searchX() { return fx(1340); }
	private int searchY() { return fy(30); }
	private int searchW() { return fx(1940) - fx(1420); }
	private int searchH() { return fy(110) - fy(30); }

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

	private static final int CARD = 20;
	private static final int GAP2 = 4;

	/** Peca do layout em grade: cartao de modulo (settingsBlock=false) ou bloco de ajustes (true). Coordenadas relativas ao conteudo, sem scroll. */
	private record Slot(Module m, boolean block, int x, int y, int w, int h) {}

	private int rowW() { return cw() - 6; }

	private List<Slot> slots() {
		List<Slot> out = new ArrayList<>();
		int full = rowW();
		int y = 0;
		for (Module m : modules()) {
			boolean open = OPEN.contains(m) && !m.settings().isEmpty();
			out.add(new Slot(m, false, 0, y, full, CARD));
			if (open) {
				y += CARD + 2;
				int total = 0;
				for (Setting st : m.settings()) total += SettingRenderer.height(st, settingsW());
				out.add(new Slot(m, true, 2, y, full - 4, total));
				y += total + GAP2;
			} else {
				y += CARD + GAP2;
			}
		}
		return out;
	}

	private int contentH() {
		int h = 0;
		for (Slot sl : slots()) h = Math.max(h, sl.y + sl.h + GAP2);
		return h;
	}


	private void clampScroll() {
		scroll = Mth.clamp(scroll, 0f, Math.max(0, contentH() - ch()));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		layout();
		float e = Mth.clamp(openAnim, 0f, 1f);
		float inv = 1f - e;
		float cx = wx + ww / 2f, cy = wy + wh / 2f;
		float tx = 0f, ty = 0f, sc = 1f;
		switch (com.witch.module.GuiAnimationModule.style()) {
			case 1 -> sc = 0.75f + 0.25f * e;
			case 2 -> ty = inv * 40f;
			case 3 -> ty = -inv * 40f;
			case 4 -> tx = -inv * 60f;
			case 5 -> sc = 0.6f + 0.4f * e + 0.10f * (float) Math.sin(Math.PI * e);
			default -> { }
		}
		// desfoque nativo do jogo (o mesmo do menu de pausa); some se a opcao "Menu Background Blurriness" for 0
		try {
			this.extractBlurredBackground(g);
		} catch (Throwable ignored) {
			// sem desfoque: o painel continua translucido
		}
		boolean moved = tx != 0f || ty != 0f || sc != 1f;
		if (moved) {
			g.pose().pushMatrix();
			g.pose().translate(cx + tx, cy + ty);
			g.pose().scale(sc, sc);
			g.pose().translate(-cx, -cy);
		}
		body(g, mx, my);
		if (moved) g.pose().popMatrix();
	}

	/** Janela de vidro: sombra suave, borda de luz, miolo translucido, barra lateral e rodape mais escuros. */
	private void glass(GuiGraphicsExtractor g, float a) {
		float op = Mth.clamp(com.witch.module.GuiSettingsModule.menuOpacity() * 0.7f, 0.45f, 0.9f) * a;
		int rad = Math.max(6, ww / 36);
		int sbX = fx(438), hy = fy(142), fl = fy(1108);
		RenderUtil.softShadow(g, wx, wy + 6, ww, wh, ww / 10, RenderUtil.alpha(0xFF000000, 0.55f * a));
		RenderUtil.roundRect(g, wx, wy, ww, wh, rad, RenderUtil.alpha(0xFFFFFFFF, 0.12f * a));
		RenderUtil.roundRect(g, wx + 1, wy + 1, ww - 2, wh - 2, rad - 1, RenderUtil.alpha(0xFF14171B, op));
		g.enableScissor(wx, wy, sbX, wy + wh);
		RenderUtil.roundRect(g, wx + 1, wy + 1, ww - 2, wh - 2, rad - 1, RenderUtil.alpha(0xFF000000, 0.16f * a));
		g.disableScissor();
		g.enableScissor(sbX, fl, wx + ww, wy + wh);
		RenderUtil.roundRect(g, wx + 1, wy + 1, ww - 2, wh - 2, rad - 1, RenderUtil.alpha(0xFF000000, 0.14f * a));
		g.disableScissor();
		int line = RenderUtil.alpha(0xFFFFFFFF, 0.07f * a);
		g.fill(sbX, wy + 1, sbX + 1, wy + wh - 1, line);
		g.fill(sbX + 1, hy, wx + ww - 1, hy + 1, line);
		g.fill(sbX + 1, fl, wx + ww - 1, fl + 1, line);
		g.fill(fx(44), fy(1046), fx(394), fy(1046) + 1, line);
		g.fill(wx + rad, wy + 1, wx + ww - rad, wy + 2, RenderUtil.alpha(0xFFFFFFFF, 0.16f * a));

		int fh = FontUtil.height();
		// logo (caixa + ponto) e nome
		int ls = Math.max(10, fx(88) - fx(42));
		int lx = fx(42), ly = fy(84) - ls / 2;
		RenderUtil.roundRect(g, lx, ly, ls, ls, 3, RenderUtil.alpha(0xFFE2E4E8, a));
		RenderUtil.roundRect(g, lx + 1, ly + 1, ls - 2, ls - 2, 2, RenderUtil.alpha(0xFF0B0D10, a));
		int dot = Math.max(2, ls / 4);
		g.fill(lx + (ls - dot) / 2, ly + (ls - dot) / 2, lx + (ls + dot) / 2, ly + (ls + dot) / 2, RenderUtil.alpha(0xFFE2E4E8, a));
		FontUtil.draw(g, "Witch", fx(112), fy(84) - fh / 2, RenderUtil.alpha(0xFFF2F3F5, a));
		small(g, "CATEGORIES", fx(44), fy(192), RenderUtil.alpha(0xFF8A8F97, a));
		// dica de tecla no rodape
		String key = "RSHIFT";
		int kw = Math.round(FontUtil.width(key) * SMALL) + 8, kh = Math.round(fh * SMALL) + 4;
		int kx = fx(496), ky = fl + (wy + wh - fl - kh) / 2;
		RenderUtil.roundRect(g, kx, ky, kw, kh, 3, RenderUtil.alpha(0xFFFFFFFF, 0.14f * a));
		RenderUtil.roundRect(g, kx + 1, ky + 1, kw - 2, kh - 2, 2, RenderUtil.alpha(0xFF0F1114, a));
		small(g, key, kx + 4, ky + kh / 2f, RenderUtil.alpha(0xFFB5B9C0, a));
		small(g, "toggle GUI", kx + kw + 6, ky + kh / 2f, RenderUtil.alpha(0xFF8A8F97, a));
	}

	private static final float SMALL = 0.72f;

	/** Texto pequeno (escala), centrado na vertical em cy. */
	private static void small(GuiGraphicsExtractor g, String t, int x, float cy, int color) {
		g.pose().pushMatrix();
		g.pose().translate(x, cy - FontUtil.height() * SMALL / 2f);
		g.pose().scale(SMALL, SMALL);
		FontUtil.draw(g, t, 0, 0, color);
		g.pose().popMatrix();
	}

	private void body(GuiGraphicsExtractor g, int mx, int my) {
		long now = System.nanoTime();
		float speed = lastNanos == 0 ? 1f : Math.min(3f, (now - lastNanos) / 16_666_667f);
		lastNanos = now;
		openAnim = Mth.lerp(Math.min(1f, speed * com.witch.module.GuiAnimationModule.rate()), openAnim, 1f);
		float a = openAnim;
		tipNow = null;
		clampScroll();
		int fh = FontUtil.height();

		g.fill(0, 0, this.width, this.height, RenderUtil.alpha(0x30000000, a));

		glass(g, a);

		// categorias (a lista fica em branco na imagem)
		Category[] cats = Category.values();
		int bx = sideX(), bw = sideItemW(), bh = catH();
		for (int i = 0; i < cats.length; i++) {
			Category c = cats[i];
			int ry = catY(i);
			boolean sel = c == selected && search.isEmpty();
			boolean hov = RenderUtil.inside(mx, my, bx, ry, bw, bh);
			if (sel) {
				RenderUtil.roundRect(g, bx, ry, bw, bh, 5, RenderUtil.alpha(0xFF1D2025, a));
				g.fill(bx - 3, ry + bh / 4, bx - 1, ry + bh - bh / 4, RenderUtil.alpha(ACC, a));
			} else if (hov) {
				RenderUtil.roundRect(g, bx, ry, bw, bh, 5, RenderUtil.alpha(0xFF14171B, a));
			}
			int col = sel ? TXT : (hov ? 0xFFC4C8CE : 0xFF8E939B);
			CategoryIcons.draw(g, c, bx + Math.max(4, bw / 12), ry + (bh - 7) / 2, RenderUtil.alpha(col, a));
			String nm = c.name().charAt(0) + c.name().substring(1).toLowerCase(Locale.ROOT);
			FontUtil.draw(g, nm, bx + Math.max(16, bw / 5), ry + (bh - fh) / 2, RenderUtil.alpha(col, a));
		}
		// Music (8o item da lista)
		int my8 = catY(cats.length);
		boolean mh = RenderUtil.inside(mx, my, bx, my8, bw, bh);
		if (mh) RenderUtil.roundRect(g, bx, my8, bw, bh, 5, RenderUtil.alpha(0xFF14171B, a));
		CategoryIcons.blit(g, CategoryIcons.MUSIC, bx + Math.max(4, bw / 12) - 1, my8 + (bh - 7) / 2 - 1, 9, RenderUtil.alpha(mh ? 0xFFC4C8CE : 0xFF8E939B, a));
		FontUtil.draw(g, "Music", bx + Math.max(16, bw / 5), my8 + (bh - fh) / 2, RenderUtil.alpha(mh ? 0xFFC4C8CE : 0xFF8E939B, a));
		CategoryIcons.blit(g, CategoryIcons.SLIDERS, bx + Math.max(4, bw / 12) - 1, editY() + (bh - 7) / 2 - 1, 9, RenderUtil.alpha(0xFFB5B9C0, a));
		boolean eh = RenderUtil.inside(mx, my, bx, editY(), bw, bh);
		FontUtil.draw(g, "Edit HUD", bx + Math.max(16, bw / 5), editY() + (bh - fh) / 2, RenderUtil.alpha(eh ? TXT : 0xFF9AA0A8, a));

		// titulo do cabecalho e texto da busca
		String title = search.isEmpty() ? selected.name().charAt(0) + selected.name().substring(1).toLowerCase(Locale.ROOT) : "Search";
		FontUtil.draw(g, title, fx(530), fy(71) - fh / 2, RenderUtil.alpha(TXT, a));
		int sx = searchX(), sy = searchY(), sw = searchW(), sbh = searchH();
		RenderUtil.roundRect(g, sx, sy, sw, sbh, 5, RenderUtil.alpha(searchFocused ? 0x55FFFFFF : 0x18FFFFFF, a));
		RenderUtil.roundRect(g, sx + 1, sy + 1, sw - 2, sbh - 2, 4, RenderUtil.alpha(0x47000000, a));
		CategoryIcons.blit(g, CategoryIcons.SEARCH, fx(1370), sy + (sbh - 8) / 2, 8, RenderUtil.alpha(0xFF8A8F97, a));
		String shown;
		int scol;
		if (search.isEmpty() && !searchFocused) {
			shown = "Search modules";
			scol = 0xFF7A7F87;
		} else {
			boolean blink = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0;
			shown = search + (blink ? "|" : "");
			scol = TXT;
		}
		FontUtil.draw(g, ModuleRow.fit(shown, fx(1940) - fx(1436) - 6), fx(1436), sy + (sbh - fh) / 2, RenderUtil.alpha(scol, a));

		// FPS no rodape (RSHIFT toggle GUI ja vem na imagem)
		String fps = Minecraft.getInstance().getFps() + " FPS";
		FontUtil.draw(g, fps, fx(1940) - FontUtil.width(fps), fy(1116) - fh / 2, RenderUtil.alpha(0xFF6B7078, a));
		String pf = "Perfis";
		boolean ph = RenderUtil.inside(mx, my, fx(1940) - FontUtil.width(fps) - FontUtil.width(pf) - 16, fy(1116) - fh / 2 - 2, FontUtil.width(pf) + 4, fh + 4);
		FontUtil.draw(g, pf, fx(1940) - FontUtil.width(fps) - FontUtil.width(pf) - 14, fy(1116) - fh / 2, RenderUtil.alpha(ph ? 0xFFE2E4E8 : 0xFF6B7078, a));

		// lista de modulos (uma coluna)
		int cx = cx(), cy = cy(), cw = cw(), ch = ch();
		g.enableScissor(cx, cy, cx + cw, cy + ch);
		int base = cy - Math.round(scroll);
		for (Slot sl : slots()) {
			Module m = sl.m;
			int sx0 = cx + sl.x, sy0 = base + sl.y;
			if (sy0 + sl.h < cy || sy0 > cy + ch) continue;
			if (sl.block) {
				RenderUtil.roundRect(g, sx0, sy0 - 1, sl.w, sl.h + 2, 5, RenderUtil.alpha(0xFF23272D, a));
				RenderUtil.roundRect(g, sx0 + 1, sy0, sl.w - 2, sl.h, 4, RenderUtil.alpha(0xFF14171B, a));
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
			int border = RenderUtil.mix(RenderUtil.mix(0xFF262A31, 0xFF3A3F47, m.hoverAnim), 0xFFE2E4E8, t);
			int fill = RenderUtil.mix(RenderUtil.mix(0xFF16191E, 0xFF1B1F25, m.hoverAnim), 0xFF1D2025, t);
			if (t > 0.02f) RenderUtil.roundRect(g, sx0 - 2, sy0 - 2, sl.w + 4, CARD + 4, 7, RenderUtil.alpha(0xFFFFFFFF, 0.06f * t * a));
			RenderUtil.roundRect(g, sx0, sy0, sl.w, CARD, 5, RenderUtil.alpha(border, a));
			RenderUtil.roundRect(g, sx0 + 1, sy0 + 1, sl.w - 2, CARD - 2, 4, RenderUtil.alpha(fill, a));
			boolean has = !m.settings().isEmpty();
			boolean open = OPEN.contains(m);
			int right = sx0 + sl.w - 6;
			if (has) {
				CategoryIcons.blit(g, open ? CategoryIcons.CHEV_UP : CategoryIcons.CHEV_DOWN, right - 9, sy0 + (CARD - 9) / 2, 9, RenderUtil.alpha(open ? TXT : 0xFF8E939B, a));
				right -= 14;
			}
			int swx = right - 20, swy = sy0 + (CARD - 10) / 2;
			RenderUtil.roundRect(g, swx, swy, 20, 10, 5, RenderUtil.alpha(RenderUtil.mix(0xFF3A3E45, ACC, t), a));
			int kx = swx + 1 + Math.round(10 * t);
			RenderUtil.roundRect(g, kx, swy + 1, 8, 8, 4, RenderUtil.alpha(RenderUtil.mix(0xFF8A8F97, 0xFF0A0C0F, t), a));
			int nameMax = swx - (sx0 + 8) - 6;
			if (m.keybind.key() != 0) {
				String kl = m.keybind.label();
				int kw = FontUtil.width(kl) + 8;
				int kxx = swx - 6 - kw;
				RenderUtil.roundRect(g, kxx, sy0 + 4, kw, CARD - 8, 3, RenderUtil.alpha(0xFF3A3F47, a));
				RenderUtil.roundRect(g, kxx + 1, sy0 + 5, kw - 2, CARD - 10, 2, RenderUtil.alpha(0xFF0F1114, a));
				FontUtil.draw(g, kl, kxx + 4, sy0 + (CARD - fh) / 2, RenderUtil.alpha(0xFFB5B9C0, a));
				nameMax = kxx - (sx0 + 8) - 6;
			}
			int tc = RenderUtil.mix(0xFFB5B9C0, TXT, t);
			FontUtil.draw(g, ModuleRow.fit(m.name, Math.max(20, nameMax)), sx0 + 8, sy0 + (CARD - fh) / 2, RenderUtil.alpha(tc, a));
			if (hover) tipNow = m;
		}
		g.disableScissor();
		int total = contentH();
		if (total > ch) {
			int th = Math.max(14, Math.round(ch * (ch / (float) total)));
			float f = scroll / Math.max(1, total - ch);
			RenderUtil.roundRect(g, cx + cw - 3, cy + Math.round((ch - th) * f), 2, th, 1, RenderUtil.alpha(0xFFB5B9C0, a * (barDrag ? 0.95f : 0.55f)));
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
				RenderUtil.roundRect(g, tx - 1, ty - 1, tw + 2, th + 2, 5, RenderUtil.alpha(0xFF3A3F47, a));
				RenderUtil.roundRect(g, tx, ty, tw, th, 4, RenderUtil.alpha(0xFF0C0E11, a * 0.97f));
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
		searchFocused = RenderUtil.inside(mx, my, searchX(), searchY(), searchW(), searchH());
		if (searchFocused) return true;
		Category[] cats = Category.values();
		for (int i = 0; i < cats.length; i++) {
			if (RenderUtil.inside(mx, my, sideX(), catY(i), sideItemW(), catH())) {
				selected = cats[i];
				search = "";
				scroll = 0f;
				return true;
			}
		}
		if (RenderUtil.inside(mx, my, sideX(), catY(Category.values().length), sideItemW(), catH())) {
			Minecraft.getInstance().gui.setScreen(new MusicScreen());
			return true;
		}
		{
			String fpsT = Minecraft.getInstance().getFps() + " FPS", pfT = "Perfis";
			int fhh = FontUtil.height();
			if (RenderUtil.inside(mx, my, fx(1940) - FontUtil.width(fpsT) - FontUtil.width(pfT) - 16, fy(1116) - fhh / 2 - 2, FontUtil.width(pfT) + 4, fhh + 4)) {
				Minecraft.getInstance().gui.setScreen(new ProfilesScreen());
				return true;
			}
		}
		if (RenderUtil.inside(mx, my, sideX(), editY(), sideItemW(), catH())) {
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
				boolean onArrow = has && mx >= sx0 + sl.w - 20;
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
