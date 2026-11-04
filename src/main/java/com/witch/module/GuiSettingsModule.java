package com.witch.module;

import com.witch.setting.IntSetting;

/** Ajustes do proprio ClickGUI (opacidade global e tooltips). Fica na lista como um modulo qualquer. */
public class GuiSettingsModule extends Module {
	private static GuiSettingsModule instance;

	private final IntSetting opacity = add(new IntSetting("Opacity", 10, 100, 55, "%"));
	private final IntSetting glow = add(new IntSetting("Glow lines (0/1)", 0, 1, 1, ""));
	private final IntSetting tooltips = add(new IntSetting("Tooltips (0/1)", 0, 1, 1, ""));

	public GuiSettingsModule() {
		super("GUI Settings", Category.UTILITY);
		instance = this;
	}

	/** 0.1 a 1.0: opacidade do fundo das janelas. */
	public static float opacity() {
		return instance == null ? 0.55f : instance.opacity.get() / 100f;
	}

	public static boolean glow() {
		return instance == null || instance.glow.get() == 1;
	}

	public static boolean tooltips() {
		return instance == null || instance.tooltips.get() == 1;
	}
}
