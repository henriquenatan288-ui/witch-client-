package com.witch.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.witch.module.ChinaHatModule;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Desenha o cone do China Hat sobre a cabeca (galaxia pelo shader do Players Galaxy, ou spectral translucido). */
public final class ChinaHat {
	private ChinaHat() {}

	private static final Identifier TEX = Identifier.fromNamespaceAndPath("witch", "textures/misc/white.png");
	private static final int SEG = 28;
	private static int dbg;

	/** ps esta na origem da entidade (pes). */
	public static void submit(float eyeHeight, PoseStack ps, SubmitNodeCollector c) {
		var m = ChinaHatModule.instance;
		if (m == null) return;
		boolean galaxy = m.look.get() == 1;
		RenderType type = galaxy ? PlayersGalaxy.type(TEX, true, m.style.get() << 2) : null;
		boolean spectral = type == null; // spectral escolhido, ou shader falhou: cai no spectral
		if (spectral) type = RenderTypes.entityTranslucent(TEX);
		if (System.getProperty("witch.debug") != null && dbg++ < 4) {
			System.out.println("[WDBG] chinahat eye=" + eyeHeight + " galaxy=" + !spectral + " status=" + PlayersGalaxy.status);
		}
		final float r = 0.3f + m.size.get() * 0.05f;
		final float h = r * 0.45f;
		final int argb = m.spectral.get();
		final float t = (float) (System.nanoTime() / 1.0e9);
		final float a = 0.34f + 0.12f * (float) Math.sin(t * 2.2);
		final boolean sp = spectral;
		ps.pushPose();
		ps.translate(0f, eyeHeight + 0.2f, 0f);
		c.submitCustomGeometry(ps, type, (pose, vc) -> {
			float side = (float) Math.sqrt(h * h + r * r);
			for (int i = 0; i < SEG; i++) {
				float a0 = (float) (Math.PI * 2 * i / SEG), a1 = (float) (Math.PI * 2 * (i + 1) / SEG);
				float x0 = (float) Math.cos(a0) * r, z0 = (float) Math.sin(a0) * r;
				float x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r;
				float am = (a0 + a1) * 0.5f;
				float nx = (float) Math.cos(am) * h / side, ny = r / side, nz = (float) Math.sin(am) * h / side;
				if (sp) {
					int cr = argb >> 16 & 255, cg = argb >> 8 & 255, cb = argb & 255;
					int ab = Math.round(255 * a), at = Math.round(255 * a * 0.2f);
					v(vc, pose, x0, 0, z0, cr, cg, cb, ab, nx, ny, nz);
					v(vc, pose, x1, 0, z1, cr, cg, cb, ab, nx, ny, nz);
					v(vc, pose, 0, h, 0, cr, cg, cb, at, nx, ny, nz);
					v(vc, pose, 0, h, 0, cr, cg, cb, at, nx, ny, nz);
					// de baixo/dentro (a camada translucida costuma usar cull)
					v(vc, pose, x1, 0, z1, cr, cg, cb, ab, -nx, -ny, -nz);
					v(vc, pose, x0, 0, z0, cr, cg, cb, ab, -nx, -ny, -nz);
					v(vc, pose, 0, h, 0, cr, cg, cb, at, -nx, -ny, -nz);
					v(vc, pose, 0, h, 0, cr, cg, cb, at, -nx, -ny, -nz);
				} else {
					v(vc, pose, x0, 0, z0, 255, 255, 255, 255, nx, ny, nz);
					v(vc, pose, x1, 0, z1, 255, 255, 255, 255, nx, ny, nz);
					v(vc, pose, 0, h, 0, 255, 255, 255, 255, nx, ny, nz);
					v(vc, pose, 0, h, 0, 255, 255, 255, 255, nx, ny, nz);
				}
			}
		});
		ps.popPose();
	}

	private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, int r, int g, int b, int a, float nx, float ny, float nz) {
		vc.addVertex(pose, x, y, z).setColor(r, g, b, a).setUv(0.5f, 0.5f)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
	}
}
