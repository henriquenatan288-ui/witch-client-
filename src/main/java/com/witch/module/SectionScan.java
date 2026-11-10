package com.witch.module;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import java.util.function.Predicate;

/**
 * Varredura de blocos por secao de chunk: pula secoes so de ar ou sem nenhum bloco desejado
 * (a paleta responde sem olhar bloco a bloco). Bem mais barato que getBlockState em cada posicao.
 */
final class SectionScan {
	private SectionScan() {}

	interface Hit {
		void at(int x, int y, int z, BlockState state);
	}

	static void scan(ClientLevel level, int cx, int cy, int cz, int r, int vr, Predicate<BlockState> wants, Hit hit) {
		int x0 = cx - r, x1 = cx + r, z0 = cz - r, z1 = cz + r, y0 = cy - vr, y1 = cy + vr;
		for (int chx = x0 >> 4; chx <= x1 >> 4; chx++) {
			for (int chz = z0 >> 4; chz <= z1 >> 4; chz++) {
				LevelChunk chunk = level.getChunkSource().getChunk(chx, chz, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				var sections = chunk.getSections();
				int minSec = chunk.getMinSectionY();
				for (int i = 0; i < sections.length; i++) {
					var sec = sections[i];
					if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(wants)) continue;
					int baseY = (minSec + i) * 16;
					int ya = Math.max(y0, baseY), yb = Math.min(y1, baseY + 15);
					int xa = Math.max(x0, chx * 16), xb = Math.min(x1, chx * 16 + 15);
					int za = Math.max(z0, chz * 16), zb = Math.min(z1, chz * 16 + 15);
					for (int x = xa; x <= xb; x++) {
						for (int y = ya; y <= yb; y++) {
							for (int z = za; z <= zb; z++) {
								BlockState st = sec.getBlockState(x & 15, y & 15, z & 15);
								if (wants.test(st)) hit.at(x, y, z, st);
							}
						}
					}
				}
			}
		}
	}
}
