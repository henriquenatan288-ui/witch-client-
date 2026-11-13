package com.witch;

import com.witch.config.Config;
import com.witch.gui.ClickGuiScreen;
import com.witch.gui.HudRenderer;
import com.witch.module.Module;
import com.witch.module.ModuleManager;
import com.witch.render.XRayRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class WitchClient implements ClientModInitializer {
	private static boolean f9Down = false;
	private static KeyMapping openGui;
	private static KeyMapping openXraySettings;

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		Config.load();

		// botao "Editar HUD (grade)" no menu de pausa
		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
				net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen).add(
						net.minecraft.client.gui.components.Button.builder(
								net.minecraft.network.chat.Component.literal("Editar HUD (grade)"),
								btn -> client.gui.setScreen(new com.witch.gui.HudEditScreen(screen)))
							.bounds(w - 128, 6, 122, 20).build());
			}
		});

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath("witch", "main"));

		openGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.witch.gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				category
		));

		// Sem tecla por padrao: voce escolhe em Controles > Witch.
		openXraySettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.witch.xray_settings",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean f9 = com.mojang.blaze3d.platform.InputConstants.isKeyDown(client.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_F9);
			if (f9 && !f9Down) {
				com.witch.gui.FontUtil.setCustom(!com.witch.gui.FontUtil.isCustom());
				com.witch.config.Config.save();
			}
			f9Down = f9;
			if (com.witch.Edition.visuals() && System.getProperty("witch.noTitleSwap") == null && client.gui.screen() != null && client.gui.screen().getClass() == net.minecraft.client.gui.screens.TitleScreen.class) {
				client.gui.setScreen(com.witch.gui.WitchEnterScreen.pending() ? new com.witch.gui.WitchEnterScreen() : new com.witch.gui.WitchTitleScreen());
			}
			while (openGui.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(new ClickGuiScreen());
				}
			}
			while (openXraySettings.consumeClick()) {
				if (client.gui.screen() == null) {
					Module xray = ModuleManager.byName("X-Ray Hologram");
					client.gui.setScreen(new ClickGuiScreen(xray));
				}
			}
			if (client.player != null) {
				ModuleManager.pollKeys(client);
				ModuleManager.tick();
			}
		});

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath("witch", "module_list"),
				HudRenderer::extract
		);

		XRayRenderer.init();

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			Config.save();
			XRayRenderer.close();
		});
	}
}