package com.witch.module;

import com.witch.config.Config;
import com.witch.render.EspRenderer;
import com.witch.render.EspSource;
import com.witch.setting.KeybindSetting;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();

	private ModuleManager() {}

	public static void init() {
		MODULES.clear();
		EspRenderer.clearSources();

		MODULES.add(new XRayModule());
		MODULES.add(new PlayerEspModule());
		MODULES.add(new StorageEspModule());
		MODULES.add(new FarmEspModule());
		MODULES.add(new TracersModule());
		MODULES.add(new SusHoleFinderModule());
		MODULES.add(new SusChunkFinderModule());
		MODULES.add(new SlowHitModule());
		MODULES.add(new NoGhostBlocksModule());
		MODULES.add(new BreakDelayModule());
		MODULES.add(new AncientFinderModule());
		MODULES.add(new TriggerBotModule());
		MODULES.add(new FullbrightModule());
		MODULES.add(new FreecamModule());
		MODULES.add(new WitchParticlesModule());
		MODULES.add(new BreakWaveModule());
		MODULES.add(new AntiVanishModule());
		MODULES.add(new ParticlesModule());
		MODULES.add(new FlyModule());
		MODULES.add(new SpeedModule());
		MODULES.add(new JesusModule());
		MODULES.add(new AutoEatModule());
		MODULES.add(new AutoToolModule());

		for (Module m : MODULES) {
			if (m instanceof EspSource s) {
				EspRenderer.register(s);
			}
		}
		EspRenderer.init();
	}

	public static List<Module> all() {
		return MODULES;
	}

	public static List<Module> byCategory(Category category) {
		List<Module> out = new ArrayList<>();
		for (Module m : MODULES) {
			if (m.category == category) out.add(m);
		}
		return out;
	}

	public static Module byName(String name) {
		for (Module m : MODULES) {
			if (m.name.equals(name)) return m;
		}
		return null;
	}

	public static void pollKeys(Minecraft mc) {
		Window window = mc.getWindow();
		boolean free = mc.gui.screen() == null;
		for (Module m : MODULES) {
			KeybindSetting kb = m.keybind;
			if (kb.key() == 0) {
				kb.wasDown = false;
				continue;
			}
			boolean down = InputConstants.isKeyDown(window, kb.key());
			if (down && !kb.wasDown && free) {
				m.toggle();
				Config.save();
			}
			kb.wasDown = down;
		}
	}

	public static void tick() {
		for (Module m : MODULES) {
			if (m.isEnabled()) m.onTick();
		}
	}
}
