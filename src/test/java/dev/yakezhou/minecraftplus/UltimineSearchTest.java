package dev.yakezhou.minecraftplus;

import net.minecraft.core.BlockPos;

import java.util.Set;

public final class UltimineSearchTest {
	public static void main(String[] args) {
		BlockPos origin = BlockPos.ZERO;
		Set<BlockPos> vein = Set.of(
			origin,
			new BlockPos(1, 1, 0),
			new BlockPos(2, 2, 1),
			new BlockPos(3, 3, 2),
			new BlockPos(20, 20, 20)
		);

		var allConnected = UltimineSearch.find(origin, 64, vein::contains);
		assert allConnected.size() == 4 : allConnected;
		assert !allConnected.contains(new BlockPos(20, 20, 20));
		assert UltimineSearch.find(origin, 2, vein::contains).size() == 2;
		assert UltimineSearch.find(origin, 0, vein::contains).isEmpty();
	}
}
