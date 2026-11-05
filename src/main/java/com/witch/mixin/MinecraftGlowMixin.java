package com.witch.mixin;

import com.witch.module.GlowPlayersModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftGlowMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true, require = 0)
	private void witch$glow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (GlowPlayersModule.wants(entity)) cir.setReturnValue(true);
	}
}
