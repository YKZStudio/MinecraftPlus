package dev.yakezhou.minecraftplus.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class MinecraftPlusConfig {
	public static final int DEFAULT_WIDTH = 33_750;
	public static final int DEFAULT_HEIGHT = 19_116;
	public static final int MAX_DIMENSION = 65_535;
	public static final long MAX_PIXELS = 1_000_000_000L;
	private static final Logger LOGGER = LoggerFactory.getLogger("Minecraft Plus Config");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("mcplus.json");
	private static volatile Size screenshotSize = new Size(DEFAULT_WIDTH, DEFAULT_HEIGHT);

	private MinecraftPlusConfig() {
	}

	public static Size screenshotSize() {
		return screenshotSize;
	}

	public static void load() {
		if (!Files.isRegularFile(PATH)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(PATH)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			set(root.get("hugeScreenshotWidth").getAsInt(), root.get("hugeScreenshotHeight").getAsInt());
		} catch (Exception exception) {
			screenshotSize = new Size(DEFAULT_WIDTH, DEFAULT_HEIGHT);
			LOGGER.warn("Couldn't read {}; using defaults", PATH, exception);
		}
	}

	public static void save(int width, int height) throws IOException {
		set(width, height);
		Files.createDirectories(PATH.getParent());
		JsonObject root = new JsonObject();
		root.addProperty("hugeScreenshotWidth", width);
		root.addProperty("hugeScreenshotHeight", height);
		Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
		Files.writeString(temporary, GSON.toJson(root));
		try {
			Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException ignored) {
			Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public static boolean isValid(int width, int height) {
		return width > 0 && height > 0
			&& width <= MAX_DIMENSION && height <= MAX_DIMENSION
			&& (long) width * height <= MAX_PIXELS;
	}

	private static void set(int width, int height) {
		if (!isValid(width, height)) {
			throw new IllegalArgumentException("Invalid huge screenshot size: " + width + "x" + height);
		}
		screenshotSize = new Size(width, height);
	}

	public record Size(int width, int height) {
		public long pixels() {
			return (long) width * height;
		}
	}
}
