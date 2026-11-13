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
		MODULES.add(new AutoCrystalModule());
		MODULES.add(new AutoAnchorModule());
		MODULES.add(new AutoMaceModule());
		MODULES.add(new SurroundModule());
		MODULES.add(new MoveModules.HighJump());
		MODULES.add(new MoveModules.LongJump());
		MODULES.add(new MoveModules.Bhop());
		MODULES.add(new MoveModules.Glide());
		MODULES.add(new MoveModules.AirJump());
		MODULES.add(new MoveModules.Hover());
		MODULES.add(new MoveModules.OmniSprint());
		MODULES.add(new MoveModules.FastFall());
		MODULES.add(new MoveModules.FastLadder());
		MODULES.add(new MoveModules.Dolphin());
		MODULES.add(new MoveModules.Parkour());
		MODULES.add(new MoveModules.SafeWalk());
		MODULES.add(new MoveModules.ElytraBoost());
		MODULES.add(new MoveModules.ElytraControl());
		MODULES.add(new MoveModules.Jetpack());
		MODULES.add(new MoveModules.Bounce());
		MODULES.add(new MoveModules.AutoSneak());
		MODULES.add(new MoveModules.AirSpeed());
		MODULES.add(new MoveModules.Slide());
		MODULES.add(new MoveModules.WallJump());
		MODULES.add(new MoveModules.Sink());
		MODULES.add(new MoveModules.AntiVoid());
		MODULES.add(new MoveModules.Dash());
		MODULES.add(new MoveModules.Launch());
		MODULES.add(new MoveModules.SneakSpeed());
		MODULES.add(new MoveModules.Levitate());
		MODULES.add(new MoveModules.AirBrake());
		MODULES.add(new MoveModules.LowGravity());
		MODULES.add(new MoveModules.Strafe());
		MODULES.add(new MoveModules.Pogo());
		MODULES.add(new CombatExtras.KillAura());
		MODULES.add(new CombatExtras.AimAssist());
		MODULES.add(new CombatExtras.AutoWeapon());
		MODULES.add(new VelocityModule());
		MODULES.add(new NametagsModule());
		MODULES.add(new AutoArmorModule());
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
		MODULES.add(new GuiAnimationModule());
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
		MODULES.add(new KillEffectModule());
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
		MODULES.add(new MiscModules.AutoClicker());
		MODULES.add(new MiscModules.AutoBlock());
		MODULES.add(new MiscModules.AutoLog());
		MODULES.add(new MiscModules.AutoGap());
		MODULES.add(new MiscModules.WTap());
		MODULES.add(new MiscModules.CooldownGuard());
		MODULES.add(new MiscModules.TargetStrafe());
		MODULES.add(new MiscModules.AutoJump());
		MODULES.add(new MiscModules.IceSpeed());
		MODULES.add(new MiscModules.WaterSpeed());
		MODULES.add(new MiscModules.EdgeJump());
		MODULES.add(new MiscModules.LavaEscape());
		MODULES.add(new MiscModules.SneakSpam());
		MODULES.add(new MiscModules.AntiAfk());
		MODULES.add(new MiscModules.AutoRespawn());
		MODULES.add(new MiscModules.AutoReplenish());
		MODULES.add(new MiscModules.AutoMine());
		MODULES.add(new MiscModules.AutoUse());
		MODULES.add(new MiscModules.AutoTorch());
		MODULES.add(new MiscModules.YawLock());
		MODULES.add(new MiscModules.PitchLock());
		MODULES.add(new MiscModules.HotbarLock());
		MODULES.add(new MiscModules.HotbarCycle());
		MODULES.add(new MiscModules.ItemDropper());
		MODULES.add(new MiscModules.Spinner());
		MODULES.add(new MiscModules.FovChanger());
		MODULES.add(new MiscModules.RenderDistance());
		MODULES.add(new MiscModules.NoBob());
		MODULES.add(new MiscModules.EntityDistance());
		MODULES.add(new MiscModules.MouseSens());

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
			if (m.isEnabled()) guarded(m, m::onTick);
		}
	}

	/** Um modulo com erro nao derruba o jogo: ele e desligado, o erro vai pro log e o resto segue. */
	public static void guarded(Module m, Runnable r) {
		try {
			r.run();
		} catch (Throwable t) {
			System.err.println("[Witch] modulo '" + m.name + "' falhou e foi desligado: " + t);
			t.printStackTrace();
			try {
				m.setEnabled(false);
			} catch (Throwable ignored) {
				// o proprio onDisable falhou; o modulo fica marcado como desligado mesmo assim
			}
		}
	}
}
