package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Filtro de cor na tela (so tinta, sem shader): 1 quente, 2 frio, 3 neon, 4 escuro, 5 por do sol, 6 menta. */
public class FilterModule extends Module {
	private static FilterModule instance;

	private static final int[] TOP = {0xFFC070, 0x6AA8E8, 0xB040FF, 0x000000, 0xFF8040, 0x70F0C0};
	private static final int[] BOTTOM = {0xFF9040, 0x3060B0, 0x2040FF, 0x101020, 0x6030A0, 0x20A080};
	private static final float[] BASE_A = {0.16f, 0.18f, 0.16f, 0.28f, 0.17f, 0.14f};

	private final IntSetting preset = add(new IntSetting("Preset", 1, 6, 5, ""));
	private final IntSetting strength = add(new IntSetting("Strength", 0, 100, 60, "%"));
	private final IntSetting vignette = add(new IntSetting("Vignette", 0, 100, 30, "%"));

	public FilterModule() {
		super("Filter", Category.VISUALS);
		instance = this;
	}

	private static int argb(float a, int rgb) {
		int ai = Math.max(0, Math.min(255, (int) (a * 255f)));
		return (ai << 24) | (rgb & 0xFFFFFF);
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1f - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1f - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1f - t) + (b & 0xFF) * t);
		return (r << 16) | (g << 8) | bl;
	}

	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled()) return;
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		int i = instance.preset.get() - 1;
		float s = instance.strength.get() / 100f;
		if (s > 0f) {
			// um unico degrade vertical: sem faixas sobrepostas (as bandas antigas deixavam linhas na tela)
			g.fillGradient(0, 0, w, h, argb(BASE_A[i] * s, TOP[i]), argb(BASE_A[i] * s, BOTTOM[i]));
		}
		float v = instance.vignette.get() / 100f;
		if (v > 0f) {
			int vc = mix(BOTTOM[i], 0x000000, 0.7f);
			int depth = h / 3;
			int c = argb(v * 0.28f, vc);
			g.fillGradient(0, 0, w, depth, c, argb(0f, vc));
			g.fillGradient(0, h - depth, w, h, argb(0f, vc), c);
		}
	}
}
