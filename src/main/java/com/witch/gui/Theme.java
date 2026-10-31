package com.witch.gui;

/** Paleta unica da GUI: escura, fria e quase monocromatica. Cores em ARGB. */
public final class Theme {
	private Theme() {}

	public static final int ACCENT = 0xFF6B8AFF;

	// Fundo do painel e campos (campos sao overlays brancos translucidos)
	public static final int PANEL = 0xE00C0C0C;
	public static final int SEARCH = 0x14FFFFFF;

	// Estados (overlays sutis)
	public static final int HOVER = 0x14FFFFFF;
	public static final int SIDEBAR_ITEM_ACTIVE = 0x1AFFFFFF;
	public static final int MODULE_ON = 0x1AFFFFFF;

	// Texto
	public static final int TEXT = 0xFFE8E8E8;
	public static final int DIM = 0xFF888888;
	public static final int PLACEHOLDER = 0xFF555555;
	public static final int INPUT = 0xFFE0E0E0;
}
