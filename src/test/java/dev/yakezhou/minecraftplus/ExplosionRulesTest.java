package dev.yakezhou.minecraftplus;

public final class ExplosionRulesTest {
	public static void main(String[] args) {
		assert ExplosionRules.plannedExplosionCount(3, false, false, 3) == 3;
		assert ExplosionRules.plannedExplosionCount(3, false, false, 2) == 0;
		assert ExplosionRules.plannedExplosionCount(3, true, false, 0) == 3;
		assert ExplosionRules.plannedExplosionCount(3, false, true, 0) == 3;
		assert ExplosionRules.plannedExplosionCount(99, true, false, 0) == 3;
	}
}
