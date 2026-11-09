package com.witch.test;

import com.witch.module.ModuleManager;
import com.witch.setting.IntSetting;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public class MiscTest implements FabricClientGameTest {
	static void log(String s) { System.out.println("WTEST " + s); }
	static void mod(ClientGameTestContext c, String n, boolean on) {
		c.runOnClient(mc -> {
			var m = ModuleManager.byName(n);
			if (m == null) { log("MISSING " + n); return; }
			if (m.isEnabled() != on) m.toggle();
		});
	}
	static void set(ClientGameTestContext c, String m, String s, int v) {
		c.runOnClient(mc -> { var x = ModuleManager.byName(m); if (x != null) for (var q : x.settings()) if (q instanceof IntSetting i && i.name.equals(s)) i.set(v); });
	}
	static String[] ALL = {"Auto Clicker","Auto Block","Auto Log","Auto Gap","W-Tap","Cooldown Guard","Target Strafe","Auto Jump","Ice Speed","Water Speed","Edge Jump","Lava Escape","Sneak Spam","Anti AFK","Auto Respawn","Auto Replenish","Auto Mine","Auto Use","Auto Torch","Yaw Lock","Pitch Lock","Hotbar Lock","Hotbar Cycle","Item Dropper","Spinner","FOV Changer","Render Distance","No Bob","Entity Distance","Mouse Sens"};

	@Override public void runTest(ClientGameTestContext c) {
		c.waitTicks(20);
		try (var sp = c.worldBuilder().create()) {
			sp.getClientLevel().waitForChunksRender();
			var srv = sp.getServer();
			c.waitTicks(30);
			srv.runCommand("gamemode survival @p");
			srv.runCommand("effect give @p minecraft:resistance 99999 4 true");
			srv.runCommand("effect give @p minecraft:regeneration 99999 4 true");
			c.waitTicks(20);
			// smoke: liga cada um por 20 ticks (Auto Log ignorado: desconectaria)
			for (String n : ALL) {
				if (n.equals("Auto Log")) continue;
				mod(c, n, true);
				try { c.waitTicks(20); } catch (Throwable t) { log("CRASH " + n + " " + t); }
				mod(c, n, false);
				log("smoke ok " + n);
			}
			// funcionais
			float[] y = new float[2];
			c.runOnClient(mc -> y[0] = mc.player.getYRot());
			mod(c, "Spinner", true); c.waitTicks(10); mod(c, "Spinner", false);
			c.runOnClient(mc -> log("Spinner yaw " + y[0] + " -> " + mc.player.getYRot()));

			set(c, "FOV Changer", "FOV", 60);
			int[] f = new int[2];
			c.runOnClient(mc -> f[0] = mc.options.fov().get());
			mod(c, "FOV Changer", true); c.waitTicks(5);
			c.runOnClient(mc -> log("FOV antes " + f[0] + " ligado " + mc.options.fov().get()));
			mod(c, "FOV Changer", false); c.waitTicks(3);
			c.runOnClient(mc -> log("FOV restaurado " + mc.options.fov().get()));

			mod(c, "Pitch Lock", true); set(c, "Pitch Lock", "Pitch", 45); c.waitTicks(5);
			c.runOnClient(mc -> log("PitchLock pitch=" + mc.player.getXRot()));
			mod(c, "Pitch Lock", false);

			set(c, "Hotbar Lock", "Slot", 4); mod(c, "Hotbar Lock", true); c.waitTicks(5);
			c.runOnClient(mc -> log("HotbarLock slot=" + mc.player.getInventory().getSelectedSlot() + " (esperado 3)"));
			mod(c, "Hotbar Lock", false);

			set(c, "Hotbar Cycle", "Delay", 2); int[] s0 = new int[1];
			c.runOnClient(mc -> s0[0] = mc.player.getInventory().getSelectedSlot());
			mod(c, "Hotbar Cycle", true); c.waitTicks(11); mod(c, "Hotbar Cycle", false);
			c.runOnClient(mc -> log("HotbarCycle slot " + s0[0] + " -> " + mc.player.getInventory().getSelectedSlot()));

			srv.runCommand("clear @p");
			srv.runCommand("give @p minecraft:cobblestone 3");
			c.waitTicks(5);
			mod(c, "Item Dropper", true); set(c, "Item Dropper", "Delay", 2); c.waitTicks(12); mod(c, "Item Dropper", false);
			c.runOnClient(mc -> log("ItemDropper itens no inv=" + countItems(mc)));

			srv.runCommand("clear @p");
			srv.runCommand("give @p minecraft:cobblestone 64");
			srv.runCommand("give @p minecraft:cobblestone 64");
			c.runOnClient(mc -> { mc.player.getInventory().setSelectedSlot(0); });
			srv.runCommand("item replace entity @p hotbar.0 with minecraft:cobblestone 3");
			c.waitTicks(5);
			mod(c, "Auto Replenish", true); set(c, "Auto Replenish", "Min count", 8); c.waitTicks(30); mod(c, "Auto Replenish", false);
			c.runOnClient(mc -> log("AutoReplenish hotbar0 count=" + mc.player.getInventory().getItem(0).getCount()));

			srv.runCommand("clear @p");
			srv.runCommand("give @p minecraft:torch 8");
			srv.runCommand("time set midnight");
			srv.runCommand("fill ~-3 ~-1 ~-3 ~3 ~3 ~3 minecraft:stone hollow");
			srv.runCommand("tp @p ~ ~ ~");
			c.waitTicks(20);
			int[] tb = new int[1];
			mod(c, "Auto Torch", true); set(c, "Auto Torch", "Max light", 14); c.waitTicks(60); mod(c, "Auto Torch", false);
			c.runOnClient(mc -> {
				int n = 0; var p = mc.player.blockPosition();
				for (int dx = -4; dx <= 4; dx++) for (int dy = -2; dy <= 3; dy++) for (int dz = -4; dz <= 4; dz++)
					if (mc.level.getBlockState(p.offset(dx, dy, dz)).is(net.minecraft.world.level.block.Blocks.TORCH)) n++;
				log("AutoTorch tochas colocadas=" + n);
			});

			srv.runCommand("kill @p");
			c.waitTicks(30);
			mod(c, "Auto Respawn", true);
			c.waitTicks(40);
			c.runOnClient(mc -> log("AutoRespawn vivo=" + (mc.player != null && !mc.player.isDeadOrDying()) + " tela=" + mc.gui.screen()));
			mod(c, "Auto Respawn", false);
			c.takeScreenshot("misc-end");
			log("FIM");
		}
	}
	static int countItems(net.minecraft.client.Minecraft mc) {
		int n = 0; for (int i = 0; i < 36; i++) n += mc.player.getInventory().getItem(i).getCount(); return n;
	}
}
