package com.witch.mixin;

import com.witch.gui.WitchLoading;
import com.witch.gui.WitchLogo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Seletor de mundo com o fundo do Witch (grafite, linhas brilhantes e logo). A lista e os botoes continuam os do jogo. */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {
	protected SelectWorldScreenMixin() {
		super(null);
	}

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$bg(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
		if (!com.witch.Edition.visuals()) return;
		double t = System.nanoTime() / 1_000_000_000.0;
		WitchLoading.backdrop(g, this.width, this.height, t);
		int lw = Math.min(64, this.width / 6);
		WitchLoading.glow(g, 12 + lw / 2, 8 + WitchLogo.height(lw) / 2, lw * 2, 0.8f);
		WitchLogo.draw(g, 12, 8, lw, 0.9f);
		ci.cancel();
	}
}
