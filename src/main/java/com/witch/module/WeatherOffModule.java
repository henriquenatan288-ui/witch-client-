package com.witch.module;

import net.minecraft.client.Minecraft;

/** Tira chuva e trovoada da sua tela (so visual, o servidor segue com o clima dele). */
public class WeatherOffModule extends Module {
	public WeatherOffModule() {
		super("Weather Off", Category.VISUALS);
	}

	@Override
	public void onTick() {
		var level = Minecraft.getInstance().level;
		if (level == null) return;
		level.setRainLevel(0f);
		level.setThunderLevel(0f);
	}
}
