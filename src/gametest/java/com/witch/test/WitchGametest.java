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
		try (var sp = context.worldBuilder().create()) {
			sp.getClientLevel().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("03-world");
			context.runOnClient(mc -> {
				for (var m : com.witch.module.ModuleManager.all()) {
					if (m.name.equals("Fullbright") || m.name.equals("Tracers") || m.name.equals("Player ESP") || m.name.equals("Fly") || m.name.equals("Crosshair") || m.name.equals("Sky") || m.name.equals("Fog") || m.name.equals("Glow Players") || m.name.equals("Jump Circles") || m.name.equals("Card Particles") || m.name.equals("Filter")) m.toggle();
				}
			});
			context.waitTicks(40);
			context.takeScreenshot("04-hud");
			context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
			context.waitTicks(30);
			context.takeScreenshot("07-inventory");
			context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(null, mc.options, false)));
			context.waitTicks(30);
			context.takeScreenshot("08-options");
			context.runOnClient(mc -> mc.gui.setScreen(new com.witch.gui.HudEditScreen()));
			context.waitTicks(20);
			context.takeScreenshot("12-hudedit");
			context.runOnClient(mc -> mc.gui.setScreen(new ClickGuiScreen()));
			context.waitTicks(60);
			context.takeScreenshot("05-clickgui-world");
		}
		context.runOnClient(mc -> mc.gui.setScreen(new ClickGuiScreen()));
		context.waitTicks(60);
		context.takeScreenshot("02-clickgui");
		System.setProperty("witch.noTitleSwap", "true");
		context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
		context.waitTicks(5);
	}
}
