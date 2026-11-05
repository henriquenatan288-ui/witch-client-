package com.witch.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Avisa o servidor que voce esta no chao quando cai de alto (sem dano de queda). Anticheats detectam isso. */
public class NoFallModule extends Module {
	public NoFallModule() {
		super("No Fall", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		var p = Minecraft.getInstance().player;
		if (p == null || p.onGround() || p.getAbilities().flying || p.isFallFlying() || p.isInWater()) return;
		if (p.fallDistance > 2.5) {
			p.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, p.horizontalCollision));
		}
	}
}
