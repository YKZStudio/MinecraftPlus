package dev.yakezhou.minecraftplus.mixin;

import dev.yakezhou.minecraftplus.client.TabPerformanceDisplay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleSetTime", at = @At("HEAD"))
	private void minecraftplus$recordServerTime(ClientboundSetTimePacket packet, CallbackInfo callbackInfo) {
		TabPerformanceDisplay.recordServerTime((ClientPacketListener) (Object) this, packet.gameTime());
	}
}
