package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

/** Auto Totem estilo Meteor: mantem um Totem of Undying na mao secundaria. */
public class AutoTotemModule extends Module {
	private final IntSetting delay = add(new IntSetting("Delay", 0, 10, 0, " ticks"));
	private final IntSetting health = add(new IntSetting("Health (0=always)", 0, 20, 0, ""));

	private int wait;

	public AutoTotemModule() {
		super("Auto Totem", Category.UTILITY);
	}

	@Override
	protected void onEnable() {
		wait = 0;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || mc.gameMode == null || mc.level == null) return;
		if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
		int hp = health.get();
		if (hp > 0 && p.getHealth() + p.getAbsorptionAmount() > hp) return;
		if (wait > 0) {
			wait--;
			return;
		}
		var slots = p.inventoryMenu.slots;
		for (int i = 9; i <= 44 && i < slots.size(); i++) {
			if (slots.get(i).getItem().is(Items.TOTEM_OF_UNDYING)) {
				// SWAP com o botao 40 troca o slot com a mao secundaria
				mc.gameMode.handleContainerInput(p.inventoryMenu.containerId, i, 40, ContainerInput.SWAP, p);
				wait = delay.get();
				return;
			}
		}
	}
}
