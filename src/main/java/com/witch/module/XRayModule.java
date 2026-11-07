package com.witch.module;

import com.witch.setting.IntSetting;
import com.witch.setting.OreSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XRayModule extends Module {
	private static final int MAX_BLOCKS = 150;
	private static final long RESCAN_TICKS = 200L;
	private static final long SCAN_BUDGET_NS = 3_000_000L;
	private static final int MAX_SCANS_PER_TICK = 24;

	private static XRayModule instance;
	private static volatile List<int[]> blocks = List.of();

	private final Map<Block, OreSetting> byBlock = new HashMap<>();
	private final List<OreSetting> oreList = new ArrayList<>();
	private final Map<Long, List<int[]>> cache = new HashMap<>();
	private final Map<Long, Long> lastScan = new HashMap<>();
	private int[][] offsets = null;
	private int offsetsRadius = -1;

	private final IntSetting range;
	private final IntSetting fill;
	private final IntSetting exposedOnly;

	private int cooldown = 0;

	public XRayModule() {
		super("X-Ray Hologram", Category.VISUALS);
		instance = this;

		range = add(new IntSetting("Range (chunks)", 1, 34, 2, ""));
		fill = add(new IntSetting("Fill opacity", 0, 50, 15, "%"));
		exposedOnly = add(new IntSetting("Exposed only (0/1)", 0, 1, 1, ""));

		// nome, ligado por padrao, cor (indice da paleta), blocos
		ore("Diamond", true, 1, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
		ore("Emerald", true, 2, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
		ore("Gold", true, 3, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
		ore("Lapis", true, 0, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
		ore("Redstone", true, 5, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
		ore("Ancient Debris", true, 4, Blocks.ANCIENT_DEBRIS);
		ore("Iron", false, 6, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
		ore("Copper", false, 4, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
		ore("Coal", false, 7, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
		ore("Quartz", false, 7, Blocks.NETHER_QUARTZ_ORE);
	}

	private void ore(String name, boolean on, int color, Block... list) {
		OreSetting s = add(new OreSetting(name, on, color));
		int idx = oreList.size();
		oreList.add(s);
		for (Block b : list) {
			byBlock.put(b, s);
		}
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** Cada item: {x, y, z, corARGB}. */
	public static List<int[]> blocks() {
		return blocks;
	}

	public static float fillAlpha() {
		return instance == null ? 0f : instance.fill.get() / 100f;
	}

	@Override
	protected void onEnable() {
		cooldown = 0;
	}

	@Override
	protected void onDisable() {
		blocks = List.of();
		cache.clear();
		lastScan.clear();
	}

	/** Exposto = algum lado encosta em ar, agua, lava ou outro bloco que nao oculta (vidro, folhas...). */
	private static boolean exposed(net.minecraft.client.multiplayer.ClientLevel level, BlockPos p) {
		BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
		for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
			n.set(p.getX() + d.getStepX(), p.getY() + d.getStepY(), p.getZ() + d.getStepZ());
			// vizinho em chunk nao carregado vem como ar: nao pode contar como "exposto" (piscava ao carregar)
			if (!level.hasChunkAt(n)) continue;
			BlockState st = level.getBlockState(n);
			if (st.isAir() || !st.getFluidState().isEmpty() || !st.canOcclude()) return true;
		}
		return false;
	}

	private void buildOffsets(int r) {
		List<int[]> l = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) l.add(new int[]{dx, dz});
		}
		l.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
		offsets = l.toArray(new int[0][]);
		offsetsRadius = r;
	}

	/** Escaneia so as secoes da chunk que tem algum minerio (a paleta da secao responde rapido). */
	private List<int[]> scanChunk(net.minecraft.client.multiplayer.ClientLevel level,
			net.minecraft.world.level.chunk.LevelChunk chunk, int cx, int cz) {
		List<int[]> out = new ArrayList<>();
		var sections = chunk.getSections();
		int minSec = chunk.getMinSectionY();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < sections.length; i++) {
			var sec = sections[i];
			if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(st -> byBlock.containsKey(st.getBlock()))) continue;
			int baseY = (minSec + i) * 16;
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						Block b = sec.getBlockState(x, y, z).getBlock();
						OreSetting os = byBlock.get(b);
						if (os == null) continue;
						pos.set(cx * 16 + x, baseY + y, cz * 16 + z);
						if (exposedOnly.get() == 1 && !exposed(level, pos)) continue;
						out.add(new int[]{pos.getX(), pos.getY(), pos.getZ(), oreList.indexOf(os)});
					}
				}
			}
		}
		return out;
	}

	@Override
	public void onTick() {
		Minecraft mc = Minecraft.getInstance();
		var level = mc.level;
		if (mc.player == null || level == null) {
			blocks = List.of();
			return;
		}
		int r = range.get();
		if (offsets == null || offsetsRadius != r) {
			buildOffsets(r);
			lastScan.clear();
			cache.clear();
		}
		BlockPos center = mc.player.blockPosition();
		int pcx = center.getX() >> 4, pcz = center.getZ() >> 4;
		long gt = level.getGameTime();

		long t0 = System.nanoTime();
		int scanned = 0;
		for (int[] off : offsets) {
			int cx = pcx + off[0], cz = pcz + off[1];
			long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
			Long prev = lastScan.get(key);
			if (prev != null && gt - prev < RESCAN_TICKS) continue;
			var chunk = level.getChunkSource().getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
			if (chunk == null) continue;
			lastScan.put(key, gt);
			List<int[]> res = scanChunk(level, chunk, cx, cz);
			if (res.isEmpty()) cache.remove(key); else cache.put(key, res);
			scanned++;
			if (scanned >= MAX_SCANS_PER_TICK || System.nanoTime() - t0 > SCAN_BUDGET_NS) break;
		}

		if (--cooldown > 0) return;
		cooldown = 5;
		cache.keySet().removeIf(k -> {
			int kx = (int) (k >> 32), kz = (int) (long) k;
			return Math.abs(kx - pcx) > r + 1 || Math.abs(kz - pcz) > r + 1;
		});
		if (lastScan.size() > 12000) lastScan.clear();
		int cx0 = center.getX(), cy0 = center.getY(), cz0 = center.getZ();
		List<int[]> found = new ArrayList<>();
		for (List<int[]> l : cache.values()) {
			for (int[] b : l) {
				OreSetting os = oreList.get(b[3]);
				if (os.enabled()) found.add(new int[]{b[0], b[1], b[2], os.color()});
			}
		}
		found.sort(Comparator.comparingLong(b -> {
			long ddx = b[0] - cx0, ddy = b[1] - cy0, ddz = b[2] - cz0;
			return ddx * ddx + ddy * ddy + ddz * ddz;
		}));
		if (found.size() > MAX_BLOCKS) found = new ArrayList<>(found.subList(0, MAX_BLOCKS));
		blocks = found;
	}
}
