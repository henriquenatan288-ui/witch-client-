package com.witch.music;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libc.LibCStdlib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Toca musicas da pasta "musicas" (mp3, ogg, wav) pelo OpenAL do proprio jogo.
 * A decodificacao roda em outra thread; todas as chamadas de audio acontecem na thread do cliente (tick()).
 */
public final class MusicPlayer {
	private MusicPlayer() {}

	private record Pcm(short[] data, int channels, int rate) {}

	private static final int MAX_SHORTS = 40_000_000; // ~7 min estereo; protege a memoria
	private static final long MAX_BYTES = 120L * 1024 * 1024;
	private static final Random RNG = new Random();
	private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "witch-music-decode");
		t.setDaemon(true);
		return t;
	});

	private static volatile Pcm pending;
	private static volatile int loadToken = 0;
	public static volatile boolean loading = false;
	public static volatile String error = "";

	private static List<Path> tracks = List.of();
	public static int current = -1;
	public static boolean shuffle = false, repeat = false, paused = false;
	public static float volume = 0.6f;

	private static int source = 0, buffer = 0;
	private static float duration = 0f;
	private static boolean active = false;

	public static Path dir() {
		return FabricLoader.getInstance().getGameDir().resolve("musicas");
	}

	public static List<Path> tracks() {
		return tracks;
	}

	public static String nameOf(Path p) {
		String n = p.getFileName().toString();
		int i = n.lastIndexOf('.');
		return i > 0 ? n.substring(0, i) : n;
	}

	/** Relê a pasta (cria se nao existir). */
	public static void refresh() {
		Path cur = current >= 0 && current < tracks.size() ? tracks.get(current) : null;
		try {
			Files.createDirectories(dir());
			try (Stream<Path> s = Files.list(dir())) {
				List<Path> l = new ArrayList<>();
				s.filter(Files::isRegularFile).forEach(p -> {
					String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
					if (n.endsWith(".mp3") || n.endsWith(".ogg") || n.endsWith(".wav")) l.add(p);
				});
				l.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));
				tracks = l;
			}
		} catch (Exception e) {
			error = "Nao consegui ler a pasta: " + e.getMessage();
			tracks = List.of();
		}
		current = cur == null ? -1 : tracks.indexOf(cur);
	}

	public static boolean isActive() { return active; }
	public static boolean isPlaying() { return active && !paused; }
	public static float duration() { return duration; }

	public static float position() {
		if (!active) return 0f;
		try {
			return AL10.alGetSourcef(source, org.lwjgl.openal.AL11.AL_SEC_OFFSET);
		} catch (Throwable t) {
			return 0f;
		}
	}

	public static void play(int idx) {
		if (idx < 0 || idx >= tracks.size()) return;
		stopAL();
		current = idx;
		paused = false;
		error = "";
		loading = true;
		int token = ++loadToken;
		Path p = tracks.get(idx);
		POOL.submit(() -> {
			try {
				Pcm pcm = decode(p);
				if (token == loadToken) pending = pcm;
			} catch (Throwable t) {
				if (token == loadToken) {
					error = "Erro ao ler " + p.getFileName() + ": " + t.getMessage();
					loading = false;
				}
			}
		});
	}

	public static void next(boolean auto) {
		if (tracks.isEmpty()) return;
		int idx;
		if (shuffle && tracks.size() > 1) {
			do { idx = RNG.nextInt(tracks.size()); } while (idx == current);
		} else {
			idx = current + 1;
		}
		if (idx >= tracks.size()) {
			if (auto && !repeat) { stopAL(); return; }
			idx = 0;
		}
		play(idx);
	}

	public static void prev() {
		if (tracks.isEmpty()) return;
		play(current <= 0 ? tracks.size() - 1 : current - 1);
	}

	public static void togglePause() {
		if (!active) {
			if (current >= 0) play(current); else if (!tracks.isEmpty()) play(0);
			return;
		}
		try {
			if (paused) AL10.alSourcePlay(source); else AL10.alSourcePause(source);
			paused = !paused;
		} catch (Throwable t) {
			error = "Audio: " + t.getMessage();
		}
	}

	public static void stop() {
		loadToken++;
		pending = null;
		loading = false;
		stopAL();
	}

	public static void setVolume(float v) {
		volume = Math.max(0f, Math.min(1f, v));
		if (active) {
			try { AL10.alSourcef(source, AL10.AL_GAIN, volume); } catch (Throwable ignored) { }
		}
	}

	public static void seek(float fraction) {
		if (!active || duration <= 0f) return;
		try { AL10.alSourcef(source, org.lwjgl.openal.AL11.AL_SEC_OFFSET, Math.max(0f, Math.min(0.999f, fraction)) * duration); } catch (Throwable ignored) { }
	}

	/** Chamar todo tick do cliente (thread principal). */
	public static void tick() {
		try {
			Pcm pcm = pending;
			if (pcm != null) {
				pending = null;
				startAL(pcm);
				loading = false;
			}
			if (!active) return;
			int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
			if (state == AL10.AL_STOPPED && !paused) next(true);
		} catch (Throwable t) {
			error = "Audio indisponivel: " + t.getMessage();
			loading = false;
			stopAL();
		}
	}

	private static void startAL(Pcm pcm) {
		try {
			if (pcm.channels() < 1 || pcm.channels() > 2) throw new IllegalStateException("so mono ou estereo");
			int fmt = pcm.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
			ShortBuffer sb = BufferUtils.createShortBuffer(pcm.data().length);
			sb.put(pcm.data()).flip();
			buffer = AL10.alGenBuffers();
			AL10.alBufferData(buffer, fmt, sb, pcm.rate());
			source = AL10.alGenSources();
			AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
			AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
			AL10.alSourcef(source, AL10.AL_GAIN, volume);
			AL10.alSourcePlay(source);
			int err = AL10.alGetError();
			if (err != AL10.AL_NO_ERROR) throw new IllegalStateException("OpenAL erro " + err);
			duration = pcm.data().length / (float) (pcm.channels() * pcm.rate());
			active = true;
			paused = false;
		} catch (Throwable t) {
			error = "Audio indisponivel: " + t.getMessage();
			stopAL();
		}
	}

	private static void stopAL() {
		try {
			if (source != 0) { AL10.alSourceStop(source); AL10.alDeleteSources(source); }
			if (buffer != 0) AL10.alDeleteBuffers(buffer);
		} catch (Throwable ignored) { }
		source = 0;
		buffer = 0;
		active = false;
		paused = false;
		duration = 0f;
	}

	// ---------- decodificacao ----------

	private static Pcm decode(Path p) throws Exception {
		if (Files.size(p) > MAX_BYTES) throw new IOException("arquivo muito grande");
		byte[] d = Files.readAllBytes(p);
		String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
		if (n.endsWith(".mp3")) return decodeMp3(d);
		if (n.endsWith(".ogg")) return decodeOgg(d);
		if (n.endsWith(".wav")) return decodeWav(d);
		throw new IOException("formato nao suportado");
	}

	private static Pcm decodeOgg(byte[] d) throws IOException {
		ByteBuffer mem = BufferUtils.createByteBuffer(d.length);
		mem.put(d).flip();
		try (MemoryStack st = MemoryStack.stackPush()) {
			IntBuffer ch = st.mallocInt(1), rate = st.mallocInt(1);
			ShortBuffer sb = STBVorbis.stb_vorbis_decode_memory(mem, ch, rate);
			if (sb == null) throw new IOException("OGG invalido");
			try {
				if (sb.remaining() > MAX_SHORTS) throw new IOException("musica muito longa");
				short[] out = new short[sb.remaining()];
				sb.get(out);
				return new Pcm(out, ch.get(0), rate.get(0));
			} finally {
				LibCStdlib.free(sb);
			}
		}
	}

	private static Pcm decodeWav(byte[] d) throws IOException {
		if (d.length < 44 || !tag(d, 0, "RIFF") || !tag(d, 8, "WAVE")) throw new IOException("WAV invalido");
		ByteBuffer b = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);
		int pos = 12, fmt = 0, ch = 0, rate = 0, bits = 0, dataOff = -1, dataLen = 0;
		while (pos + 8 <= d.length) {
			int sz = b.getInt(pos + 4), body = pos + 8;
			if (tag(d, pos, "fmt ")) {
				fmt = b.getShort(body) & 0xFFFF;
				ch = b.getShort(body + 2);
				rate = b.getInt(body + 4);
				bits = b.getShort(body + 14);
			} else if (tag(d, pos, "data")) {
				dataOff = body;
				dataLen = Math.min(sz < 0 ? Integer.MAX_VALUE : sz, d.length - body);
				break;
			}
			pos = body + sz + (sz & 1);
			if (sz < 0) break;
		}
		if (dataOff < 0 || (fmt != 1 && fmt != 0xFFFE) || (bits != 16 && bits != 8) || ch < 1 || ch > 2)
			throw new IOException("WAV nao suportado (use PCM 8/16 bits)");
		int n = bits == 16 ? dataLen / 2 : dataLen;
		if (n > MAX_SHORTS) throw new IOException("musica muito longa");
		short[] out = new short[n];
		for (int i = 0; i < n; i++) {
			out[i] = bits == 16 ? b.getShort(dataOff + i * 2) : (short) (((d[dataOff + i] & 0xFF) - 128) << 8);
		}
		return new Pcm(out, ch, rate);
	}

	private static boolean tag(byte[] d, int off, String t) {
		for (int i = 0; i < 4; i++) if (d[off + i] != (byte) t.charAt(i)) return false;
		return true;
	}

	private static Pcm decodeMp3(byte[] d) throws Exception {
		javazoom.jl.decoder.Bitstream bs = new javazoom.jl.decoder.Bitstream(new ByteArrayInputStream(d));
		javazoom.jl.decoder.Decoder dec = new javazoom.jl.decoder.Decoder();
		short[] out = new short[1 << 20];
		int n = 0, ch = 2, rate = 44100;
		try {
			javazoom.jl.decoder.Header h;
			while ((h = bs.readFrame()) != null) {
				javazoom.jl.decoder.SampleBuffer sb = (javazoom.jl.decoder.SampleBuffer) dec.decodeFrame(h, bs);
				ch = sb.getChannelCount();
				rate = sb.getSampleFrequency();
				int len = sb.getBufferLength();
				if (n + len > MAX_SHORTS) throw new IOException("musica muito longa");
				if (n + len > out.length) out = Arrays.copyOf(out, Math.max(out.length * 2, n + len));
				System.arraycopy(sb.getBuffer(), 0, out, n, len);
				n += len;
				bs.closeFrame();
			}
		} finally {
			try { bs.close(); } catch (Exception ignored) { }
		}
		if (n == 0) throw new IOException("MP3 vazio ou invalido");
		return new Pcm(Arrays.copyOf(out, n), ch, rate);
	}
}
