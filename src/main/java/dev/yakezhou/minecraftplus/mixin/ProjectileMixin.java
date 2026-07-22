package dev.yakezhou.minecraftplus.mixin;

import dev.yakezhou.minecraftplus.ArmedExplosionProjectile;
import dev.yakezhou.minecraftplus.ExplosionRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin implements ArmedExplosionProjectile {
	@Unique
	private int minecraftplus$explosionLevel;
	@Unique
	private boolean minecraftplus$hasInfinity;
	@Unique
	private boolean minecraftplus$exploded;

	@Override
	public void minecraftplus$arm(int level, boolean hasInfinity) {
		this.minecraftplus$explosionLevel = Math.max(0, Math.min(3, level));
		this.minecraftplus$hasInfinity = hasInfinity;
	}

	@Inject(method = "onHit", at = @At("HEAD"))
	private void minecraftplus$onHit(HitResult hit, CallbackInfo callbackInfo) {
		Projectile projectile = (Projectile) (Object) this;
		if (this.minecraftplus$exploded
			|| this.minecraftplus$explosionLevel == 0
			|| !(projectile.level() instanceof ServerLevel level)
			|| !(projectile.getOwner() instanceof Player player)) {
			return;
		}

		Inventory inventory = player.getInventory();
		int count = ExplosionRules.plannedExplosionCount(
			this.minecraftplus$explosionLevel,
			this.minecraftplus$hasInfinity,
			player.isCreative(),
			minecraftplus$countTnt(inventory)
		);
		if (count == 0) {
			return;
		}

		this.minecraftplus$exploded = true;
		if (!this.minecraftplus$hasInfinity && !player.isCreative()) {
			minecraftplus$removeTnt(inventory, count);
		}

		var pos = hit.getLocation();
		for (int i = 0; i < count; i++) {
			PrimedTnt tnt = new PrimedTnt(level, pos.x, pos.y, pos.z, player);
			tnt.setFuse(0);
			level.addFreshEntity(tnt);
		}
	}

	@Unique
	private static int minecraftplus$countTnt(Inventory inventory) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.getItem() == Items.TNT) {
				count += stack.getCount();
			}
		}
		return count;
	}

	@Unique
	private static void minecraftplus$removeTnt(Inventory inventory, int count) {
		for (int slot = 0; slot < inventory.getContainerSize() && count > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.getItem() == Items.TNT) {
				int removed = Math.min(count, stack.getCount());
				stack.shrink(removed);
				count -= removed;
			}
		}
		inventory.setChanged();
	}
}
