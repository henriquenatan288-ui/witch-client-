package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * FreeLook estilo Meteor: em 3ª pessoa a camera gira livre (mouse),
 * o corpo do player nao acompanha. Camera atravessa parede (sem clip).
 */
public class FreeLookModule extends Module {
	private static FreeLookModule instance;

	private final IntSetting sensitivity = add(new IntSetting("Sensitivity", 1, 20, 8, ""));
	private final IntSetting distance = add(new IntSetting("Distance", 1, 20, 4, ""));

	public float cameraYaw;
	public float cameraPitch;
	private CameraType prePers;

	public FreeLookModule() {
		super("FreeLook", Category.VISUALS);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled() && Minecraft.getInstance().player != null;
	}

	public static float yaw() {
		return instance.cameraYaw;
	}

	public static float pitch() {
		return instance.cameraPitch;
	}

	public static float distance() {
		return instance.distance.get();
	}

	/** Mouse gira so a camera (chamado pelo MouseHandlerMixin). */
	public static void turn(double dx, double dy) {
		float sens = instance.sensitivity.get() * 0.15f;
		instance.cameraYaw += (float) dx * sens;
		instance.cameraPitch = Mth.clamp(instance.cameraPitch + (float) dy * sens, -90f, 90f);
	}

	@Override
	protected void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		if (p == null) return;
		cameraYaw = p.getYRot();
		cameraPitch = p.getXRot();
		prePers = mc.options.getCameraType();
		if (prePers == CameraType.FIRST_PERSON) {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
	}

	@Override
	protected void onDisable() {
		Minecraft mc = Minecraft.getInstance();
		if (prePers != null && mc.options.getCameraType() != prePers) {
			mc.options.setCameraType(prePers);
		}
		prePers = null;
	}

	@Override
	public void onTick() {
		cameraPitch = Mth.clamp(cameraPitch, -90f, 90f);
	}
}
