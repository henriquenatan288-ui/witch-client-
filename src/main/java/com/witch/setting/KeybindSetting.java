package com.witch.setting;

import org.lwjgl.glfw.GLFW;

public final class KeybindSetting extends Setting {
	private int key = 0;
	public boolean listening = false;
	public boolean wasDown = false;

	public KeybindSetting() {
		super("Keybind");
	}

	public int key() {
		return this.key;
	}

	public void setKey(int k) {
		this.key = Math.max(0, k);
	}

	public String label() {
		if (this.listening) return "Press a key...";
		if (this.key == 0) return "None";
		switch (this.key) {
			case 32: return "Space";
			case 340: return "L-Shift";
			case 344: return "R-Shift";
			case 341: return "L-Ctrl";
			case 345: return "R-Ctrl";
			case 342: return "L-Alt";
			case 346: return "R-Alt";
			default: break;
		}
		String name = GLFW.glfwGetKeyName(this.key, 0);
		if (name != null) return name.toUpperCase();
		return "Key " + this.key;
	}
}
