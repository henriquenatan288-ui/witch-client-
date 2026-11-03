package com.witch.mixin;

import com.witch.module.HideNameModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancela o desenho da scoreboard da direita quando Hide Name esta ligado.
 * require=0 para nao quebrar se o metodo mudar de assinatura na 26.2.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

	@Inject(
			method = "renderScoreboardSidebar",
			at = @At("HEAD"),
			cancellable = true,
			require = 0
	)
	private void witch$hideScoreboard(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
		if (HideNameModule.active()) {
			ci.cancel();
		}
	}
}
