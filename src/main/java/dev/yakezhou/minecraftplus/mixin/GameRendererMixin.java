package dev.yakezhou.minecraftplus.mixin;

import dev.yakezhou.minecraftplus.client.HugeScreenshot;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "extract", at = @At("HEAD"))
	private void minecraftplus$prepareHugeScreenshot(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callbackInfo) {
		HugeScreenshot.beforeExtract((GameRenderer) (Object) this);
	}

	@Inject(method = "extract", at = @At("RETURN"))
	private void minecraftplus$restoreHugeScreenshotState(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callbackInfo) {
		HugeScreenshot.afterExtract((GameRenderer) (Object) this);
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void minecraftplus$applyHugeScreenshotProjection(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
		HugeScreenshot.beforeRender((GameRenderer) (Object) this);
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void minecraftplus$captureHugeScreenshotTile(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
		HugeScreenshot.afterRender((GameRenderer) (Object) this);
	}
}
