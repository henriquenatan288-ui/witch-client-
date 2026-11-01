package com.witch.gui;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Carrega a skin do jogador logado: comeca com a skin padrao e troca pela skin real quando ela chega. */
public final class TitleSkin {
	private TitleSkin() {}

	private static volatile Identifier texture;
	private static volatile boolean slim;
	private static boolean started = false;

	public static Identifier texture() {
		ensure();
		return texture;
	}

	public static boolean slim() {
		return slim;
	}

	private static void apply(PlayerSkin sk) {
		texture = sk.body().texturePath();
		slim = sk.model() == PlayerModelType.SLIM;
	}

	private static void ensure() {
		if (started) return;
		started = true;
		Minecraft mc = Minecraft.getInstance();
		UUID id = mc.getUser().getProfileId();
		apply(DefaultPlayerSkin.get(id));
		try {
			CompletableFuture
					.supplyAsync(() -> mc.services().sessionService().fetchProfile(id, true), Util.ioPool())
					.thenCompose(res -> {
						GameProfile p = res != null ? res.profile() : null;
						if (p == null) return CompletableFuture.completedFuture(java.util.Optional.<PlayerSkin>empty());
						return mc.getSkinManager().get(p);
					})
					.thenAccept(opt -> opt.ifPresent(TitleSkin::apply));
		} catch (Exception ignored) {
			// fica com a skin padrao
		}
	}

	public static String nick() {
		return Minecraft.getInstance().getUser().getName();
	}
}
