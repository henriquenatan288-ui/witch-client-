package com.witch.test;

import com.witch.config.Config;
import com.witch.gui.ClickGuiScreen;
import com.witch.gui.MusicScreen;
import com.witch.module.Category;
import com.witch.module.Module;
import com.witch.module.ModuleManager;
import com.witch.music.MusicPlayer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Teste unico: tela de entrada, titulo, ClickGUI, protecao de modulos, config e player de musica. */
public class AllTest implements FabricClientGameTest {
	private static void log(String s) { System.out.println(s); }

	private static void ff(Path out, String... extra) {
		try {
			java.util.List<String> cmd = new java.util.ArrayList<>(java.util.List.of("ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi"));
			cmd.addAll(java.util.List.of(extra));
			cmd.add(out.toString());
			Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			String o = new String(p.getInputStream().readAllBytes());
			p.waitFor();
			log("MUSICTEST gerou " + out.getFileName() + " tam=" + Files.size(out) + (o.isBlank() ? "" : " ffmpeg=" + o.trim()));
		} catch (Throwable t) {
			log("MUSICTEST ffmpeg falhou para " + out.getFileName() + ": " + t);
		}
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		c.waitTicks(100);
		c.takeScreenshot("01-enter");
		c.runOnClient(mc -> mc.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(10, 10, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false));
		c.waitTicks(30);
		c.takeScreenshot("02-drag");
		c.waitTicks(140);
		c.takeScreenshot("03-title");

		c.runOnClient(mc -> mc.gui.setScreen(new ClickGuiScreen()));
		c.waitTicks(40);
		c.takeScreenshot("04-clickgui");

		// protecao de modulos
		c.runOnClient(mc -> {
			Module boom = new Module("BoomTest", Category.UTILITY) {
				@Override public void onTick() { throw new RuntimeException("erro de proposito"); }
			};
			ModuleManager.all().add(boom);
			boom.setEnabled(true);
			try {
				ModuleManager.tick();
				log("GUARDTEST " + (!boom.isEnabled() ? "ok: modulo com erro foi desligado e o jogo seguiu" : "FALHOU: modulo continuou ligado"));
			} catch (Throwable t) {
				log("GUARDTEST FALHOU: excecao vazou " + t);
			}
			ModuleManager.all().remove(boom);
		});

		// config: salvar, corromper, carregar
		c.runOnClient(mc -> {
			try {
				Config.save();
				Path f = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("witch.json");
				log("CONFIGTEST salvou tam=" + Files.size(f) + " tmpSobrando=" + Files.exists(f.resolveSibling("witch.json.tmp")));
				Files.writeString(f, "{ isto nao e json valido");
				Config.load();
				log("CONFIGTEST arquivo corrompido: load nao lancou erro");
				Files.writeString(f, "{\"Fly\": {\"enabled\": \"abc\", \"settings\": {\"x\": \"y\"}}, \"Fullbright\": {\"enabled\": false}}");
				Config.load();
				log("CONFIGTEST valor invalido: load nao lancou erro");
			} catch (Throwable t) {
				log("CONFIGTEST FALHOU " + t);
			}
		});

		// arquivos de musica de teste
		Path dir = MusicPlayer.dir();
		try { Files.createDirectories(dir); } catch (Exception e) { log("MUSICTEST pasta: " + e); }
		ff(dir.resolve("a-tom.mp3"), "-i", "sine=frequency=440:duration=12:sample_rate=44100", "-ac", "2", "-b:a", "192k");
		ff(dir.resolve("b-tom.ogg"), "-i", "sine=frequency=550:duration=12:sample_rate=44100", "-ac", "2", "-c:a", "libvorbis");
		ff(dir.resolve("c-tom.wav"), "-i", "sine=frequency=660:duration=12:sample_rate=44100", "-ac", "1");
		ff(dir.resolve("d-grande.mp3"), "-i", "sine=frequency=220:duration=134:sample_rate=48000", "-ac", "2", "-b:a", "320k");

		c.runOnClient(mc -> { MusicPlayer.refresh(); mc.gui.setScreen(new MusicScreen()); });
		c.waitTicks(20);
		c.takeScreenshot("05-music-lista");

		AtomicInteger n = new AtomicInteger();
		c.runOnClient(mc -> n.set(MusicPlayer.tracks().size()));
		log("MUSICTEST faixas=" + n.get());
		for (int i = 0; i < n.get(); i++) {
			final int k = i;
			c.runOnClient(mc -> MusicPlayer.play(k));
			AtomicBoolean loading = new AtomicBoolean(true);
			for (int w = 0; w < 400 && loading.get(); w++) {
				c.waitTicks(2);
				c.runOnClient(mc -> loading.set(MusicPlayer.loading));
			}
			c.waitTicks(60);
			final String[] snap = new String[1];
			c.runOnClient(mc -> snap[0] = "ativo=" + MusicPlayer.isActive() + " tocando=" + MusicPlayer.isPlaying()
					+ " pos=" + MusicPlayer.position() + " dur=" + MusicPlayer.duration() + " info=[" + MusicPlayer.info + "] erro=[" + MusicPlayer.error + "]");
			log("MUSICTEST faixa " + k + " " + snap[0]);
			if (k == 0 || k == n.get() - 1) c.takeScreenshot("06-music-" + k);
			if (k == 0) {
				c.runOnClient(mc -> MusicPlayer.togglePause());
				c.waitTicks(10);
				final String[] s2 = new String[1];
				c.runOnClient(mc -> s2[0] = "pausado=" + MusicPlayer.paused + " pos=" + MusicPlayer.position());
				c.waitTicks(20);
				final String[] s3 = new String[1];
				c.runOnClient(mc -> s3[0] = "pos depois=" + MusicPlayer.position());
				log("MUSICTEST pausa " + s2[0] + " " + s3[0]);
				c.runOnClient(mc -> MusicPlayer.togglePause());
			}
		}
		c.runOnClient(mc -> MusicPlayer.stop());
		log("MUSICTEST fim");
	}
}
