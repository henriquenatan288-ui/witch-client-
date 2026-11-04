package com.witch.module;

/** Preset: liga o filtro de humor e a poeira ambiente de uma vez (e desliga ao desativar). */
public class WitchThemeModule extends Module {
	private static final String[] PARTS = {"Mood Filter", "Ambient Dust"};

	public WitchThemeModule() {
		super("Witch Theme", Category.VISUALS);
	}

	private static void set(boolean on) {
		for (Module m : ModuleManager.all()) {
			for (String n : PARTS) {
				if (m.name.equals(n)) m.setEnabled(on);
			}
		}
	}

	@Override
	protected void onEnable() {
		set(true);
	}

	@Override
	protected void onDisable() {
		set(false);
	}
}
