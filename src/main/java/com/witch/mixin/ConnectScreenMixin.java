package com.witch.mixin;

import com.witch.gui.WitchLoading;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Entrando no servidor": mesmo visual do Witch; a mensagem de status continua a do jogo. */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {
	@Shadow
	private Component status;

	protected ConnectScreenMixin() {
		super(null);
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$render(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
		String sub = status == null ? "" : status.getString();
		WitchLoading.draw(g, this.width, this.height, "Entrando no servidor", sub, -1f, "Esc para cancelar");
		ci.cancel();
	}
}
