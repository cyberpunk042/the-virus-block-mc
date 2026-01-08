package net.cyberpunk042.client.screen;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.client.state.VirusDifficultyClientState;
import net.cyberpunk042.config.ColorConfig;
import net.cyberpunk042.config.ColorConfig.ColorSlot;
import net.cyberpunk042.item.PurificationOption;
import net.cyberpunk042.network.PurificationTotemSelectPayload;
import net.cyberpunk042.screen.handler.PurificationTotemScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PurificationTotemScreen extends HandledScreen<PurificationTotemScreenHandler> {
	private final List<OptionButton> optionButtons = new ArrayList<>();
	@Nullable
	private Text pendingTooltip;

	public PurificationTotemScreen(PurificationTotemScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 280;
		this.backgroundHeight = 100;
	}

	@Override
	protected void init() {
		super.init();
		clearChildren();
		optionButtons.clear();

		int buttonSize = 42;
		List<PurificationOption> visible = new ArrayList<>();
		for (PurificationOption option : PurificationOption.values()) {
			if (option.isVisible(VirusDifficultyClientState.get())) {
				visible.add(option);
			}
		}
		int count = visible.size();
		int gap = 22;
		int totalWidth = buttonSize * count + gap * Math.max(0, count - 1);
		this.backgroundWidth = Math.max(220, totalWidth + 40);
		this.x = (width - backgroundWidth) / 2;
		this.y = (height - backgroundHeight) / 2;
		int startX = x + (backgroundWidth - totalWidth) / 2;
		int yPos = y + (backgroundHeight - buttonSize) / 2;

		for (int i = 0; i < visible.size(); i++) {
			PurificationOption option = visible.get(i);
			int buttonX = startX + i * (buttonSize + gap);
			OptionButton button = new OptionButton(buttonX, yPos, buttonSize, option);
			optionButtons.add(button);
			addDrawableChild(button);
		}
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = x;
		int top = y;
		int right = left + backgroundWidth;
		int bottom = top + backgroundHeight;
		context.fill(left, top, right, bottom, ColorConfig.argb(ColorSlot.UI_PURIFICATION_BACKGROUND));
		context.drawBorder(left, top, backgroundWidth, backgroundHeight, ColorConfig.argb(ColorSlot.UI_PURIFICATION_BORDER));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		pendingTooltip = null;
		super.render(context, mouseX, mouseY, delta);
		drawMouseoverTooltip(context, mouseX, mouseY);
		if (pendingTooltip != null) {
			context.drawTooltip(textRenderer, pendingTooltip, mouseX, mouseY);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(textRenderer, title, 8, 6, ColorConfig.argb(ColorSlot.UI_PURIFICATION_TITLE), false);
	}

	private void selectOption(PurificationOption option) {
		ClientPlayNetworking.send(new PurificationTotemSelectPayload(handler.syncId, option));
		optionButtons.forEach(button -> button.active = false);
	}

	private class OptionButton extends PressableWidget {
		private final PurificationOption option;
		private final ItemStack icon;
		@Nullable
		private final Identifier iconTexture;

		OptionButton(int x, int y, int size, PurificationOption option) {
			super(x, y, size, size, option.title());
			this.option = option;
			this.icon = option.createIcon();
			this.iconTexture = option.iconTexture();
		}

		@Override
		public void onPress() {
			if (!active) {
				return;
			}
			selectOption(option);
		}

		@Override
		protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			boolean hovered = isHovered();
			int borderColor = hovered
					? ColorConfig.argb(ColorSlot.UI_PURIFICATION_OPTION_BORDER_HOVER)
					: ColorConfig.argb(ColorSlot.UI_PURIFICATION_OPTION_BORDER_IDLE);
			int fillColor = hovered
					? ColorConfig.argb(ColorSlot.UI_PURIFICATION_OPTION_FILL_HOVER)
					: ColorConfig.argb(ColorSlot.UI_PURIFICATION_OPTION_FILL_IDLE);
			context.fill(getX(), getY(), getX() + width, getY() + height, fillColor);
			context.drawBorder(getX(), getY(), width, height, borderColor);
			context.drawCenteredTextWithShadow(PurificationTotemScreen.this.textRenderer, option.title(), getX() + width / 2, getY() - 12, 0xFFFFFF);
			int iconX = getX() + width / 2 - 8;
			int iconY = getY() + height / 2 - 8;
			if (iconTexture != null) {
				context.drawTexture(RenderPipelines.GUI_TEXTURED, iconTexture, iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
			} else if (!icon.isEmpty()) {
				context.drawItem(icon, iconX, iconY);
			}
			if (hovered) {
				pendingTooltip = option.description();
			}
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			builder.put(NarrationPart.TITLE, getMessage());
		}
	}
}

