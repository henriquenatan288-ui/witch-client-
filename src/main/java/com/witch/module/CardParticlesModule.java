package com.witch.module;

import com.witch.setting.IntSetting;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

/** Cartas de baralho (naipes em 128x128, lisas) ao acertar um golpe, ao pular ou flutuando em volta de voce. */
public class CardParticlesModule extends Module {
	private static CardParticlesModule instance;
	private static boolean hooked;
	private static final String[] SUITS = {"hearts", "diamonds", "clubs", "spades"};
	private static final RandomSource RNG = RandomSource.create();

	private final IntSetting onHit = add(new IntSetting("On hit (0/1)", 0, 1, 1, ""));
	private final IntSetting onJump = add(new IntSetting("On jump (0/1)", 0, 1, 0, ""));
	private final IntSetting ambient = add(new IntSetting("Ambient (0/1)", 0, 1, 0, ""));
	private final IntSetting amount = add(new IntSetting("Amount", 1, 20, 8, ""));
	private final IntSetting size = add(new IntSetting("Size", 1, 10, 3, ""));

	private boolean wasGround = true;
	private int tickN = 0;

	public CardParticlesModule() {
		super("Card Particles", Category.EFFECTS);
		instance = this;
		hook();
	}

	private static void hook() {
		if (hooked) return;
		hooked = true;
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (level.isClientSide() && instance != null && instance.isEnabled() && instance.onHit.get() == 1
					&& player == Minecraft.getInstance().player && entity instanceof LivingEntity le) {
				burst(le.getX(), le.getY() + le.getBbHeight() * 0.6, le.getZ(), instance.amount.get());
			}
			return InteractionResult.PASS;
		});
	}

	private static TextureAtlasSprite sprite(String suit) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES)
				.getSprite(Identifier.fromNamespaceAndPath("witch", "card_" + suit));
	}

	/** Explosao de cartas em volta de um ponto. */
	public static void burst(double x, double y, double z, int n) {
		var mc = Minecraft.getInstance();
		var level = mc.level;
		if (level == null || instance == null) return;
		float sz = 0.07f * instance.size.get();
		for (int i = 0; i < n; i++) {
			double a = RNG.nextDouble() * Math.PI * 2;
			double sp = 0.07 + RNG.nextDouble() * 0.12;
			mc.particleEngine.add(new CardParticle(level, x, y, z, Math.cos(a) * sp, 0.08 + RNG.nextDouble() * 0.1,
					Math.sin(a) * sp, sprite(SUITS[RNG.nextInt(4)]), sz, 0.18f, 30 + RNG.nextInt(20)));
		}
	}

	private static void floaty(double x, double y, double z) {
		var mc = Minecraft.getInstance();
		if (mc.level == null || instance == null) return;
		float sz = 0.07f * instance.size.get();
		mc.particleEngine.add(new CardParticle(mc.level, x, y, z, (RNG.nextDouble() - 0.5) * 0.02, 0.01,
				(RNG.nextDouble() - 0.5) * 0.02, sprite(SUITS[RNG.nextInt(4)]), sz, -0.01f, 70 + RNG.nextInt(40)));
	}

	@Override
	public void onTick() {
		var p = Minecraft.getInstance().player;
		if (p == null) return;
		boolean ground = p.onGround();
		if (onJump.get() == 1 && wasGround && !ground && p.getDeltaMovement().y > 0.2 && !p.getAbilities().flying) {
			burst(p.getX(), p.getY() + 0.2, p.getZ(), Math.max(3, amount.get() / 2));
		}
		wasGround = ground;
		if (ambient.get() == 1 && ++tickN % 6 == 0) {
			floaty(p.getX() + (RNG.nextDouble() - 0.5) * 6, p.getY() + RNG.nextDouble() * 2.5,
					p.getZ() + (RNG.nextDouble() - 0.5) * 6);
		}
	}
}
