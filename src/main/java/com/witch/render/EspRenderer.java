package com.witch.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Renderizador generico de caixas (quads) e tracers (DEBUG_LINES, estilo Meteor).
 * Tracers usam PrimitiveTopology.DEBUG_LINES = linha de 1px na tela, nao fita/retangulo.
 */
public final class EspRenderer {
	/** Caixas / preenchimento (quads). */
	private static final RenderPipeline PIPELINE_FILLED = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("witch", "pipeline/esp_filled"))
					.withDepthStencilState(Optional.empty())
					.build()
	);

	/**
	 * Tracers / linhas finas — mesmo padrao do Meteor:
	 * LINES_SNIPPET + DEBUG_LINES + depth desligado = linha de 1px atraves das paredes.
	 */
	private static final RenderPipeline PIPELINE_LINES = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("witch", "pipeline/esp_lines"))
					.withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
					.withDepthStencilState(Optional.empty())
					.build()
	);

	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
	private static final StagedVertexBuffer STAGED =
			new StagedVertexBuffer(() -> "Witch ESP Buffer", RenderType.SMALL_BUFFER_SIZE);

	private static final List<EspSource> SOURCES = new ArrayList<>();
	private static boolean inited = false;

	/** Espessura das arestas de caixa (nao dos tracers). */
	private static final float K = 0.0012f;

	private EspRenderer() {}

	public static void clearSources() {
		SOURCES.clear();
	}

	public static void register(EspSource source) {
		SOURCES.add(source);
	}

	public static void init() {
		if (inited) return;
		inited = true;
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(EspRenderer::onRender);
	}

	private static void onRender(LevelRenderContext context) {
		boolean any = false;
		for (EspSource s : SOURCES) {
			if (s.active()) {
				any = true;
				break;
			}
		}
		if (!any) return;
		if (PIPELINE_FILLED.getVertexFormatBinding(0) == null) return;
		if (PIPELINE_LINES.getVertexFormatBinding(0) == null) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

		PoseStack pose = context.poseStack();
		Vec3 cam = context.levelState().cameraRenderState.pos;

		// Origem dos tracers = ponto ~0.1 a frente da camera (centro da tela), igual Meteor
		var crs = context.levelState().cameraRenderState;
		double yr = Math.toRadians(crs.yRot);
		double pr = Math.toRadians(crs.xRot);
		Vec3 origin = new Vec3(
				cam.x - Math.sin(yr) * Math.cos(pr) * 0.1,
				cam.y - Math.sin(pr) * 0.1,
				cam.z + Math.cos(yr) * Math.cos(pr) * 0.1);

		pose.pushPose();
		pose.translate(-cam.x, -cam.y, -cam.z);
		Esp esp = new Esp(pose.last().pose(), cam, pt, origin);
		for (EspSource s : SOURCES) {
			if (s.active()) s.collect(esp);
		}
		pose.popPose();

		STAGED.upload();

		if (esp.drawFilled != null) {
			StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(esp.drawFilled);
			if (info != null) execute(mc, info, PIPELINE_FILLED);
		}
		if (esp.drawLines != null) {
			StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(esp.drawLines);
			if (info != null) execute(mc, info, PIPELINE_LINES);
		}

		STAGED.endFrame();
	}

	private static void execute(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
				RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

		RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
		GpuTextureView colorTexture = mainTarget.getColorTextureView();
		if (colorTexture == null) return;

		try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "witch esp",
				colorTexture,
				Optional.empty(),
				mainTarget.getDepthTextureView(),
				OptionalDouble.empty())) {
			renderPass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", dynamicTransforms);
			renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
			renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
			renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
		}
	}

	/** Entregue aos modulos: box() = quads, tracer()/thinLine() = DEBUG_LINES. */
	public static final class Esp {
		private final Matrix4fc m;
		private final Vec3 cam;
		private final float partial;
		private final Vec3 start;

		private StagedVertexBuffer.Draw drawFilled;
		private VertexConsumer bufFilled;

		private StagedVertexBuffer.Draw drawLines;
		private VertexConsumer bufLines;

		private Esp(Matrix4fc m, Vec3 cam, float partial, Vec3 origin) {
			this.m = m;
			this.cam = cam;
			this.partial = partial;
			this.start = origin;
		}

		public float partial() {
			return partial;
		}

		private VertexConsumer filled() {
			if (bufFilled == null) {
				var binding = PIPELINE_FILLED.getVertexFormatBinding(0);
				var primitive = PIPELINE_FILLED.getPrimitiveTopology();
				var sorting = primitive == PrimitiveTopology.QUADS
						? RenderSystem.getProjectionType().vertexSorting()
						: null;
				drawFilled = STAGED.appendDraw(binding, primitive, sorting);
				bufFilled = STAGED.getVertexBuilder(drawFilled);
			}
			return bufFilled;
		}

		private VertexConsumer lines() {
			if (bufLines == null) {
				var binding = PIPELINE_LINES.getVertexFormatBinding(0);
				var primitive = PIPELINE_LINES.getPrimitiveTopology();
				drawLines = STAGED.appendDraw(binding, primitive, null);
				bufLines = STAGED.getVertexBuilder(drawLines);
			}
			return bufLines;
		}

		private float dist(double x, double y, double z) {
			double dx = x - cam.x;
			double dy = y - cam.y;
			double dz = z - cam.z;
			return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		/** Caixa com contorno e preenchimento opcional (fillA 0 = so contorno). */
		public void box(double x0, double y0, double z0, double x1, double y1, double z1, int argb, float fillA) {
			VertexConsumer vb = filled();
			float h = Math.max(0.015f, K * dist((x0 + x1) * 0.5, (y0 + y1) * 0.5, (z0 + z1) * 0.5));
			float[] line = rgba(argb, 1f);
			if (fillA > 0f) {
				fillBox(vb, m, (float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1,
						rgba(argb, fillA));
			}
			outline(vb, m, (float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1, line, h);
		}

		/** Linha do centro da tela ate o ponto (DEBUG_LINES, 1px). */
		public void tracer(double tx, double ty, double tz, int argb) {
			thinLine(start.x, start.y, start.z, tx, ty, tz, argb, 1f);
		}

		/** Igual ao tracer, com transparencia propria. */
		public void tracer(double tx, double ty, double tz, int argb, float alpha) {
			thinLine(start.x, start.y, start.z, tx, ty, tz, argb, alpha);
		}

		/** Caixa so de preenchimento (sem contorno). */
		public void solid(double x0, double y0, double z0, double x1, double y1, double z1, int argb, float alpha) {
			fillBox(filled(), m, (float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1, rgba(argb, alpha));
		}

		/** Linha grossa (ainda fita) — usado por outros ESP se precisar. */
		public void line(double sx, double sy, double sz, double tx, double ty, double tz, int argb, float alpha) {
			ribbon(sx, sy, sz, tx, ty, tz, argb, alpha, K, 0.003f, false);
		}

		/**
		 * Tracer real estilo Meteor: 2 vertices, PrimitiveTopology.DEBUG_LINES.
		 * Resultado = linha de 1 pixel na tela, sem virar retangulo.
		 * MC 26.2 exige Normal + LineWidth no vertex format de linhas.
		 */
		public void thinLine(double sx, double sy, double sz, double tx, double ty, double tz, int argb, float alpha) {
			float[] c = rgba(argb, alpha);
			VertexConsumer vb = lines();

			float dx = (float) (tx - sx);
			float dy = (float) (ty - sy);
			float dz = (float) (tz - sz);
			float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (len < 1.0E-6f) return;
			dx /= len;
			dy /= len;
			dz /= len;

			vb.addVertex(m, (float) sx, (float) sy, (float) sz)
					.setColor(c[0], c[1], c[2], c[3])
					.setNormal(dx, dy, dz)
					.setLineWidth(1.0f);
			vb.addVertex(m, (float) tx, (float) ty, (float) tz)
					.setColor(c[0], c[1], c[2], c[3])
					.setNormal(dx, dy, dz)
					.setLineWidth(1.0f);
		}

		private void ribbon(double sx, double sy, double sz, double tx, double ty, double tz, int argb, float alpha,
							float kw, float minW, boolean equalWidth) {
			float ax = (float) sx, ay = (float) sy, az = (float) sz;
			float bx = (float) tx, by = (float) ty, bz = (float) tz;
			float dx = bx - ax, dy = by - ay, dz = bz - az;
			float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (len < 0.01f) return;
			float nx = dx / len, ny = dy / len, nz = dz / len;

			float rx = 0f, ry = 1f, rz = 0f;
			if (Math.abs(ny) > 0.99f) {
				rx = 1f;
				ry = 0f;
			}
			float ux = ny * rz - nz * ry;
			float uy = nz * rx - nx * rz;
			float uz = nx * ry - ny * rx;
			float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
			if (ul < 1.0E-6f) return;
			ux /= ul;
			uy /= ul;
			uz /= ul;
			float vx = ny * uz - nz * uy;
			float vy = nz * ux - nx * uz;
			float vz = nx * uy - ny * ux;

			float w0 = Math.max(minW, kw * dist(ax, ay, az));
			float w1 = Math.max(minW, kw * dist(bx, by, bz));
			if (equalWidth) {
				float w = Math.max(w0, w1);
				w0 = w;
				w1 = w;
			}
			float[] c = rgba(argb, alpha);
			VertexConsumer vb = filled();
			quad(vb, m, c,
					ax - ux * w0, ay - uy * w0, az - uz * w0,
					bx - ux * w1, by - uy * w1, bz - uz * w1,
					bx + ux * w1, by + uy * w1, bz + uz * w1,
					ax + ux * w0, ay + uy * w0, az + uz * w0);
			quad(vb, m, c,
					ax - vx * w0, ay - vy * w0, az - vz * w0,
					bx - vx * w1, by - vy * w1, bz - vz * w1,
					bx + vx * w1, by + vy * w1, bz + vz * w1,
					ax + vx * w0, ay + vy * w0, az + vz * w0);
		}
	}

	private static float[] rgba(int argb, float alpha) {
		return new float[]{
				((argb >> 16) & 0xFF) / 255f,
				((argb >> 8) & 0xFF) / 255f,
				(argb & 0xFF) / 255f,
				alpha
		};
	}

	private static void fillBox(VertexConsumer buf, Matrix4fc m,
								float bx0, float by0, float bz0, float bx1, float by1, float bz1, float[] c) {
		float e = 0.003f;
		float x0 = bx0 - e, y0 = by0 - e, z0 = bz0 - e;
		float x1 = bx1 + e, y1 = by1 + e, z1 = bz1 + e;

		quad(buf, m, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		quad(buf, m, c, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		quad(buf, m, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
		quad(buf, m, c, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		quad(buf, m, c, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		quad(buf, m, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
	}

	private static void outline(VertexConsumer buf, Matrix4fc m,
								float x0, float y0, float z0, float x1, float y1, float z1, float[] c, float h) {
		float[] ys = {y0, y1};
		float[] zs = {z0, z1};
		float[] xs = {x0, x1};

		for (float y : ys) {
			for (float z : zs) {
				edge(buf, m, c, h, x0, y, z, x1, y, z, 0f, 1f, 0f, 0f, 0f, 1f);
			}
		}
		for (float x : xs) {
			for (float z : zs) {
				edge(buf, m, c, h, x, y0, z, x, y1, z, 1f, 0f, 0f, 0f, 0f, 1f);
			}
		}
		for (float x : xs) {
			for (float y : ys) {
				edge(buf, m, c, h, x, y, z0, x, y, z1, 1f, 0f, 0f, 0f, 1f, 0f);
			}
		}
	}

	private static void edge(VertexConsumer buf, Matrix4fc m, float[] c, float h,
							 float px, float py, float pz, float qx, float qy, float qz,
							 float ux, float uy, float uz, float vx, float vy, float vz) {
		quad(buf, m, c,
				px - ux * h, py - uy * h, pz - uz * h,
				qx - ux * h, qy - uy * h, qz - uz * h,
				qx + ux * h, qy + uy * h, qz + uz * h,
				px + ux * h, py + uy * h, pz + uz * h);
		quad(buf, m, c,
				px - vx * h, py - vy * h, pz - vz * h,
				qx - vx * h, qy - vy * h, qz - vz * h,
				qx + vx * h, qy + vy * h, qz + vz * h,
				px + vx * h, py + vy * h, pz + vz * h);
	}

	private static void quad(VertexConsumer buf, Matrix4fc m, float[] c,
							 float ax, float ay, float az, float bx, float by, float bz,
							 float cx, float cy, float cz, float dx, float dy, float dz) {
		v(buf, m, c, ax, ay, az);
		v(buf, m, c, bx, by, bz);
		v(buf, m, c, cx, cy, cz);
		v(buf, m, c, dx, dy, dz);

		v(buf, m, c, dx, dy, dz);
		v(buf, m, c, cx, cy, cz);
		v(buf, m, c, bx, by, bz);
		v(buf, m, c, ax, ay, az);
	}

	private static void v(VertexConsumer buf, Matrix4fc m, float[] c, float x, float y, float z) {
		buf.addVertex(m, x, y, z).setColor(c[0], c[1], c[2], c[3]);
	}
}
