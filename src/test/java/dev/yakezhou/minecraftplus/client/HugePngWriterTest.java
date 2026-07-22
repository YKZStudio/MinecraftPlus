package dev.yakezhou.minecraftplus.client;

import org.joml.Matrix4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HugePngWriterTest {
	public static void main(String[] args) throws Exception {
		assert (long) MinecraftPlusConfig.DEFAULT_WIDTH * MinecraftPlusConfig.DEFAULT_HEIGHT == 645_165_000L;
		assert Math.abs(HugeScreenshot.projectionScale(19_116, 637) * 637 - 19_116) < 0.01F;
		assert Math.abs(HugeScreenshot.projectionScale(33_750, 4096) * 4096 - 33_750) < 0.01F;
		Matrix4f base = new Matrix4f().perspective((float) Math.toRadians(70), 4096F / 662F, 0.05F, 1000F);
		Matrix4f unchanged = new Matrix4f(base);
		Matrix4f firstTile = HugeScreenshot.tileProjection(base, 33_750, 19_116, 4096, 662, 0, 0);
		Matrix4f secondTile = HugeScreenshot.tileProjection(base, 33_750, 19_116, 4096, 662, 4096, 0);
		assert base.equals(unchanged, 0F);
		assert !firstTile.equals(secondTile, 0F);
		Path path = Files.createTempFile("mcplus-huge-png-", ".png");
		try {
			try (HugePngWriter writer = new HugePngWriter(path, 2, 2)) {
				writer.writeRows(new byte[] {
					(byte) 255, 0, 0, 0, (byte) 255, 0,
					0, 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255
				}, 2);
			}
			BufferedImage image = ImageIO.read(path.toFile());
			assert image.getWidth() == 2 && image.getHeight() == 2;
			assert (image.getRGB(0, 0) & 0xFFFFFF) == 0xFF0000;
			assert (image.getRGB(1, 0) & 0xFFFFFF) == 0x00FF00;
			assert (image.getRGB(0, 1) & 0xFFFFFF) == 0x0000FF;
			assert (image.getRGB(1, 1) & 0xFFFFFF) == 0xFFFFFF;
		} finally {
			Files.deleteIfExists(path);
		}
	}
}
