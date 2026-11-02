package com.witch.module;

import com.witch.gui.FontUtil;
import com.witch.gui.RenderUtil;
import com.witch.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mostra a porcentagem da bateria do PC/notebook no HUD.
 * Posicao: 0 TL, 1 TR, 2 BL, 3 BR.
 * Atualiza em background a cada ~5s (Linux sysfs / Windows powercfg / macOS pmset).
 */
public class BatteryModule extends Module {
	private static BatteryModule instance;

	/** 0=Top-Left 1=Top-Right 2=Bottom-Left 3=Bottom-Right */
	private final IntSetting position = add(new IntSetting("Position", 0, 3, 1, ""));

	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "witch-battery");
		t.setDaemon(true);
		return t;
	});

	private final AtomicInteger percent = new AtomicInteger(-1);
	private final AtomicReference<String> status = new AtomicReference<>("");
	private int tickCooldown = 0;
	private volatile boolean querying = false;

	public BatteryModule() {
		super("Battery %", Category.UTILITY);
		instance = this;
	}

	@Override
	protected void onEnable() {
		tickCooldown = 0;
		requestUpdate();
	}

	@Override
	public void onTick() {
		if (--tickCooldown > 0) return;
		tickCooldown = 100; // ~5s
		requestUpdate();
	}

	private void requestUpdate() {
		if (querying) return;
		querying = true;
		WORKER.execute(() -> {
			try {
				int[] r = readBattery();
				percent.set(r[0]);
				status.set(r[1] == 1 ? "CHG" : (r[1] == 2 ? "AC" : ""));
			} finally {
				querying = false;
			}
		});
	}

	/**
	 * @return {percent 0-100 or -1, chargingFlag 0=unknown/discharging 1=charging 2=full/ac}
	 */
	private static int[] readBattery() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		try {
			if (os.contains("win")) return readWindows();
			if (os.contains("mac")) return readMac();
			return readLinux();
		} catch (Throwable t) {
			return new int[]{-1, 0};
		}
	}

	private static int[] readLinux() throws Exception {
		Path base = Path.of("/sys/class/power_supply");
		if (!Files.isDirectory(base)) return new int[]{-1, 0};
		int best = -1;
		int chg = 0;
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
			for (Path p : ds) {
				String name = p.getFileName().toString().toUpperCase(Locale.ROOT);
				if (!name.startsWith("BAT") && !name.contains("BATTERY")) continue;
				Path cap = p.resolve("capacity");
				if (!Files.isRegularFile(cap)) continue;
				int pct = Integer.parseInt(Files.readString(cap).trim());
				best = Math.max(best, Math.max(0, Math.min(100, pct)));
				Path st = p.resolve("status");
				if (Files.isRegularFile(st)) {
					String s = Files.readString(st).trim().toLowerCase(Locale.ROOT);
					if (s.contains("charg")) chg = 1;
					else if (s.contains("full")) chg = 2;
				}
			}
		}
		return new int[]{best, chg};
	}

	private static int[] readWindows() throws Exception {
		// powercfg /batteryreport e lento; WMIC e deprecated mas ainda funciona em muitos PCs
		ProcessBuilder pb = new ProcessBuilder(
				"cmd.exe", "/c",
				"wmic path Win32_Battery get EstimatedChargeRemaining,BatteryStatus /format:list"
		);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		int pct = -1;
		int chg = 0;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("EstimatedChargeRemaining=")) {
					String v = line.substring("EstimatedChargeRemaining=".length()).trim();
					if (!v.isEmpty()) pct = Math.max(0, Math.min(100, Integer.parseInt(v)));
				} else if (line.startsWith("BatteryStatus=")) {
					String v = line.substring("BatteryStatus=".length()).trim();
					if (!v.isEmpty()) {
						int code = Integer.parseInt(v);
						// 1=discharging 2=AC 6-9 often charging variants
						if (code == 2) chg = 2;
						else if (code >= 6 && code <= 9) chg = 1;
					}
				}
			}
		}
		p.waitFor();
		if (pct < 0) {
			// fallback PowerShell (Windows 10+)
			ProcessBuilder ps = new ProcessBuilder(
					"powershell.exe", "-NoProfile", "-Command",
					"(Get-CimInstance Win32_Battery).EstimatedChargeRemaining"
			);
			ps.redirectErrorStream(true);
			Process p2 = ps.start();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p2.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.matches("\\d+")) {
						pct = Math.max(0, Math.min(100, Integer.parseInt(line)));
						break;
					}
				}
			}
			p2.waitFor();
		}
		return new int[]{pct, chg};
	}

	private static int[] readMac() throws Exception {
		ProcessBuilder pb = new ProcessBuilder("pmset", "-g", "batt");
		pb.redirectErrorStream(true);
		Process p = pb.start();
		int pct = -1;
		int chg = 0;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				// e.g. " -InternalBattery-0 (id=...) 78%; charging; ..."
				int idx = line.indexOf('%');
				if (idx > 0) {
					int start = idx - 1;
					while (start >= 0 && Character.isDigit(line.charAt(start))) start--;
					String num = line.substring(start + 1, idx).trim();
					if (!num.isEmpty()) pct = Math.max(0, Math.min(100, Integer.parseInt(num)));
				}
				String low = line.toLowerCase(Locale.ROOT);
				if (low.contains("charging")) chg = 1;
				else if (low.contains("charged") || low.contains("ac attached")) chg = 2;
			}
		}
		p.waitFor();
		return new int[]{pct, chg};
	}

	public static void drawHud(GuiGraphicsExtractor g) {
		if (instance == null || !instance.isEnabled()) return;
		int pct = instance.percent.get();
		String st = instance.status.get();
		String text;
		if (pct < 0) {
			text = "BAT --%";
		} else if (st != null && !st.isEmpty()) {
			text = "BAT " + pct + "% " + st;
		} else {
			text = "BAT " + pct + "%";
		}

		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int tw = FontUtil.width(text);
		int th = FontUtil.height();
		int pad = 4;
		int bw = tw + pad * 2;
		int bh = th + pad * 2;

		int pos = instance.position.get();
		int x, y;
		switch (pos) {
			case 0 -> { // top left (abaixo da watermark)
				x = 5;
				y = 40;
			}
			case 2 -> { // bottom left
				x = 5;
				y = sh - bh - 5;
			}
			case 3 -> { // bottom right
				x = sw - bw - 5;
				y = sh - bh - 5;
			}
			default -> { // top right
				x = sw - bw - 5;
				y = 5;
			}
		}

		int col;
		if (pct < 0) col = 0xFF9A9A9A;
		else if (pct <= 15) col = 0xFFEF4444;
		else if (pct <= 30) col = 0xFFFB923C;
		else if (pct <= 60) col = 0xFFFACC15;
		else col = 0xFF34D399;

		RenderUtil.roundRect(g, x, y, bw, bh, 4, 0xB0181428);
		RenderUtil.roundRect(g, x, y, 3, bh, 1, col);
		FontUtil.draw(g, text, x + pad, y + pad, 0xFFFFFFFF);
	}
}
