package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/** Caindo com alvo na mira: troca pra mace e ataca (smash). Depois volta pro slot anterior. */
public class AutoMaceModule extends Module {
	private final IntSetting minFall = add(new IntSetting("Min fall", 2, 30, 4, " blocks"));
	private final IntSetting players = add(new IntSetting("Players (0/1)", 0, 1, 1, ""));
	private final IntSetting mobs = add(new IntSetting("Mobs (0/1)", 0, 1, 0, ""));

	private int back = -1;

	public AutoMaceModule() {
		super("Auto Mace", Category.UTILITY);
	}

	@Override
	protected void onEnable() {
		back = -1;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return;
		if (back >= 0 && !p.getMainHandItem().is(Items.MACE) ) back = -1;
		if (!p.isAlive() || p.isSpectator() || p.onGround() || p.isFallFlying() || p.getAbilities().flying) {
			restore(p);
			return;
		}
		if (p.fallDistance < minFall.get()) { restore(p); return; }
		if (!(mc.hitResult instanceof EntityHitResult ehr) || !(ehr.getEntity() instanceof LivingEntity le)
			|| !le.isAlive() || le == p) { restore(p); return; }
		boolean isPlayer = le instanceof net.minecraft.world.entity.player.Player;
		if (isPlayer ? players.get() == 0 : mobs.get() == 0) return;

		if (!p.getMainHandItem().is(Items.MACE)) {
			int prev = p.getInventory().getSelectedSlot();
			if (!CombatUtil.select(p, Items.MACE)) return;
			back = prev;
		}
		mc.gameMode.attack(p, le);
		p.swing(InteractionHand.MAIN_HAND);
	}

	private void restore(net.minecraft.client.player.LocalPlayer p) {
		if (back >= 0 && back < 9) p.getInventory().setSelectedSlot(back);
		back = -1;
	}
}
