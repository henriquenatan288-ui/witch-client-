package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.ColorSetting;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;

/** Bordas das chunks: colunas finas nos cantos e retangulos da chunk atual a cada 16 blocos de altura. */
public class ChunkBordersModule extends Module implements EspSource {
	private final IntSetting radius = add(new IntSetting("Chunks", 1, 4, 2, ""));
	private final ColorSetting color = add(new ColorSetting("Color", 3));

	public ChunkBordersModule() {
		super("Chunk Borders", Category.VISUALS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		var p = Minecraft.getInstance().player;
		if (p == null) return;
		int pcx = p.getBlockX() >> 4;
		int pcz = p.getBlockZ() >> 4;
		int r = radius.get();
		int c = color.get();
		double y0 = p.getY() - 24;
		double y1 = p.getY() + 24;
		for (int cx = pcx - r; cx <= pcx + r + 1; cx++) {
			for (int cz = pcz - r; cz <= pcz + r + 1; cz++) {
				double x = cx * 16.0;
				double z = cz * 16.0;
				esp.box(x - 0.015, y0, z - 0.015, x + 0.015, y1, z + 0.015, c, 0f);
			}
		}
		double x0 = pcx * 16.0;
		double z0 = pcz * 16.0;
		int base = Math.floorDiv(p.getBlockY(), 16) * 16;
		for (int k = -1; k <= 2; k++) {
			double y = base + k * 16.0;
			esp.box(x0, y, z0, x0 + 16, y + 0.015, z0 + 16, c, 0f);
		}
	}
}
