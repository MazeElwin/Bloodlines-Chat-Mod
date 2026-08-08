package com.example.chatplus.gui;

import com.example.chatplus.chat.PlayerProfileData;
import com.example.chatplus.chat.PlayerProfileStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PlayerProfileScreen extends Screen {
	private static final int FIELD_WIDTH = 260;
	private static final int FIELD_HEIGHT = 20;

	private final Screen parent;
	private final String playerName;
	private final String displayName;
	private final String styleTrace;
	private EditBox nicknameBox;
	private EditBox motherBox;
	private EditBox fatherBox;
	private EditBox noteBox;
	private String derivedFamily = "";
	private String derivedClan = "";

	public PlayerProfileScreen(Screen parent, String playerName) {
		this(parent, playerName, playerName, "");
	}

	public PlayerProfileScreen(Screen parent, String playerName, String displayName, String styleTrace) {
		super(Component.literal(playerName));
		this.parent = parent;
		this.playerName = playerName;
		this.displayName = displayName == null ? playerName : displayName;
		this.styleTrace = styleTrace == null ? "" : styleTrace;
	}

	@Override
	protected void init() {
		PlayerProfileData data = PlayerProfileStore.get(playerName);
		derivedFamily = deriveFamilyName(data.getFamilyName());
		derivedClan = deriveClan(data.getClan());
		int centerX = width / 2;
		int startY = height / 2 - 92;

		nicknameBox = new EditBox(font, centerX - FIELD_WIDTH / 2, startY + 16, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Nickname"));
		nicknameBox.setMaxLength(128);
		nicknameBox.setValue(data.getNickname());
		addRenderableWidget(nicknameBox);

		motherBox = new EditBox(font, centerX - FIELD_WIDTH / 2, startY + 52, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Mother"));
		motherBox.setMaxLength(128);
		motherBox.setValue(data.getMother());
		addRenderableWidget(motherBox);

		fatherBox = new EditBox(font, centerX - FIELD_WIDTH / 2, startY + 88, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Father"));
		fatherBox.setMaxLength(128);
		fatherBox.setValue(data.getFather());
		addRenderableWidget(fatherBox);

		noteBox = new EditBox(font, centerX - FIELD_WIDTH / 2, startY + 160, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Note"));
		noteBox.setMaxLength(512);
		noteBox.setValue(data.getNote());
		addRenderableWidget(noteBox);

		addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
					PlayerProfileStore.save(playerName, new PlayerProfileData(motherBox.getValue(), fatherBox.getValue(), derivedFamily, derivedClan, nicknameBox.getValue(), noteBox.getValue()));
					minecraft.setScreen(parent);
				})
				.bounds(centerX - 104, startY + 194, 100, 20)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> minecraft.setScreen(parent))
				.bounds(centerX + 4, startY + 194, 100, 20)
				.build());

		setInitialFocus(nicknameBox);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int centerX = width / 2;
		int startY = height / 2 - 92;
		graphics.text(font, Component.literal(playerName), centerX - FIELD_WIDTH / 2, startY - 10, 0xFFFFFFFF, false);
		graphics.text(font, Component.literal("Nickname"), centerX - FIELD_WIDTH / 2, startY + 5, 0xFFAAAAAA, false);
		graphics.text(font, Component.literal("Mother"), centerX - FIELD_WIDTH / 2, startY + 41, 0xFFAAAAAA, false);
		graphics.text(font, Component.literal("Father"), centerX - FIELD_WIDTH / 2, startY + 77, 0xFFAAAAAA, false);
		graphics.text(font, Component.literal("Family: " + displayValue(derivedFamily)), centerX - FIELD_WIDTH / 2, startY + 116, 0xFFAAAAAA, false);
		graphics.text(font, Component.literal("Clan: " + displayValue(derivedClan)), centerX - FIELD_WIDTH / 2, startY + 132, 0xFFAAAAAA, false);
		graphics.text(font, Component.literal("Note"), centerX - FIELD_WIDTH / 2, startY + 149, 0xFFAAAAAA, false);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractTransparentBackground(graphics);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private String deriveFamilyName(String existingValue) {
		String cleanedDisplay = displayName.trim();
		if (cleanedDisplay.startsWith("You -> ")) {
			cleanedDisplay = cleanedDisplay.substring("You -> ".length()).trim();
		}

		if (cleanedDisplay.endsWith(playerName)) {
			String prefix = cleanedDisplay.substring(0, cleanedDisplay.length() - playerName.length()).trim();
			if (!prefix.isBlank()) {
				return prefix;
			}
		}

		return existingValue == null ? "" : existingValue;
	}

	private String deriveClan(String existingValue) {
		String color = firstUsefulColor(styleTrace);
		if (!color.isBlank()) {
			return color;
		}

		return existingValue == null ? "" : existingValue;
	}

	private String firstUsefulColor(String trace) {
		for (String part : trace.split(",")) {
			String value = part.trim();
			String lower = value.toLowerCase();
			if (lower.startsWith("color=")) {
				String color = value.substring("color=".length());
				if (!isDefaultNameColor(color)) {
					return color;
				}
			}
			if (lower.startsWith("rgb=#")) {
				String color = value.substring("rgb=".length()).toUpperCase();
				if (!"#FFFFFF".equals(color) && !"#AAAAAA".equals(color)) {
					return color;
				}
			}
		}
		return "";
	}

	private boolean isDefaultNameColor(String color) {
		String normalized = color.toLowerCase();
		return normalized.equals("white") || normalized.equals("gray") || normalized.equals("reset");
	}

	private String displayValue(String value) {
		return value == null || value.isBlank() ? "Unknown" : value;
	}
}
