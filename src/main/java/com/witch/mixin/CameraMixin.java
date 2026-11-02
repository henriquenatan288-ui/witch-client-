package com.witch.mixin;

import com.witch.module.FreeLookModule;
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

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setPosition(Vec3 pos);

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	/** Freecam: posicao livre no fim do update. */
	@Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"), require = 0)
	private void witch$freecam(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!FreecamModule.active()) return;
		float partial = deltaTracker.getGameTimeDeltaPartialTick(true);
		setPosition(FreecamModule.pos(partial));
		setRotation(FreecamModule.yaw(), FreecamModule.pitch());
		detached = true;
	}

	/** FreeLook: so a rotacao da camera (corpo fica; camera gira e atravessa parede). */
	@Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"), require = 0)
	private void witch$freelook(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (FreecamModule.active() || !FreeLookModule.active()) return;
		setRotation(FreeLookModule.yaw(), FreeLookModule.pitch());
		detached = true;
	}

	/**
	 * FreeLook/Freecam: sem clip de parede no zoom da 3ª pessoa (estilo Meteor).
	 */
	@ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true, require = 0)
	private float witch$noClipZoom(float cameraDist) {
		if (FreeLookModule.active()) return FreeLookModule.distance();
		if (FreecamModule.active()) return 0f;
		return cameraDist;
	}

	@Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$cancelClip(float cameraDist, CallbackInfoReturnable<Float> cir) {
		if (FreeLookModule.active()) {
			// devolve a distancia pedida sem raycast de bloco → camera atravessa parede
			cir.setReturnValue(FreeLookModule.distance());
		}
	}

	/** Smart-culling: trata como spectator pra nao ficar preto dentro de bloco. */
	@Redirect(
			method = "extractRenderState",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"),
			require = 0
	)
	private boolean witch$spectatorForCull(LocalPlayer player) {
		return player.isSpectator() || FreecamModule.active() || FreeLookModule.active();
	}
}
