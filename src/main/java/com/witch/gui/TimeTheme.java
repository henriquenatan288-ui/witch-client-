package com.witch.gui;

import net.minecraft.util.Mth;

import java.time.LocalTime;

/**
 * Paleta forçada no visual escuro/atmosférico.
 * Mantém a estrutura antiga só por compatibilidade,
 * mas o resultado final é sempre o tema dark.
 */
public final class TimeTheme {
	private TimeTheme() {}

	// panel, search, dim, placeholder, input, hover
	private static final int[] DARK = {
		0xE00A0A0A,   // panel
		0xFF121212,   // search
		0xFF9A9A9A,   // dim
		0xFF555555,   // placeholder
		0xFFE8E8E8,   // input
		0x14FFFFFF    // hover
	};

	private static final float[] OVERRIDE_HOURS = {12f, 17.9f, 23f};
	private static int override = 2; // força noite por padrão

	public static int panel = DARK[0];
	public static int search = DARK[1];
	public static int dim = DARK[2];
	public static int placeholder = DARK[3];
	public static int input = DARK[4];
	public static int hover = DARK[5];

	public static float night = 1f;
	public static boolean sunUp = false;
	public static float bodyT = 0.5f;

	public static void cycleOverride() {
		override = override >= 2 ? -1 : override + 1;
	}

	public static void update() {
		// Sempre aplica o tema escuro (visual cinematográfico)
		panel = DARK[0];
		search = DARK[1];
		dim = DARK[2];
		placeholder = DARK[3];
		input = DARK[4];
		hover = DARK[5];
		night = 1f;
		sunUp = false;

		// Mantém o cálculo de bodyT só por compatibilidade
		float h;
		if (override >= 0) {
			h = OVERRIDE_HOURS[override];
		} else {
			LocalTime t = LocalTime.now();
			h = t.getHour() + t.getMinute() / 60f + t.getSecond() / 3600f;
		}
		if (h >= 6f && h < 18f) {
			bodyT = (h - 6f) / 12f;
		} else {
			float hh = h >= 18f ? h - 18f : h + 6f;
			bodyT = hh / 12f;
		}
	}
}
