package com.witch.module;

import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;

/** China hat: chapeu em cone flutuando na cabeca, em galaxia (shader) ou spectral (translucido, brilhante). Cosmetico. */
public class ChinaHatModule extends Module {
	public static ChinaHatModule instance;
	public static volatile boolean on;

	public final IntSetting target = add(new IntSetting("Alvo (1 eu, 2 todos os jogadores)", 1, 2, 1, ""));
	public final IntSetting look = add(new IntSetting("Visual (1 galaxy, 2 spectral)", 1, 2, 1, ""));
	public final IntSetting style = add(new IntSetting("Estilo da galaxy (1 ciano 2 roxo 3 fogo 4 vazio 5 aurora 6 rosa 7 arco-iris)", 1, 7, 2, ""));
	public final IntSetting size = add(new IntSetting("Tamanho", 1, 10, 5, ""));
	public final ColorSetting spectral = add(new ColorSetting("Cor do spectral", 1));

	public ChinaHatModule() {
		super("China Hat", Category.AVATAR);
		instance = this;
	}

	@Override
	protected void onEnable() {
		on = true;
	}

	@Override
	protected void onDisable() {
		on = false;
	}

	public static boolean wants(boolean isSelf) {
		var m = instance;
		return m != null && on && (m.target.get() == 2 || isSelf);
	}
}
