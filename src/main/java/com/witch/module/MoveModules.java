package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Modulos de movimento simples (fisica do cliente). Servidor com anticheat pode corrigir ou banir. */
public final class MoveModules {
	private MoveModules() {}

	abstract static class Mv extends Module {
		boolean lastJump, lastGround;
		double lastVy;

		Mv(String n) {
			super(n, Category.MOVEMENT);
		}

		static Minecraft mc() {
			return Minecraft.getInstance();
		}

		/** Jogador utilizavel (vivo, nao espectador, sem tela aberta). */
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

		static boolean jumpHeld() {
			return mc().options.keyJump.isDown();
		}

		static boolean sneakHeld() {
			return mc().options.keyShift.isDown();
		}

		/** Direcao horizontal das teclas (vetor unitario), ou zero. */
		static Vec3 dir(LocalPlayer p) {
			var o = mc().options;
			double f = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
			double s = (o.keyRight.isDown() ? 1 : 0) - (o.keyLeft.isDown() ? 1 : 0);
			if (f == 0 && s == 0) return Vec3.ZERO;
			double yaw = Math.toRadians(p.getYRot());
			double x = -Math.sin(yaw) * f - Math.cos(yaw) * s;
			double z = Math.cos(yaw) * f - Math.sin(yaw) * s;
			double l = Math.sqrt(x * x + z * z);
			return new Vec3(x / l, 0, z / l);
		}

		static void vel(LocalPlayer p, double x, double y, double z) {
			p.setDeltaMovement(x, y, z);
		}

		/** Borda de subida da tecla de pulo. */
		boolean jumpEdge() {
			boolean j = jumpHeld();
			boolean e = j && !lastJump;
			lastJump = j;
			return e;
		}

		/** Acabou de sair do chao pulando (para modulos que modificam o pulo). */
		boolean justJumped(LocalPlayer p) {
			boolean r = lastGround && !p.onGround() && p.getDeltaMovement().y > 0.2;
			lastGround = p.onGround();
			return r;
		}
	}

	public static final class HighJump extends Mv {
		private final IntSetting h = add(new IntSetting("Height", 1, 10, 2, "x"));
		public HighJump() { super("High Jump"); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (justJumped(p)) { var v = p.getDeltaMovement(); vel(p, v.x, 0.42 * h.get(), v.z); }
		}
	}

	public static final class LongJump extends Mv {
		private final IntSetting b = add(new IntSetting("Boost", 1, 10, 4, ""));
		public LongJump() { super("Long Jump"); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (justJumped(p) && moving()) {
				Vec3 d = dir(p), v = p.getDeltaMovement();
				vel(p, v.x + d.x * 0.1 * b.get(), v.y, v.z + d.z * 0.1 * b.get());
			}
		}
	}

	public static final class Bhop extends Mv {
		private final IntSetting b = add(new IntSetting("Air boost", 0, 10, 2, ""));
		public Bhop() { super("Bhop"); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (p.onGround() && moving() && !sneakHeld() && !p.isInWater()) p.jumpFromGround();
			else if (!p.onGround() && moving() && b.get() > 0) {
				Vec3 d = dir(p), v = p.getDeltaMovement();
				vel(p, v.x + d.x * 0.004 * b.get(), v.y, v.z + d.z * 0.004 * b.get());
			}
		}
	}

	public static final class Glide extends Mv {
		private final IntSetting f = add(new IntSetting("Fall speed", 1, 10, 3, ""));
		public Glide() { super("Glide"); }
		@Override public void onTick() {
			var p = me(); if (p == null || p.onGround() || p.isInWater() || p.isInLava() || p.isFallFlying()) return;
			var v = p.getDeltaMovement(); double lim = -0.02 * f.get();
			if (v.y < lim) vel(p, v.x, lim, v.z);
		}
	}

	public static final class AirJump extends Mv {
		private final IntSetting n = add(new IntSetting("Jumps", 1, 10, 2, ""));
		private int used;
		public AirJump() { super("Air Jump"); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (p.onGround()) used = 0;
			if (jumpEdge() && !p.onGround() && used < n.get()) {
				var v = p.getDeltaMovement(); vel(p, v.x, 0.42, v.z); used++;
				p.fallDistance = 0;
			}
		}
	}

	public static final class Hover extends Mv {
		public Hover() { super("Hover"); }
		@Override public void onTick() {
			var p = me(); if (p == null || p.onGround() || p.isFallFlying()) return;
			if (sneakHeld() && jumpHeld()) { var v = p.getDeltaMovement(); vel(p, v.x, 0, v.z); p.fallDistance = 0; }
		}
	}

	public static final class OmniSprint extends Mv {
		public OmniSprint() { super("Omni Sprint"); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (moving() && !p.isUsingItem() && p.getFoodData().getFoodLevel() > 6) p.setSprinting(true);
		}
	}

