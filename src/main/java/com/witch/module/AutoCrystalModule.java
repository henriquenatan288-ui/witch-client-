package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Quebra cristais perto do alvo e coloca novos em obsidiana/bedrock proximo a ele. */
public class AutoCrystalModule extends Module {
	private final IntSetting range = add(new IntSetting("Range", 3, 6, 5, " blocks"));
	private final IntSetting delay = add(new IntSetting("Delay", 0, 10, 2, " ticks"));
	private final IntSetting minHp = add(new IntSetting("Min HP", 0, 20, 8, ""));
	private final IntSetting selfDist = add(new IntSetting("Min dist self", 1, 5, 3, " blocks"));

	private int wait;

	public AutoCrystalModule() {
		super("Auto Crystal", Category.UTILITY);
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return;
		if (!p.isAlive() || p.isSpectator() || p.isUsingItem()) return;
		if (wait > 0) { wait--; return; }
		if (p.getHealth() + p.getAbsorptionAmount() < minHp.get()) return;
		double r = range.get();
		Player t = CombatUtil.target(mc, r + 4);
		if (t == null) return;

		// 1) quebrar cristal perto do alvo
		EndCrystal best = null;
		double bd = 1e9;
		for (EndCrystal c : mc.level.getEntitiesOfClass(EndCrystal.class, p.getBoundingBox().inflate(r))) {
			if (c.distanceTo(p) > r || c.distanceTo(t) > 6 || c.distanceTo(p) < selfDist.get()) continue;
			double d = c.distanceToSqr(t);
			if (d < bd) { bd = d; best = c; }
		}
		if (best != null) {
			mc.gameMode.attack(p, best);
			p.swing(InteractionHand.MAIN_HAND);
			wait = delay.get();
			return;
		}

		// 2) colocar cristal
		boolean hasCrystal = p.getMainHandItem().is(Items.END_CRYSTAL) || p.getOffhandItem().is(Items.END_CRYSTAL);
		if (!hasCrystal && !CombatUtil.select(p, Items.END_CRYSTAL)) return;
		BlockPos tp = t.blockPosition();
		BlockPos bestPos = null;
		double bs = 1e9;
		int ir = (int) Math.ceil(r);
		BlockPos me = p.blockPosition();
		for (int x = -3; x <= 3; x++) for (int y = -2; y <= 1; y++) for (int z = -3; z <= 3; z++) {
			BlockPos b = tp.offset(x, y, z);
			var s = mc.level.getBlockState(b);
			if (!s.is(Blocks.OBSIDIAN) && !s.is(Blocks.BEDROCK)) continue;
			if (Math.abs(b.getX() - me.getX()) > ir || Math.abs(b.getZ() - me.getZ()) > ir) continue;
			if (p.getEyePosition().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(b)) > r) continue;
			if (net.minecraft.world.phys.Vec3.atCenterOf(b.above()).distanceTo(p.position()) < selfDist.get()) continue;
			if (!CombatUtil.free(mc, b.above(), 2)) continue;
			double d = net.minecraft.world.phys.Vec3.atCenterOf(b.above()).distanceToSqr(t.position());
			if (d < bs) { bs = d; bestPos = b; }
		}
		if (bestPos != null && CombatUtil.useOn(mc, bestPos, Direction.UP)) wait = delay.get();
	}
}
