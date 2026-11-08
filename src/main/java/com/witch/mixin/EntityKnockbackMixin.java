package com.witch.mixin;

import com.witch.module.VelocityModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityKnockbackMixin {
	/** Velocity: o servidor manda a velocidade nova do jogador local por aqui; aplica so a fracao configurada. */
	@Inject(method = "lerpMotion", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$velocity(Vec3 v, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (!VelocityModule.active() || self != Minecraft.getInstance().player) return;
		self.setDeltaMovement(VelocityModule.apply(self.getDeltaMovement(), v));
		ci.cancel();
	}
}
