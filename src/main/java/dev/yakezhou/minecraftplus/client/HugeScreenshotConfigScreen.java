package dev.yakezhou.minecraftplus.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Locale;

public final class HugeScreenshotConfigScreen extends Screen {
	private final Screen parent;
	private EditBox widthBox;
	private EditBox heightBox;
	private StringWidget summary;
	private Button done;

	public HugeScreenshotConfigScreen(Screen parent) {
		super(Component.translatable("screen.mcplus.huge_screenshot"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int center = this.width / 2;
		MinecraftPlusConfig.Size size = MinecraftPlusConfig.screenshotSize();
		addRenderableOnly(new StringWidget(center - 150, 35, 300, 20, this.title, this.font));
		addRenderableOnly(new StringWidget(center - 150, 75, 90, 20,
			Component.translatable("option.mcplus.huge_screenshot.width"), this.font));
		addRenderableOnly(new StringWidget(center - 150, 107, 90, 20,
			Component.translatable("option.mcplus.huge_screenshot.height"), this.font));

		this.widthBox = addRenderableWidget(numberBox(center - 50, 75, size.width()));
		this.heightBox = addRenderableWidget(numberBox(center - 50, 107, size.height()));
		this.summary = addRenderableOnly(new StringWidget(center - 150, 143, 300, 20, Component.empty(), this.font));
		addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> reset())
			.bounds(center - 154, this.height - 42, 150, 20).build());
		this.done = addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
			.bounds(center + 4, this.height - 42, 150, 20).build());
		updateValidation();
	}

	private EditBox numberBox(int x, int y, int value) {
		EditBox box = new EditBox(this.font, x, y, 200, 20, Component.empty());
		box.setMaxLength(5);
		box.setValue(Integer.toString(value));
		box.setResponder(ignored -> updateValidation());
		return box;
	}

	private void reset() {
		this.widthBox.setValue(Integer.toString(MinecraftPlusConfig.DEFAULT_WIDTH));
		this.heightBox.setValue(Integer.toString(MinecraftPlusConfig.DEFAULT_HEIGHT));
	}

	private void updateValidation() {
		int width = parse(this.widthBox);
		int height = parse(this.heightBox);
		boolean valid = MinecraftPlusConfig.isValid(width, height);
		this.done.active = valid;
		this.widthBox.setTextColor(valid ? 0xE0E0E0 : 0xFF5555);
		this.heightBox.setTextColor(valid ? 0xE0E0E0 : 0xFF5555);
		this.summary.setMessage(valid
			? Component.translatable("option.mcplus.huge_screenshot.pixels",
				String.format(Locale.ROOT, "%,d", (long) width * height))
			: Component.translatable("option.mcplus.huge_screenshot.invalid",
				MinecraftPlusConfig.MAX_DIMENSION, MinecraftPlusConfig.MAX_PIXELS));
	}

	private void save() {
		try {
			MinecraftPlusConfig.save(parse(this.widthBox), parse(this.heightBox));
			onClose();
		} catch (IOException exception) {
			this.summary.setMessage(Component.translatable("option.mcplus.huge_screenshot.save_failed",
				exception.getMessage()));
		}
	}

	private static int parse(EditBox box) {
		try {
			return Integer.parseInt(box.getValue());
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
