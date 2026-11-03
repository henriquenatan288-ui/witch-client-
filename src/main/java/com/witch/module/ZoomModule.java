package com.witch.module;

import com.witch.setting.IntSetting;

/** Zoom estilo Meteor: liga pela tecla do modulo e aproxima o mundo com transicao suave. */
public class ZoomModule extends Module {
	private static ZoomModule instance;
	private static float cur = 1f;
	private static long last = 0L;

	private final IntSetting zoom = add(new IntSetting("Zoom", 2, 30, 6, "x"));
	private final IntSetting smooth = add(new IntSetting("Smoothness", 1, 20, 10, ""));

	public ZoomModule() {
		super("Zoom", Category.VISUALS);
		instance = this;
	}

	/** Chamado todo frame com o FOV calculado; devolve o FOV ja com zoom. */
	public static float apply(float fov) {
		long now = System.nanoTime();
		double dt = last == 0 ? 0.016 : Math.min(0.1, (now - last) / 1e9);
		last = now;
		if (instance == null) return fov;
		float target = instance.isEnabled() ? instance.zoom.get() : 1f;
		cur += (target - cur) * (float) (1.0 - Math.exp(-dt * instance.smooth.get()));
		if (Math.abs(cur - 1f) < 0.005f && target == 1f) {
			cur = 1f;
			return fov;
		}
		return fov / cur;
	}
}
