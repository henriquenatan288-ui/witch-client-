package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** 30 modulos extras (combate, utilidade, visual, movimento). Tudo roda no cliente via teclas/API normais. */
public final class MiscModules {
	private MiscModules() {}

	abstract static class Md extends Module {
		int t;
		Md(String n, Category c) { super(n, c); }
		static Minecraft mc() { return Minecraft.getInstance(); }
		static LocalPlayer me() {
			var m = mc();
			var p = m.player;
			if (p == null || m.level == null || p.isSpectator() || p.isDeadOrDying() || m.gui.screen() != null) return null;
			return p;
		}
		static boolean moving() {
			var o = mc().options;
			return o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown();
		}
		static Vec3 dir(LocalPlayer p) {
			var o = mc().options;
			double f = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
			double s = (o.keyRight.isDown() ? 1 : 0) - (o.keyLeft.isDown() ? 1 : 0);
			if (f == 0 && s == 0) return Vec3.ZERO;
			double yaw = Math.toRadians(p.getYRot());
			double x = -Math.sin(yaw) * f - Math.cos(yaw) * s, z = Math.cos(yaw) * f - Math.sin(yaw) * s;
			double l = Math.sqrt(x * x + z * z);
			return new Vec3(x / l, 0, z / l);
		}
		boolean every(int n) { if (++t >= Math.max(1, n)) { t = 0; return true; } return false; }
	}

	/** Opcao inteira do jogo salva/restaurada. */
	abstract static class Opt extends Md {
		Integer saved;
		Opt(String n, Category c) { super(n, c); }
		abstract OptionInstance<Integer> opt();
		abstract int want();
		@Override public void onTick() {
			if (mc().player == null) return;
			var o = opt();
			if (saved == null) saved = o.get();
			if (o.get() != want()) o.set(want());
		}
		@Override protected void onDisable() {
			if (saved != null && mc().player != null) opt().set(saved);
			saved = null;
		}
	}

	// ---------------- COMBAT ----------------
	public static final class AutoClicker extends Md {
		private final IntSetting cps = add(new IntSetting("CPS", 4, 20, 10, ""));
		public AutoClicker() { super("Auto Clicker", Category.COMBAT); }
		@Override public void onTick() {
			var p = me(); if (p == null || !mc().options.keyAttack.isDown()) return;
			if (every(20 / cps.get())) mc().missTime = 0;
		}
	}

	public static final class AutoBlock extends Md {
		private final IntSetting range = add(new IntSetting("Range", 2, 8, 5, " blocks"));
		private boolean held;
		public AutoBlock() { super("Auto Block", Category.COMBAT); }
		@Override public void onTick() {
			var p = me();
			boolean want = false;
			if (p != null && p.getOffhandItem().is(Items.SHIELD)) {
				var e = CombatExtras.nearest(mc(), range.get(), true, false);
				want = e != null;
			}
			if (want) { mc().options.keyUse.setDown(true); held = true; }
			else if (held) { mc().options.keyUse.setDown(false); held = false; }
		}
		@Override protected void onDisable() { if (held) mc().options.keyUse.setDown(false); held = false; }
	}

	public static final class AutoLog extends Md {
		private final IntSetting hp = add(new IntSetting("Min HP", 1, 19, 4, ""));
		public AutoLog() { super("Auto Log", Category.COMBAT); }
		@Override public void onTick() {
			var m = mc(); var p = m.player;
			if (p == null || m.getConnection() == null || p.isDeadOrDying() || p.getHealth() > hp.get()) return;
			setEnabled(false);
			m.getConnection().getConnection().disconnect(Component.literal("Witch Auto Log: vida baixa"));
		}
	}

