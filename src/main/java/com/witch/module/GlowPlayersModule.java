package com.witch.module;

import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Contorno brilhante nos outros jogadores (o mesmo efeito do brilho do jogo), na cor escolhida. */
public class GlowPlayersModule extends Module {
	private static GlowPlayersModule instance;

	private final ColorSetting color = add(new ColorSetting("Color", 0));
	private final IntSetting range = add(new IntSetting("Range", 8, 128, 64, ""));

	public GlowPlayersModule() {
		super("Glow Players", Category.VISUALS);
		instance = this;
	}

	public static boolean wants(Entity e) {
		if (instance == null || !instance.isEnabled() || !(e instanceof Player p)) return false;
		var me = Minecraft.getInstance().player;
		if (me == null || p == me || p.isSpectator()) return false;
		return p.distanceToSqr(me) <= (double) instance.range.get() * instance.range.get();
	}

	public static int rgb() {
		return instance == null ? 0xFFFFFF : instance.color.get() & 0xFFFFFF;
	}
}
