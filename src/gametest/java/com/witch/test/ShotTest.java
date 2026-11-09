package com.witch.test;

import com.witch.gui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public class ShotTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext c) {
		c.takeScreenshot("00-loading-a");
		c.waitTicks(10);
		c.takeScreenshot("00-loading-b");
		c.waitTicks(120);
		c.takeScreenshot("01-title");
		c.waitTicks(60);
		c.takeScreenshot("01b-title");
		c.runOnClient(mc -> mc.gui.setScreen(new ClickGuiScreen()));
		c.waitTicks(60);
		c.takeScreenshot("02-clickgui");
		try (var sp = c.worldBuilder().create()) {
			sp.getClientLevel().waitForChunksRender();
			c.waitTicks(20);
			c.runOnClient(mc -> mc.gui.setScreen(new ClickGuiScreen()));
			c.waitTicks(60);
			c.takeScreenshot("03-clickgui-world");
			c.runOnClient(mc -> mc.gui.setScreen(new com.witch.gui.AntiVanishScreen(new ClickGuiScreen())));
			c.waitTicks(30);
			c.takeScreenshot("04-antivanish");
		}
	}
}