	public static final class AutoGap extends Md {
		private final IntSetting hp = add(new IntSetting("Min HP", 2, 18, 10, ""));
		private int back = -1;
		public AutoGap() { super("Auto Gap", Category.COMBAT); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (p.getHealth() <= hp.get()) {
				if (back < 0) back = p.getInventory().getSelectedSlot();
				if (CombatUtil.select(p, Items.GOLDEN_APPLE) || CombatUtil.select(p, Items.ENCHANTED_GOLDEN_APPLE)) {
					mc().options.keyUse.setDown(true);
					return;
				}
			}
			if (back >= 0 && !p.isUsingItem()) {
				mc().options.keyUse.setDown(false);
				p.getInventory().setSelectedSlot(back);
				back = -1;
			}
		}
		@Override protected void onDisable() { mc().options.keyUse.setDown(false); back = -1; }
	}

	public static final class WTap extends Md {
		private boolean was;
		public WTap() { super("W-Tap", Category.COMBAT); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			boolean atk = mc().options.keyAttack.isDown();
			if (atk && !was && mc().hitResult instanceof EntityHitResult && p.isSprinting()) p.setSprinting(false);
			was = atk;
		}
	}

	public static final class CooldownGuard extends Md {
		private final IntSetting min = add(new IntSetting("Min charge", 50, 100, 90, "%"));
		public CooldownGuard() { super("Cooldown Guard", Category.COMBAT); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (mc().hitResult instanceof EntityHitResult && p.getAttackStrengthScale(0f) * 100 < min.get()) mc().missTime = 2;
		}
	}

	// ---------------- MOVEMENT ----------------
	public static final class TargetStrafe extends Md {
		private final IntSetting spd = add(new IntSetting("Speed", 1, 10, 3, ""));
		public TargetStrafe() { super("Target Strafe", Category.MOVEMENT); }
		@Override public void onTick() {
			var p = me(); if (p == null || !moving()) return;
			var e = CombatExtras.nearest(mc(), 5, true, false); if (e == null) return;
			Vec3 r = e.position().subtract(p.position());
			double l = Math.sqrt(r.x * r.x + r.z * r.z); if (l < 0.1) return;
			double s = spd.get() * 0.03;
			var v = p.getDeltaMovement();
			p.setDeltaMovement(v.x + (-r.z / l) * s, v.y, v.z + (r.x / l) * s);
		}
	}

	public static final class AutoJump extends Md {
		public AutoJump() { super("Auto Jump", Category.MOVEMENT); }
		@Override public void onTick() {
			var p = me(); if (p != null && p.onGround() && !mc().options.keyShift.isDown() && moving()) p.jumpFromGround();
		}
	}

