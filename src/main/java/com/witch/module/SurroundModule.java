package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;

/** Cerca seus pes com obsidiana (4 lados), um bloco por tick, refazendo o que for quebrado. */
public class SurroundModule extends Module {
	private final IntSetting perTick = add(new IntSetting("Blocks/tick", 1, 4, 1, ""));

	private int prevSlot = -1;

	public SurroundModule() {
		super("Surround", Category.UTILITY);
	}

	@Override
	protected void onDisable() {
		restore();
	}

	private void restore() {
		var p = Minecraft.getInstance().player;
		if (p != null && prevSlot >= 0 && prevSlot < 9) p.getInventory().setSelectedSlot(prevSlot);
		prevSlot = -1;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return;
		if (!p.isAlive() || p.isSpectator() || !p.onGround()) { restore(); return; }

		BlockPos feet = p.blockPosition();
		int placed = 0;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos t = feet.relative(d);
			if (!mc.level.getBlockState(t).canBeReplaced()) continue;
			BlockPos sup = t.below();
			if (mc.level.getBlockState(sup).isAir() || !mc.level.getBlockState(sup).isSolidRender()) continue;
			if (!CombatUtil.free(mc, t, 1)) continue;
			if (!p.getMainHandItem().is(Items.OBSIDIAN)) {
				int prev = p.getInventory().getSelectedSlot();
				if (!CombatUtil.select(p, Items.OBSIDIAN)) return;
				if (prevSlot < 0) prevSlot = prev;
			}
			if (CombatUtil.useOn(mc, sup, Direction.UP) && ++placed >= perTick.get()) return;
		}
		if (placed == 0) restore();
	}
}
