package com.witch.module;

import com.witch.gui.FontUtil;
import com.witch.gui.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Durabilidade (%) da armadura e do item na mao, no canto direito. */
public class ArmorHudModule extends Module {
	private static ArmorHudModule instance;
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final String[] NAMES = {"Helmet", "Chest", "Legs", "Boots"};

	public ArmorHudModule() {
		super("Armor HUD", Category.UTILITY);
		instance = this;
	}

	private static int pct(ItemStack st) {
		int max = st.getMaxDamage();
		return max <= 0 ? 100 : Math.max(0, 100 - st.getDamageValue() * 100 / max);
	}

	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled()) return;
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null) return;
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int lh = FontUtil.height() + 3;
		int y = sh / 2 - 20;
		for (int i = 0; i < 5; i++) {
			ItemStack st = i < 4 ? p.getItemBySlot(SLOTS[i]) : p.getMainHandItem();
			if (st.isEmpty() || !st.isDamageableItem()) continue;
			int v = pct(st);
			int col = v > 60 ? 0xFF34D399 : v > 25 ? 0xFFFACC15 : 0xFFEF4444;
			String label = i < 4 ? NAMES[i] : "Hand";
			String val = v + "%";
			int bw = FontUtil.width(label) + FontUtil.width(val) + 22;
			int x = sw - 5 - bw;
			RenderUtil.roundRect(g, x, y, bw, lh, 3, RenderUtil.alpha(0xFF000000, 0.55f));
			FontUtil.draw(g, label, x + 6, y + 2, 0xFFCFCFCF);
			FontUtil.draw(g, val, x + bw - 6 - FontUtil.width(val), y + 2, col);
			y += lh + 1;
		}
	}
}
