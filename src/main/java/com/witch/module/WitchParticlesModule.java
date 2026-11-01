package com.witch.module;

import com.witch.render.EspRenderer;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public class WitchParticlesModule extends Module implements EspSource {
	private final IntSetting amount = this.add(new IntSetting("Amount", 1, 24, 10, ""));
	private final IntSetting size = this.add(new IntSetting("Size", 1, 6, 3, ""));
	private final IntSetting speed = this.add(new IntSetting("Speed", 1, 10, 4, ""));
	private final IntSetting distance = this.add(new IntSetting("Distance", 1, 6, 2, " blocks"));

	public WitchParticlesModule() {
		super("Witch Particles", Category.EFFECTS);
	}

	@Override
	public boolean active() {
		return this.isEnabled();
	}

	@Override
	public void collect(EspRenderer.Esp esp) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null) return;
		float pt = esp.partial();
		double px = Mth.lerp((double) pt, p.xOld, p.getX());
		double py = Mth.lerp((double) pt, p.yOld, p.getY());
		double pz = Mth.lerp((double) pt, p.zOld, p.getZ());
		double t = (double) System.nanoTime() / 1.0E9;
		int n = this.amount.get();
		double sp = (double) this.speed.get() * 0.15;
		double u = (double) this.size.get() * 0.05;
		for (int i = 0; i < n; ++i) {
			double phase = (double) i / (double) n;
			double life = (t * 0.12 * (0.6 + (double) this.speed.get() * 0.12) + phase) % 1.0;
			double ang = t * sp * (0.7 + 0.3 * (double) (i * 7 % 5) / 4.0) + (double) i * 2.399963;
			double rad = (double) this.distance.get() * (0.55 + 0.45 * (double) (i * 37 % 10) / 9.0);
			double y = py + 0.2 + life * 2.4;
			double x = px + Math.cos(ang) * rad;
			double z = pz + Math.sin(ang) * rad;
			float fade = (float) Math.min(1.0, Math.min(life * 6.0, (1.0 - life) * 6.0));
			hat(esp, x, y, z, u, fade * 0.9f);
		}
	}

	private static void hat(EspRenderer.Esp esp, double x, double y, double z, double u, float a) {
		esp.solid(x - 3.0 * u, y, z - 3.0 * u, x + 3.0 * u, y + 0.4 * u, z + 3.0 * u, 0xFF3A2A3A, a);
		esp.solid(x - 1.8 * u, y + 0.4 * u, z - 1.8 * u, x + 1.8 * u, y + 1.3 * u, z + 1.8 * u, 0xFFC94F95, a);
		esp.solid(x - 1.6 * u, y + 1.3 * u, z - 1.6 * u, x + 1.6 * u, y + 2.6 * u, z + 1.6 * u, 0xFF6A2B95, a);
		esp.solid(x - 1.1 * u, y + 2.6 * u, z - 1.1 * u, x + 1.1 * u, y + 3.8 * u, z + 1.1 * u, 0xFF6A2B95, a);
		esp.solid(x - 0.5 * u + 0.5 * u, y + 3.8 * u, z - 0.5 * u, x + 0.5 * u + 0.5 * u, y + 5.0 * u, z + 0.5 * u, 0xFF6A2B95, a);
	}
}
