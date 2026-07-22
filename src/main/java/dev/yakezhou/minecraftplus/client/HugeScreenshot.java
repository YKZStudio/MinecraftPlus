package dev.yakezhou.minecraftplus.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HugeScreenshot {
	private static final Logger LOGGER = LoggerFactory.getLogger("Minecraft Plus Huge Screenshot");
	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
	private static final long STRIP_MEMORY = 64L * 1024 * 1024;
	private static final long CAPTURE_TIMEOUT_NANOS = 30_000_000_000L;
	private static volatile Capture active;

	private HugeScreenshot() {
	}

	public static boolean start(Minecraft minecraft) {
		if (active != null) {
			message(minecraft, "message.mcplus.huge_screenshot.busy");
			return false;
		}
		if (minecraft.level == null || minecraft.player == null) {
			message(minecraft, "message.mcplus.huge_screenshot.no_world");
			return false;
		}

		MinecraftPlusConfig.Size size = MinecraftPlusConfig.screenshotSize();
		Path screenshots = minecraft.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR);
		Path temporary = null;
		HugePngWriter writer = null;
		Capture capture = null;
		try {
			Files.createDirectories(screenshots);
			deleteStaleParts(screenshots);
			long worstCaseBytes = Math.addExact(Math.multiplyExact(size.pixels(), 3L), 80L * 1024 * 1024);
			FileStore store = Files.getFileStore(screenshots);
			if (store.getUsableSpace() < worstCaseBytes) {
				throw new IOException("Not enough free disk space (need about " + (worstCaseBytes >> 20) + " MiB)");
			}

			Path output = uniqueOutput(screenshots);
			temporary = output.resolveSibling(output.getFileName() + ".part");
			int maxTexture = RenderSystem.getDevice().getDeviceInfo().limits().maxTextureSize();
			int tileWidth = Math.min(size.width(), Math.min(4096, maxTexture));
			int stripHeight = (int) Math.max(1, Math.min(Math.min(size.height(), maxTexture),
				STRIP_MEMORY / ((long) size.width() * 3)));
			stripHeight = Math.max(stripHeight,
				Math.min(Math.min(size.height(), maxTexture), minecraft.gameRenderer.mainRenderTarget().height));
			byte[] strip = new byte[Math.multiplyExact(Math.multiplyExact(size.width(), stripHeight), 3)];
			writer = new HugePngWriter(temporary, size.width(), size.height());
			capture = new Capture(minecraft, size.width(), size.height(), tileWidth, stripHeight,
				output, temporary, writer, strip);
			message(minecraft, "message.mcplus.huge_screenshot.started", size.width(), size.height());
			active = capture;
			return true;
		} catch (Exception | OutOfMemoryError exception) {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException closeFailure) {
					exception.addSuppressed(closeFailure);
				}
			}
			if (temporary != null) {
				try {
					Files.deleteIfExists(temporary);
				} catch (IOException deleteFailure) {
					exception.addSuppressed(deleteFailure);
				}
			}
			LOGGER.error("Couldn't start huge screenshot", exception);
			message(minecraft, "message.mcplus.huge_screenshot.failed", safeMessage(exception));
			return false;
		}
	}

	public static void beforeExtract(GameRenderer renderer) {
		Capture capture = active;
		if (capture == null) {
			return;
		}
		if (capture.minecraft.level == null) {
			fail(capture, new IOException("World was closed during capture"));
			return;
		}
		if (capture.pending && !capture.processing
			&& System.nanoTime() - capture.pendingSince > CAPTURE_TIMEOUT_NANOS) {
			fail(capture, new IOException("GPU readback timed out"));
			return;
		}
	}

	public static void afterExtract(GameRenderer renderer) {
		Capture capture = active;
		if (capture == null) {
			return;
		}
		try {
			LevelRenderState levelState = renderer.gameRenderState().levelRenderState;
			if (!capture.gameTimeCaptured) {
				capture.gameTime = levelState.gameTime;
				capture.gameTimeCaptured = true;
			} else {
				levelState.gameTime = capture.gameTime;
			}
			renderer.gameRenderState().windowRenderState.width = capture.maxTileWidth;
			renderer.gameRenderState().windowRenderState.height = capture.maxTileHeight;
			if (renderer.mainRenderTarget().width != capture.maxTileWidth
				|| renderer.mainRenderTarget().height != capture.maxTileHeight) {
				renderer.resize(capture.maxTileWidth, capture.maxTileHeight);
			}
			capture.resized = true;
		} catch (Exception | OutOfMemoryError exception) {
			fail(capture, exception);
		}
	}

	public static void beforeRender(GameRenderer renderer) {
		Capture capture = active;
		if (capture == null || !capture.resized) {
			return;
		}
		CameraRenderState camera = renderer.gameRenderState().levelRenderState.cameraRenderState;
		if (capture.baseProjection == null) {
			capture.baseProjection = new Matrix4f(camera.projectionMatrix);
		}
		camera.projectionMatrix = tileProjection(capture.baseProjection, capture.width, capture.height,
			capture.maxTileWidth, capture.maxTileHeight, capture.x, capture.y);
	}

	public static void afterRender(GameRenderer renderer) {
		Capture capture = active;
		if (capture == null || capture.pending || !capture.resized) {
			return;
		}
		capture.pending = true;
		capture.pendingSince = System.nanoTime();
		try {
			Screenshot.takeScreenshot(renderer.mainRenderTarget(), image -> {
				capture.processing = true;
				Util.ioPool().execute(() -> consume(capture, image));
			});
		} catch (Exception | OutOfMemoryError exception) {
			capture.pending = false;
			fail(capture, exception);
		}
	}

	private static void consume(Capture capture, NativeImage image) {
		try (image) {
			if (active != capture || capture.terminal.get()) {
				return;
			}
			int tileWidth = capture.tileWidth();
			int tileHeight = capture.tileHeight();
			if (image.getWidth() != capture.maxTileWidth || image.getHeight() != capture.maxTileHeight) {
				throw new IOException("GPU returned an unexpected tile size");
			}
			for (int row = 0; row < tileHeight; row++) {
				int destination = ((row * capture.width) + capture.x) * 3;
				for (int column = 0; column < tileWidth; column++) {
					int pixel = image.getPixel(column, row);
					capture.strip[destination++] = (byte) (pixel >> 16);
					capture.strip[destination++] = (byte) (pixel >> 8);
					capture.strip[destination++] = (byte) pixel;
				}
			}

			boolean endOfStrip = capture.x + tileWidth == capture.width;
			boolean complete = endOfStrip && capture.y + tileHeight == capture.height;
			if (endOfStrip) {
				capture.writer.writeRows(capture.strip, tileHeight);
			}
			if (complete) {
				finish(capture);
			} else {
				capture.minecraft.execute(() -> advance(capture, endOfStrip));
			}
		} catch (Exception | OutOfMemoryError exception) {
			fail(capture, exception);
		}
	}

	private static void advance(Capture capture, boolean endOfStrip) {
		if (active != capture || capture.terminal.get()) {
			return;
		}
		if (endOfStrip) {
			capture.x = 0;
			capture.y += capture.tileHeight();
			capture.strip = new byte[Math.multiplyExact(Math.multiplyExact(capture.width, capture.tileHeight()), 3)];
		} else {
			capture.x += capture.tileWidth();
		}
		capture.pending = false;
		capture.processing = false;
		capture.resized = false;
		int percent = (int) (100L * capture.y / capture.height);
		if (percent >= capture.nextProgress) {
			capture.nextProgress += 10;
			message(capture.minecraft, true, "message.mcplus.huge_screenshot.progress", percent);
		}
	}

	private static void finish(Capture capture) {
		if (!capture.terminal.compareAndSet(false, true)) {
			return;
		}
		try {
			if (capture.writer.writtenRows() != capture.height) {
				throw new IOException("Incomplete PNG data");
			}
			capture.writer.close();
			try {
				Files.move(capture.temporary, capture.output, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(capture.temporary, capture.output);
			}
		} catch (IOException exception) {
			cleanupFailedCapture(capture, exception);
			return;
		}
		capture.minecraft.execute(() -> {
			restore(capture);
			message(capture.minecraft, "message.mcplus.huge_screenshot.saved", capture.output.getFileName());
		});
	}

	private static void fail(Capture capture, Throwable throwable) {
		if (!capture.terminal.compareAndSet(false, true)) {
			return;
		}
		LOGGER.error("Huge screenshot failed", throwable);
		cleanupFailedCapture(capture, throwable);
	}

	private static void cleanupFailedCapture(Capture capture, Throwable throwable) {
		try {
			capture.writer.close();
		} catch (IOException closeFailure) {
			throwable.addSuppressed(closeFailure);
		}
		try {
			Files.deleteIfExists(capture.temporary);
		} catch (IOException deleteFailure) {
			throwable.addSuppressed(deleteFailure);
		}
		capture.minecraft.execute(() -> {
			restore(capture);
			message(capture.minecraft, "message.mcplus.huge_screenshot.failed", safeMessage(throwable));
		});
	}

	private static void restore(Capture capture) {
		if (active != capture) {
			return;
		}
		try {
			capture.minecraft.gameRenderer.resize(capture.originalTargetWidth, capture.originalTargetHeight);
		} catch (Exception | OutOfMemoryError exception) {
			LOGGER.error("Couldn't restore render target after huge screenshot", exception);
		} finally {
			active = null;
		}
	}

	public static boolean shouldFreezeGame(Minecraft minecraft) {
		Capture capture = active;
		return capture != null && capture.minecraft == minecraft && minecraft.hasSingleplayerServer()
			&& !minecraft.getSingleplayerServer().isPublished();
	}

	private static Path uniqueOutput(Path directory) {
		String base = "huge_" + FILE_TIME.format(LocalDateTime.now());
		Path path = directory.resolve(base + ".png");
		for (int suffix = 2; Files.exists(path) || Files.exists(path.resolveSibling(path.getFileName() + ".part")); suffix++) {
			path = directory.resolve(base + "_" + suffix + ".png");
		}
		return path;
	}

	static float projectionScale(int fullHeight, int tileHeight) {
		return (float) fullHeight / tileHeight;
	}

	static Matrix4f tileProjection(Matrix4f base, int fullWidth, int fullHeight,
		int tileWidth, int tileHeight, int x, int y) {
		float scaleX = projectionScale(fullWidth, tileWidth);
		float scaleY = projectionScale(fullHeight, tileHeight);
		float offsetX = (fullWidth - 2.0F * x - tileWidth) / tileWidth;
		float offsetY = (2.0F * y + tileHeight - fullHeight) / tileHeight;
		return new Matrix4f().translation(offsetX, offsetY, 0.0F).scale(scaleX, scaleY, 1.0F).mul(base);
	}

	private static void deleteStaleParts(Path directory) {
		try (DirectoryStream<Path> parts = Files.newDirectoryStream(directory, "huge_*.png.part")) {
			for (Path part : parts) {
				try {
					Files.deleteIfExists(part);
				} catch (IOException exception) {
					LOGGER.warn("Couldn't delete stale huge screenshot {}", part, exception);
				}
			}
		} catch (IOException exception) {
			LOGGER.warn("Couldn't scan for stale huge screenshots in {}", directory, exception);
		}
	}

	private static String safeMessage(Throwable throwable) {
		String message = throwable.getMessage();
		return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
	}

	private static void message(Minecraft minecraft, String key, Object... arguments) {
		message(minecraft, false, key, arguments);
	}

	private static void message(Minecraft minecraft, boolean actionBar, String key, Object... arguments) {
		if (minecraft.player != null) {
			if (actionBar) {
				minecraft.player.sendOverlayMessage(Component.translatable(key, arguments));
			} else {
				minecraft.player.sendSystemMessage(Component.translatable(key, arguments));
			}
		}
	}

	private static final class Capture {
		private final Minecraft minecraft;
		private final int width;
		private final int height;
		private final int maxTileWidth;
		private final int maxTileHeight;
		private final Path output;
		private final Path temporary;
		private final HugePngWriter writer;
		private final int originalTargetWidth;
		private final int originalTargetHeight;
		private final AtomicBoolean terminal = new AtomicBoolean();
		private byte[] strip;
		private int x;
		private int y;
		private int nextProgress = 10;
		private boolean pending;
		private volatile boolean processing;
		private boolean resized;
		private boolean gameTimeCaptured;
		private long pendingSince;
		private long gameTime;
		private Matrix4f baseProjection;

		private Capture(Minecraft minecraft, int width, int height, int maxTileWidth, int maxTileHeight,
			Path output, Path temporary, HugePngWriter writer, byte[] strip) {
			this.minecraft = minecraft;
			this.width = width;
			this.height = height;
			this.maxTileWidth = maxTileWidth;
			this.maxTileHeight = maxTileHeight;
			this.output = output;
			this.temporary = temporary;
			this.writer = writer;
			this.originalTargetWidth = minecraft.gameRenderer.mainRenderTarget().width;
			this.originalTargetHeight = minecraft.gameRenderer.mainRenderTarget().height;
			this.strip = strip;
		}

		private int tileWidth() {
			return Math.min(this.maxTileWidth, this.width - this.x);
		}

		private int tileHeight() {
			return Math.min(this.maxTileHeight, this.height - this.y);
		}
	}
}
