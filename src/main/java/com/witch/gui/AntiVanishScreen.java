package com.witch.gui;

import com.witch.module.AntiVanishModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lista de jogadores online (cabeca, nome, cargo). Os marcados viram os "perigosos" do Anti Vanish. */
public class AntiVanishScreen extends Screen {
	private static final int ROW = 20;
	private static final int GREEN = 0xFF2F7BFF;

	private final Screen parent;
	private float scroll = 0f;
	private boolean pressed, moved;
	private double pressX, pressY, scrollStart;
	private int pressedRow = -1;

	public AntiVanishScreen(Screen parent) {
		super(Component.literal("Anti Vanish"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(parent);
	}

	private record Row(PlayerInfo info, String name, String rank, boolean danger) {}

	private List<Row> rows() {
		List<Row> out = new ArrayList<>();
		var conn = Minecraft.getInstance().getConnection();
		if (conn == null) return out;
		var me = Minecraft.getInstance().player;
		for (PlayerInfo i : conn.getOnlinePlayers()) {
			String n = i.getProfile().name();
			if (me != null && n.equals(me.getGameProfile().name())) continue;
			out.add(new Row(i, n, AntiVanishModule.rankOf(i), AntiVanishModule.isDangerous(n)));
		}
		out.sort(Comparator.comparing((Row r) -> !r.danger).thenComparing(r -> r.rank.toLowerCase(Locale.ROOT))
				.thenComparing(r -> r.name.toLowerCase(Locale.ROOT)));
		return out;
	}

	private int bw() { return Math.min(this.width - 12, 300); }
	private int bx() { return (this.width - bw()) / 2; }
	private int by() { return 8; }
	private int bh() { return this.height - 16; }
	private int listTop() { return by() + 30; }
	private int listBottom() { return by() + bh() - 22; }

	private float maxScroll(int n) {
		return Math.max(0, n * ROW - (listBottom() - listTop()));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		List<Row> rows = rows();
		scroll = Mth.clamp(scroll, 0f, maxScroll(rows.size()));
		g.fill(0, 0, this.width, this.height, 0xA0000000);
		RenderUtil.roundRect(g, bx(), by(), bw(), bh(), 6, 0xF0080C09);
		RenderUtil.roundRect(g, bx(), by(), 3, bh(), 1, GREEN);
		int fh = FontUtil.height();
		FontUtil.draw(g, "Anti Vanish - marque os perigosos", bx() + 10, by() + 8, 0xFFFFFFFF);
		FontUtil.draw(g, AntiVanishModule.dangerousNames().size() + " marcado(s) | toque para marcar", bx() + 10, by() + 8 + fh + 2, 0xFF8A93A8);

		int top = listTop(), bot = listBottom();
		g.enableScissor(bx(), top, bx() + bw(), bot);
		int y = top - Math.round(scroll);
		for (Row r : rows) {
			if (y + ROW > top && y < bot) {
				boolean hov = RenderUtil.inside(mx, my, bx() + 4, y, bw() - 8, ROW - 2) && my >= top && my < bot;
				RenderUtil.roundRect(g, bx() + 4, y, bw() - 8, ROW - 2, 4,
						r.danger ? 0x50FF4040 : (hov ? 0x30FFFFFF : 0x18FFFFFF));
				try {
					PlayerFaceExtractor.extractRenderState(g, r.info.getSkin(), bx() + 8, y + 2, ROW - 6, 0xFFFFFFFF);
				} catch (Throwable ignored) { }
				int tx = bx() + 8 + ROW - 6 + 6;
				FontUtil.draw(g, r.name, tx, y + (ROW - 2 - fh) / 2, r.danger ? 0xFFFF8A8A : 0xFFFFFFFF);
				String rk = ModuleRow.fit(r.rank, bw() / 3);
				int rw = FontUtil.width(rk);
				FontUtil.draw(g, rk, bx() + bw() - 34 - rw, y + (ROW - 2 - fh) / 2, 0xFF8A93A8);
				// caixa de marcacao
				int cx = bx() + bw() - 24, cy = y + (ROW - 2 - 10) / 2;
				RenderUtil.roundRect(g, cx, cy, 10, 10, 3, 0xFF3A3A3A);
				RenderUtil.roundRect(g, cx + 1, cy + 1, 8, 8, 2, 0xFF0C0C0C);
				if (r.danger) RenderUtil.roundRect(g, cx + 2, cy + 2, 6, 6, 1, 0xFFFF5555);
			}
			y += ROW;
		}
		g.disableScissor();
		if (rows.isEmpty()) FontUtil.draw(g, "Nenhum jogador online", bx() + 10, top + 6, 0xFF8A93A8);

		// botao Pronto
		int dw = 70, dx = bx() + (bw() - dw) / 2, dy = by() + bh() - 18;
		boolean dh = RenderUtil.inside(mx, my, dx, dy, dw, 14);
		RenderUtil.roundRect(g, dx, dy, dw, 14, 5, dh ? 0xFF2F7BFF : 0xFF1E56C8);
		String d = "Pronto";
		FontUtil.draw(g, d, dx + (dw - FontUtil.width(d)) / 2, dy + (14 - fh) / 2, 0xFF04120A);
	}

	private int rowAt(double my, int n) {
		if (my < listTop() || my >= listBottom()) return -1;
		int i = (int) ((my - listTop() + scroll) / ROW);
		return i >= 0 && i < n ? i : -1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mx = click.x(), my = click.y();
		int dw = 70, dx = bx() + (bw() - dw) / 2, dy = by() + bh() - 18;
		if (RenderUtil.inside(mx, my, dx, dy, dw, 14)) {
			onClose();
			return true;
		}
		pressed = true;
		moved = false;
		pressX = mx;
		pressY = my;
		scrollStart = scroll;
		pressedRow = rowAt(my, rows().size());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (!pressed) return false;
		double dy = click.y() - pressY;
		if (Math.abs(dy) > 4) moved = true;
		if (moved) scroll = Mth.clamp((float) (scrollStart - dy), 0f, maxScroll(rows().size()));
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (pressed && !moved && pressedRow >= 0) {
			List<Row> rows = rows();
			if (pressedRow < rows.size()) AntiVanishModule.toggleDangerous(rows.get(pressedRow).name);
		}
		pressed = false;
		pressedRow = -1;
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scroll = Mth.clamp(scroll - (float) verticalAmount * ROW, 0f, maxScroll(rows().size()));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(input);
	}
}
