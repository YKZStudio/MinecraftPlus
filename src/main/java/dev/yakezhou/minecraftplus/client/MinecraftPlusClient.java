package dev.yakezhou.minecraftplus.client;

import net.fabricmc.api.ClientModInitializer;

public final class MinecraftPlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MinecraftPlusConfig.load();
		UltimineClient.initialize();
	}
}
