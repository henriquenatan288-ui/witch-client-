package com.witch.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Gravacao: pega madeira, faz ferramentas de pedra, volta pro solo, faz casa de terra, camera de frente e agacha 5x. */
public class VideoGametest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext c) {
		if (System.getenv("WITCH_VIDEO") == null) return;
		c.getInput().resizeWindow(1280, 720);
		c.waitTicks(40);
		try (TestSingleplayerContext sp = c.worldBuilder().create()) {
			sp.getClientLevel().waitForChunksRender();
			var srv = sp.getServer();
			srv.runCommand("gamemode survival @p");
			srv.runCommand("difficulty peaceful");
			srv.runCommand("gamerule doMobSpawning false");
			srv.runCommand("gamerule doDaylightCycle false");
			srv.runCommand("time set 1000");
			srv.runCommand("weather clear");
			buildScene(srv);
			srv.runCommand("tp @p 0 61 0 -90 10");
			c.waitTicks(40);
			c.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			// 1) madeira: anda ate a arvore e corta 3 troncos
			walkTo(c, 6.0, 0.0, false);
			for (int y = 61; y <= 63; y++) {
				mine(c, new BlockPos(8, y, 0));
			}
			walkTo(c, 8.0, 0.0, false);
			c.waitTicks(20);

			// 2) abre o inventario (craft) e recebe a ferramenta de madeira
			c.getInput().pressKey(o -> o.keyInventory);
			c.waitTicks(50);
			c.getInput().pressKey(o -> o.keyInventory);
			c.waitTicks(10);
			srv.runCommand("item replace entity @p hotbar.1 with minecraft:wooden_pickaxe");
			c.getInput().pressKey(o -> o.keyHotbarSlots[1]);
			c.waitTicks(20);

			// 3) desce a rampa ate a caverna e minera pedra
			walkTo(c, -6.0, 0.0, false);
			walkTo(c, -18.0, 0.0, false);
			for (int i = 0; i < 3; i++) {
				mine(c, new BlockPos(-21 + 0, 52 + (i % 2), (i / 2)));
				c.waitTicks(5);
			}
			c.waitTicks(20);

			// 4) ferramentas de pedra
			c.getInput().pressKey(o -> o.keyInventory);
			c.waitTicks(50);
			c.getInput().pressKey(o -> o.keyInventory);
			c.waitTicks(10);
			srv.runCommand("item replace entity @p hotbar.1 with minecraft:stone_pickaxe");
			srv.runCommand("item replace entity @p hotbar.2 with minecraft:stone_axe");
			srv.runCommand("item replace entity @p hotbar.3 with minecraft:stone_sword");
			srv.runCommand("item replace entity @p hotbar.4 with minecraft:dirt 64");
			for (int s = 1; s <= 3; s++) {
				final int slot = s;
				c.getInput().pressKey(o -> o.keyHotbarSlots[slot]);
				c.waitTicks(25);
			}

			// 5) volta pro solo
			walkTo(c, -13.0, 0.0, false);
			walkTo(c, -6.0, 0.0, true);
			walkTo(c, 4.0, 8.0, true);
			c.getInput().pressKey(o -> o.keyHotbarSlots[4]);
			c.waitTicks(15);

			// 6) casa de terra 3x3, paredes de 2 blocos, porta no lado de z=3
			for (int y = 61; y <= 62; y++) {
				for (int x = 3; x <= 5; x++) {
					for (int z = 3; z <= 5; z++) {
						if (x == 4 && z == 4) continue;
						if (x == 4 && z == 3) continue;
						BlockPos below = new BlockPos(x, y - 1, z);
						place(c, below, Direction.UP);
					}
				}
			}
			for (int x = 3; x <= 5; x++) {
				for (int z = 3; z <= 5; z++) {
					if (x == 4 && z == 4) continue;
					if (x == 4 && z == 3) {
						place(c, new BlockPos(3, 63, 3), Direction.EAST);
						continue;
					}
					place(c, new BlockPos(x, 62, z), Direction.UP);
				}
			}
			place(c, new BlockPos(3, 63, 4), Direction.EAST);
			c.waitTicks(30);

			// 7) camera de frente e agacha 5 vezes
			c.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			c.waitTicks(40);
			for (int i = 0; i < 5; i++) {
				c.getInput().holdKeyFor(o -> o.keyShift, 8);
				c.waitTicks(10);
			}
			c.waitTicks(40);
		}
	}

	private static void buildScene(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext srv) {
		srv.runCommand("fill -24 40 -24 24 57 24 minecraft:stone");
		srv.runCommand("fill -24 58 -24 24 59 24 minecraft:dirt");
		srv.runCommand("fill -24 60 -24 24 60 24 minecraft:grass_block");
		srv.runCommand("fill -24 61 -24 24 90 24 minecraft:air");
		// arvore
		srv.runCommand("fill 7 61 -2 9 61 2 minecraft:air");
		srv.runCommand("fill 7 64 -1 9 65 1 minecraft:oak_leaves");
		srv.runCommand("fill 8 61 0 8 63 0 minecraft:oak_log");
		srv.runCommand("setblock 8 66 0 minecraft:oak_leaves");
		// rampa e caverna
		for (int i = 0; i < 8; i++) {
			srv.runCommand("fill " + (-6 - i) + " " + (60 - i) + " -1 " + (-6 - i) + " " + (63 - i) + " 1 minecraft:air");
		}
		srv.runCommand("fill -20 52 -3 -13 56 3 minecraft:air");
		srv.runCommand("fill -20 51 -3 -13 51 3 minecraft:stone");
		srv.runCommand("fill -21 52 -2 -21 55 2 minecraft:stone");
	}

	private static double[] pos(ClientGameTestContext c) {
		return c.computeOnClient(mc -> new double[] {mc.player.getX(), mc.player.getZ(), mc.player.getY()});
	}

	private static void walkTo(ClientGameTestContext c, double x, double z, boolean jump) {
		c.getInput().holdKey(o -> o.keyUp);
		if (jump) c.getInput().holdKey(o -> o.keyJump);
		for (int i = 0; i < 400; i++) {
			double[] p = pos(c);
			double dx = x - p[0];
			double dz = z - p[1];
			if (Math.hypot(dx, dz) < 0.5) break;
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			c.getInput().lookAt(yaw, 12f);
			c.waitTick();
		}
		c.getInput().releaseKey(o -> o.keyUp);
		if (jump) c.getInput().releaseKey(o -> o.keyJump);
		c.waitTicks(4);
	}

	private static void mine(ClientGameTestContext c, BlockPos p) {
		c.getInput().lookAt(p);
		c.getInput().holdMouse(0);
		c.waitFor(mc -> mc.level.getBlockState(p).isAir(), 500);
		c.getInput().releaseMouse(0);
	}

	/** Coloca o bloco da mao na face 'face' do bloco 'against' (olha pra ele antes, pra aparecer no video). */
	private static void place(ClientGameTestContext c, BlockPos against, Direction face) {
		c.getInput().lookAt(against);
		c.waitTicks(3);
		c.runOnClient(mc -> {
			Vec3 hit = Vec3.atCenterOf(against).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, against, false));
			mc.player.swing(InteractionHand.MAIN_HAND);
		});
		c.waitTicks(4);
	}
}
