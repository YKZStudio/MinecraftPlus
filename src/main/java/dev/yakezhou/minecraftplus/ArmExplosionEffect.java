package dev.yakezhou.minecraftplus;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record ArmExplosionEffect() implements EnchantmentEntityEffect {
	public static final MapCodec<ArmExplosionEffect> CODEC = MapCodec.unit(ArmExplosionEffect::new);

	@Override
	public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 pos) {
		if (!(target instanceof ArmedExplosionProjectile projectile)) {
			return;
		}

		var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		var infinity = enchantments.getOrThrow(Enchantments.INFINITY);
		boolean hasInfinity = EnchantmentHelper.getItemEnchantmentLevel(infinity, context.itemStack()) > 0;
		projectile.minecraftplus$arm(enchantmentLevel, hasInfinity);
	}

	@Override
	public MapCodec<? extends EnchantmentEntityEffect> codec() {
		return CODEC;
	}
}