	public static final class IceSpeed extends Md {
		private final IntSetting m = add(new IntSetting("Boost", 1, 10, 3, "%"));
		public IceSpeed() { super("Ice Speed", Category.MOVEMENT); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.onGround() || !moving()) return;
			var b = mc().level.getBlockState(p.blockPosition().below());
			if (b.is(net.minecraft.world.level.block.Blocks.ICE) || b.is(net.minecraft.world.level.block.Blocks.PACKED_ICE) || b.is(net.minecraft.world.level.block.Blocks.BLUE_ICE)) {
				var v = p.getDeltaMovement(); double f = 1 + m.get() * 0.02;
				p.setDeltaMovement(v.x * f, v.y, v.z * f);
			}
		}
	}

	public static final class WaterSpeed extends Md {
		private final IntSetting b = add(new IntSetting("Boost", 1, 10, 3, ""));
		public WaterSpeed() { super("Water Speed", Category.MOVEMENT); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.isInWater() || !moving()) return;
			Vec3 d = dir(p), v = p.getDeltaMovement();
			p.setDeltaMovement(v.x + d.x * 0.01 * b.get(), v.y, v.z + d.z * 0.01 * b.get());
		}
	}

	public static final class EdgeJump extends Md {
		public EdgeJump() { super("Edge Jump", Category.MOVEMENT); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.onGround() || !moving()) return;
			Vec3 d = dir(p);
			var box = p.getBoundingBox().move(d.x * 0.35, -0.6, d.z * 0.35);
			if (mc().level.noCollision(p, box)) p.jumpFromGround();
		}
	}

	public static final class LavaEscape extends Md {
		public LavaEscape() { super("Lava Escape", Category.MOVEMENT); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.isInLava()) return;
			var v = p.getDeltaMovement(); p.setDeltaMovement(v.x, Math.max(v.y, 0.15), v.z);
		}
	}

	public static final class SneakSpam extends Md {
		public SneakSpam() { super("Sneak Spam", Category.MOVEMENT); }
		@Override public void onTick() { if (me() != null) mc().options.keyShift.setDown((++t & 1) == 0); }
		@Override protected void onDisable() { mc().options.keyShift.setDown(false); }
	}

	// ---------------- UTILITY ----------------
	public static final class AntiAfk extends Md {
		private final IntSetting sec = add(new IntSetting("Interval", 5, 60, 20, "s"));
		public AntiAfk() { super("Anti AFK", Category.UTILITY); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (every(sec.get() * 20)) {
				p.setYRot(p.getYRot() + ((p.tickCount / 20) % 2 == 0 ? 15 : -15));
				if (p.onGround()) p.jumpFromGround();
			}
		}
	}

	public static final class AutoRespawn extends Md {
		public AutoRespawn() { super("Auto Respawn", Category.UTILITY); }
		@Override public void onTick() {
			var m = mc();
			if (m.player != null && m.gui.screen() instanceof DeathScreen && every(10)) m.player.respawn();
		}
	}

	public static final class AutoReplenish extends Md {
		private final IntSetting min = add(new IntSetting("Min count", 1, 32, 8, ""));
		public AutoReplenish() { super("Auto Replenish", Category.UTILITY); }
		@Override public void onTick() {
			var p = me(); if (p == null || !every(10)) return;
			int sel = p.getInventory().getSelectedSlot();
			var cur = p.getInventory().getItem(sel);
			if (cur.isEmpty() || cur.getCount() > min.get() || cur.getMaxStackSize() < 2) return;
			for (int i = 9; i < 36; i++) {
				var s = p.getInventory().getItem(i);
				if (!s.isEmpty() && s.getItem() == cur.getItem()) {
					mc().gameMode.handleContainerInput(p.inventoryMenu.containerId, i, sel, ContainerInput.SWAP, p);
					return;
				}
			}
		}
	}

	public static final class AutoMine extends Md {
		private boolean held;
		public AutoMine() { super("Auto Mine", Category.WORLD); }
		@Override public void onTick() { if (me() != null) { mc().options.keyAttack.setDown(true); held = true; } }
		@Override protected void onDisable() { if (held) mc().options.keyAttack.setDown(false); held = false; }
	}

	public static final class AutoUse extends Md {
		private boolean held;
		public AutoUse() { super("Auto Use", Category.UTILITY); }
		@Override public void onTick() { if (me() != null) { mc().options.keyUse.setDown(true); held = true; } }
		@Override protected void onDisable() { if (held) mc().options.keyUse.setDown(false); held = false; }
	}

	public static final class AutoTorch extends Md {
		private final IntSetting lvl = add(new IntSetting("Max light", 0, 14, 7, ""));
		public AutoTorch() { super("Auto Torch", Category.WORLD); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.onGround() || !every(20)) return;
			BlockPos feet = p.blockPosition();
			if (mc().level.getMaxLocalRawBrightness(feet) > lvl.get() || !mc().level.getBlockState(feet).isAir()) return;
			int back = p.getInventory().getSelectedSlot();
			if (CombatUtil.select(p, Items.TORCH)) {
				CombatUtil.useOn(mc(), feet.below(), Direction.UP);
				p.getInventory().setSelectedSlot(back);
			}
		}
	}

	public static final class YawLock extends Md {
		private final IntSetting step = add(new IntSetting("Step", 15, 90, 45, " deg"));
		public YawLock() { super("Yaw Lock", Category.UTILITY); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			float s = step.get();
			p.setYRot(Math.round(p.getYRot() / s) * s);
		}
	}

	public static final class PitchLock extends Md {
		private final IntSetting pitch = add(new IntSetting("Pitch", -90, 90, 0, " deg"));
		public PitchLock() { super("Pitch Lock", Category.UTILITY); }
		@Override public void onTick() { var p = me(); if (p != null) p.setXRot(pitch.get()); }
	}

	public static final class HotbarLock extends Md {
		private final IntSetting slot = add(new IntSetting("Slot", 1, 9, 1, ""));
		public HotbarLock() { super("Hotbar Lock", Category.UTILITY); }
		@Override public void onTick() {
			var p = me(); if (p != null && p.getInventory().getSelectedSlot() != slot.get() - 1) p.getInventory().setSelectedSlot(slot.get() - 1);
		}
	}

	public static final class HotbarCycle extends Md {
		private final IntSetting tk = add(new IntSetting("Delay", 2, 40, 10, " ticks"));
		public HotbarCycle() { super("Hotbar Cycle", Category.UTILITY); }
		@Override public void onTick() {
			var p = me(); if (p == null || !every(tk.get())) return;
			p.getInventory().setSelectedSlot((p.getInventory().getSelectedSlot() + 1) % 9);
		}
	}

	public static final class ItemDropper extends Md {
		private final IntSetting tk = add(new IntSetting("Delay", 2, 40, 10, " ticks"));
		public ItemDropper() { super("Item Dropper", Category.UTILITY); }
		@Override public void onTick() {
			var p = me(); if (p == null || !every(tk.get())) return;
			int sel = p.getInventory().getSelectedSlot();
			if (p.getInventory().getItem(sel).isEmpty()) return;
			mc().gameMode.handleContainerInput(p.inventoryMenu.containerId, 36 + sel, 1, ContainerInput.THROW, p);
		}
	}

	public static final class Spinner extends Md {
		private final IntSetting spd = add(new IntSetting("Speed", 1, 30, 10, ""));
		public Spinner() { super("Spinner", Category.VISUALS); }
		@Override public void onTick() { var p = me(); if (p != null) p.setYRot(p.getYRot() + spd.get()); }
	}

	// ---------------- VISUALS / OPTIONS ----------------
	public static final class FovChanger extends Opt {
		private final IntSetting f = add(new IntSetting("FOV", 30, 110, 100, ""));
		public FovChanger() { super("FOV Changer", Category.VISUALS); }
		@Override OptionInstance<Integer> opt() { return mc().options.fov(); }
		@Override int want() { return f.get(); }
	}

	public static final class RenderDistance extends Opt {
		private final IntSetting d = add(new IntSetting("Chunks", 2, 32, 8, ""));
		public RenderDistance() { super("Render Distance", Category.VISUALS); }
		@Override OptionInstance<Integer> opt() { return mc().options.renderDistance(); }
		@Override int want() { return d.get(); }
	}

	public static final class NoBob extends Md {
		private Boolean saved;
		public NoBob() { super("No Bob", Category.VISUALS); }
		@Override public void onTick() {
			var o = mc().options.bobView();
			if (saved == null) saved = o.get();
			if (o.get()) o.set(false);
		}
		@Override protected void onDisable() {
			if (saved != null) mc().options.bobView().set(saved);
			saved = null;
		}
	}

	public static final class EntityDistance extends Md {
		private final IntSetting d = add(new IntSetting("Scale", 50, 100, 100, "%"));
		private Double saved;
		public EntityDistance() { super("Entity Distance", Category.VISUALS); }
		@Override public void onTick() {
			var o = mc().options.entityDistanceScaling();
			if (saved == null) saved = o.get();
			double w = d.get() / 100.0;
			if (Math.abs(o.get() - w) > 1e-6) o.set(w);
		}
		@Override protected void onDisable() {
			if (saved != null) mc().options.entityDistanceScaling().set(saved);
			saved = null;
		}
	}

	public static final class MouseSens extends Md {
		private final IntSetting s = add(new IntSetting("Sensitivity", 1, 200, 50, "%"));
		private Double saved;
		public MouseSens() { super("Mouse Sens", Category.UTILITY); }
		@Override public void onTick() {
			var o = mc().options.sensitivity();
			if (saved == null) saved = o.get();
			double w = s.get() / 200.0;
			if (Math.abs(o.get() - w) > 1e-6) o.set(w);
		}
		@Override protected void onDisable() {
			if (saved != null) mc().options.sensitivity().set(saved);
			saved = null;
		}
	}
}
