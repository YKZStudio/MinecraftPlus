package dev.yakezhou.minecraftplus;

public final class ExplosionRules {
	private ExplosionRules() {
	}

	public static int plannedExplosionCount(int level, boolean hasInfinity, boolean creative, int availableTnt) {
		int count = Math.max(0, Math.min(3, level));
		return hasInfinity || creative || availableTnt >= count ? count : 0;
	}
}
