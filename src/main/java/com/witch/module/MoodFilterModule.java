package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Filtro de clima na tela: escurece, puxa para cinza-azulado e aplica vinheta. Nao usa shader, funciona sem Iris. */
public class MoodFilterModule extends Module {
	private static MoodFilterModule instance;

	private final IntSetting dark = add(new IntSetting("Darkness", 0, 80, 8, "%"));
	private final IntSetting tint = add(new IntSetting("Cold tint", 0, 60, 8, "%"));
	private final IntSetting vignette = add(new IntSetting("Vignette", 0, 100, 35, "%"));

	public MoodFilterModule() {
		super("Mood Filter", Category.VISUALS);
		instance = this;
	}

	private static int argb(float a, int rgb) {
		int ai = Math.max(0, Math.min(255, (int) (a * 255f)));
		return (ai << 24) | (rgb & 0xFFFFFF);
	}

	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled()) return;
		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		float d = instance.dark.get() / 100f * 0.6f; // mais leve que o valor bruto: nao deixa o mundo lavado
		float t = instance.tint.get() / 100f * 0.6f;
		if (d > 0f) g.fill(0, 0, w, h, argb(d, 0x05070A));
		if (t > 0f) g.fill(0, 0, w, h, argb(t * 0.5f, 0x30445A));

		float v = instance.vignette.get() / 100f;
		if (v > 0f) {
			int layers = 14;
			int depth = Math.min(w, h) / 3;
			for (int i = 0; i < layers; i++) {
				float k = 1f - i / (float) layers;
				int in0 = depth * i / layers;
				int in1 = depth * (i + 1) / layers;
				int col = argb(v * 0.30f * k * k, 0x000000);
				g.fill(in0, in0, w - in0, in1, col);
				g.fill(in0, h - in1, w - in0, h - in0, col);
				g.fill(in0, in1, in1, h - in1, col);
				g.fill(w - in1, in1, w - in0, h - in1, col);
			}
		}
	}
}
