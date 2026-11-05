package com.witch.mixin;

import com.witch.module.GlowPlayersModule;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowColorMixin {
	@Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true, require = 0)
	private void witch$glowColor(CallbackInfoReturnable<Integer> cir) {
		if (GlowPlayersModule.wants((Entity) (Object) this)) cir.setReturnValue(GlowPlayersModule.rgb());
	}
}
