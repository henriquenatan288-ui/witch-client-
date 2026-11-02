package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Particulas brancas minusculas flutuando no ar em volta de voce (clima de cinza/poeira). So aparecem em blocos de ar. */
public class AmbientDustModule extends Module implements EspSource {
	private final IntSetting amount = add(new IntSetting("Amount", 10, 120, 50, ""));
	private final IntSetting range = add(new IntSetting("Range", 6, 24, 14, " blocks"));
	private final IntSetting size = add(new IntSetting("Size", 1, 6, 2, ""));
	private final IntSetting drift = add(new IntSetting("Drift", 1, 10, 3, ""));

	public AmbientDustModule() {
		super("Ambient Dust", Category.EFFECTS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	private static double hash(int i, int k) {
		long h = i * 73856093L ^ k * 19349663L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFFL) / 65535.0;
	}

	private static double wrap(double v, double r) {
		return v - Math.floor(v / r) * r;
	}

	@Override
	public void collect(Esp esp) {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		var level = mc.level;
		if (p == null || level == null) return;
		float pt = esp.partial();
		double px = Mth.lerp(pt, p.xOld, p.getX());
		double py = Mth.lerp(pt, p.yOld + p.getEyeHeight(), p.getY() + p.getEyeHeight());
		double pz = Mth.lerp(pt, p.zOld, p.getZ());

		double t = System.nanoTime() / 1_000_000_000.0;
		double r = range.get() * 2.0;
		double s = size.get() * 0.012;
		double d = drift.get() * 0.08;
		int n = amount.get();
		for (int i = 0; i < n; i++) {
			double vx = (hash(i, 4) - 0.5) * d;
			double vy = (hash(i, 5) - 0.3) * d * 0.6;
			double vz = (hash(i, 6) - 0.5) * d;
			double x = px + wrap(hash(i, 1) * r + t * vx - px, r) - r / 2;
			double y = py + wrap(hash(i, 2) * r * 0.6 + t * vy - py, r * 0.6) - r * 0.3;
			double z = pz + wrap(hash(i, 3) * r + t * vz - pz, r) - r / 2;
			if (!level.getBlockState(BlockPos.containing(x, y, z)).isAir()) continue;
			double dx = x - px, dy = y - py, dz = z - pz;
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			float edge = (float) Math.min(1.0, (r / 2 - dist) / 3.0);
			if (edge <= 0f) continue;
			float tw = 0.55f + 0.45f * (float) Math.sin(t * (1.0 + hash(i, 7) * 2.0) + i);
			esp.solid(x - s, y - s, z - s, x + s, y + s, z + s, 0xFFFFFFFF, 0.55f * edge * tw);
		}
	}
}
