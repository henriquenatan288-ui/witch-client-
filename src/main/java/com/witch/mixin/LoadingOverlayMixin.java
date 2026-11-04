package com.witch.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

/**
 * Tela inicial de carregamento do jogo (logo da Mojang): mantem a logo, troca o fundo vermelho pelo grafite do Witch,
 * a barra por uma fina e adiciona a marca do client. Usa so a fonte padrao (a Inter ainda nao carregou nessa etapa).
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
	@Shadow
	private long fadeOutStart;

	@Shadow
	private float currentProgress;

	@Redirect(method = "extractRenderState",
			at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"), require = 0)
	private int witch$bg(IntSupplier original) {
		return com.witch.Edition.visuals() ? 0xFF0B0D10 : original.getAsInt();
	}

	@Inject(method = "extractProgressBar", at = @At("HEAD"), cancellable = true, require = 0)
	private void witch$bar(GuiGraphicsExtractor g, int minX, int minY, int maxX, int maxY, float alpha, CallbackInfo ci) {
		if (!com.witch.Edition.visuals()) return;
		int cy = (minY + maxY) / 2;
		int a = Mth.clamp((int) (alpha * 255f), 0, 255);
		g.fill(minX, cy - 1, maxX, cy + 1, ((a * 40 / 255) << 24) | 0xFFFFFF);
		int fw = Math.round((maxX - minX) * Mth.clamp(currentProgress, 0f, 1f));
		g.fill(minX, cy - 1, minX + fw, cy + 1, (a << 24) | 0xFFFFFF);
		ci.cancel();
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
	private void witch$brand(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
		if (!com.witch.Edition.visuals()) return;
		float fo = fadeOutStart > -1L ? (Util.getMillis() - fadeOutStart) / 1000.0f : -1.0f;
		float a = fo >= 1.0f ? 1.0f - Mth.clamp(fo - 1.0f, 0.0f, 1.0f) : 1.0f;
		if (a <= 0.02f) return;
		var mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		int ai = (int) (a * 255f) << 24;
		String t1 = "WITCH CLIENT";
		String t2 = "MADE BY BISTECK";
		g.text(mc.font, Component.literal(t1), (w - mc.font.width(t1)) / 2, (int) (h * 0.9f), ai | 0xCFCFCF, false);
		g.text(mc.font, Component.literal(t2), (w - mc.font.width(t2)) / 2, (int) (h * 0.9f) + 11, ai | 0x666666, false);
	}
}
