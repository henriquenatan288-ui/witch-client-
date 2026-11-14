package com.witch.gui;

public final class Theme {
	private Theme() {}

	/** Cor de destaque escolhida em GUI Settings. */
	public static int accent() {
		return com.witch.module.GuiSettingsModule.accent();
	}

	// Paleta unica (cinza frio): GUI, HUD, titulo e efeitos devem usar estes valores.
	public static final int SURFACE = 0xFF16191E, SURFACE_HI = 0xFF1D2025, WELL = 0xFF0F1114;
	public static final int BORDER = 0xFF262A31, BORDER_HI = 0xFF3A3F47, TRACK = 0xFF2A2E35;
	public static final int TXT_HI = 0xFFE2E4E8, TXT_MID = 0xFFB5B9C0, TXT_LOW = 0xFF8E939B, TXT_MUTE = 0xFF6B7078;

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