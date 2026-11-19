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
		context.runOnClient(mc -> mc.gui.setScreen(new com.witch.gui.WitchEnterScreen()));
		context.waitTicks(90);
		context.takeScreenshot("00-enter");
		context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("t")) {
			@Override
			public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float d) {
				com.witch.gui.WitchLoading.draw(g, this.width, this.height, "Carregando o mundo", "Gerando e baixando terreno", 0.45f, null);
			}
		}));
		context.waitTicks(40);
		context.takeScreenshot("00b-loading");
		context.runOnClient(mc -> mc.gui.setScreen(new com.witch.gui.WitchTitleScreen()));
		context.waitTicks(140);
		context.takeScreenshot("01b-title-menu");
		try (var sp = context.worldBuilder().create()) {
			sp.getClientLevel().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("03-world");
			context.runOnClient(mc -> {
				for (var m : com.witch.module.ModuleManager.all()) {
					if (m.name.equals("Fullbright") || m.name.equals("Tracers") || m.name.equals("Player ESP") || m.name.equals("Fly") || m.name.equals("Crosshair") || m.name.equals("Sky") || m.name.equals("Fog") || m.name.equals("Glow Players") || m.name.equals("Jump Circles") || m.name.equals("Particles 2D") || m.name.equals("Filter")) m.toggle();
				}
			});
			context.waitTicks(40);
			context.runOnClient(mc -> com.witch.module.CardParticlesModule.burst(mc.player.getX(), mc.player.getY() + 1.6, mc.player.getZ() + 2.5, 14));
			context.waitTicks(8);
			context.takeScreenshot("04-hud");
			context.runOnClient(mc -> mc.player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD)));
			context.waitTicks(10);
			context.takeScreenshot("13-karambit");
			context.runOnClient(mc -> { var gm = com.witch.module.ModuleManager.byName("Glow Items"); if (gm != null && !gm.isEnabled()) gm.toggle(); });
			context.waitTicks(10);
			context.takeScreenshot("14-glowitem");
			context.runOnClient(mc -> { var gm = com.witch.module.ModuleManager.byName("Glow Items"); if (gm != null && gm.isEnabled()) gm.toggle(); });
			context.runOnClient(mc -> { for (String n : new String[] {"Players Galaxy", "No Legs", "Particles 3D"}) { var m = com.witch.module.ModuleManager.byName(n); if (m != null && !m.isEnabled()) m.toggle(); } mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); });
			context.waitTicks(40);
			context.takeScreenshot("15-avatar");
			System.setProperty("witch.debug", "1");
			sp.getServer().runCommand("summon minecraft:armor_stand ~ ~ ~3 {ShowArms:1b,equipment:{head:{id:\"minecraft:diamond_helmet\",count:1},chest:{id:\"minecraft:diamond_chestplate\",count:1},legs:{id:\"minecraft:diamond_leggings\",count:1},feet:{id:\"minecraft:diamond_boots\",count:1}}}");
			sp.getServer().runCommand("summon minecraft:armor_stand ~2 ~ ~3 {ShowArms:1b,equipment:{chest:{id:\"minecraft:elytra\",count:1},mainhand:{id:\"minecraft:trident\",count:1},offhand:{id:\"minecraft:golden_apple\",count:1}}}");
			context.runOnClient(mc -> { for (String n : new String[] {"Fire Head"}) { var m = com.witch.module.ModuleManager.byName(n); if (m != null && !m.isEnabled()) m.toggle(); } });
			context.runOnClient(mc -> {
				var gm = com.witch.module.ModuleManager.byName("Galaxy");
				if (gm != null && !gm.isEnabled()) gm.toggle();
				com.witch.module.GalaxyModule.instance.mode.set(2);
				var inv = mc.player.getInventory();
				inv.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE));
				inv.setItem(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
				inv.setItem(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ELYTRA));
				inv.setItem(4, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TRIDENT));
			});
			context.waitTicks(40);
			context.takeScreenshot("16-armorstands-galaxy");
			context.runOnClient(mc -> {
				for (String n : new String[] {"Players Galaxy", "Galaxy"}) { var m = com.witch.module.ModuleManager.byName(n); if (m != null && m.isEnabled()) m.toggle(); }
				var em = com.witch.module.ModuleManager.byName("Elytra Shader");
				if (em != null && !em.isEnabled()) em.toggle();
				com.witch.module.ElytraShaderModule.instance.style.set(3);
			});
			context.waitTicks(40);
			context.takeScreenshot("22-elytra-only");
			context.runOnClient(mc -> { mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); mc.player.getInventory().setSelectedSlot(3); });
			context.waitTicks(30);
			context.takeScreenshot("23-elytra-hand");
			context.runOnClient(mc -> { var em = com.witch.module.ModuleManager.byName("Elytra Shader"); if (em != null && em.isEnabled()) em.toggle(); mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); });
			context.runOnClient(mc -> { mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); mc.player.getInventory().setSelectedSlot(4); });
			context.waitTicks(30);
			context.takeScreenshot("18-trident-hand");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(1));
			context.waitTicks(30);
			context.takeScreenshot("19-apple-hand");
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
			context.waitTicks(30);
			context.takeScreenshot("17-firehead-third");
			context.runOnClient(mc -> {
				var hm = com.witch.module.ModuleManager.byName("China Hat");
				if (hm != null && !hm.isEnabled()) hm.toggle();
				mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
			});
			context.waitTicks(30);
			context.takeScreenshot("24-chinahat-galaxy");
			context.runOnClient(mc -> com.witch.module.ChinaHatModule.instance.look.set(2));
			context.waitTicks(20);
			context.takeScreenshot("25-chinahat-spectral");
			context.runOnClient(mc -> { var hm = com.witch.module.ModuleManager.byName("China Hat"); if (hm != null && hm.isEnabled()) hm.toggle(); });
			context.runOnClient(mc -> { for (String n : new String[] {"Players Galaxy", "No Legs", "Particles 3D", "Fire Head"}) { var m = com.witch.module.ModuleManager.byName(n); if (m != null && m.isEnabled()) m.toggle(); } mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); });
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
		context.runOnClient(mc -> mc.gui.setScreen(new com.witch.gui.MeteorGuiScreen(com.witch.module.ModuleManager.byName("Players Galaxy"))));
		context.waitTicks(60);
		context.takeScreenshot("21-windows-gui");
		context.runOnClient(mc -> mc.gui.setScreen(new ClickGuiScreen()));
		context.waitTicks(60);
		context.takeScreenshot("02-clickgui");
		System.setProperty("witch.noTitleSwap", "true");
		context.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
		context.waitTicks(5);
	}
}
