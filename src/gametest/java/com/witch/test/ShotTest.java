package com.witch.test;

import com.witch.gui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public class ShotTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext c) {
		c.waitTicks(100);
		c.runOnClient(mc -> mc.gui.setScreen(new ClickGuiScreen()));
		c.waitTicks(60);
		c.takeScreenshot("clickgui-a");
		c.runOnClient(mc -> mc.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mc.getWindow().getGuiScaledWidth() * 0.5, mc.getWindow().getGuiScaledHeight() * 0.5, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false));
		c.waitTicks(10);
		c.takeScreenshot("clickgui-b");
	}
}
