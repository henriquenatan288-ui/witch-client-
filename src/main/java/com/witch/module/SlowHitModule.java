package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * So o visual: deixa a animacao do braco mais lenta e suave (alonga a duracao do balanco, que o jogo interpola por frame).
 * Nao muda o dano, o cooldown nem a velocidade de quebrar.
 */
public class SlowHitModule extends Module {
	private static SlowHitModule instance;
	private final IntSetting speed = add(new IntSetting("Swing speed", 20, 100, 50, "%"));

	public SlowHitModule() {
		super("Slow Hit Visual", Category.VISUALS);
		instance = this;
	}

	/** Duracao do balanco (ticks) para o seu jogador; outros jogadores ficam como estao. */
	public static int scale(LivingEntity e, int duration) {
		if (instance == null || !instance.isEnabled() || e != Minecraft.getInstance().player) return duration;
		return Math.max(duration, Math.round(duration * 100f / instance.speed.get()));
	}
}
