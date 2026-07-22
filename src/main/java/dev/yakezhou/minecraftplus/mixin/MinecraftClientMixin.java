package dev.yakezhou.minecraftplus.mixin;

import dev.yakezhou.minecraftplus.client.HugeScreenshot;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "isPaused", at = @At("HEAD"), cancellable = true)
	private void minecraftplus$freezeHugeScreenshot(CallbackInfoReturnable<Boolean> callbackInfo) {
		if (HugeScreenshot.shouldFreezeGame((Minecraft) (Object) this)) {
			callbackInfo.setReturnValue(true);
		}
	}
}
