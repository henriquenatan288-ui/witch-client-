package com.witch.mixin;

import com.witch.module.FreecamModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam estilo Meteor:
 * - forca posicao/rotacao da camera livre
 * - desativa clip de parede (getMaxZoom)
 * - detached = true
 * - isSpectator no extractRenderState pra desligar smart-culling dentro de bloco
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setPosition(Vec3 pos);

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	/** Aplica camera livre no fim do update. */
	@Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"), require = 0)
	private void witch$freecam(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!FreecamModule.active()) return;
		float partial = deltaTracker.getGameTimeDeltaPartialTick(true);
		setPosition(FreecamModule.pos(partial));
		setRotation(FreecamModule.yaw(), FreecamModule.pitch());
		detached = true;
	}

	/**
	 * Meteor: freecam zera o max zoom pra camera nao ser puxada pra fora do bloco.
	 */
	@ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true, require = 0)
	private float witch$noClipZoom(float cameraDist) {
		if (FreecamModule.active()) return 0f;
		return cameraDist;
	}

	/** Cancela o raycast de clip se getMaxZoom existir. */
	@Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$cancelClip(float cameraDist, CallbackInfoReturnable<Float> cir) {
		if (FreecamModule.active()) {
			cir.setReturnValue(cameraDist);
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), require = 0)
	private void witch$forceDetachedForCull(CallbackInfo ci) {
		if (FreecamModule.active()) {
			detached = true;
		}
	}

	/**
	 * Meteor: trata como spectator no extractRenderState → desliga smart culling
	 * quando a camera esta dentro de bloco solido (evita tela preta).
	 */
	@Redirect(
			method = "extractRenderState",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"),
			require = 0
	)
	private boolean witch$spectatorForCull(LocalPlayer player) {
		return player.isSpectator() || FreecamModule.active();
	}
}
