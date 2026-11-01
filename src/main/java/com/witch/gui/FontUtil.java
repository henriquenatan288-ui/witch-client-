package com.witch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class FontUtil {
	// Inter (TrueType) fica DESLIGADA por padrao: a versao OTF fez as letras sumirem. Aperte F9 no jogo pra ligar/desligar.
	private static volatile boolean custom = false;

	private static final Style CUSTOM_STYLE =
			Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("witch", "inter")));

	public static boolean isCustom() {
		return custom;
	}

	public static void setCustom(boolean v) {
		custom = v;
	}

	private FontUtil() {}

	public static Component text(String s) {
		return Component.literal(s).setStyle(custom ? CUSTOM_STYLE : Style.EMPTY);
	}

	public static void draw(GuiGraphicsExtractor g, String s, int x, int y, int color) {
		g.text(Minecraft.getInstance().font, text(s), x, y, color, false);
	}

	public static int width(String s) {
		return Minecraft.getInstance().font.width(text(s));
	}

	public static int height() {
		return Minecraft.getInstance().font.lineHeight;
	}
}