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
	private final IntSetting size = add(new IntSetting("Size", 2, 14, 5, ""));
	private final IntSetting gap = add(new IntSetting("Gap", 0, 10, 3, ""));
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
	private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, int col) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x99000000);
		g.fill(x, y, x + w, y + h, col);
	}

	private void draw(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (!mc.options.getCameraType().isFirstPerson()) return;
		int cx = mc.getWindow().getGuiScaledWidth() / 2;
		int cy = mc.getWindow().getGuiScaledHeight() / 2;
		int col = color.get();
		int t = thick.get();
		int s = size.get();
		int gp = gap.get();
		int st = style.get();
		int half = t / 2;

		if (st == 1 || st == 4) {
			bar(g, cx - gp - s, cy - half, s, t, col);
			bar(g, cx + gp, cy - half, s, t, col);
			bar(g, cx - half, cy - gp - s, t, s, col);
			bar(g, cx - half, cy + gp, t, s, col);
		}
		if (st == 2 || st == 4) {
			int d = Math.max(1, t);
			bar(g, cx - d / 2, cy - d / 2, d, d, col);
		}
		if (st == 3) {
			int r = gp + s / 2 + 2;
			for (int i = 0; i < 72; i++) {
				double a = i * Math.PI * 2 / 72;
				int px = cx + (int) Math.round(Math.cos(a) * r);
				int py = cy + (int) Math.round(Math.sin(a) * r);
				g.fill(px - 1, py - 1, px + t + 1, py + t + 1, 0x66000000);
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
