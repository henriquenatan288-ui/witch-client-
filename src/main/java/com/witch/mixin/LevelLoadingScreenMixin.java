package com.witch.mixin;

import com.witch.gui.WitchLoading;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Troca o visual da tela "carregando terreno" pelo do Witch (a logica e o progresso continuam os do jogo). */
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {
	@Shadow
	private float smoothedProgress;

	@Shadow
	private LevelLoadingScreen.Reason reason;

	protected LevelLoadingScreenMixin() {
		super(null);
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$render(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
		String title = switch (reason) {
			case NETHER_PORTAL -> "Atravessando o portal";
			case END_PORTAL -> "Viajando para o End";
			default -> "Carregando o mundo";
		};
		float p = smoothedProgress;
		String sub = p >= 0.99f ? "Pronto" : "Gerando e baixando terreno";
		WitchLoading.draw(g, this.width, this.height, title, sub, p, null);
		ci.cancel();
	}

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$bg(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
		ci.cancel();
	}
}
