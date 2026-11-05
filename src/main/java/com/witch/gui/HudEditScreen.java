package com.witch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Editor do HUD: grade de 8 px, arraste cada elemento (encaixa na grade). Esc ou Done salva e volta ao ClickGUI. */
public class HudEditScreen extends Screen {
	private static final int GRID = 8;

	private String dragId = null;
	private int grabX, grabY;
	private int startX, startY, startDx, startDy;

	public HudEditScreen() {
		super(Component.literal("Edit HUD"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int doneW() {
		return FontUtil.width("Done") + 20;
	}

	private int resetW() {
		return FontUtil.width("Reset HUD") + 20;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		g.fill(0, 0, this.width, this.height, 0x66000000);
		int accent = Theme.accent();
		for (int x = 0; x <= this.width; x += GRID) {
			boolean major = x % (GRID * 4) == 0;
			g.fill(x, 0, x + 1, this.height, RenderUtil.alpha(0xFFFFFFFF, major ? 0.10f : 0.04f));
		}
		for (int y = 0; y <= this.height; y += GRID) {
			boolean major = y % (GRID * 4) == 0;
			g.fill(0, y, this.width, y + 1, RenderUtil.alpha(0xFFFFFFFF, major ? 0.10f : 0.04f));
		}
		g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, RenderUtil.alpha(accent, 0.25f));
		g.fill(0, this.height / 2, this.width, this.height / 2 + 1, RenderUtil.alpha(accent, 0.25f));

		for (int i = 0; i < HudLayout.IDS.length; i++) {
			String id = HudLayout.IDS[i];
			int[] r = HudLayout.rect(id);
			boolean hot = id.equals(dragId) || (dragId == null && RenderUtil.inside(mx, my, r[0], r[1], r[2], r[3]));
			int col = RenderUtil.alpha(accent, hot ? 0.95f : 0.55f);
			g.fill(r[0], r[1], r[0] + r[2], r[1] + 1, col);
			g.fill(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], col);
			g.fill(r[0], r[1], r[0] + 1, r[1] + r[3], col);
			g.fill(r[0] + r[2] - 1, r[1], r[0] + r[2], r[1] + r[3], col);
			g.fill(r[0] + 1, r[1] + 1, r[0] + r[2] - 1, r[1] + r[3] - 1, RenderUtil.alpha(accent, hot ? 0.16f : 0.07f));
			String lab = HudLayout.LABELS[i];
			FontUtil.draw(g, lab, r[0] + 3, Math.max(1, r[1] - FontUtil.height() - 2), RenderUtil.alpha(0xFFFFFFFF, hot ? 1f : 0.8f));
		}

		String hint = "Arraste os elementos. Esc salva e volta.";
		FontUtil.draw(g, hint, (this.width - FontUtil.width(hint)) / 2, 8, 0xFFCFCFCF);

		int by = this.height - 26;
		int dw = doneW();
		int rw = resetW();
		int total = dw + 8 + rw;
		int dx0 = (this.width - total) / 2;
		boolean hd = RenderUtil.inside(mx, my, dx0, by, dw, 16);
		RenderUtil.roundRect(g, dx0, by, dw, 16, 5, RenderUtil.alpha(hd ? 0xFF2A2A2A : 0xFF121212, 0.9f));
		FontUtil.draw(g, "Done", dx0 + 10, by + (16 - FontUtil.height()) / 2, hd ? 0xFFFFFFFF : 0xFFCFCFCF);
		int rx0 = dx0 + dw + 8;
		boolean hr = RenderUtil.inside(mx, my, rx0, by, rw, 16);
		RenderUtil.roundRect(g, rx0, by, rw, 16, 5, RenderUtil.alpha(hr ? 0xFF2A2A2A : 0xFF121212, 0.9f));
		FontUtil.draw(g, "Reset HUD", rx0 + 10, by + (16 - FontUtil.height()) / 2, hr ? 0xFFFFFFFF : 0xFF8E8E8E);
	}

	private void back() {
		HudLayout.save();
		Minecraft.getInstance().gui.setScreen(new ClickGuiScreen());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mx = click.x();
		double my = click.y();
		int by = this.height - 26;
		int dw = doneW();
		int rw = resetW();
		int dx0 = (this.width - (dw + 8 + rw)) / 2;
		if (RenderUtil.inside(mx, my, dx0, by, dw, 16)) {
			back();
			return true;
		}
		if (RenderUtil.inside(mx, my, dx0 + dw + 8, by, rw, 16)) {
			HudLayout.reset();
			return true;
		}
		for (int i = HudLayout.IDS.length - 1; i >= 0; i--) {
			String id = HudLayout.IDS[i];
			int[] r = HudLayout.rect(id);
			if (RenderUtil.inside(mx, my, r[0], r[1], r[2], r[3])) {
				dragId = id;
				grabX = (int) mx - r[0];
				grabY = (int) my - r[1];
				startX = r[0];
				startY = r[1];
				startDx = HudLayout.dx(id);
				startDy = HudLayout.dy(id);
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (dragId == null) return super.mouseDragged(click, offsetX, offsetY);
		int[] r = HudLayout.rect(dragId);
		int tx = Math.round((float) (click.x() - grabX) / GRID) * GRID;
		int ty = Math.round((float) (click.y() - grabY) / GRID) * GRID;
		tx = Mth.clamp(tx, 0, Math.max(0, this.width - r[2]));
		ty = Mth.clamp(ty, 0, Math.max(0, this.height - r[3]));
		HudLayout.set(dragId, startDx + (tx - startX), startDy + (ty - startY));
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (dragId != null) {
			dragId = null;
			HudLayout.save();
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public void onClose() {
		back();
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		return super.keyPressed(input);
	}
}
