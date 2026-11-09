package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Modulos de combate extras: Kill Aura, Aim Assist, Auto Weapon. */
public final class CombatExtras {
	private CombatExtras() {}

	static LivingEntity nearest(Minecraft mc, double range, boolean players, boolean mobs) {
		LivingEntity best = null;
		double bd = range * range;
		var p = mc.player;
		for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range))) {
			if (e == p || !e.isAlive() || e.isSpectator()) continue;
			boolean isPlayer = e instanceof Player;
			if (isPlayer ? !players : !mobs) continue;
			double d = e.distanceToSqr(p);
			if (d < bd) { bd = d; best = e; }
		}
		return best;
	}

	public static final class KillAura extends Module {
		private final IntSetting range = add(new IntSetting("Range", 2, 6, 4, " blocks"));
		private final IntSetting players = add(new IntSetting("Players (0/1)", 0, 1, 1, ""));
		private final IntSetting mobs = add(new IntSetting("Mobs (0/1)", 0, 1, 0, ""));
		public KillAura() { super("Kill Aura", Category.COMBAT); }
		@Override public void onTick() {
			Minecraft mc = Minecraft.getInstance();
			var p = mc.player;
			if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null || !p.isAlive() || p.isSpectator() || p.isUsingItem()) return;
			if (p.getAttackStrengthScale(0f) < 0.95f) return;
			LivingEntity t = nearest(mc, range.get(), players.get() == 1, mobs.get() == 1);
			if (t == null) return;
			mc.gameMode.attack(p, t);
			p.swing(InteractionHand.MAIN_HAND);
		}
	}

	public static final class AimAssist extends Module {
		private final IntSetting range = add(new IntSetting("Range", 2, 8, 5, " blocks"));
		private final IntSetting smooth = add(new IntSetting("Smoothness", 1, 10, 6, ""));
		private final IntSetting fov = add(new IntSetting("FOV", 10, 180, 90, "deg"));
		public AimAssist() { super("Aim Assist", Category.COMBAT); }
		@Override public void onTick() {
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer p = mc.player;
			if (p == null || mc.level == null || mc.gui.screen() != null || !p.isAlive() || p.isSpectator()) return;
			if (!mc.options.keyAttack.isDown()) return; // so ajuda enquanto voce ataca
			LivingEntity t = nearest(mc, range.get(), true, false);
			if (t == null) return;
			Vec3 d = t.getEyePosition().subtract(p.getEyePosition());
			float ty = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
			float tp = (float) (-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
			float dy = Mth.wrapDegrees(ty - p.getYRot()), dp = tp - p.getXRot();
			if (Math.abs(dy) > fov.get() / 2f) return;
			float k = 1f / (1f + smooth.get() * 2.5f);
			p.setYRot(p.getYRot() + dy * k);
			p.setXRot(Mth.clamp(p.getXRot() + dp * k, -90f, 90f));
		}
	}

	public static final class AutoWeapon extends Module {
		public AutoWeapon() { super("Auto Weapon", Category.COMBAT); }
		private static boolean weapon(ItemStack s) {
			String n = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
			return n.endsWith("_sword") || n.endsWith("_axe") || n.equals("mace");
		}
		private static int score(ItemStack s) {
			String n = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
			int base = n.startsWith("netherite") ? 6 : n.startsWith("diamond") ? 5 : n.startsWith("iron") ? 4 : n.startsWith("stone") ? 3 : n.startsWith("golden") ? 2 : 1;
			if (n.equals("mace")) base = 5;
			return base * 2 + (n.endsWith("_sword") ? 1 : 0);
		}
		@Override public void onTick() {
			Minecraft mc = Minecraft.getInstance();
			var p = mc.player;
			if (p == null || mc.gui.screen() != null) return;
			if (!mc.options.keyAttack.isDown() || !(mc.hitResult instanceof EntityHitResult)) return;
			if (weapon(p.getMainHandItem())) return;
			int bestSlot = -1, bs = 0;
			for (int i = 0; i < 9; i++) {
				ItemStack s = p.getInventory().getItem(i);
				if (s.isEmpty() || !weapon(s)) continue;
				int sc = score(s);
				if (sc > bs) { bs = sc; bestSlot = i; }
			}
			if (bestSlot >= 0) p.getInventory().setSelectedSlot(bestSlot);
		}
	}
}
