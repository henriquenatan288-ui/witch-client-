package com.witch.module;

import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;

/** Ajustes do proprio ClickGUI (opacidade global e tooltips). Fica na lista como um modulo qualquer. */
public class GuiSettingsModule extends Module {
	private static GuiSettingsModule instance;

	private final ColorSetting accent = add(new ColorSetting("Accent", 7));
	private final IntSetting opacity = add(new IntSetting("Window opacity", 0, 100, 12, "%"));
	private final IntSetting menuOpacity = add(new IntSetting("Menu opacity", 5, 100, 100, "%"));
	private final IntSetting tooltips = add(new IntSetting("Tooltips (0/1)", 0, 1, 1, ""));

	public GuiSettingsModule() {
		super("GUI Settings", Category.UTILITY);
		instance = this;
	}

	/** 0.1 a 1.0: opacidade do fundo das janelas. */
	public static float opacity() {
		return instance == null ? 0.12f : instance.opacity.get() / 100f;
	}

	/** 0.05 a 1.0: opacidade da moldura e dos botoes do ClickGUI (texto e icones nao mudam). */
	public static float menuOpacity() {
		return instance == null ? 1f : instance.menuOpacity.get() / 100f;
	}

	/** Cor de destaque (padrao branco). */
	public static int accent() {
		return instance == null ? 0xFFFFFFFF : instance.accent.get();
	}

	public static boolean glow() {
		return false; // linhas brilhantes removidas
	}

	public static boolean tooltips() {
		return instance == null || instance.tooltips.get() == 1;
	}
}
