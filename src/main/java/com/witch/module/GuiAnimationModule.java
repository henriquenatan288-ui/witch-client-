package com.witch.module;

import com.witch.setting.IntSetting;

/** Muda a animacao de abertura do ClickGUI. Desligado = fade simples (padrao). */
public class GuiAnimationModule extends Module {
	private static GuiAnimationModule instance;
	// 1 Zoom, 2 Sobe, 3 Desce, 4 Lateral, 5 Pop (estoura e assenta), 6 Fade
	private final IntSetting style = add(new IntSetting("Style (1-6)", 1, 6, 1, ""));
	private final IntSetting speed = add(new IntSetting("Speed", 1, 10, 5, ""));

	public GuiAnimationModule() {
		super("GUI Animation", Category.VISUALS);
		instance = this;
	}

	/** 0 = fade padrao (modulo desligado). */
	public static int style() {
		return instance == null || !instance.isEnabled() ? 6 : instance.style.get();
	}

	/** Fator de lerp por frame (0.04 .. 0.4). */
	public static float rate() {
		return instance == null || !instance.isEnabled() ? 0.2f : instance.speed.get() * 0.04f;
	}
}
