package com.witch.module;

import com.witch.render.EspRenderer.Esp;
import com.witch.render.EspSource;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;

import java.util.ArrayList;
import java.util.List;

/** Marca em vermelho o chao com luz de bloco 0 (onde mobs podem nascer a noite ou em caverna). */
public class LightLevelModule extends Module implements EspSource {
	private static final int RED = 0xFFEF4444;
	private final IntSetting range = add(new IntSetting("Range", 4, 16, 8, ""));

	private volatile List<int[]> spots = List.of();
	private int cooldown = 0;

	public LightLevelModule() {
		super("Light Level", Category.VISUALS);
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public void collect(Esp esp) {
		for (int[] s : spots) {
			esp.box(s[0] + 0.1, s[1] + 0.02, s[2] + 0.1, s[0] + 0.9, s[1] + 0.05, s[2] + 0.9, RED, 0.3f);
		}
	}

	@Override
	protected void onDisable() {
		spots = List.of();
	}

	@Override
	public void onTick() {
		if (--cooldown > 0) return;
		cooldown = 8;
		Minecraft mc = Minecraft.getInstance();
		var level = mc.level;
		var p = mc.player;
		if (level == null || p == null) {
			spots = List.of();
			return;
		}
		int r = range.get();
		BlockPos pp = p.blockPosition();
		List<int[]> out = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
		for (int dx = -r; dx <= r && out.size() < 700; dx++) {
			for (int dz = -r; dz <= r && out.size() < 700; dz++) {
				for (int dy = -3; dy <= 3; dy++) {
					pos.set(pp.getX() + dx, pp.getY() + dy, pp.getZ() + dz);
					below.set(pos.getX(), pos.getY() - 1, pos.getZ());
					if (!level.getBlockState(pos).isAir()) continue;
					if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
					if (level.getBrightness(LightLayer.BLOCK, pos) == 0) {
						out.add(new int[]{pos.getX(), pos.getY(), pos.getZ()});
					}
				}
			}
		}
		spots = out;
	}
}
