package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FreecamModule extends Module {
	private static FreecamModule instance;
	private final IntSetting speed = this.add(new IntSetting("Speed", 1, 20, 5, ""));
	private Vec3 pos = Vec3.ZERO;
	private Vec3 prev = Vec3.ZERO;
	private float yaw;
	private float pitch;
	private ClientInput savedInput;

	public FreecamModule() {
		super("Freecam", Category.VISUALS);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled() && Minecraft.getInstance().player != null;
	}

	public static Vec3 pos(float partial) {
		return instance.prev.lerp(instance.pos, (double) partial);
	}

	public static float yaw() {
		return instance.yaw;
	}

	public static float pitch() {
		return instance.pitch;
	}

	public static void turn(double dx, double dy) {
		instance.yaw += (float) dx * 0.15f;
		instance.pitch = Mth.clamp(instance.pitch + (float) dy * 0.15f, -90.0f, 90.0f);
	}

	@Override
	protected void onEnable() {
		LocalPlayer p = Minecraft.getInstance().player;
		if (p == null) return;
		this.prev = this.pos = p.getEyePosition();
		this.yaw = p.getYRot();
		this.pitch = p.getXRot();
		this.savedInput = p.input;
		p.input = new ClientInput();
	}

	@Override
	protected void onDisable() {
		LocalPlayer p = Minecraft.getInstance().player;
		if (p != null && this.savedInput != null) {
			p.input = this.savedInput;
		}
		this.savedInput = null;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null) return;
		this.prev = this.pos;
		if (mc.gui.screen() != null) return;
		Options o = mc.options;
		double fwd = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
		double side = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
		double up = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
		double r = Math.toRadians(this.yaw);
		double sin = Math.sin(r);
		double cos = Math.cos(r);
		double step = (double) this.speed.get() * 0.1;
		double dx = (-sin * fwd + cos * side) * step;
		double dz = (cos * fwd + sin * side) * step;
		this.pos = this.pos.add(dx, up * step, dz);
	}
}
