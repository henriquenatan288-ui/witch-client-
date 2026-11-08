package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.world.phys.Vec3;

/** Reduz o knockback recebido (porcentagem horizontal e vertical). 0% = cancela, 100% = normal. */
public class VelocityModule extends Module {
	private static VelocityModule instance;
	private final IntSetting horizontal = add(new IntSetting("Horizontal", 0, 100, 0, "%"));
	private final IntSetting vertical = add(new IntSetting("Vertical", 0, 100, 0, "%"));

	public VelocityModule() {
		super("Velocity", Category.UTILITY);
		instance = this;
	}

	/** Chamado no lugar de lerpMotion do jogador local: devolve a nova velocidade, ja reduzida. */
	public static Vec3 apply(Vec3 current, Vec3 incoming) {
		if (instance == null || !instance.isEnabled()) return incoming;
		double h = instance.horizontal.get() / 100.0;
		double v = instance.vertical.get() / 100.0;
		return new Vec3(current.x + (incoming.x - current.x) * h,
				current.y + (incoming.y - current.y) * v,
				current.z + (incoming.z - current.z) * h);
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
