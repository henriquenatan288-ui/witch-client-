package com.witch.setting;

public final class ColorSetting extends Setting {
	public static final int[] PALETTE = {
			0xFF8A2BE2, // roxo
			0xFF22D3EE, // ciano
			0xFF34D399, // verde
			0xFFFACC15, // amarelo
			0xFFFB923C, // laranja
			0xFFEF4444, // vermelho
			0xFFF472B6, // rosa
			0xFFFFFFFF  // branco
	};

	private int index;
	/** Cor livre escolhida no seletor (HSV). Quando custom=true, ela vale mais que a paleta. */
	private boolean custom;
	private int customRgb = 0xFFFFFFFF;
	public float hue, sat, val = 1f;

	public ColorSetting(String name, int defIndex) {
		super(name);
		setIndex(defIndex);
	}

	public int get() {
		return custom ? customRgb : PALETTE[index];
	}

	public boolean isCustom() {
		return custom;
	}

	/** Define a cor livre por HSV (todos 0..1). */
	public void setHsv(float h, float sv, float v) {
		hue = Math.max(0f, Math.min(1f, h));
		sat = Math.max(0f, Math.min(1f, sv));
		val = Math.max(0f, Math.min(1f, v));
		customRgb = hsvToRgb(hue, sat, val);
		custom = true;
	}

	public static int hsvToRgb(float h, float s, float v) {
		float hh = (h >= 1f ? 0f : h) * 6f;
		int i = (int) Math.floor(hh);
		float f = hh - i;
		float p = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
		float r, g, b;
		switch (i) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
	}

	private void setFromRgb(int argb) {
		customRgb = 0xFF000000 | (argb & 0xFFFFFF);
		int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
		float max = Math.max(r, Math.max(g, b)) / 255f, min = Math.min(r, Math.min(g, b)) / 255f;
		float d = max - min;
		val = max;
		sat = max == 0f ? 0f : d / max;
		float h;
		if (d == 0f) h = 0f;
		else if (max == r / 255f) h = (((g - b) / 255f) / d + 6f) % 6f;
		else if (max == g / 255f) h = ((b - r) / 255f) / d + 2f;
		else h = ((r - g) / 255f) / d + 4f;
		hue = h / 6f;
	}

	public int index() {
		return index;
	}

	public void setIndex(int i) {
		index = Math.max(0, Math.min(PALETTE.length - 1, i));
		custom = false;
		setFromRgb(PALETTE[index]);
		custom = false;
	}

	@Override
	public double toNumber() {
		// indice da paleta (0..7) ou o ARGB da cor livre (sempre >= 0xFF000000, nunca colide com o indice)
		return custom ? (double) (customRgb & 0xFFFFFFFFL) : index;
	}

	@Override
	public void fromNumber(double v) {
		if (v >= PALETTE.length) {
			setFromRgb((int) (long) v);
			custom = true;
		} else {
			setIndex((int) Math.round(v));
		}
	}
}