package com.witch.gui;

public final class Theme {
	private Theme() {}

	/** Cor de destaque escolhida em GUI Settings. */
	public static int accent() {
		return com.witch.module.GuiSettingsModule.accent();
	}

	public static final int ACCENT = 0xFFE0E0E0;
	/** Cor de destaque do ClickGUI (monocromatico preto/branco). */
	public static final int GUI_ACCENT = 0xFFFFFFFF;
	public static final int PANEL = 0xF2FFFFFF;
	public static final int SIDEBAR_ITEM_ACTIVE = 0xFF3A3A3A;
	public static final int SEARCH_BG = 0xFF101010;
	public static final int MODULE_ON = 0xFF3A3A3A;
	public static final int MODULE_HOVER = 0x228A2BE2;

	// TEXT = texto branco (usado sobre a pilula roxa e no HUD escuro)
	public static final int TEXT = 0xFFFFFFFF;
	// TEXT_DIM = texto roxo sobre o painel branco
	public static final int TEXT_DIM = 0xFF8E8E8E;
	public static final int TEXT_PLACEHOLDER = 0xFF666666;
	public static final int TEXT_INPUT = 0xFFFFFFFF;
}