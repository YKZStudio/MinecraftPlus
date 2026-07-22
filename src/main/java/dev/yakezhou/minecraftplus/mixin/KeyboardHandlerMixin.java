package dev.yakezhou.minecraftplus.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.yakezhou.minecraftplus.client.HugeScreenshot;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void minecraftplus$takeHugeScreenshot(long window, int action, KeyEvent event, CallbackInfo callbackInfo) {
		if (action == InputConstants.PRESS
			&& event.key() == InputConstants.KEY_F2
			&& this.minecraft.level != null
			&& InputConstants.isKeyDown(this.minecraft.getWindow(), InputConstants.KEY_UP)) {
			HugeScreenshot.start(this.minecraft);
			callbackInfo.cancel();
		}
	}
}
