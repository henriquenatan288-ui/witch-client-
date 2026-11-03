package com.witch.gui;

import com.witch.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import static com.witch.gui.ClickGuiMetrics.*;

/**
 * Uma linha de módulo no ClickGUI (nome, animação on/hover, seta +/−).
 * Lógica de desenho idêntica à original.
 */
public final class ModuleRow {
	private ModuleRow() {}

	public static String fit(String t, int maxW) {
		if (FontUtil.width(t) <= maxW) return t;
		String r = t;
		while (r.length() > 1 && FontUtil.width(r + "..") > maxW) r = r.substring(0, r.length() - 1);
		return r.trim() + "..";
	}

	/**
	 * Desenha a linha do módulo. Atualiza hoverAnim/toggleAnim no próprio Module.
	 * @return true se o mouse está sobre a linha (para tooltip)
	 */
	public static boolean draw(GuiGraphicsExtractor g, Module m, int x, int ry, int w,
							   boolean hover, boolean open, boolean dragging, float speed, float a) {
		int fh = FontUtil.height();
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
		if (!m.settings().isEmpty()) {
			String ar = open ? "-" : "+";
			FontUtil.draw(g, ar, x + w - 11, ry + (ROW - fh) / 2, RenderUtil.alpha(open ? TXT : TXT_DIM, a));
		}
		return hover && !dragging;
	}
}
