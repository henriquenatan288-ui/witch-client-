package com.witch.module;

import com.witch.setting.KeybindSetting;
import com.witch.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
	public final String name;
	public final Category category;
	private boolean enabled;
	private final List<Setting> settings = new ArrayList<>();
	public final KeybindSetting keybind = new KeybindSetting();
	public float hoverAnim = 0.0f;
	public float toggleAnim = 0.0f;

	protected Module(String name, Category category) {
		this.name = name;
		this.category = category;
		this.settings.add(this.keybind);
	}

	protected <T extends Setting> T add(T setting) {
		this.settings.add(setting);
		return setting;
	}

	public List<Setting> settings() {
		return this.settings;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean value) {
		if (this.enabled == value) return;
		this.enabled = value;
		if (value) this.onEnable();
		else this.onDisable();
	}

	public void toggle() {
		this.setEnabled(!this.enabled);
	}

	public void onTick() {}

	protected void onEnable() {}

	protected void onDisable() {}
}
