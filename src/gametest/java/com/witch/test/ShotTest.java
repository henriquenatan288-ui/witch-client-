package com.witch.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public class ShotTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext c) {
		c.waitTicks(40);
		for (int i = 0; i < 30; i++) {
			c.waitTicks(4);
			c.takeScreenshot("t" + (i < 10 ? "0" : "") + i);
		}
	}
}
