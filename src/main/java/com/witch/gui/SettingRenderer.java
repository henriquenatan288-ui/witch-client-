package com.witch.gui;

import com.witch.config.Config;
import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import com.witch.setting.KeybindSetting;
import com.witch.setting.OreSetting;
import com.witch.setting.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static com.witch.gui.ClickGuiMetrics.*;

/**
 * Desenho e clique dos settings dentro de um módulo expandido.
 * Lógica idêntica à do ClickGuiScreen original.
 */
public final class SettingRenderer {
	private SettingRenderer() {}

	/** Opcao de sim/nao (0 ou 1): desenhada como interruptor. */
	private static boolean isToggle(Setting s) {
		return s instanceof IntSetting is && is.min == 0 && is.max == 1;
	}

	private static String toggleName(String n) {
		return n.replace(" (0/1)", "");
	}

	public static int height(Setting s, int w) {
		if (isToggle(s)) return ORE_H;
		if (s instanceof IntSetting) return INT_H;
		if (s instanceof ColorSetting) {
			int per = Math.max(1, (w - 16) / (SW + SW_GAP));
			int rows = (ColorSetting.PALETTE.length + per - 1) / per;
			return 12 + rows * (SW + SW_GAP) + 3;
		}
		return ORE_H;
	}

	public static void draw(GuiGraphicsExtractor g, Setting s, int x, int ry, int w, int sh, float a, float glAdd) {
		int fh = FontUtil.height();
		if (isToggle(s)) {
			IntSetting t = (IntSetting) s;
			boolean on = t.get() == 1;
			FontUtil.draw(g, toggleName(t.name), x + 7, ry + (sh - fh) / 2, RenderUtil.alpha(on ? TXT : TXT_DIM, a));
			int sx = x + w - 6 - 18;
			int sy = ry + (sh - 9) / 2;
			RenderUtil.roundRect(g, sx, sy, 18, 9, 4, RenderUtil.alpha(on ? Theme.accent() : 0xFF2A2A2A, a * (on ? 0.9f : 1f)));
			int kx = on ? sx + 10 : sx + 1;
			RenderUtil.roundRect(g, kx, sy + 1, 7, 7, 3, RenderUtil.alpha(on ? 0xFF111111 : 0xFFA8A8A8, a));
		} else if (s instanceof IntSetting is) {
			FontUtil.draw(g, is.name, x + 7, ry + 3, RenderUtil.alpha(TXT_DIM, a));
			String val = is.get() + is.suffix;
			FontUtil.draw(g, val, x + w - 6 - FontUtil.width(val), ry + 3, RenderUtil.alpha(TXT, a));
			int tx = x + 7;
			int tw = w - 14;
			int ty = ry + fh + 8;
			g.fill(tx, ty, tx + tw, ty + 3, RenderUtil.alpha(0xFF2A2A2A, a));
			int fw = Math.round(tw * is.fraction());
			if (fw > 0) g.fill(tx, ty, tx + fw, ty + 3, RenderUtil.alpha(Theme.accent(), a));
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

	/**
	 * Trata clique em setting. Retorna o IntSetting se o usuário começou a arrastar um slider;
	 * caso contrário null. Ações adiantadas vão em {@code pendingOut[0]}.
	 */
	public static IntSetting click(Setting s, int x, int ry, int w, int sh, double mx, double my, int fh,
								   Runnable[] pendingOut) {
		if (isToggle(s)) {
			IntSetting t = (IntSetting) s;
			pendingOut[0] = () -> { t.set(t.get() == 1 ? 0 : 1); Config.save(); };
			return null;
		}
		if (s instanceof IntSetting is) {
			int tx = x + 7;
			int tw = w - 14;
			int ty = ry + fh + 8;
			if (RenderUtil.inside(mx, my, tx - 4, ty - 7, tw + 8, 17)) {
				is.setFraction((float) ((mx - tx) / Math.max(1, tw)));
				return is;
			}
		} else if (s instanceof ColorSetting cs) {
			int per = Math.max(1, (w - 16) / (SW + SW_GAP));
			for (int i = 0; i < ColorSetting.PALETTE.length; i++) {
				int sx = x + 7 + (i % per) * (SW + SW_GAP);
				int sy = ry + 13 + (i / per) * (SW + SW_GAP);
				if (RenderUtil.inside(mx, my, sx - 2, sy - 2, SW + 4, SW + 4)) {
					final int idx = i;
					pendingOut[0] = () -> { cs.setIndex(idx); Config.save(); };
					return null;
				}
			}
		} else if (s instanceof KeybindSetting ks) {
			pendingOut[0] = () -> ks.listening = !ks.listening;
		} else if (s instanceof OreSetting os) {
			final boolean onChip = mx >= x + w - 6 - 16;
			pendingOut[0] = () -> {
				if (onChip) os.cycleColor(); else os.toggle();
				Config.save();
			};
		}
		return null;
	}
}
