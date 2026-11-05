package com.witch.module;

import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import net.minecraft.client.renderer.fog.FogData;

/** Neblina: aproxima o fim da visao e puxa a cor da nevoa. So a nevoa de distancia (agua e lava ficam como estao). */
public class FogModule extends Module {
	private static FogModule instance;

	private final IntSetting density = add(new IntSetting("Density", 0, 100, 35, "%"));
	private final ColorSetting color = add(new ColorSetting("Color", 0));
	private final IntSetting tint = add(new IntSetting("Tint", 0, 100, 40, "%"));

	public FogModule() {
		super("Fog", Category.VISUALS);
		instance = this;
	}

	public static void apply(FogData d) {
		if (instance == null || !instance.isEnabled() || d == null) return;
		float k = instance.density.get() / 100f;
		if (k > 0f && d.renderDistanceEnd > 0f) {
			d.renderDistanceEnd = Math.max(12f, d.renderDistanceEnd * (1f - 0.75f * k));
			d.renderDistanceStart = Math.max(0f, d.renderDistanceStart * (1f - 0.9f * k));
		}
		float t = instance.tint.get() / 100f;
		if (t > 0f && d.color != null) {
			int c = instance.color.get();
			d.color.x = d.color.x * (1f - t) + ((c >> 16) & 0xFF) / 255f * t;
			d.color.y = d.color.y * (1f - t) + ((c >> 8) & 0xFF) / 255f * t;
			d.color.z = d.color.z * (1f - t) + (c & 0xFF) / 255f * t;
		}
	}
}
