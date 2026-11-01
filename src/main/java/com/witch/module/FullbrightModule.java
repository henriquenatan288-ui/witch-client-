package com.witch.module;

import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class FullbrightModule extends Module {
	private final IntSetting fade = this.add(new IntSetting("Fade time", 1, 5, 2, " s"));
	private final IntSetting nightVision = this.add(new IntSetting("Night vision (0/1)", 0, 1, 1, ""));
	private double original = 0.5;
	private int ticks = 0;

	public FullbrightModule() {
		super("Fullbright", Category.VISUALS);
	}

	@Override
	protected void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		this.original = (Double) mc.options.gamma().get();
		this.ticks = 0;
	}

	@Override
	protected void onDisable() {
		Minecraft mc = Minecraft.getInstance();
		mc.options.gamma().set(this.original);
		if (mc.player != null) {
			mc.player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder((MobEffect) MobEffects.NIGHT_VISION.value()));
		}
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		++this.ticks;
		float t = Math.min(1.0f, (float) this.ticks / ((float) this.fade.get() * 20.0f));
		float eased = t * t * (3.0f - 2.0f * t);
		mc.options.gamma().set(this.original + (1.0 - this.original) * (double) eased);
		if (this.nightVision.get() == 1 && t >= 1.0f && mc.player != null) {
			Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder((MobEffect) MobEffects.NIGHT_VISION.value());
			MobEffectInstance cur = mc.player.getEffect(holder);
			if (cur == null || cur.getDuration() < 300) {
				mc.player.addEffect(new MobEffectInstance(holder, 420, 0));
			}
		}
	}
}
