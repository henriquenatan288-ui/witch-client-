package com.witch.module;

import net.minecraft.client.Minecraft;

/** Corre sozinho enquanto voce anda pra frente. */
public class AutoSprintModule extends Module {
	public AutoSprintModule() {
		super("Auto Sprint", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null) return;
		if (mc.options.keyUp.isDown() && !p.isShiftKeyDown() && p.getFoodData().getFoodLevel() > 6) {
			mc.options.keySprint.setDown(true);
		}
	}

	@Override
	protected void onDisable() {
		Minecraft.getInstance().options.keySprint.setDown(false);
	}
}
