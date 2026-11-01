package com.witch.module;

import com.witch.setting.IntSetting;

public class BreakDelayModule extends Module {
	public static BreakDelayModule INSTANCE;
	private final IntSetting cooldown = this.add(new IntSetting("Cooldown", 0, 5, 0, " ticks"));

	public BreakDelayModule() {
		super("Break Delay", Category.UTILITY);
		INSTANCE = this;
	}

	public static int cooldownOrVanilla() {
		return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE.cooldown.get() : -1;
	}
}
