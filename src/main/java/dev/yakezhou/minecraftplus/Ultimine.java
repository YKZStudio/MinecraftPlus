package dev.yakezhou.minecraftplus;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Ultimine {
	public static final int MAX_BLOCKS = 64;
	private static final float EXHAUSTION_PER_BLOCK = 0.5F;
	private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();
	private static boolean mining;

	private Ultimine() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(StatePayload.TYPE, StatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) -> {
			UUID id = context.player().getUUID();
			if (payload.active()) {
				ACTIVE_PLAYERS.add(id);
			} else {
				ACTIVE_PLAYERS.remove(id);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			ACTIVE_PLAYERS.remove(handler.getPlayer().getUUID()));
		PlayerBlockBreakEvents.AFTER.register(Ultimine::afterBlockBreak);
	}

	private static void afterBlockBreak(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
		BlockPos origin, BlockState originalState, net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
		if (mining || !(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)
			|| !ACTIVE_PLAYERS.contains(player.getUUID()) || player.isSpectator()) {
			return;
		}

		ItemStack originalTool = player.getMainHandItem();
		boolean hadTool = !originalTool.isEmpty();
		mining = true;
		try {
			var blocks = UltimineSearch.find(level, origin, originalState, MAX_BLOCKS);
			for (int index = 1; index < blocks.size(); index++) {
				BlockPos pos = blocks.get(index);
				if (hadTool && (originalTool.isEmpty() || player.getMainHandItem() != originalTool)) {
					break;
				}
				if (UltimineSearch.matches(originalState, level.getBlockState(pos))
					&& serverPlayer.gameMode.destroyBlock(pos) && !player.isCreative()) {
					player.causeFoodExhaustion(EXHAUSTION_PER_BLOCK);
				}
			}
		} finally {
			mining = false;
		}
	}

	public record StatePayload(boolean active) implements CustomPacketPayload {
		public static final Type<StatePayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(MinecraftPlus.MOD_ID, "ultimine_state")
		);
		public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			StatePayload::active,
			StatePayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
