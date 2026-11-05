package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Reconecta no ultimo servidor quando a conexao cai (tela de desconectado). */
public class AutoReconnectModule extends Module {
	private final IntSetting delay = add(new IntSetting("Delay", 2, 30, 5, "s"));
	private final IntSetting tries = add(new IntSetting("Max tries", 1, 10, 3, ""));

	private ServerData last = null;
	private int ticks = 0;
	private int attempts = 0;

	public AutoReconnectModule() {
		super("Auto Reconnect", Category.UTILITY);
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		ServerData cur = mc.getCurrentServer();
		if (mc.level != null && cur != null && !mc.isLocalServer()) {
			last = cur;
			ticks = 0;
			attempts = 0;
			return;
		}
		if (last == null || !(mc.gui.screen() instanceof DisconnectedScreen)) return;
		if (attempts >= tries.get()) return;
		if (++ticks < delay.get() * 20) return;
		ticks = 0;
		attempts++;
		ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc,
				ServerAddress.parseString(last.ip), last, false, null);
	}

	@Override
	protected void onDisable() {
		ticks = 0;
		attempts = 0;
	}
}
