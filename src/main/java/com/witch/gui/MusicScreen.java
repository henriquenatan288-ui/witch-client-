package com.witch.gui;

import com.witch.music.MusicPlayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.nio.file.Path;
import java.util.List;

/** Lista as musicas da pasta "musicas" e controla a reproducao. */
public class MusicScreen extends Screen {
	private static final int ROW = 14;
	private int wx, wy, ww, wh;
	private float scroll = 0f;
	private boolean volDrag = false, barDrag = false;

	public MusicScreen() {
		super(Component.literal("Music"));
		MusicPlayer.refresh();
	}

	private void layout() {
		ww = Math.min(this.width - 12, 340);
		wh = Math.min(this.height - 12, 230);
		wx = (this.width - ww) / 2;
		wy = (this.height - wh) / 2;
	}

	private int listX() { return wx + 10; }
	private int listY() { return wy + 36; }
	private int listW() { return ww - 20; }
	private int listH() { return wh - 36 - 62; }
	private int barY() { return wy + wh - 50; }
	private int btnY() { return wy + wh - 32; }
	private int volX() { return wx + ww - 10 - 70; }

	private float maxScroll() {
		return Math.max(0, MusicPlayer.tracks().size() * ROW - listH());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		layout();
		int fh = FontUtil.height();
		g.fill(0, 0, this.width, this.height, 0x90000000);
		RenderUtil.roundRect(g, wx, wy, ww, wh, 8, 0xFF2A2E35);
		RenderUtil.roundRect(g, wx + 1, wy + 1, ww - 2, wh - 2, 7, 0xFF0A0C0F);

		FontUtil.draw(g, "Music", wx + 10, wy + 8, 0xFFFFFFFF);
		String hint = "pasta: musicas (.mp3 .ogg .wav)";
		FontUtil.draw(g, ModuleRow.fit(hint, ww - 90), wx + 10, wy + 8 + fh + 2, 0xFF6B7078);
		String rf = "Refresh";
		int rw = FontUtil.width(rf) + 10;
		button(g, wx + ww - 10 - rw, wy + 7, rw, 14, rf, false, mx, my);

		// lista
		List<Path> tr = MusicPlayer.tracks();
		g.enableScissor(listX(), listY(), listX() + listW(), listY() + listH());
		if (tr.isEmpty()) {
			FontUtil.draw(g, "Nenhuma musica. Coloque arquivos na pasta e toque em Refresh.", listX() + 2, listY() + 4, 0xFF8E939B);
		}
		scroll = Mth.clamp(scroll, 0f, maxScroll());
		for (int i = 0; i < tr.size(); i++) {
			int ry = listY() + i * ROW - Math.round(scroll);
			if (ry + ROW < listY() || ry > listY() + listH()) continue;
			boolean cur = i == MusicPlayer.current;
			boolean hov = RenderUtil.inside(mx, my, listX(), ry, listW(), ROW) && my >= listY() && my < listY() + listH();
			if (cur) RenderUtil.roundRect(g, listX(), ry, listW(), ROW - 1, 4, 0xFF1D2025);
			else if (hov) RenderUtil.roundRect(g, listX(), ry, listW(), ROW - 1, 4, 0xFF14171B);
			String nm = MusicPlayer.nameOf(tr.get(i));
			String mark = cur && MusicPlayer.isPlaying() ? "> " : "  ";
			FontUtil.draw(g, ModuleRow.fit(mark + nm, listW() - 8), listX() + 4, ry + (ROW - fh) / 2, cur ? 0xFFFFFFFF : 0xFFA0A5AD);
		}
		g.disableScissor();

		// agora tocando
		String now;
		int nc = 0xFFE2E4E8;
		if (!MusicPlayer.error.isEmpty()) { now = MusicPlayer.error; nc = 0xFFE08080; }
		else if (MusicPlayer.loading) now = "Carregando...";
		else if (MusicPlayer.isActive() && MusicPlayer.current >= 0 && MusicPlayer.current < tr.size()) now = MusicPlayer.nameOf(tr.get(MusicPlayer.current));
		else now = "Nada tocando";
		FontUtil.draw(g, ModuleRow.fit(now, ww - 20), wx + 10, barY() - fh - 4, nc);

		// progresso
		float dur = MusicPlayer.duration(), pos = MusicPlayer.position();
		float frac = dur > 0 ? Mth.clamp(pos / dur, 0f, 1f) : 0f;
		RenderUtil.roundRect(g, listX(), barY(), listW(), 4, 2, 0xFF2A2E35);
		if (frac > 0) RenderUtil.roundRect(g, listX(), barY(), Math.max(4, Math.round(listW() * frac)), 4, 2, 0xFFE2E4E8);
		String tm = fmt(pos) + " / " + fmt(dur);
		FontUtil.draw(g, tm, wx + ww - 10 - FontUtil.width(tm), barY() + 7, 0xFF6B7078);

		// botoes
		int x = listX(), y = btnY();
		x = btn(g, x, y, "<<", false, mx, my);
		x = btn(g, x, y, MusicPlayer.isPlaying() ? "||" : ">", false, mx, my);
		x = btn(g, x, y, ">>", false, mx, my);
		x = btn(g, x, y, "[]", false, mx, my);
		x = btn(g, x, y, "Shuf", MusicPlayer.shuffle, mx, my);
		x = btn(g, x, y, "Rep", MusicPlayer.repeat, mx, my);
		FontUtil.draw(g, "Vol", volX() - FontUtil.width("Vol") - 6, y + (14 - fh) / 2, 0xFF8E939B);
		RenderUtil.roundRect(g, volX(), y + 5, 70, 4, 2, 0xFF2A2E35);
		RenderUtil.roundRect(g, volX(), y + 5, Math.max(3, Math.round(70 * MusicPlayer.volume)), 4, 2, 0xFFE2E4E8);
		int kx = volX() + Math.round(70 * MusicPlayer.volume) - 3;
		RenderUtil.roundRect(g, kx, y + 3, 6, 8, 3, 0xFFFFFFFF);
	}

