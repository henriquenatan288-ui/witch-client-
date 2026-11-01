package com.witch.test;

import com.witch.gui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Roda o jogo sem tela no CI e tira screenshots das telas do Witch Client. */
public class WitchGametest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.waitTicks(120);
		context.takeScreenshot("01-title");
		context.runOnClient(mc -> mc.gui.setScreen(new ClickGuiScreen()));
		context.waitTicks(60);
		context.takeScreenshot("02-clickgui");
		System.setProperty("witch.noTitleSwap", "true");
		context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
		context.waitTicks(5);
	}
}
