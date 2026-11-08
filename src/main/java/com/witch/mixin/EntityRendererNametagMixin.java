package com.witch.mixin;

import com.witch.module.NametagsModule;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererNametagMixin {
	/** Nametags: acrescenta vida e distancia no nome do jogador. */
	@Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
	private void witch$nametag(Entity entity, EntityRenderState state, float partial, CallbackInfo ci) {
		if (state.nameTag == null) return;
		var t = NametagsModule.decorate(entity, state.nameTag);
		if (t != null) state.nameTag = t;
	}
}
