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

	private static final int SV_H = 34, HUE_H = 7, PICK_GAP = 4;

	// arraste do seletor de cor (um por vez)
	private static ColorSetting dragColor;
	private static int dragMode; // 1 = quadrado SV, 2 = barra de matiz
	private static int dragX, dragY, dragW;

	public static boolean dragging() { return dragColor != null; }

	public static void drag(double mx, double my) {
		if (dragColor == null) return;
		float fx = (float) Math.max(0, Math.min(1, (mx - dragX) / Math.max(1, dragW)));
		if (dragMode == 1) {
			float fy = (float) Math.max(0, Math.min(1, (my - dragY) / SV_H));
			dragColor.setHsv(dragColor.hue, fx, 1f - fy);
		} else {
			dragColor.setHsv(fx, dragColor.sat == 0f && dragColor.val == 0f ? 1f : dragColor.sat, dragColor.val == 0f ? 1f : dragColor.val);
		}
	}

	public static void release() {
		if (dragColor != null) Config.save();
		dragColor = null;
	}

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
			return 12 + rows * (SW + SW_GAP) + 3 + PICK_GAP + SV_H + PICK_GAP + HUE_H + 4;
		}
		return ORE_H;
	}

	public static void draw(GuiGraphicsExtractor g, Setting s, int x, int ry, int w, int sh, float a, float glAdd) {
		int fh = FontUtil.height();
		if (isToggle(s)) {
			IntSetting t = (IntSetting) s;
			boolean on = t.get() == 1;
			FontUtil.draw(g, toggleName(t.name), x + 7, ry + (sh - fh) / 2, RenderUtil.alpha(on ? TXT : TXT_DIM, a));
			int sx = x + w - 6 - 20;
			int sy = ry + (sh - 10) / 2;
			RenderUtil.roundRect(g, sx, sy, 20, 10, 5, RenderUtil.alpha(on ? 0xFFFFFFFF : 0xFF3A3E45, a));
			int kx = on ? sx + 11 : sx + 1;
			RenderUtil.roundRect(g, kx, sy + 1, 8, 8, 4, RenderUtil.alpha(on ? 0xFF0A0C0F : 0xFF8A8F97, a));
		} else if (s instanceof IntSetting is) {
			FontUtil.draw(g, is.name, x + 7, ry + 3, RenderUtil.alpha(TXT_DIM, a));
			String val = is.get() + is.suffix;
			FontUtil.draw(g, val, x + w - 6 - FontUtil.width(val), ry + 3, RenderUtil.alpha(TXT, a));
			int tx = x + 7;
			int tw = w - 14;
			int ty = ry + fh + 8;
			RenderUtil.roundRect(g, tx, ty, tw, 3, 1, RenderUtil.alpha(0xFF2A2E35, a));
			int fw = Math.round(tw * is.fraction());
			if (fw > 0) RenderUtil.roundRect(g, tx, ty, fw, 3, 1, RenderUtil.alpha(0xFFFFFFFF, a));
			RenderUtil.roundRect(g, tx + fw - 4, ty - 3, 9, 9, 4, RenderUtil.alpha(0xFF3A3F47, a));
			RenderUtil.roundRect(g, tx + fw - 3, ty - 2, 7, 7, 3, RenderUtil.alpha(0xFFFFFFFF, a));
		} else if (s instanceof ColorSetting cs) {
			FontUtil.draw(g, cs.name, x + 7, ry + 3, RenderUtil.alpha(TXT_DIM, a));
			int per = Math.max(1, (w - 16) / (SW + SW_GAP));
			for (int i = 0; i < ColorSetting.PALETTE.length; i++) {
				int sx = x + 7 + (i % per) * (SW + SW_GAP);
				int sy = ry + 13 + (i / per) * (SW + SW_GAP);
				if (i == cs.index()) RenderUtil.roundRect(g, sx - 1, sy - 1, SW + 2, SW + 2, 3, RenderUtil.alpha(TXT, a));
				RenderUtil.roundRect(g, sx, sy, SW, SW, 2, RenderUtil.alpha(ColorSetting.PALETTE[i], a));
			}
			int rows = (ColorSetting.PALETTE.length + per - 1) / per;
			int py = ry + 13 + rows * (SW + SW_GAP) + PICK_GAP - SW_GAP + 1;
			int px = x + 7, pw = w - 14;
			RenderUtil.roundRect(g, px - 1, py - 1, pw + 2, SV_H + 2, 3, RenderUtil.alpha(0xFF3A3A3A, a));
			for (int cxp = 0; cxp < pw; cxp += 2) {
				int cw2 = Math.min(2, pw - cxp);
				int top = ColorSetting.hsvToRgb(cs.hue, (cxp + cw2 / 2f) / pw, 1f);
				g.fillGradient(px + cxp, py, px + cxp + cw2, py + SV_H, RenderUtil.alpha(top, a), RenderUtil.alpha(0xFF000000, a));
			}
			int kx = px + Math.round(cs.sat * (pw - 1));
			int ky = py + Math.round((1f - cs.val) * (SV_H - 1));
			RenderUtil.roundRect(g, kx - 3, ky - 3, 7, 7, 3, RenderUtil.alpha(0xFFFFFFFF, a));
			RenderUtil.roundRect(g, kx - 2, ky - 2, 5, 5, 2, RenderUtil.alpha(cs.get(), a));
			int hy = py + SV_H + PICK_GAP;
			for (int cxp = 0; cxp < pw; cxp += 2) {
				int cw2 = Math.min(2, pw - cxp);
				g.fill(px + cxp, hy, px + cxp + cw2, hy + HUE_H, RenderUtil.alpha(ColorSetting.hsvToRgb((cxp + cw2 / 2f) / pw, 1f, 1f), a));
			}
			int hx = px + Math.round(cs.hue * (pw - 1));
			RenderUtil.roundRect(g, hx - 2, hy - 2, 5, HUE_H + 4, 2, RenderUtil.alpha(0xFFFFFFFF, a));
		} else if (s instanceof KeybindSetting ks) {
			FontUtil.draw(g, ks.name, x + 7, ry + (sh - fh) / 2, RenderUtil.alpha(TXT_DIM, a));
			String lab = ks.listening ? "..." : ks.label();
			int cw = Math.max(26, FontUtil.width(lab) + 10);
			int cx = x + w - 6 - cw;
			RenderUtil.roundRect(g, cx, ry + 2, cw, sh - 4, 3, RenderUtil.alpha(ks.listening ? 0xFFE2E4E8 : 0xFF3A3F47, a));
			RenderUtil.roundRect(g, cx + 1, ry + 3, cw - 2, sh - 6, 2, RenderUtil.alpha(0xFF0F1114, a));
			FontUtil.draw(g, lab, cx + (cw - FontUtil.width(lab)) / 2, ry + (sh - fh) / 2, RenderUtil.alpha(0xFFB5B9C0, a));
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
			int rows = (ColorSetting.PALETTE.length + per - 1) / per;
			int py = ry + 13 + rows * (SW + SW_GAP) + PICK_GAP - SW_GAP + 1;
			int px = x + 7, pw = w - 14;
			if (RenderUtil.inside(mx, my, px, py, pw, SV_H)) {
				dragColor = cs; dragMode = 1; dragX = px; dragY = py; dragW = pw;
				drag(mx, my);
			} else if (RenderUtil.inside(mx, my, px, py + SV_H + PICK_GAP - 2, pw, HUE_H + 4)) {
				dragColor = cs; dragMode = 2; dragX = px; dragY = py; dragW = pw;
				drag(mx, my);
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
