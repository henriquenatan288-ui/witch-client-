package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Sobe blocos sem pular (aumenta a altura de degrau). Anticheats detectam isso. */
public class StepModule extends Module {
	private final IntSetting height = add(new IntSetting("Height", 1, 3, 1, ""));

	public StepModule() {
		super("Step", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		var p = Minecraft.getInstance().player;
		if (p == null) return;
		var a = p.getAttribute(Attributes.STEP_HEIGHT);
		if (a != null) a.setBaseValue(height.get());
	}

	@Override
	protected void onDisable() {
		var p = Minecraft.getInstance().player;
		if (p == null) return;
		var a = p.getAttribute(Attributes.STEP_HEIGHT);
		if (a != null) a.setBaseValue(0.6);
	}
}
