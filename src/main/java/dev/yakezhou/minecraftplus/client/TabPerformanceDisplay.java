package dev.yakezhou.minecraftplus.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public final class TabPerformanceDisplay {
	private static ClientPacketListener lastConnection;
	private static long lastGameTime;
	private static long lastSampleNanos;
	private static double estimatedTps = Double.NaN;

	private TabPerformanceDisplay() {
	}

	public static void recordServerTime(ClientPacketListener connection, long gameTime) {
		long now = System.nanoTime();
		if (connection != lastConnection) {
			lastConnection = connection;
			lastSampleNanos = 0;
			estimatedTps = Double.NaN;
		}

		if (lastSampleNanos != 0) {
			long elapsed = now - lastSampleNanos;
			long ticks = gameTime - lastGameTime;
			if (elapsed > 0 && elapsed <= 5_000_000_000L && ticks > 0 && ticks <= 200) {
				double sample = Math.min(20.0, ticks * 1_000_000_000.0 / elapsed);
				estimatedTps = Double.isNaN(estimatedTps) ? sample : estimatedTps * 0.75 + sample * 0.25;
			} else {
				estimatedTps = Double.NaN;
			}
		}

		lastGameTime = gameTime;
		lastSampleNanos = now;
	}

	public static Component create(Minecraft minecraft) {
		double tps = getTps(minecraft);
		int fps = minecraft.getFps();
		int ping = getPing(minecraft);
		Runtime runtime = Runtime.getRuntime();
		int memory = (int) ((runtime.totalMemory() - runtime.freeMemory()) * 100 / runtime.maxMemory());

		MutableComponent result = Component.empty();
		append(result, "TPS", Double.isNaN(tps) ? "--" : String.format(Locale.ROOT, "%.1f", tps),
			Double.isNaN(tps) ? ChatFormatting.GRAY : colorHigherIsBetter(tps, 19, 15));
		append(result, "FPS", Integer.toString(fps), colorHigherIsBetter(fps, 60, 30));
		append(result, "Ping", ping < 0 ? "--" : ping + "ms",
			ping < 0 ? ChatFormatting.GRAY : colorLowerIsBetter(ping, 100, 200));
		append(result, "MEM", memory + "%", colorLowerIsBetter(memory, 70, 90));
		return result;
	}

	private static double getTps(Minecraft minecraft) {
		var server = minecraft.getSingleplayerServer();
		return server == null
			? estimatedTps
			: Math.min(20.0, 1000.0 / Math.max(50.0, server.getCurrentSmoothedTickTime()));
	}

	private static int getPing(Minecraft minecraft) {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection == null || minecraft.player == null) {
			return -1;
		}
		PlayerInfo info = connection.getPlayerInfo(minecraft.player.getUUID());
		return info == null ? -1 : info.getLatency();
	}

	private static void append(MutableComponent line, String label, String value, ChatFormatting color) {
		if (!line.getSiblings().isEmpty()) {
			line.append(Component.literal("  |  ").withStyle(ChatFormatting.DARK_GRAY));
		}
		line.append(Component.literal(label + ": ").withStyle(ChatFormatting.GRAY));
		line.append(Component.literal(value).withStyle(color));
	}

	private static ChatFormatting colorHigherIsBetter(double value, double good, double warning) {
		return value >= good ? ChatFormatting.GREEN : value >= warning ? ChatFormatting.YELLOW : ChatFormatting.RED;
	}

	private static ChatFormatting colorLowerIsBetter(int value, int good, int warning) {
		return value <= good ? ChatFormatting.GREEN : value <= warning ? ChatFormatting.YELLOW : ChatFormatting.RED;
	}
}
