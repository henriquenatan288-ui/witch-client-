package com.witch.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.witch.module.GlowItemsModule;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Halo do item na mao: redesenha o modelo 3 vezes, um pouco maior no plano do item, tingido com a cor do modulo
 * e em brilho maximo, ATRAS do item normal (a profundidade e achatada para ele nao cobrir o original).
 * Usa reflexao nos campos privados do ItemStackRenderState; qualquer falha desliga o efeito em silencio.
 */
public final class HeldItemGlow {
	private HeldItemGlow() {}

	private static boolean failed = false;
	private static boolean ready = false;
	private static Field fActive, fLayers, fCtx, fQuads, fSpecial;
	private static Method mApply;

	private static void init() throws Exception {
		Class<?> st = ItemStackRenderState.class;
		fActive = st.getDeclaredField("activeLayerCount");
		fLayers = st.getDeclaredField("layers");
		fCtx = st.getDeclaredField("displayContext");
		Class<?> layer = Class.forName("net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState");
		fQuads = layer.getDeclaredField("quads");
		fSpecial = layer.getDeclaredField("specialRenderer");
		mApply = layer.getDeclaredMethod("applyTransform", PoseStack.Pose.class);
		for (Field f : new Field[] {fActive, fLayers, fCtx, fQuads, fSpecial}) f.setAccessible(true);
		mApply.setAccessible(true);
		ready = true;
	}

	public static void draw(ItemStackRenderState st, PoseStack pose, SubmitNodeCollector c) {
		if (failed) return;
		int argb = GlowItemsModule.handColor();
		if (argb == 0) return;
		try {
			if (!ready) init();
			int n = fActive.getInt(st);
			Object[] layers = (Object[]) fLayers.get(st);
			ItemDisplayContext ctx = (ItemDisplayContext) fCtx.get(st);
			float step = GlowItemsModule.handSize() * 0.012f;
			for (int i = 0; i < n; i++) {
				Object layer = layers[i];
				if (fSpecial.get(layer) != null) continue;
				@SuppressWarnings("unchecked")
				List<BakedQuad> quads = (List<BakedQuad>) fQuads.get(layer);
				if (quads == null || quads.isEmpty()) continue;
				List<BakedQuad> tinted = new ArrayList<>(quads.size());
				for (BakedQuad q : quads) {
					var mi = q.materialInfo();
					var nmi = new BakedQuad.MaterialInfo(mi.sprite(), mi.layer(), mi.itemRenderType(), 0, false, 15);
					tinted.add(new BakedQuad(q.position0(), q.position1(), q.position2(), q.position3(),
							q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(), q.direction(), nmi));
				}
				// de fora para dentro: mais escuro na borda, mais claro perto do item
				for (int pass = 3; pass >= 1; pass--) {
					float k = 0.45f + 0.55f * (4 - pass) / 3f;
					int r = (int) (((argb >> 16) & 0xFF) * k), g = (int) (((argb >> 8) & 0xFF) * k), b = (int) ((argb & 0xFF) * k);
					int tint = 0xFF000000 | (r << 16) | (g << 8) | b;
					pose.pushPose();
					mApply.invoke(layer, pose.last());
					float s = 1f + step * pass;
					pose.scale(s, s, 0.25f);
					c.submitItem(pose, ctx, 0xF000F0, OverlayTexture.NO_OVERLAY, 0, new int[] {tint}, tinted,
							ItemStackRenderState.FoilType.NONE);
					pose.popPose();
				}
			}
		} catch (Throwable t) {
			failed = true;
		}
	}
}
