package com.witch.mixin;

import com.witch.module.SlowHitModule;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Slow Hit Visual: alonga a duracao do balanco do seu braco (a animacao segue interpolada, sem soluco). */
	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
	private void witch$slowSwing(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(SlowHitModule.scale((LivingEntity) (Object) this, cir.getReturnValue()));
	}
}
