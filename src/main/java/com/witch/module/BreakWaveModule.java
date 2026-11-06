package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/** Ao quebrar um bloco abaixo dos seus pes (por padrao), solta ondas finas que se expandem pelo chao. */
public class BreakWaveModule extends Module implements EspSource {
	private static final double DURATION = 1.3; // segundos
	private static final int MAX_WAVES = 3;

	private final IntSetting radius = add(new IntSetting("Radius", 2, 10, 4, " blocks"));
	private final IntSetting rings = add(new IntSetting("Rings", 1, 4, 2, ""));
	private final IntSetting downOnly = add(new IntSetting("Down only (0/1)", 0, 1, 1, ""));

	private final List<double[]> waves = new ArrayList<>(); // {x, y, z, inicioSeg}
	private BlockPos last = null;
	private int lastFeetY = 0;

	public BreakWaveModule() {
		super("Break Wave", Category.EFFECTS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	protected void onDisable() {
		waves.clear();
		last = null;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		var level = mc.level;
		if (p == null || level == null) return;

		if (last != null && level.getBlockState(last).isAir()) {
			boolean down = downOnly.get() == 0 || (last.getY() < lastFeetY
					&& Math.abs(last.getX() + 0.5 - p.getX()) < 1.6 && Math.abs(last.getZ() + 0.5 - p.getZ()) < 1.6);
			if (down) {
				waves.add(new double[]{last.getX() + 0.5, last.getY() + 1.02, last.getZ() + 0.5, now()});
				if (waves.size() > MAX_WAVES) waves.remove(0);
			}
			last = null;
		}
		if (mc.hitResult instanceof BlockHitResult bhr && mc.options.keyAttack.isDown()) {
			BlockPos pos = bhr.getBlockPos();
			if (!level.getBlockState(pos).isAir()) {
				last = pos.immutable();
				lastFeetY = p.blockPosition().getY();
			}
		}
	}

	private static final class RenderColor {
		static int mix(int a, int b, float t) {
			int r = Math.round(((a >> 16) & 0xFF) * (1f - t) + ((b >> 16) & 0xFF) * t);
			int g = Math.round(((a >> 8) & 0xFF) * (1f - t) + ((b >> 8) & 0xFF) * t);
			int bl = Math.round((a & 0xFF) * (1f - t) + (b & 0xFF) * t);
			return 0xFF000000 | (r << 16) | (g << 8) | bl;
		}
	}

	private static double now() {
		return System.nanoTime() / 1_000_000_000.0;
	}

	@Override
	public void collect(Esp esp) {
		double t = now();
		waves.removeIf(w -> t - w[3] > DURATION + 0.5);
		int nRings = rings.get();
		for (double[] w : waves) {
			double prog = (t - w[3]) / DURATION;
			for (int k = 0; k < nRings; k++) {
				double pr = prog - k * 0.18;
				if (pr <= 0 || pr >= 1) continue;
				double r = 0.3 + (radius.get() - 0.3) * (1.0 - Math.pow(1.0 - pr, 3.0));
				float fade = (float) Math.pow(1.0 - pr, 1.6) * (1f - 0.35f * k);
				float width = (float) (1.05 - 0.55 * pr);
				int col = RenderColor.mix(0xFFFFFFFF, 0xFF9FD8FF, (float) pr);
				ring(esp, w[0], w[1] + pr * 0.18, w[2], r, col, fade * 0.85f, width);
			}
		}
	}

	private static void ring(Esp esp, double cx, double cy, double cz, double r, int color, float alpha, float width) {
		int seg = (int) Math.max(20, Math.min(36, 16 + r * 6));
		double px = cx + r;
		double pz = cz;
		for (int i = 1; i <= seg; i++) {
			double a = i * (Math.PI * 2 / seg);
			double nx = cx + Math.cos(a) * r;
			double nz = cz + Math.sin(a) * r;
			esp.thinLine(px, cy, pz, nx, cy, nz, color, alpha, width);
			px = nx;
			pz = nz;
		}
	}
}
