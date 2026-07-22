package dev.yakezhou.minecraftplus.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	@Inject(method = {"canEnchant", "isPrimaryItem", "isSupportedItem"}, at = @At("HEAD"), cancellable = true)
	private void minecraftplus$allowAnyItem(ItemStack stack, CallbackInfoReturnable<Boolean> callbackInfo) {
		callbackInfo.setReturnValue(true);
	}

	@Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
	private static void minecraftplus$allowAnyCombination(
		Holder<Enchantment> first,
		Holder<Enchantment> second,
		CallbackInfoReturnable<Boolean> callbackInfo
	) {
		callbackInfo.setReturnValue(!first.equals(second));
	}
}
