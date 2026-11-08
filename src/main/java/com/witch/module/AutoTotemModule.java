package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

/** Auto Totem estilo Meteor: mantem um Totem of Undying na mao secundaria. */
public class AutoTotemModule extends Module {
	private final IntSetting delay = add(new IntSetting("Delay", 0, 10, 0, " ticks"));
	private final IntSetting health = add(new IntSetting("Health (0=always)", 0, 20, 0, ""));

	private final IntSetting openInv = add(new IntSetting("Open inventory (0/1)", 0, 1, 1, ""));

	private int wait;
	private int phase; // 0 = parado, 1 = inventario aberto por nos (esperando), 2 = trocou, fechar
	private boolean weOpened;

	public AutoTotemModule() {
		super("Auto Totem", Category.UTILITY);
	}

	@Override
	protected void onEnable() {
		wait = 0;
		phase = 0;
		weOpened = false;
	}

	@Override
	protected void onDisable() {
		Minecraft mc = Minecraft.getInstance();
		if (weOpened && mc.player != null && mc.gui.screen() instanceof InventoryScreen) mc.player.closeContainer();
		weOpened = false;
		phase = 0;
	}

	private static int findTotem(net.minecraft.client.player.LocalPlayer p) {
		var slots = p.inventoryMenu.slots;
		for (int i = 9; i <= 44 && i < slots.size(); i++) {
			if (slots.get(i).getItem().is(Items.TOTEM_OF_UNDYING)) return i;
		}
		return -1;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || mc.gameMode == null || mc.level == null) return;

		if (phase == 1) { // inventario aberto por nos: espera 'delay' ticks e pega o totem
			if (!(mc.gui.screen() instanceof InventoryScreen)) { phase = 0; weOpened = false; return; }
			if (wait-- > 0) return;
			int i = findTotem(p);
			if (i >= 0 && !p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
				mc.gameMode.handleContainerInput(p.inventoryMenu.containerId, i, 40, ContainerInput.SWAP, p);
			}
			phase = 2;
			wait = 1;
			return;
		}
		if (phase == 2) {
			if (wait-- > 0) return;
			if (weOpened && mc.gui.screen() instanceof InventoryScreen) p.closeContainer();
			weOpened = false;
			phase = 0;
			wait = 2;
			return;
		}

		if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
		int hp = health.get();
		if (hp > 0 && p.getHealth() + p.getAbsorptionAmount() > hp) return;
		if (wait > 0) {
			wait--;
			return;
		}
		int i = findTotem(p);
		if (i < 0) return;

		var cur = mc.gui.screen();
		if (openInv.get() == 1 && cur == null) {
			mc.setScreenAndShow(new InventoryScreen(p));
			weOpened = true;
			phase = 1;
			wait = Math.max(1, delay.get());
			return;
		}
		// ja tem uma tela aberta (ou modo direto): troca sem abrir nada
		mc.gameMode.handleContainerInput(p.inventoryMenu.containerId, i, 40, ContainerInput.SWAP, p);
		wait = delay.get();
	}
}
