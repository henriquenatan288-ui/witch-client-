package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import com.witch.setting.OreSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Tracers estilo Meteor, so para os alvos do Player ESP, Farm ESP e Storage ESP (precisam estar ligados): linha do centro da tela ate alvos, com cor por distancia, fade, alvo (cabeca/corpo/pes) e stem. */
public class TracersModule extends Module implements EspSource {
	private final IntSetting range = add(new IntSetting("Range", 16, 256, 128, ""));
	private final IntSetting target = add(new IntSetting("Target 1=head 2=body 3=feet", 1, 3, 2, ""));
	private final IntSetting fade = add(new IntSetting("Distance fade", 0, 100, 40, "%"));
	private final OreSetting distColor = add(new OreSetting("Color by distance", false, 2));
	private final OreSetting stem = add(new OreSetting("Stem", true, 7));
	private final OreSetting players = add(new OreSetting("Player ESP", true, 5));
	private final OreSetting farm = add(new OreSetting("Farm ESP", true, 2));
	private final OreSetting storage = add(new OreSetting("Storage ESP", true, 3));

	public TracersModule() {
		super("Tracers", Category.VISUALS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	/** Verde (perto) -> amarelo -> vermelho (longe). */
	private static int gradient(float t) {
		t = Mth.clamp(t, 0f, 1f);
		int r, g;
		if (t < 0.5f) {
			r = (int) (255 * (t * 2f));
			g = 230;
		} else {
			r = 255;
			g = (int) (230 * (1f - (t - 0.5f) * 2f));
		}
		return 0xFF000000 | (r << 16) | (g << 8) | 0x40;
	}

	private void draw(Esp esp, double x, double y, double z, double h, double dist, int base) {
		float t = (float) (dist / range.get());
		int argb = distColor.enabled() ? gradient(t) : base;
		float alpha = 1f - (fade.get() / 100f) * 0.75f * Mth.clamp(t, 0f, 1f);
		double ty = switch (target.get()) {
			case 1 -> y + h * 0.9;
			case 3 -> y;
			default -> y + h * 0.5;
		};
		esp.tracer(x, ty, z, argb, alpha);
		if (stem.enabled()) {
			esp.line(x, y, z, x, y + h, z, argb, alpha * 0.8f);
		}
	}

	@Override
	public void collect(Esp esp) {
		Minecraft mc = Minecraft.getInstance();
		var level = mc.level;
		var me = mc.player;
		if (level == null || me == null) return;

		float pt = esp.partial();
		double mx = Mth.lerp(pt, me.xOld, me.getX());
		double my = Mth.lerp(pt, me.yOld, me.getY());
		double mz = Mth.lerp(pt, me.zOld, me.getZ());
		double r2 = (double) range.get() * range.get();

		if (players.enabled() && isOn(PlayerEspModule.class)) {
			int base = players.color();
			for (Player e : level.players()) {
				if (e == me || e.isSpectator()) continue;
				double x = Mth.lerp(pt, e.xOld, e.getX());
				double y = Mth.lerp(pt, e.yOld, e.getY());
				double z = Mth.lerp(pt, e.zOld, e.getZ());
				double dx = x - mx, dy = y - my, dz = z - mz;
				double d2 = dx * dx + dy * dy + dz * dz;
				if (d2 > r2) continue;
				draw(esp, x, y, z, e.getBbHeight(), Math.sqrt(d2), base);
			}
		}

		if (farm.enabled() && isOn(FarmEspModule.class)) {
			blocks(esp, FarmEspModule.blocks(), farm.color(), mx, my, mz);
		}

		if (storage.enabled() && isOn(StorageEspModule.class)) {
			blocks(esp, StorageEspModule.blocks(), storage.color(), mx, my, mz);
		}
	}

	private static boolean isOn(Class<? extends Module> c) {
		for (Module m : ModuleManager.all()) {
			if (c.isInstance(m)) return m.isEnabled();
		}
		return false;
	}

	private void blocks(Esp esp, java.util.List<int[]> list, int argb, double mx, double my, double mz) {
		for (int[] b : list) {
			double dx = b[0] + 0.5 - mx, dy = b[1] + 0.5 - my, dz = b[2] + 0.5 - mz;
			double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (d > range.get()) continue;
			float t = (float) (d / range.get());
			float alpha = 1f - (fade.get() / 100f) * 0.75f * Mth.clamp(t, 0f, 1f);
			esp.tracer(b[0] + 0.5, b[1] + 0.5, b[2] + 0.5, distColor.enabled() ? gradient(t) : argb, alpha);
		}
	}
}
