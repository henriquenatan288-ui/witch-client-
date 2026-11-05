package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Um anel de luz que se abre no chao quando voce pula. */
public class JumpCirclesModule extends Module implements EspSource {
	private static final long LIFE_MS = 1100L;
	private final ColorSetting color = add(new ColorSetting("Color", 0));
	private final IntSetting size = add(new IntSetting("Size", 1, 6, 3, ""));

	private final List<double[]> rings = new ArrayList<>();
	private boolean wasGround = true;

	public JumpCirclesModule() {
		super("Jump Circles", Category.EFFECTS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	protected void onDisable() {
		rings.clear();
	}

	@Override
	public void onTick() {
		var p = Minecraft.getInstance().player;
		if (p == null) return;
		boolean ground = p.onGround();
		if (wasGround && !ground && p.getDeltaMovement().y > 0.2 && !p.isInWater() && !p.getAbilities().flying) {
			rings.add(new double[] {p.getX(), p.getY(), p.getZ(), System.currentTimeMillis()});
			if (rings.size() > 12) rings.remove(0);
		}
		wasGround = ground;
	}

	@Override
	public void collect(Esp esp) {
		long now = System.currentTimeMillis();
		rings.removeIf(r -> now - (long) r[3] > LIFE_MS);
		int c = color.get();
		for (double[] r : rings) {
			double k = (now - (long) r[3]) / (double) LIFE_MS;
			float a = (float) Math.pow(1.0 - k, 1.5);
			double rad = 0.4 + k * size.get() * 1.6;
			circle(esp, r[0], r[1] + 0.05, r[2], rad, c, a, 2.0f);
			circle(esp, r[0], r[1] + 0.05, r[2], rad * 0.6, c, a * 0.5f, 1.5f);
		}
	}

	private static void circle(Esp esp, double cx, double y, double cz, double rad, int c, float a, float width) {
		int n = 28;
		for (int i = 0; i < n; i++) {
			double a0 = i * Math.PI * 2 / n;
			double a1 = (i + 1) * Math.PI * 2 / n;
			esp.thinLine(cx + Math.cos(a0) * rad, y, cz + Math.sin(a0) * rad,
					cx + Math.cos(a1) * rad, y, cz + Math.sin(a1) * rad, c, a, width);
		}
	}
}
