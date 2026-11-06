package com.witch.module;

import com.witch.render.EspRenderer;
import com.witch.render.EspSource;

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
		MODULES.add(new SusSoloFinderModule());
		MODULES.add(new WitchThemeModule());
		MODULES.add(new SlowHitModule());
		MODULES.add(new NoGhostBlocksModule());
		MODULES.add(new BreakDelayModule());
		MODULES.add(new AncientFinderModule());
		MODULES.add(new TriggerBotModule());
		MODULES.add(new FullbrightModule());
		MODULES.add(new FreecamModule());
		MODULES.add(new FreeLookModule());
		MODULES.add(new BreakWaveModule());
		MODULES.add(new AntiVanishModule());
		MODULES.add(new ParticlesModule());
		MODULES.add(new FlyModule());
		MODULES.add(new SpeedModule());
		MODULES.add(new JesusModule());
		MODULES.add(new AutoEatModule());
		MODULES.add(new AutoToolModule());
		MODULES.add(new AmbientDustModule());
		MODULES.add(new ZoomModule());
		MODULES.add(new BoatFlyModule());
		MODULES.add(new GuiSettingsModule());
		MODULES.add(new NoHurtCamModule());
		MODULES.add(new AutoTotemModule());
		MODULES.add(new MoodFilterModule());
		MODULES.add(new HideNameModule());
		MODULES.add(new WaypointsModule());
		MODULES.add(new CoordsHudModule());
		MODULES.add(new ArmorHudModule());
		MODULES.add(new ChunkBordersModule());
		MODULES.add(new LightLevelModule());
		MODULES.add(new WeatherOffModule());
		MODULES.add(new AutoSprintModule());
		MODULES.add(new AutoReconnectModule());
		MODULES.add(new ChatTimestampsModule());
		MODULES.add(new NoFallModule());
		MODULES.add(new StepModule());
		MODULES.add(new SpiderModule());
		MODULES.add(new CrosshairModule());
		MODULES.add(new SkyModule());
		MODULES.add(new FogModule());
		MODULES.add(new GlowPlayersModule());
		MODULES.add(new JumpCirclesModule());
		MODULES.add(new CardParticlesModule());
		MODULES.add(new FilterModule());
		MODULES.add(new InfoModules.Fps());
		MODULES.add(new InfoModules.Ping());
		MODULES.add(new InfoModules.Cps());
		MODULES.add(new InfoModules.Clock());
		MODULES.add(new InfoModules.Speed());
		MODULES.add(new InfoModules.Direction());
		MODULES.add(new InfoModules.Day());
		MODULES.add(new InfoModules.Memory());
		MODULES.add(new InfoModules.Potions());
		MODULES.add(new InfoModules.NetherCoords());
		MODULES.add(new InfoModules.ChunkCoords());
		MODULES.add(new InfoModules.PlayerCount());
		MODULES.add(new InfoModules.HeldItem());
		MODULES.add(new InfoModules.ServerIp());
		MODULES.add(new InfoModules.SprintStatus());
		MODULES.add(new InfoModules.Health());
		MODULES.add(new InfoModules.Keystrokes());
		MODULES.add(new InfoModules.ComboCounter());
		MODULES.add(new InfoModules.ReachDisplay());
		MODULES.add(new InfoModules.TargetHud());
		MODULES.add(new InfoModules.HitMarker());
		MODULES.add(new InfoModules.LowHealthAlert());
		MODULES.add(new InfoModules.NearbyPlayers());
		MODULES.add(new InfoModules.MobCounter());
		MODULES.add(new InfoModules.WeatherHud());
		MODULES.add(new InfoModules.InventoryCounter());
		MODULES.add(new InfoModules.ArmorWarning());
		MODULES.add(new InfoModules.ItemCount());
		MODULES.add(new InfoModules.AutoWalk());
		MODULES.add(new InfoModules.ToggleSneak());

		MODULES.removeIf(m -> !com.witch.Edition.allows(m.name));

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

	/** Liga/desliga modulos pela tecla configurada (so fora de telas). */
	public static void pollKeys(net.minecraft.client.Minecraft mc) {
		var window = mc.getWindow();
		boolean free = mc.gui.screen() == null;
		for (Module m : MODULES) {
			var kb = m.keybind;
			if (kb.key() == 0) {
				kb.wasDown = false;
				continue;
			}
			boolean down = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, kb.key());
			if (down && !kb.wasDown && free) {
				m.toggle();
				com.witch.config.Config.save();
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