	public static final class FastFall extends Mv {
		private final IntSetting m = add(new IntSetting("Multiplier", 1, 10, 3, "x"));
		public FastFall() { super("Fast Fall"); }
		@Override public void onTick() {
			var p = me(); if (p == null || p.onGround() || p.isInWater() || p.isInLava() || p.isFallFlying()) return;
			var v = p.getDeltaMovement();
			if (v.y < -0.1) vel(p, v.x, Math.max(-3.0, v.y * (1.0 + 0.1 * m.get())), v.z);
		}
	}

	public static final class FastLadder extends Mv {
		private final IntSetting s = add(new IntSetting("Speed", 1, 10, 3, ""));
		public FastLadder() { super("Fast Ladder"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.onClimbable()) return;
			var v = p.getDeltaMovement();
			if (jumpHeld() || (moving() && p.horizontalCollision)) vel(p, v.x, 0.1 * s.get(), v.z);
		}
	}

	public static final class Dolphin extends Mv {
		private final IntSetting s = add(new IntSetting("Speed", 1, 10, 3, ""));
		public Dolphin() { super("Dolphin"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.isInWater() || !moving()) return;
			Vec3 d = dir(p), v = p.getDeltaMovement();
			vel(p, v.x + d.x * 0.01 * s.get(), v.y, v.z + d.z * 0.01 * s.get());
		}
	}

	public static final class Parkour extends Mv {
		public Parkour() { super("Parkour"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.onGround() || !moving() || sneakHeld()) return;
			Vec3 d = dir(p);
			var box = p.getBoundingBox().move(d.x * 0.5, -0.6, d.z * 0.5);
			if (mc().level.noCollision(p, box)) p.jumpFromGround();
		}
	}

	public static final class SafeWalk extends Mv {
		public SafeWalk() { super("Safe Walk"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.onGround() || jumpHeld()) return;
			var v = p.getDeltaMovement(); double x = v.x, z = v.z;
			var bb = p.getBoundingBox();
			if (mc().level.noCollision(p, bb.move(x * 2.0, -0.6, 0))) x = 0;
			if (mc().level.noCollision(p, bb.move(0, -0.6, z * 2.0))) z = 0;
			if (x != v.x || z != v.z) vel(p, x, v.y, z);
		}
	}

	public static final class ElytraBoost extends Mv {
		private final IntSetting b = add(new IntSetting("Boost", 1, 10, 3, ""));
		public ElytraBoost() { super("Elytra Boost"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.isFallFlying() || !mc().options.keyUp.isDown()) return;
			Vec3 l = p.getLookAngle(), v = p.getDeltaMovement();
			if (v.length() > 3.0) return;
			double k = 0.01 * b.get();
			vel(p, v.x + l.x * k, v.y + l.y * k, v.z + l.z * k);
		}
	}

	public static final class ElytraControl extends Mv {
		private final IntSetting s = add(new IntSetting("Speed", 1, 5, 2, ""));
		public ElytraControl() { super("Elytra Control"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.isFallFlying()) return;
			Vec3 l = p.getLookAngle();
			if (mc().options.keyUp.isDown()) vel(p, l.x * 0.6 * s.get(), l.y * 0.6 * s.get(), l.z * 0.6 * s.get());
			else if (sneakHeld()) vel(p, 0, 0, 0);
		}
	}

	public static final class Jetpack extends Mv {
		private final IntSetting s = add(new IntSetting("Power", 1, 10, 3, ""));
		public Jetpack() { super("Jetpack"); }
		@Override public void onTick() {
			var p = me(); if (p == null || p.onGround() || !jumpHeld() || p.isFallFlying()) return;
			var v = p.getDeltaMovement();
			vel(p, v.x, Math.min(0.8, v.y + 0.03 * s.get()), v.z);
			p.fallDistance = 0;
		}
	}

	public static final class Bounce extends Mv {
		public Bounce() { super("Bounce"); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (p.onGround() && !lastGround && lastVy < -0.4 && !sneakHeld()) {
				var v = p.getDeltaMovement(); vel(p, v.x, Math.min(1.5, -lastVy * 0.8), v.z);
			}
			lastGround = p.onGround();
			lastVy = p.getDeltaMovement().y;
		}
	}

	public static final class AutoSneak extends Mv {
		public AutoSneak() { super("Auto Sneak"); }
		@Override public void onTick() {
			if (me() != null) mc().options.keyShift.setDown(true);
		}
		@Override protected void onDisable() {
			mc().options.keyShift.setDown(false);
		}
	}

	public static final class AirSpeed extends Mv {
		private final IntSetting s = add(new IntSetting("Speed", 1, 10, 3, ""));
		public AirSpeed() { super("Air Speed"); }
		@Override public void onTick() {
			var p = me(); if (p == null || p.onGround() || !moving() || p.isFallFlying()) return;
			Vec3 d = dir(p), v = p.getDeltaMovement();
			if (Math.hypot(v.x, v.z) > 0.7) return;
			vel(p, v.x + d.x * 0.008 * s.get(), v.y, v.z + d.z * 0.008 * s.get());
		}
	}

