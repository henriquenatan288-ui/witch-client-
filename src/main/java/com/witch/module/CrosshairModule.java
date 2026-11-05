package com.witch.module;

import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Mira customizada no lugar da vanilla (4 estilos, cor, tamanho, espaco e espessura). */
public class CrosshairModule extends Module {
	private static CrosshairModule instance;
	private static boolean hooked;

	private final IntSetting style = add(new IntSetting("Style (1 cross 2 dot 3 ring 4 both)", 1, 4, 1, ""));
	private final ColorSetting color = add(new ColorSetting("Color", 7));
	private final IntSetting size = add(new IntSetting("Size", 1, 14, 2, ""));
	private final IntSetting gap = add(new IntSetting("Gap", 0, 10, 2, ""));
	private final IntSetting opacity = add(new IntSetting("Opacity", 20, 100, 85, "%"));
	private final IntSetting outline = add(new IntSetting("Outline (0/1)", 0, 1, 1, ""));
	private final IntSetting thick = add(new IntSetting("Thickness", 1, 3, 1, ""));

	public CrosshairModule() {
		super("Crosshair", Category.VISUALS);
		instance = this;
		hook();
	}

	private static boolean active() {
		return instance != null && instance.isEnabled();
	}

	private static void hook() {
		if (hooked) return;
		hooked = true;
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) -> {
			if (!active()) {
				original.extractRenderState(graphics, delta);
				return;
			}
			instance.draw(graphics);
		});
	}

	/** Barra com contorno escuro pra ler em qualquer fundo. */
	private void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, int col) {
		if (outline.get() == 1) g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x40000000);
		g.fill(x, y, x + w, y + h, col);
	}

	private void draw(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (!mc.options.getCameraType().isFirstPerson()) return;
		int cx = mc.getWindow().getGuiScaledWidth() / 2;
		int cy = mc.getWindow().getGuiScaledHeight() / 2;
		int col = com.witch.gui.RenderUtil.alpha(color.get(), opacity.get() / 100f);
		int t = thick.get();
		int s = size.get();
		int gp = gap.get();
		int st = style.get();
		// o centro da mira e o bloco t x t em (x0, y0): bracos simetricos dos dois lados
		int x0 = (mc.getWindow().getGuiScaledWidth() - t) / 2;
		int y0 = (mc.getWindow().getGuiScaledHeight() - t) / 2;

		if (st == 1 || st == 4) {
			bar(g, x0 - gp - s, y0, s, t, col);
			bar(g, x0 + t + gp, y0, s, t, col);
			bar(g, x0, y0 - gp - s, t, s, col);
			bar(g, x0, y0 + t + gp, t, s, col);
		}
		if (st == 2 || st == 4) {
			bar(g, x0, y0, t, t, col);
		}
		if (st == 3) {
			int r = gp + s / 2 + 2;
			cx = x0 + t / 2;
			cy = y0 + t / 2;
			for (int i = 0; i < 72; i++) {
				double a = i * Math.PI * 2 / 72;
				int px = cx + (int) Math.round(Math.cos(a) * r);
				int py = cy + (int) Math.round(Math.sin(a) * r);
				if (outline.get() == 1) g.fill(px - 1, py - 1, px + t + 1, py + t + 1, 0x30000000);
			}
			for (int i = 0; i < 72; i++) {
				double a = i * Math.PI * 2 / 72;
				int px = cx + (int) Math.round(Math.cos(a) * r);
				int py = cy + (int) Math.round(Math.sin(a) * r);
				g.fill(px, py, px + t, py + t, col);
			}
		}
	}
}
