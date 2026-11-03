package com.witch.module;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/**
 * Esconde a scoreboard da direita (sidebar) inteira.
 * Util pra prints e videos.
 * Usa HudElementRegistry da Fabric API (compativel com o HUD extraction da 26.2).
 */
public class HideNameModule extends Module {
	private static HideNameModule instance;
	private static boolean hooked;

	public HideNameModule() {
		super("Hide Name", Category.VISUALS);
		instance = this;
		hook();
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** Substitui o layer SCOREBOARD por um wrapper que so desenha se o modulo estiver desligado. */
	private static void hook() {
		if (hooked) return;
		hooked = true;
		HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, original -> (graphics, delta) -> {
			if (!active()) {
				original.extractRenderState(graphics, delta);
			}
		});
	}
}
