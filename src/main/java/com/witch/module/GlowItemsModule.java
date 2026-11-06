package com.witch.module;

import com.witch.setting.ColorSetting;

/** Glow Items: contorno brilhante no item da mao (usa o mesmo canal de contorno dos mobs brilhando). */
public class GlowItemsModule extends Module {
	private static GlowItemsModule instance;
	private final ColorSetting color = add(new ColorSetting("Color", 0));

	public GlowItemsModule() {
		super("Glow Items", Category.VISUALS);
		instance = this;
	}

	/** Cor ARGB do contorno, ou 0 (sem contorno) se o modulo estiver desligado. */
	public static int outline() {
		if (instance == null || !instance.isEnabled()) return 0;
		return 0xFF000000 | (instance.color.get() & 0xFFFFFF);
	}
}
