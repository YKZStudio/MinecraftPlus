package dev.yakezhou.minecraftplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.yakezhou.minecraftplus.Ultimine;
import dev.yakezhou.minecraftplus.UltimineSearch;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class UltimineClient {
	private static final KeyMapping KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.mcplus.ultimine",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_GRAVE,
		KeyMapping.Category.GAMEPLAY
	));
	private static final GizmoStyle OUTLINE = GizmoStyle.stroke(0xFFFFFFFF, 1.5F);
	private static boolean lastSent;

	private UltimineClient() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(UltimineClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lastSent = false);
		LevelExtractionEvents.END_EXTRACTION.register(context -> renderPreview());
	}

	private static void tick(Minecraft client) {
		boolean active = client.player != null && client.level != null && KEY.isDown();
		if (active != lastSent && client.getConnection() != null && ClientPlayNetworking.canSend(Ultimine.StatePayload.TYPE)) {
			ClientPlayNetworking.send(new Ultimine.StatePayload(active));
			lastSent = active;
		}
	}

	private static void renderPreview() {
		Minecraft client = Minecraft.getInstance();
		if (!KEY.isDown() || client.level == null
			|| !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		var origin = hit.getBlockPos();
		var target = client.level.getBlockState(origin);
		SimpleGizmoCollector collector = new SimpleGizmoCollector();
		try (var ignored = Gizmos.withCollector(collector)) {
			for (var pos : UltimineSearch.find(client.level, origin, target, Ultimine.MAX_BLOCKS)) {
				Gizmos.cuboid(pos, OUTLINE);
			}
		}
		client.levelRenderer.addMainThreadGizmos(collector.drainGizmos());
	}
}
