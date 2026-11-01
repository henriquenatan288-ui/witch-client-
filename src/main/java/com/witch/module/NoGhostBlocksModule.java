package com.witch.module;

public class NoGhostBlocksModule extends Module {
	public static NoGhostBlocksModule INSTANCE;

	public NoGhostBlocksModule() {
		super("No Ghost Blocks", Category.UTILITY);
		INSTANCE = this;
	}

	public static boolean active() {
		return INSTANCE != null && INSTANCE.isEnabled();
	}
}
