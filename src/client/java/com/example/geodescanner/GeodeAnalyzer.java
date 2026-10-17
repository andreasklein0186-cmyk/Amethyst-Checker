package com.example.geodescanner;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Findet Amethystgeoden in den geladenen Chunks und bewertet, wie weit die Amethysten
 * darin gewachsen sind.
 *
 * Ablauf:
 *  1. Alle Amethyst-Blöcke (Block, Budding, kleine/mittlere/große Knospe, Cluster) in den
 *     geladenen Chunks um den Spieler einsammeln. Chunk-Sektionen ohne Amethyst werden über
 *     die Palette übersprungen, das ist schnell.
 *  2. Nahe beieinanderliegende Blöcke zu einer Geode zusammenfassen (Flood-Fill).
 *  3. Pro Geode zählen, wie viele Blöcke in welcher Wachstumsstufe sind.
 */
public final class GeodeAnalyzer {
	private GeodeAnalyzer() {}

	private static final byte NONE = 0;
	private static final byte AMETHYST_BLOCK = 1;
	private static final byte BUDDING = 2;
	private static final byte SMALL_BUD = 3;
	private static final byte MEDIUM_BUD = 4;
	private static final byte LARGE_BUD = 5;
	private static final byte CLUSTER = 6;

	/** Ergebnis der Analyse einer einzelnen Geode. */
	public record Geode(
			BlockPos center,
			AABB bounds,
			int amethystBlocks,
			int buddingBlocks,
			int smallBuds,
			int mediumBuds,
			int largeBuds,
			int clusters,
			List<BlockPos> clusterPositions
	) {
		/** Sehr groß gewachsen = viele voll ausgewachsene Cluster. */
		public boolean isBig() {
			return clusters >= GeodeScannerConfig.BIG_CLUSTER_THRESHOLD;
		}
	}

	private static byte classify(BlockState state) {
		Block block = state.getBlock();
		if (block == Blocks.AMETHYST_CLUSTER) return CLUSTER;
		if (block == Blocks.LARGE_AMETHYST_BUD) return LARGE_BUD;
		if (block == Blocks.MEDIUM_AMETHYST_BUD) return MEDIUM_BUD;
		if (block == Blocks.SMALL_AMETHYST_BUD) return SMALL_BUD;
		if (block == Blocks.BUDDING_AMETHYST) return BUDDING;
		if (block == Blocks.AMETHYST_BLOCK) return AMETHYST_BLOCK;
		return NONE;
	}

	private static final Predicate<BlockState> IS_AMETHYST_FAMILY = state -> classify(state) != NONE;

	/** Scannt alle geladenen Chunks im Radius und gibt alle gefundenen Geoden zurück. */
	public static List<Geode> scan(ClientLevel level, BlockPos origin, int chunkRadius) {
		Map<Long, Byte> blocks = collectAmethystBlocks(level, origin, chunkRadius);
		return cluster(blocks);
	}

	private static Map<Long, Byte> collectAmethystBlocks(ClientLevel level, BlockPos origin, int chunkRadius) {
		Map<Long, Byte> blocks = new HashMap<>();
		int centerChunkX = origin.getX() >> 4;
		int centerChunkZ = origin.getZ() >> 4;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
			for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;

				LevelChunkSection[] sections = chunk.getSections();
				for (int i = 0; i < sections.length; i++) {
					LevelChunkSection section = sections[i];
					if (section == null || section.hasOnlyAir()) continue;
					// Schneller Vorabtest über die Block-Palette der Sektion.
					if (!section.maybeHas(IS_AMETHYST_FAMILY)) continue;

					int baseX = cx << 4;
					int baseY = level.getMinY() + (i << 4);
					int baseZ = cz << 4;

					for (int x = 0; x < 16; x++) {
						for (int y = 0; y < 16; y++) {
							for (int z = 0; z < 16; z++) {
								byte kind = classify(section.getBlockState(x, y, z));
								if (kind == NONE) continue;
								pos.set(baseX + x, baseY + y, baseZ + z);
								blocks.put(pos.asLong(), kind);
							}
						}
					}
				}
			}
		}
		return blocks;
	}

	private static List<Geode> cluster(Map<Long, Byte> blocks) {
		List<Geode> result = new ArrayList<>();
		Set<Long> visited = new HashSet<>();
		int link = GeodeScannerConfig.LINK_DISTANCE;

		for (long start : blocks.keySet()) {
			if (!visited.add(start)) continue;

			Stats stats = new Stats();
			ArrayDeque<Long> queue = new ArrayDeque<>();
			queue.add(start);

			while (!queue.isEmpty()) {
				long current = queue.poll();
				stats.add(current, blocks.get(current));

				int cx = BlockPos.getX(current);
				int cy = BlockPos.getY(current);
				int cz = BlockPos.getZ(current);
				for (int dx = -link; dx <= link; dx++) {
					for (int dy = -link; dy <= link; dy++) {
						for (int dz = -link; dz <= link; dz++) {
							if (dx == 0 && dy == 0 && dz == 0) continue;
							long neighbor = BlockPos.asLong(cx + dx, cy + dy, cz + dz);
							if (blocks.containsKey(neighbor) && visited.add(neighbor)) {
								queue.add(neighbor);
							}
						}
					}
				}
			}

			// Einzelne verirrte Amethystblöcke (z. B. aus Mods/Strukturen) ignorieren.
			if (stats.amethystBlocks + stats.budding >= GeodeScannerConfig.MIN_GEODE_CORE_BLOCKS) {
				result.add(stats.toGeode());
			}
		}
		return result;
	}

	/** Sammelt die Zahlen einer Geode während des Flood-Fills. */
	private static final class Stats {
		int amethystBlocks, budding, small, medium, large, clusters;
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		final List<BlockPos> clusterPositions = new ArrayList<>();

		void add(long packedPos, byte kind) {
			int x = BlockPos.getX(packedPos);
			int y = BlockPos.getY(packedPos);
			int z = BlockPos.getZ(packedPos);
			minX = Math.min(minX, x); maxX = Math.max(maxX, x);
			minY = Math.min(minY, y); maxY = Math.max(maxY, y);
			minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);

			switch (kind) {
				case AMETHYST_BLOCK -> amethystBlocks++;
				case BUDDING -> budding++;
				case SMALL_BUD -> small++;
				case MEDIUM_BUD -> medium++;
				case LARGE_BUD -> large++;
				case CLUSTER -> {
					clusters++;
					clusterPositions.add(BlockPos.of(packedPos));
				}
				default -> { }
			}
		}

		Geode toGeode() {
			AABB bounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
			BlockPos center = BlockPos.containing(bounds.getCenter());
			return new Geode(center, bounds, amethystBlocks, budding, small, medium, large, clusters,
					List.copyOf(clusterPositions));
		}
	}
}
