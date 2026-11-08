package com.witch.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Ajudantes dos modulos de combate (Auto Crystal / Auto Anchor). */
final class CombatUtil {
	private CombatUtil() {}

	static Player target(Minecraft mc, double range) {
		Player best = null;
		double bd = range * range;
		for (Player o : mc.level.players()) {
			if (o == mc.player || !o.isAlive() || o.isSpectator() || o.isCreative()) continue;
			double d = o.distanceToSqr(mc.player);
			if (d < bd) { bd = d; best = o; }
		}
		return best;
	}

	/** Seleciona o item na hotbar; false se nao tem. */
	static boolean select(LocalPlayer p, Item item) {
		if (p.getMainHandItem().is(item)) return true;
		for (int i = 0; i < 9; i++) {
			if (p.getInventory().getItem(i).is(item)) { p.getInventory().setSelectedSlot(i); return true; }
		}
		return false;
	}

	/** Seleciona qualquer slot da hotbar que NAO seja o item dado. */
	static void selectOther(LocalPlayer p, Item not) {
		if (!p.getMainHandItem().is(not)) return;
		for (int i = 0; i < 9; i++) {
			if (!p.getInventory().getItem(i).is(not)) { p.getInventory().setSelectedSlot(i); return; }
		}
	}

	static boolean useOn(Minecraft mc, BlockPos pos, Direction face) {
		Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		var r = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
		if (r.consumesAction()) mc.player.swing(InteractionHand.MAIN_HAND);
		return r.consumesAction();
	}

	static boolean free(Minecraft mc, BlockPos pos, int height) {
		for (int i = 0; i < height; i++) if (!mc.level.getBlockState(pos.above(i)).isAir()) return false;
		AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1);
		return mc.level.getEntities(null, box).isEmpty();
	}
}
