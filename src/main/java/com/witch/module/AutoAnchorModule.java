package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.Vec3;

/** Coloca ancora de respawn perto do alvo, carrega com glowstone e detona (so fora do Nether). */
public class AutoAnchorModule extends Module {
	private final IntSetting range = add(new IntSetting("Range", 3, 6, 5, " blocks"));
	private final IntSetting delay = add(new IntSetting("Delay", 0, 10, 2, " ticks"));
	private final IntSetting minHp = add(new IntSetting("Min HP", 0, 20, 10, ""));
	private final IntSetting selfDist = add(new IntSetting("Min dist self", 2, 6, 4, " blocks"));

	private int wait;

	public AutoAnchorModule() {
		super("Auto Anchor", Category.UTILITY);
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return;
		if (!p.isAlive() || p.isSpectator() || p.isUsingItem()) return;
		if (mc.level.dimension() == Level.NETHER) return; // la a ancora nao explode
		if (wait > 0) { wait--; return; }
		if (p.getHealth() + p.getAbsorptionAmount() < minHp.get()) return;
		double r = range.get();
		Player t = CombatUtil.target(mc, r + 3);
		if (t == null) return;

		BlockPos tp = t.blockPosition();
		// ancora ja existente perto do alvo
		BlockPos anchor = null;
		double bd = 1e9;
		for (int x = -3; x <= 3; x++) for (int y = -2; y <= 3; y++) for (int z = -3; z <= 3; z++) {
			BlockPos b = tp.offset(x, y, z);
			if (!mc.level.getBlockState(b).is(Blocks.RESPAWN_ANCHOR)) continue;
			Vec3 c = Vec3.atCenterOf(b);
			if (p.getEyePosition().distanceTo(c) > r || c.distanceTo(p.position()) < selfDist.get()) continue;
			double d = c.distanceToSqr(t.position());
			if (d < bd) { bd = d; anchor = b; }
		}

		if (anchor != null) {
			int charge = mc.level.getBlockState(anchor).getValue(RespawnAnchorBlock.CHARGE);
			if (charge == 0) {
				if (CombatUtil.select(p, Items.GLOWSTONE) && CombatUtil.useOn(mc, anchor, Direction.UP)) wait = delay.get();
			} else {
				// detona com item que nao seja glowstone na mao
				CombatUtil.selectOther(p, Items.GLOWSTONE);
				if (!p.getOffhandItem().is(Items.GLOWSTONE) && CombatUtil.useOn(mc, anchor, Direction.UP)) wait = delay.get();
			}
			return;
		}

		// colocar nova ancora em cima de um bloco solido perto do alvo
		if (!CombatUtil.select(p, Items.RESPAWN_ANCHOR) || !p.getMainHandItem().is(Items.RESPAWN_ANCHOR)) return;
		BlockPos bestPos = null;
		double bs = 1e9;
		for (int x = -2; x <= 2; x++) for (int y = -2; y <= 0; y++) for (int z = -2; z <= 2; z++) {
			BlockPos b = tp.offset(x, y, z);
			if (!mc.level.getBlockState(b).isSolidRender()) continue;
			Vec3 c = Vec3.atCenterOf(b.above());
			if (p.getEyePosition().distanceTo(c) > r || c.distanceTo(p.position()) < selfDist.get()) continue;
			if (!CombatUtil.free(mc, b.above(), 1)) continue;
			double d = c.distanceToSqr(t.position());
			if (d < bs) { bs = d; bestPos = b; }
		}
		if (bestPos != null && CombatUtil.useOn(mc, bestPos, Direction.UP)) wait = delay.get();
	}
}
