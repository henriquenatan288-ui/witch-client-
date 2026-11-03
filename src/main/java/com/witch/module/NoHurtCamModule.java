package com.witch.module;

/** Remove o tranco/inclinacao da camera ao tomar dano (o efeito vermelho continua). */
public class NoHurtCamModule extends Module {
	private static NoHurtCamModule instance;

	public NoHurtCamModule() {
		super("No Hurt Cam", Category.VISUALS);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
