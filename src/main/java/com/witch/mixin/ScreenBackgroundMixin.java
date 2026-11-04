package com.witch.mixin;

import com.witch.gui.WitchLoading;
import com.witch.gui.WitchLogo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fundo do Witch (grafite, linhas brilhantes, logo) nas telas de menu: seletor de mundo, criar mundo, multiplayer e opcoes.
 * extractBackground e declarado na Screen (as subclasses nao o redefinem), por isso o mixin fica nela e filtra pela tela.
 * So fora de um mundo: no menu de pausa continua o fundo desfocado do jogo.
 */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$bg(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
		if (!com.witch.Edition.visuals()) return;
		Object self = this;
		if (!(self instanceof SelectWorldScreen || self instanceof CreateWorldScreen
				|| self instanceof JoinMultiplayerScreen || self instanceof OptionsScreen)) return;
		if (Minecraft.getInstance().level != null) return;
		double t = System.nanoTime() / 1_000_000_000.0;
		WitchLoading.backdrop(g, this.width, this.height, t);
		int lw = Math.min(64, this.width / 6);
		WitchLoading.glow(g, 12 + lw / 2, 8 + WitchLogo.height(lw) / 2, lw * 2, 0.8f);
		WitchLogo.draw(g, 12, 8, lw, 0.9f);
		ci.cancel();
	}
}
