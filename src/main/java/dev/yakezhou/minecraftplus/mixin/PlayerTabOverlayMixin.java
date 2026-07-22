package dev.yakezhou.minecraftplus.mixin;

import dev.yakezhou.minecraftplus.client.TabPerformanceDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	private Component footer;
	@Unique
	private Component minecraftplus$serverFooter;

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void minecraftplus$addPerformanceFooter(
		GuiGraphicsExtractor graphics,
		int width,
		Scoreboard scoreboard,
		Objective objective,
		CallbackInfo callbackInfo
	) {
		this.minecraftplus$serverFooter = this.footer;
		Component performance = TabPerformanceDisplay.create(this.minecraft);
		this.footer = this.footer == null
			? performance
			: this.footer.copy().append("\n").append(performance);
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void minecraftplus$restoreServerFooter(
		GuiGraphicsExtractor graphics,
		int width,
		Scoreboard scoreboard,
		Objective objective,
		CallbackInfo callbackInfo
	) {
		this.footer = this.minecraftplus$serverFooter;
	}
}
