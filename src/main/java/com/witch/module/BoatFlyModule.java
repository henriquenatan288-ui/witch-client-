package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

/**
 * Boat Fly: o barco em que voce esta voa. WASD move na direcao em que voce olha, Espaco sobe, Ctrl (correr) desce,
 * sem input o barco fica parado no ar. "No clip" deixa o barco atravessar blocos.
 * Servidores com anticheat costumam rejeitar movimento de veiculo fora do normal.
 */
public class BoatFlyModule extends Module {
	private final IntSetting speed = add(new IntSetting("Speed", 1, 20, 6, ""));
	private final IntSetting vSpeed = add(new IntSetting("Vertical speed", 1, 10, 3, ""));
	private final IntSetting noClip = add(new IntSetting("No clip (0/1)", 0, 1, 0, ""));

	private AbstractBoat last;

	public BoatFlyModule() {
		super("Boat Fly", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		var mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null) return;
		if (!(p.getVehicle() instanceof AbstractBoat boat)) {
			restore();
			return;
		}
		last = boat;
		var o = mc.options;

		boat.setYRot(p.getYRot());
		boat.setNoGravity(true);
		boat.noPhysics = noClip.get() == 1;

		double fwd = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
		double str = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
		double yr = Math.toRadians(p.getYRot());
		double sp = speed.get() * 0.1;
		double vx = (-Math.sin(yr) * fwd + Math.cos(yr) * str) * sp;
		double vz = (Math.cos(yr) * fwd + Math.sin(yr) * str) * sp;
		double vy = ((o.keyJump.isDown() ? 1 : 0) - (o.keySprint.isDown() ? 1 : 0)) * vSpeed.get() * 0.1;
		boat.setDeltaMovement(vx, vy, vz);
	}

	private void restore() {
		if (last != null) {
			last.setNoGravity(false);
			last.noPhysics = false;
			last = null;
		}
	}

	@Override
	protected void onDisable() {
		restore();
	}
}
