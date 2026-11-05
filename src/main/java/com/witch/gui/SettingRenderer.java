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
			RenderUtil.roundRect(g, tx, ty, tw, 3, 1, RenderUtil.alpha(0xFF2A2A2A, a));
			int fw = Math.round(tw * is.fraction());
			if (fw > 0) RenderUtil.roundRect(g, tx, ty, fw, 3, 1, RenderUtil.alpha(Theme.accent(), a));
			RenderUtil.roundRect(g, tx + fw - 2, ty - 2, 4, 7, 2, RenderUtil.alpha(TXT, a));
		} else if (s instanceof ColorSetting cs) {
			FontUtil.draw(g, cs.name, x + 7, ry + 3, RenderUtil.alpha(TXT_DIM, a));
			int per = Math.max(1, (w - 16) / (SW + SW_GAP));
			for (int i = 0; i < ColorSetting.PALETTE.length; i++) {
				int sx = x + 7 + (i % per) * (SW + SW_GAP);
				int sy = ry + 13 + (i / per) * (SW + SW_GAP);
				if (i == cs.index()) RenderUtil.roundRect(g, sx - 1, sy - 1, SW + 2, SW + 2, 3, RenderUtil.alpha(TXT, a));
				RenderUtil.roundRect(g, sx, sy, SW, SW, 2, RenderUtil.alpha(ColorSetting.PALETTE[i], a));
			}
		} else if (s instanceof KeybindSetting ks) {
			FontUtil.draw(g, ks.name, x + 7, ry + (sh - fh) / 2, RenderUtil.alpha(TXT_DIM, a));
			String lab = ks.listening ? "..." : ks.label();
			int cw = Math.max(26, FontUtil.width(lab) + 10);
			int cx = x + w - 6 - cw;
			RenderUtil.roundRect(g, cx, ry + 2, cw, sh - 4, 3, RenderUtil.alpha(ks.listening ? 0xFF4A4A4A : 0xFF222222, a));
			FontUtil.draw(g, lab, cx + (cw - FontUtil.width(lab)) / 2, ry + (sh - fh) / 2, RenderUtil.alpha(TXT, a));
		} else if (s instanceof OreSetting os) {
			int bx = x + 7;
			int bY = ry + (sh - 9) / 2;
			RenderUtil.roundRect(g, bx, bY, 9, 9, 3, RenderUtil.alpha(0xFF3A3A3A, a));
			RenderUtil.roundRect(g, bx + 1, bY + 1, 7, 7, 2, RenderUtil.alpha(0xFF0C0C0C, a));
			if (os.enabled()) RenderUtil.roundRect(g, bx + 2, bY + 2, 5, 5, 1, RenderUtil.alpha(TXT, a));
			FontUtil.draw(g, os.name, bx + 14, ry + (sh - fh) / 2, RenderUtil.alpha(os.enabled() ? TXT : TXT_DIM, a));
			int cx = x + w - 6 - 12;
			RenderUtil.roundRect(g, cx - 1, bY - 1, 14, 11, 3, RenderUtil.alpha(0xFF3A3A3A, a));
			RenderUtil.roundRect(g, cx, bY, 12, 9, 2, RenderUtil.alpha(os.color(), a));
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
