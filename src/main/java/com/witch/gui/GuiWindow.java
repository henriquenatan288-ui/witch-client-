package com.witch.gui;

import com.witch.module.Category;
import com.witch.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Set;

import static com.witch.gui.ClickGuiMetrics.*;

/**
 * Janela de uma categoria do ClickGUI (header, lista de módulos, settings, scroll).
 * Lógica de desenho idêntica à original.
 */
public final class GuiWindow {
	private GuiWindow() {}

	/** Sweep animado no topo/base da janela. */
	public static void sweep(GuiGraphicsExtractor g, int x0, int x1, int y, float a, int seed, boolean rev) {
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

	private static float gl(float add) {
		return Math.min(1f, com.witch.module.GuiSettingsModule.opacity() + add);
	}

	/**
	 * Desenha a janela completa.
	 * @param tipOut se não-null, tipOut[0] recebe o módulo sob o mouse (tooltip)
	 */
	public static void render(GuiGraphicsExtractor g, Category c, int x, int y, int w,
							  int vh, int ch, float scroll, List<Module> modules, Set<Module> open,
							  Category dragWin, Category sbWin, int mx, int my, float speed, float a,
							  Module[] tipOut) {
		float maxScroll = Math.max(0, ch - vh);
		scroll = Mth.clamp(scroll, 0, maxScroll);

		int totalH = HEAD_H + vh + BOT;
		RenderUtil.roundRect(g, x - 1, y - 1, w + 2, totalH + 2, 7, RenderUtil.alpha(LINE, a));
		sweep(g, x + 6, x + w - 6, y - 1, a, c.ordinal(), false);
		sweep(g, x + 6, x + w - 6, y + totalH, a, c.ordinal(), true);
		RenderUtil.roundRect(g, x, y, w, totalH, 6, RenderUtil.alpha(0xFF121212, a * gl(0f)));
		RenderUtil.roundRect(g, x, y, w, HEAD_H, 6, RenderUtil.alpha(0xFF232323, a * gl(0.12f)));
		g.fill(x, y + HEAD_H - 6, x + w, y + HEAD_H, RenderUtil.alpha(0xFF232323, a * gl(0.12f)));
		g.fill(x, y + HEAD_H - 1, x + w, y + HEAD_H, RenderUtil.alpha(LINE, a * 0.8f));
		String title = c.label;
		FontUtil.draw(g, title, x + (w - FontUtil.width(title)) / 2, y + (HEAD_H - 1 - FontUtil.height()) / 2, RenderUtil.alpha(TXT, a));

		int by = y + HEAD_H;
		g.enableScissor(x, by, x + w, by + vh);
		int ry = by - Math.round(scroll);
		for (Module m : modules) {
			boolean hover = RenderUtil.inside(mx, my, x, ry, w, ROW) && RenderUtil.inside(mx, my, x, by, w, vh);
			boolean isOpen = open.contains(m);
			if (ModuleRow.draw(g, m, x, ry, w, hover, isOpen, dragWin != null, speed, a) && tipOut != null) {
				tipOut[0] = m;
			}
			ry += ROW;
			if (isOpen) {
				for (var s : m.settings()) {
					int sh = SettingRenderer.height(s, w);
					g.fill(x, ry, x + w, ry + sh, RenderUtil.alpha(SUB_BG, a * gl(0.05f)));
					g.fill(x, ry, x + 1, ry + sh, RenderUtil.alpha(LINE, a));
					SettingRenderer.draw(g, s, x, ry, w, sh, a, 0f);
					ry += sh;
				}
			}
		}
		g.disableScissor();

		if (maxScroll > 0) {
			int th = Math.max(16, Math.round(vh * (vh / (float) ch)));
			int ty = by + Math.round((vh - th) * (scroll / maxScroll));
			int bx = x + w - SB_VIS - 1;
			g.fill(bx, by + 1, bx + SB_VIS, by + vh - 1, RenderUtil.alpha(0xFFFFFFFF, a * 0.06f));
			g.fill(bx, ty, bx + SB_VIS, ty + th, RenderUtil.alpha(sbWin == c ? 0xFFFFFFFF : 0xFFA8A8A8, a * 0.9f));
		}
	}
}
