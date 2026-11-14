package com.witch.gui;

import com.witch.config.Config;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Cinco perfis de config: Salvar grava o estado atual, Carregar aplica o perfil. */
public class ProfilesScreen extends Screen {
	private static final int ROW = 22;
	private int wx, wy, ww, wh;
	private String msg = "";

	public ProfilesScreen() {
		super(Component.literal("Perfis"));
	}

	private void layout() {
		ww = Math.min(this.width - 12, 240);
		wh = 34 + Config.PROFILES * ROW + 24;
		wx = (this.width - ww) / 2;
		wy = (this.height - wh) / 2;
	}

	private int rowY(int i) { return wy + 30 + i * ROW; }
	private int saveX() { return wx + ww - 10 - 100; }
	private int loadX() { return wx + ww - 10 - 48; }

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		layout();
		int fh = FontUtil.height();
		g.fill(0, 0, this.width, this.height, 0x90000000);
		RenderUtil.roundRect(g, wx, wy, ww, wh, 8, Theme.TRACK);
		RenderUtil.roundRect(g, wx + 1, wy + 1, ww - 2, wh - 2, 7, 0xFF0A0C0F);
		FontUtil.draw(g, "Perfis", wx + 10, wy + 9, 0xFFFFFFFF);
		for (int i = 1; i <= Config.PROFILES; i++) {
			int y = rowY(i - 1);
			boolean has = Config.profileExists(i);
			FontUtil.draw(g, "Perfil " + i, wx + 10, y + 3, 0xFFE2E4E8);
			FontUtil.draw(g, has ? "salvo" : "vazio", wx + 10 + 52, y + 3, has ? 0xFF8E939B : 0xFF6B7078);
			button(g, saveX(), y, 46, 14, "Salvar", true, mx, my);
			button(g, loadX(), y, 48, 14, "Carregar", has, mx, my);
		}
		if (!msg.isEmpty()) FontUtil.draw(g, msg, wx + 10, wy + wh - fh - 8, 0xFFB5B9C0);
	}

	private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String t, boolean enabled, int mx, int my) {
		boolean hov = enabled && RenderUtil.inside(mx, my, x, y, w, h);
		RenderUtil.roundRect(g, x, y, w, h, 5, hov ? Theme.BORDER_HI : Theme.TRACK);
		RenderUtil.roundRect(g, x + 1, y + 1, w - 2, h - 2, 4, hov ? 0xFF1B1F25 : 0xFF111317);
		FontUtil.draw(g, t, x + (w - FontUtil.width(t)) / 2, y + (h - FontUtil.height()) / 2, enabled ? 0xFFC4C8CE : 0xFF4A4F57);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		layout();
		double mx = click.x(), my = click.y();
		for (int i = 1; i <= Config.PROFILES; i++) {
			int y = rowY(i - 1);
			if (RenderUtil.inside(mx, my, saveX(), y, 46, 14)) {
				Config.saveProfile(i);
				msg = "Perfil " + i + " salvo";
				return true;
			}
			if (RenderUtil.inside(mx, my, loadX(), y, 48, 14)) {
				msg = Config.loadProfile(i) ? "Perfil " + i + " carregado" : "Perfil " + i + " esta vazio";
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean isPauseScreen() { return false; }
}
