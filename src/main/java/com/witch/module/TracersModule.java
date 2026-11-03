package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import com.witch.setting.OreSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Tracers baseados no Tracers.java do Meteor (linhas do centro da tela, alvo cabeca/corpo/pes, stem,
 * cor por distancia, ignore self/invisiveis, max distance). Aqui so desenham para os alvos do
 * Player ESP, Farm ESP e Storage ESP, que precisam estar ligados.
 */
public class TracersModule extends Module implements EspSource {
	/** Alpha padrao das cores do Meteor (127/255). */
	private static final float ALPHA = 127f / 255f;

	private final IntSetting range = add(new IntSetting("Max distance", 16, 256, 256, ""));
	private final IntSetting target = add(new IntSetting("Target 1=head 2=body 3=feet", 1, 3, 2, ""));
	private final IntSetting ignoreSelf = add(new IntSetting("Ignore self (0/1)", 0, 1, 0, ""));
	private final IntSetting showInvis = add(new IntSetting("Show invisible (0/1)", 0, 1, 1, ""));
	private final OreSetting distColor = add(new OreSetting("Distance colors", false, 2));
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

	/** Igual ao EntityUtils.getColorFromDistance do Meteor: perto = vermelho, meio = amarelo, longe (60+) = verde. */
	private static int distanceColor(double dist) {
		double percent = dist / 60.0;
		if (percent < 0 || percent > 1) return 0xFF00FF00;
		int r, g;
		if (percent < 0.5) {
			r = 255;
			g = (int) (255 * percent / 0.5);
		} else {
			g = 255;
			r = 255 - (int) (255 * (percent - 0.5) / 0.5);
		}
		return 0xFF000000 | (r << 16) | (g << 8);
	}

	private void entity(Esp esp, double x, double feetY, double z, double height, double dist, int base) {
		boolean dc = distColor.enabled();
		int argb = dc ? distanceColor(dist) : base;
		float alpha = dc ? 1f : ALPHA;
		double ty = switch (target.get()) {
			case 1 -> feetY + height;
			case 3 -> feetY;
			default -> feetY + height / 2.0;
		};
		esp.tracer(x, ty, z, argb, alpha);
		if (stem.enabled()) {
			esp.line(x, feetY, z, x, feetY + height, z, argb, alpha);
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
		double max = range.get();

		if (players.enabled() && isOn(PlayerEspModule.class)) {
			int base = players.color();
			boolean freecam = FreecamModule.active();
			for (Player e : level.players()) {
				if (e.isSpectator()) continue;
				if (e == me && (!freecam || ignoreSelf.get() == 1)) continue;
				if (showInvis.get() == 0 && e.isInvisible()) continue;
				double x = Mth.lerp(pt, e.xOld, e.getX());
				double y = Mth.lerp(pt, e.yOld, e.getY());
				double z = Mth.lerp(pt, e.zOld, e.getZ());
				double dx = x - mx, dy = y - my, dz = z - mz;
				double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
				if (d > max) continue;
				entity(esp, x, y, z, e.getBbHeight(), d, base);
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

	private void blocks(Esp esp, java.util.List<int[]> list, int base, double mx, double my, double mz) {
		boolean dc = distColor.enabled();
		for (int[] b : list) {
			double dx = b[0] + 0.5 - mx, dy = b[1] + 0.5 - my, dz = b[2] + 0.5 - mz;
			double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (d > range.get()) continue;
			esp.tracer(b[0] + 0.5, b[1] + 0.5, b[2] + 0.5, dc ? distanceColor(d) : base, dc ? 1f : ALPHA);
		}
	}
}
