package com.witch.module;

import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import net.minecraft.client.renderer.state.level.SkyRenderState;

/** Ceu com cor de sua escolha e mais estrelas (usa o estado de render do ceu, sem shader). */
public class SkyModule extends Module {
	private static SkyModule instance;

	private final ColorSetting color = add(new ColorSetting("Color", 0));
	private final IntSetting tint = add(new IntSetting("Tint", 0, 100, 55, "%"));
	private final IntSetting stars = add(new IntSetting("Stars", 0, 100, 70, "%"));

	public SkyModule() {
		super("Sky", Category.VISUALS);
		instance = this;
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1f - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1f - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1f - t) + (b & 0xFF) * t);
		return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
	}

	/** Chamado pelo SkyRendererMixin depois do jogo preencher o estado do ceu. */
	public static void apply(SkyRenderState s) {
		if (instance == null || !instance.isEnabled() || s == null) return;
		s.skyColor = mix(s.skyColor, instance.color.get(), instance.tint.get() / 100f);
		float st = instance.stars.get() / 100f;
		if (st > s.starBrightness) s.starBrightness = st;
	}
}
