package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Equipa sozinho a melhor armadura do inventario (por material). Nao troca elytra. */
public class AutoArmorModule extends Module {
	private final IntSetting delay = add(new IntSetting("Delay", 0, 20, 5, " ticks"));

	private int wait;

	public AutoArmorModule() {
		super("Auto Armor", Category.UTILITY);
	}

	private static int rank(ItemStack s) {
		String n = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
		if (n.startsWith("netherite")) return 7;
		if (n.startsWith("diamond")) return 6;
		if (n.startsWith("iron")) return 5;
		if (n.startsWith("copper")) return 4;
		if (n.startsWith("chainmail")) return 3;
		if (n.startsWith("golden")) return 2;
		if (n.startsWith("leather") || n.startsWith("turtle")) return 1;
		return 0;
	}

	private static EquipmentSlot slotOf(ItemStack s) {
		var eq = s.get(DataComponents.EQUIPPABLE);
		return eq == null ? null : eq.slot();
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || mc.gameMode == null || mc.level == null) return;
		if (mc.gui.screen() != null && !(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
		if (wait > 0) { wait--; return; }
		var menu = p.inventoryMenu;
		// slots de armadura do menu do jogador: 5 cabeca, 6 peito, 7 calca, 8 bota
		EquipmentSlot[] order = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int a = 0; a < 4; a++) {
			EquipmentSlot es = order[a];
			ItemStack cur = menu.slots.get(5 + a).getItem();
			if (cur.getItem().toString().contains("elytra")) continue;
			int curRank = cur.isEmpty() ? -1 : rank(cur);
			int best = -1, bestRank = curRank;
			for (int i = 9; i <= 44 && i < menu.slots.size(); i++) {
				ItemStack s = menu.slots.get(i).getItem();
				if (s.isEmpty() || slotOf(s) != es) continue;
				int r = rank(s);
				if (r > bestRank) { bestRank = r; best = i; }
			}
			if (best < 0) continue;
			int id = menu.containerId;
			mc.gameMode.handleContainerInput(id, best, 0, ContainerInput.PICKUP, p);
			mc.gameMode.handleContainerInput(id, 5 + a, 0, ContainerInput.PICKUP, p);
			if (!cur.isEmpty()) mc.gameMode.handleContainerInput(id, best, 0, ContainerInput.PICKUP, p);
			wait = delay.get();
			return;
		}
	}
}
