package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Som e efeito quando voce mata um jogador ou mob (detecta o alvo do seu ultimo golpe morrendo). */
public class KillEffectModule extends Module {
	private final IntSetting players = add(new IntSetting("Players (0/1)", 0, 1, 1, ""));
	private final IntSetting mobs = add(new IntSetting("Mobs (0/1)", 0, 1, 1, ""));
	private final IntSetting sound = add(new IntSetting("Sound (0/1)", 0, 1, 1, ""));
	private final IntSetting soundType = add(new IntSetting("Sound type", 1, 3, 1, ""));
	private final IntSetting volume = add(new IntSetting("Volume", 1, 100, 70, "%"));
	private final IntSetting effect = add(new IntSetting("Effect", 0, 4, 1, ""));

	private LivingEntity watching = null;

	public KillEffectModule() {
		super("Kill Effect", Category.EFFECTS);
		InfoModules.hook();
	}

	private SoundEvent snd() {
		return switch (soundType.get()) {
			case 2 -> SoundEvents.PLAYER_LEVELUP;
			case 3 -> SoundEvents.ARROW_HIT_PLAYER;
			default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
		};
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		LivingEntity t = InfoModules.lastTarget;
		if (t != null) watching = t;
		if (watching == null) return;
		if (System.currentTimeMillis() - InfoModules.lastHit > 4000) {
			watching = null;
			InfoModules.lastTarget = null;
			return;
		}
		if (watching.isDeadOrDying() || watching.isRemoved()) {
			LivingEntity v = watching;
			watching = null;
			InfoModules.lastTarget = null;
			boolean isPlayer = v instanceof Player;
			if ((isPlayer && players.get() == 0) || (!isPlayer && mobs.get() == 0)) return;
			if (sound.get() == 1) mc.getSoundManager().play(SimpleSoundInstance.forUI(snd(), 1.0f + (soundType.get() == 1 ? 0.2f : 0f), volume.get() / 100f));
			double x = v.getX(), y = v.getY() + v.getBbHeight() * 0.5, z = v.getZ();
			switch (effect.get()) {
				case 1 -> CardParticlesModule.burst(x, y, z, 12);
				case 2 -> { for (int i = 0; i < 18; i++) mc.level.addParticle(ParticleTypes.END_ROD, x, y, z, (Math.random() - 0.5) * 0.3, Math.random() * 0.3, (Math.random() - 0.5) * 0.3); }
				case 3 -> { for (int i = 0; i < 8; i++) mc.level.addParticle(ParticleTypes.HEART, x + (Math.random() - 0.5), y + Math.random(), z + (Math.random() - 0.5), 0, 0.05, 0); }
				case 4 -> { for (int i = 0; i < 24; i++) mc.level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, (Math.random() - 0.5) * 0.5, Math.random() * 0.5, (Math.random() - 0.5) * 0.5); }
				default -> { }
			}
		}
	}
}
