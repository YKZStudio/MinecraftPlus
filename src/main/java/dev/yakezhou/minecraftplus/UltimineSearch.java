package dev.yakezhou.minecraftplus;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class UltimineSearch {
	private UltimineSearch() {
	}

	public static List<BlockPos> find(Level level, BlockPos origin, BlockState target, int limit) {
		List<TagKey<Block>> mergedTags = target.typeHolder().tags().filter(UltimineSearch::isMergedTag).toList();
		return find(origin, limit, pos -> {
			BlockState candidate = level.getBlockState(pos);
			return candidate.is(target.getBlock()) || mergedTags.stream().anyMatch(candidate::is);
		});
	}

	public static boolean matches(BlockState target, BlockState candidate) {
		return candidate.is(target.getBlock())
			|| target.typeHolder().tags().filter(UltimineSearch::isMergedTag).anyMatch(candidate::is);
	}

	private static boolean isMergedTag(TagKey<Block> tag) {
		var id = tag.location();
		return tag.equals(BlockTags.BASE_STONE_OVERWORLD)
			|| (id.getNamespace().equals("c") && id.getPath().startsWith("ores/"));
	}

	static List<BlockPos> find(BlockPos origin, int limit, Predicate<BlockPos> matches) {
		if (limit <= 0) {
			return List.of();
		}

		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		List<BlockPos> result = new ArrayList<>(limit);
		queue.add(origin.immutable());
		visited.add(origin);

		while (!queue.isEmpty() && result.size() < limit) {
			BlockPos current = queue.removeFirst();
			result.add(current);
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						if (x == 0 && y == 0 && z == 0) {
							continue;
						}
						BlockPos next = current.offset(x, y, z);
						if (visited.add(next) && matches.test(next)) {
							queue.addLast(next);
						}
					}
				}
			}
		}
		return result;
	}
}