	public static final class Slide extends Mv {
		private final IntSetting s = add(new IntSetting("Slipperiness", 1, 10, 4, ""));
		public Slide() { super("Slide"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.onGround() || moving()) return;
			var v = p.getDeltaMovement();
			if (Math.hypot(v.x, v.z) < 0.03) return;
			double k = 1.0 + 0.06 * s.get();
			vel(p, v.x * k, v.y, v.z * k);
		}
	}

	public static final class WallJump extends Mv {
		public WallJump() { super("Wall Jump"); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (jumpEdge() && p.horizontalCollision && !p.onGround()) {
				var v = p.getDeltaMovement(); vel(p, v.x, 0.42, v.z); p.fallDistance = 0;
			}
		}
	}

	public static final class Sink extends Mv {
		private final IntSetting s = add(new IntSetting("Speed", 1, 10, 3, ""));
		public Sink() { super("Sink"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !(p.isInWater() || p.isInLava()) || !sneakHeld()) return;
			var v = p.getDeltaMovement(); vel(p, v.x, -0.05 * s.get(), v.z);
		}
	}

	public static final class AntiVoid extends Mv {
		public AntiVoid() { super("Anti Void"); }
		@Override public void onTick() {
			var p = me(); if (p == null) return;
			if (p.getY() < mc().level.getMinY() + 2 && p.getDeltaMovement().y < 0) {
				var v = p.getDeltaMovement(); vel(p, v.x, 0.7, v.z); p.fallDistance = 0;
			}
		}
	}

	public static final class Dash extends Mv {
		private final IntSetting s = add(new IntSetting("Power", 1, 10, 4, ""));
		private boolean fire;
		public Dash() { super("Dash"); }
		@Override protected void onEnable() { fire = true; }
		@Override public void onTick() {
			if (!fire) return;
			fire = false;
			var p = me();
			if (p != null) { Vec3 l = p.getLookAngle(), v = p.getDeltaMovement(); double k = 0.4 * s.get(); vel(p, v.x + l.x * k, v.y + Math.max(0, l.y) * k, v.z + l.z * k); }
			setEnabled(false);
		}
	}

	public static final class Launch extends Mv {
		private final IntSetting s = add(new IntSetting("Power", 1, 10, 4, ""));
		private boolean fire;
		public Launch() { super("Launch"); }
		@Override protected void onEnable() { fire = true; }
		@Override public void onTick() {
			if (!fire) return;
			fire = false;
			var p = me();
			if (p != null) { var v = p.getDeltaMovement(); vel(p, v.x, 0.3 * s.get(), v.z); }
			setEnabled(false);
		}
	}

	public static final class SneakSpeed extends Mv {
		private final IntSetting s = add(new IntSetting("Speed", 1, 10, 3, ""));
		public SneakSpeed() { super("Sneak Speed"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.onGround() || !sneakHeld() || !moving()) return;
			Vec3 d = dir(p), v = p.getDeltaMovement();
			vel(p, v.x + d.x * 0.01 * s.get(), v.y, v.z + d.z * 0.01 * s.get());
		}
	}

	public static final class Levitate extends Mv {
		private final IntSetting s = add(new IntSetting("Speed", 1, 10, 3, ""));
		public Levitate() { super("Levitate"); }
		@Override public void onTick() {
			var p = me(); if (p == null || p.onGround() || p.isFallFlying() || !jumpHeld()) return;
			var v = p.getDeltaMovement(); vel(p, v.x, 0.03 * s.get(), v.z); p.fallDistance = 0;
		}
	}

	public static final class AirBrake extends Mv {
		public AirBrake() { super("Air Brake"); }
		@Override public void onTick() {
			var p = me(); if (p == null || p.onGround() || p.isFallFlying() || moving()) return;
			var v = p.getDeltaMovement(); vel(p, v.x * 0.5, v.y, v.z * 0.5);
		}
	}

	public static final class LowGravity extends Mv {
		private final IntSetting g = add(new IntSetting("Strength", 1, 10, 4, ""));
		public LowGravity() { super("Low Gravity"); }
		@Override public void onTick() {
			var p = me(); if (p == null || p.onGround() || p.isInWater() || p.isInLava() || p.isFallFlying()) return;
			var v = p.getDeltaMovement(); if (v.y < 0) vel(p, v.x, v.y + 0.007 * g.get(), v.z);
		}
	}

	public static final class Strafe extends Mv {
		public Strafe() { super("Strafe"); }
		@Override public void onTick() {
			var p = me(); if (p == null || p.onGround() || p.isFallFlying() || !moving()) return;
			Vec3 d = dir(p), v = p.getDeltaMovement();
			double sp = Math.max(0.2, Math.hypot(v.x, v.z));
			vel(p, d.x * sp, v.y, d.z * sp);
		}
	}

	public static final class Pogo extends Mv {
		private final IntSetting h = add(new IntSetting("Height", 1, 10, 2, "x"));
		public Pogo() { super("Pogo"); }
		@Override public void onTick() {
			var p = me(); if (p == null || !p.onGround() || p.isInWater()) return;
			if (jumpHeld()) { var v = p.getDeltaMovement(); vel(p, v.x, 0.42 * h.get(), v.z); }
		}
	}
}
