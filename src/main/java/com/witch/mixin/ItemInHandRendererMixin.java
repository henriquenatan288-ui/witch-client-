package com.witch.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.witch.render.HeldItemGlow;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	/** Glow Items: antes de desenhar o item da mao, desenha o halo atras dele. */
	@Redirect(method = "renderItem", require = 0,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
	private void witch$glowItem(ItemStackRenderState state, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline) {
		HeldItemGlow.draw(state, pose, collector);
		state.submit(pose, collector, light, overlay, outline);
	}
}
