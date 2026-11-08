package com.witch.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Acrescenta vida (+ absorcao) e distancia ao nome dos jogadores. */
public class NametagsModule extends Module {
	private static NametagsModule instance;

	public NametagsModule() {
		super("Nametags", Category.VISUALS);
		instance = this;
	}

	public static Component decorate(net.minecraft.world.entity.Entity e, Component base) {
		if (instance == null || !instance.isEnabled() || !(e instanceof Player p)) return null;
		var me = Minecraft.getInstance().player;
		if (me == null || p == me) return null;
		float hp = p.getHealth() + p.getAbsorptionAmount();
		int color = hp > 14 ? 0x55FF55 : hp > 7 ? 0xFFFF55 : 0xFF5555;
		return Component.empty().append(base)
				.append(Component.literal(" " + Math.round(hp) + "hp").withColor(color))
				.append(Component.literal(" " + Math.round(p.distanceTo(me)) + "m").withColor(0xAAAAAA));
	}
}
