package com.witch.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.witch.module.ChinaHatModule;
import com.witch.render.ChinaHat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** China Hat: depois de desenhar o corpo do jogador, submete o cone sobre a cabeca. */
@Mixin(LivingEntityRenderer.class)
public abstract class ChinaHatMixin {
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("RETURN"), require = 0)
	private void witch$chinaHat(LivingEntityRenderState state, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam, CallbackInfo ci) {
		if (!ChinaHatModule.on || !(state instanceof AvatarRenderState av) || state.isInvisible) return;
		try {
			var mc = net.minecraft.client.Minecraft.getInstance();
			boolean self = mc.player != null && av.id == mc.player.getId();
			if (!ChinaHatModule.wants(self)) return;
			ChinaHat.submit(state.eyeHeight, ps, c);
		} catch (Throwable ignored) {
		}
	}
}
