package com.witch.mixin;

import com.witch.module.NoHurtCamModule;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	/** No Hurt Cam: cancela o tranco da camera ao tomar dano. */
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$noHurt(CallbackInfo ci) {
		if (NoHurtCamModule.active()) ci.cancel();
	}
}