	private static String fmt(float s) {
		int t = Math.max(0, Math.round(s));
		return (t / 60) + ":" + String.format("%02d", t % 60);
	}

	private int btnW(String t) { return Math.max(22, FontUtil.width(t) + 12); }

	private int btn(GuiGraphicsExtractor g, int x, int y, String t, boolean on, int mx, int my) {
		int w = btnW(t);
		button(g, x, y, w, 14, t, on, mx, my);
		return x + w + 4;
	}

	private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String t, boolean on, int mx, int my) {
		boolean hov = RenderUtil.inside(mx, my, x, y, w, h);
		RenderUtil.roundRect(g, x, y, w, h, 5, on ? 0xFFE2E4E8 : (hov ? 0xFF3A3F47 : 0xFF2A2E35));
		RenderUtil.roundRect(g, x + 1, y + 1, w - 2, h - 2, 4, on ? 0xFF1D2025 : (hov ? 0xFF1B1F25 : 0xFF111317));
		FontUtil.draw(g, t, x + (w - FontUtil.width(t)) / 2, y + (h - FontUtil.height()) / 2, on ? 0xFFFFFFFF : 0xFFC4C8CE);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		layout();
		double mx = click.x(), my = click.y();
		String rf = "Refresh";
		int rw = FontUtil.width(rf) + 10;
		if (RenderUtil.inside(mx, my, wx + ww - 10 - rw, wy + 7, rw, 14)) { MusicPlayer.refresh(); return true; }

		int x = listX(), y = btnY();
		String[] names = {"<<", MusicPlayer.isPlaying() ? "||" : ">", ">>", "[]", "Shuf", "Rep"};
		for (int i = 0; i < names.length; i++) {
			int w = btnW(names[i]);
			if (RenderUtil.inside(mx, my, x, y, w, 14)) {
				switch (i) {
					case 0 -> MusicPlayer.prev();
					case 1 -> MusicPlayer.togglePause();
					case 2 -> MusicPlayer.next(false);
					case 3 -> MusicPlayer.stop();
					case 4 -> MusicPlayer.shuffle = !MusicPlayer.shuffle;
					default -> MusicPlayer.repeat = !MusicPlayer.repeat;
				}
				return true;
			}
			x += w + 4;
		}
		if (RenderUtil.inside(mx, my, volX() - 4, y, 78, 14)) {
			volDrag = true;
			MusicPlayer.setVolume((float) ((mx - volX()) / 70.0));
			return true;
		}
		if (RenderUtil.inside(mx, my, listX(), barY() - 3, listW(), 10)) {
			barDrag = true;
			MusicPlayer.seek((float) ((mx - listX()) / listW()));
			return true;
		}
		if (RenderUtil.inside(mx, my, listX(), listY(), listW(), listH())) {
			int i = (int) ((my - listY() + scroll) / ROW);
			if (i >= 0 && i < MusicPlayer.tracks().size()) MusicPlayer.play(i);
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (volDrag) {
			MusicPlayer.setVolume((float) ((click.x() - volX()) / 70.0));
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (barDrag) MusicPlayer.seek((float) ((click.x() - listX()) / listW()));
		volDrag = false;
		barDrag = false;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scroll = Mth.clamp(scroll - (float) verticalAmount * ROW * 2, 0f, maxScroll());
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
