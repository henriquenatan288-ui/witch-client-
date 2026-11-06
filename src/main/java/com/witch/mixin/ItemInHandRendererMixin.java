package com.witch.mixin;

import com.witch.module.GlowItemsModule;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	/** Glow Items: o ultimo argumento do submit() e a cor do contorno (0 = sem contorno). */
	@ModifyArg(method = "renderItem", index = 4, require = 0,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
	private int witch$glowItem(int outline) {
		int c = GlowItemsModule.outline();
		return c != 0 ? c : outline;
	}
}
