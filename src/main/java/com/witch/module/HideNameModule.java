package com.witch.module;

/**
 * Esconde a scoreboard da direita (sidebar) inteira.
 * Util pra prints e videos — some o rank/online/nome e qualquer linha do servidor.
 * O render e cancelado via mixin em Gui.renderScoreboardSidebar.
 */
public class HideNameModule extends Module {
	private static HideNameModule instance;

	public HideNameModule() {
		super("Hide Name", Category.VISUALS);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
