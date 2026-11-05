package com.witch.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Sobe paredes enquanto voce anda de encontro a elas. Anticheats detectam isso. */
public class SpiderModule extends Module {
	public SpiderModule() {
		super("Spider", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || p.isPassenger() || p.getAbilities().flying) return;
		if (p.horizontalCollision && mc.options.keyUp.isDown()) {
			Vec3 v = p.getDeltaMovement();
			p.setDeltaMovement(v.x, Math.max(v.y, 0.2), v.z);
		}
	}
}
