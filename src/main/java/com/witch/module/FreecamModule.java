package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * FreeLook / Freecam estilo Meteor: personagem parado, camera voa com WASD.
 * Camera atravessa parede (sem clip de zoom e sem smart-culling de bloco).
 * A camera e aplicada no CameraMixin.
 */
public class FreecamModule extends Module {
	private static FreecamModule instance;

	private final IntSetting speed = add(new IntSetting("Speed", 1, 20, 5, ""));

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
		return instance.prev.lerp(instance.pos, partial);
	}

	public static double x(float partial) {
		return pos(partial).x;
	}

	public static double y(float partial) {
		return pos(partial).y;
	}

	public static double z(float partial) {
		return pos(partial).z;
	}

	public static float yaw() {
		return instance.yaw;
	}

	public static float pitch() {
		return instance.pitch;
	}

	/** Chamado pelo MouseHandlerMixin: o mouse gira a camera, nao o personagem. */
	public static void turn(double dx, double dy) {
		instance.yaw += (float) dx * 0.15f;
		instance.pitch = Mth.clamp(instance.pitch + (float) dy * 0.15f, -90f, 90f);
	}

	@Override
	protected void onEnable() {
		var p = Minecraft.getInstance().player;
		if (p == null) return;
		pos = p.getEyePosition();
		prev = pos;
		yaw = p.getYRot();
		pitch = p.getXRot();
		savedInput = p.input;
		p.input = new ClientInput(); // personagem sem comandos: fica parado
	}

	@Override
	protected void onDisable() {
		var p = Minecraft.getInstance().player;
		if (p != null && savedInput != null) p.input = savedInput;
		savedInput = null;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null) return;
		prev = pos;
		if (mc.gui.screen() != null) return;

		var o = mc.options;
		double fwd = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
		double side = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
		double up = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);

		// movimento no plano do olhar (estilo Meteor): inclui pitch no forward
		double yawRad = Math.toRadians(yaw);
		double pitchRad = Math.toRadians(pitch);
		double cosPitch = Math.cos(pitchRad);
		double sinYaw = Math.sin(yawRad);
		double cosYaw = Math.cos(yawRad);

		double step = speed.get() * 0.15;

		// forward: na direcao que a camera olha (atravessa bloco em qualquer eixo)
		double fdx = -sinYaw * cosPitch * fwd;
		double fdy = -Math.sin(pitchRad) * fwd;
		double fdz = cosYaw * cosPitch * fwd;

		// strafe horizontal
		double sdx = cosYaw * side;
		double sdz = sinYaw * side;

		pos = pos.add(
				(fdx + sdx) * step,
				(fdy + up) * step,
				(fdz + sdz) * step
		);
	}
}
