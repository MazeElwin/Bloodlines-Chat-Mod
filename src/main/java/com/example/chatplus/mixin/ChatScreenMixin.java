package com.example.chatplus.mixin;

import com.example.chatplus.chat.ChatInputMode;
import com.example.chatplus.chat.ChatColorFormatter;
import com.example.chatplus.chat.ChatMessage;
import com.example.chatplus.chat.ChatMessageParser;
import com.example.chatplus.chat.ChatPlusConfig;
import com.example.chatplus.chat.ChatTabManager;
import com.example.chatplus.chat.ChatTabType;
import com.example.chatplus.chat.ChatWindowState;
import com.example.chatplus.gui.PlayerProfileScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
	private record TabHit(String label, ChatInputMode mode, String whisperPartner, int x, int y, int width, int height) {
	}

	private record TabCloseHit(String whisperPartner, int x, int y, int width, int height) {
	}

	private record OverflowHit(int x, int y, int width, int height) {
	}

	private record SenderHit(String sender, String displayName, String styleTrace, int x, int y, int width, int height) {
	}

	private record LinkHit(String url, int x, int y, int width, int height) {
	}

	private static final int TAB_HEIGHT = 16;
	private static final int TAB_BUTTON_HEIGHT = 12;
	private static final int TAB_BREAK_WIDTH = 2;
	private static final int TAB_GAP = 5;
	private static final int OVERFLOW_DROPDOWN_WIDTH = 126;
	private static final int OVERFLOW_DROPDOWN_ROW_HEIGHT = 14;
	private static final int INPUT_HEIGHT = 12;
	private static final int PANEL_PADDING = 4;
	private static final int PANEL_BORDER_COLOR = 0x66FFFFFF;
	private static final int ICON_SIZE = 12;
	private static final int ICON_GAP = 2;
	private static final int SETTINGS_WIDTH = 178;
	private static final int SETTINGS_ROW_HEIGHT = 16;
	private static final int SLIDER_WIDTH = 70;
	private static final int HEAD_SIZE = 8;
	private static final int HEAD_GAP = 3;
	private static final int HEAD_Y_OFFSET = -2;
	private static final int SCROLLBAR_WIDTH = 4;
	private static final int RESIZE_EDGE = 5;
	private static final int DEATH_BACKGROUND_COLOR = 0x99AA2222;
	private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
	private static final int[] COLOR_PALETTE = {
			0xFFFFFFFF, 0xFFAAAAAA, 0xFF555555, 0xFFFFFF55, 0xFFFFAA00,
			0xFFFF5555, 0xFFFF55FF, 0xFFAA55FF, 0xFF5555FF, 0xFF55FFFF,
			0xFF00AAAA, 0xFF55FF55, 0xFF00AA00
	};
	private static final String[] COLOR_NAMES = {
			"White", "Gray", "Dark", "Yellow", "Gold",
			"Red", "Pink", "Purple", "Blue", "Aqua",
			"Teal", "Green", "DGreen"
	};
	private static final FontDescription.Resource LEXEND_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("bloodline-chat", "lexend"));

	@Shadow
	protected EditBox input;

	@Shadow
	CommandSuggestions commandSuggestions;

	private static final int MIN_PREFIX_BOX_WIDTH = 18;
	private static final int MAX_PREFIX_BOX_WIDTH = 92;
	private static final int INPUT_GAP = 3;

	private final List<Button> chatplus$modeButtons = new ArrayList<>();
	private final List<TabHit> chatplus$tabHits = new ArrayList<>();
	private final List<TabCloseHit> chatplus$tabCloseHits = new ArrayList<>();
	private final List<TabHit> chatplus$overflowTabHits = new ArrayList<>();
	private final List<SenderHit> chatplus$senderHits = new ArrayList<>();
	private final List<LinkHit> chatplus$linkHits = new ArrayList<>();
	private OverflowHit chatplus$overflowHit;
	private String chatplus$prefixValue = "";
	private String chatplus$selectedWhisperPartner = "";
	private boolean chatplus$applyingPrefix;
	private boolean chatplus$showSettings;
	private boolean chatplus$showWhisperOverflow;
	private int chatplus$whisperOverflowScroll;
	private int chatplus$whisperOverflowStartIndex;
	private int chatplus$settingsPage;
	private long chatplus$historyClearFlashUntil;
	private int chatplus$textSize = ChatPlusConfig.get().textSize;
	private int chatplus$lineSpacing = ChatPlusConfig.get().lineSpacing;
	private int chatplus$fontStyleIndex = ChatPlusConfig.get().fontStyleIndex;
	private int chatplus$backgroundOpacity = ChatPlusConfig.get().backgroundOpacity;
	private boolean chatplus$hudBackgroundEnabled = ChatPlusConfig.get().hudBackgroundEnabled;
	private boolean chatplus$chatHeadsEnabled = ChatPlusConfig.get().chatHeadsEnabled;
	private int chatplus$generalColor = ChatPlusConfig.get().generalColor;
	private int chatplus$localColor = ChatPlusConfig.get().localColor;
	private int chatplus$teamColor = ChatPlusConfig.get().teamColor;
	private int chatplus$whisperColor = ChatPlusConfig.get().whisperColor;
	private int chatplus$systemColor = ChatPlusConfig.get().systemColor;
	private int chatplus$historyRetentionDays = ChatPlusConfig.get().historyRetentionDays;
	private boolean chatplus$dragging;
	private boolean chatplus$resizing;
	private boolean chatplus$draggingOpacity;
	private int chatplus$dragOffsetX;
	private int chatplus$dragOffsetY;
	private int chatplus$resizeStartWidth;
	private int chatplus$resizeStartHeight;
	private int chatplus$resizeStartMouseX;
	private int chatplus$resizeStartMouseY;
	private boolean chatplus$resizeRight;
	private boolean chatplus$resizeBottom;
	private int chatplus$scrollOffset;

	protected ChatScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void chatplus$addModeButtons(CallbackInfo ci) {
		chatplus$modeButtons.clear();

		chatplus$layoutWindow();

		int x = ChatWindowState.getX() + PANEL_PADDING;
		int y = ChatWindowState.getY(height) + 2;
		for (ChatInputMode mode : ChatInputMode.values()) {
			int buttonWidth = chatplus$getButtonWidth(mode);
			Button button = Button.builder(chatplus$getButtonLabel(mode), pressedButton -> {
						chatplus$selectMode(mode);
					})
					.bounds(x, y, buttonWidth, TAB_BUTTON_HEIGHT)
					.build();
			chatplus$modeButtons.add(addRenderableWidget(button));
			x += buttonWidth + 2;
		}

		chatplus$syncPrefixBox();
		chatplus$focusInput();
		chatplus$refreshButtonLabels();
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void chatplus$renderPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		chatplus$layoutWindow();
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);
		int panelHeight = ChatWindowState.getHeight(height);
		int inputY = chatplus$getInputY();
		if (!chatplus$selectedWhisperPartner.isEmpty()) {
			ChatTabManager.getInstance().markWhisperRead(chatplus$selectedWhisperPartner);
		}

		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, chatplus$withAlpha(0x000000, chatplus$opacityToAlpha(chatplus$backgroundOpacity)));
		graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, PANEL_BORDER_COLOR);
		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + TAB_HEIGHT, 0xAA202020);
		graphics.fill(panelX, inputY - 2, panelX + panelWidth, panelY + panelHeight, 0xAA202020);

		chatplus$renderTabs(graphics, panelX, panelY);
		chatplus$renderHeaderIcons(graphics, panelX, panelY, panelWidth);
		chatplus$renderMessages(graphics, panelX, panelY, panelWidth, inputY);
		chatplus$renderPrefixButton(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		if (chatplus$showSettings) {
			chatplus$renderSettingsPanel(graphics, panelX, panelY, panelWidth);
		}
		commandSuggestions.render(graphics, mouseX, mouseY);
		ci.cancel();
	}

	@Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
	private void chatplus$prefixChatInput(String message, boolean addToRecentChat, CallbackInfo ci) {
		if (chatplus$applyingPrefix) {
			return;
		}

		String prefixed = chatplus$applyPrefix(message);
		if (!prefixed.equals(message)) {
			chatplus$applyingPrefix = true;
			try {
				((ChatScreen) (Object) this).handleChatInput(prefixed, addToRecentChat);
				ci.cancel();
			} finally {
				chatplus$applyingPrefix = false;
			}
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void chatplus$startDragging(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (event.button() != 0) {
			return;
		}

		if (chatplus$handleHeaderClick(event.x(), event.y())) {
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$showSettings && chatplus$handleSettingsClick(event.x(), event.y())) {
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$handleOverflowClick(event.x(), event.y())) {
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$openLinkAt(event.x(), event.y())) {
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$closeTabAt(event.x(), event.y())) {
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$selectTabAt(event.x(), event.y())) {
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$isInPrefixButton(event.x(), event.y())) {
			chatplus$focusInput();
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$openProfileAt(event.x(), event.y())) {
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$isOnResizeEdge(event.x(), event.y())) {
			chatplus$resizing = true;
			chatplus$resizeRight = chatplus$isOnRightResizeEdge(event.x(), event.y());
			chatplus$resizeBottom = chatplus$isOnBottomResizeEdge(event.x(), event.y());
			chatplus$resizeStartWidth = ChatWindowState.getWidth(width);
			chatplus$resizeStartHeight = ChatWindowState.getHeight(height);
			chatplus$resizeStartMouseX = (int) event.x();
			chatplus$resizeStartMouseY = (int) event.y();
			cir.setReturnValue(true);
			return;
		}

		if (!chatplus$isInDragHandle(event.x(), event.y())) {
			return;
		}

		chatplus$dragging = true;
		chatplus$dragOffsetX = (int) event.x() - ChatWindowState.getX();
		chatplus$dragOffsetY = (int) event.y() - ChatWindowState.getY(height);
		cir.setReturnValue(true);
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void chatplus$handleChatKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (event.key() == 257 || event.key() == 335 || event.input() == 257 || event.input() == 335) {
			String message = input.getValue();
			if (!message.trim().isEmpty()) {
				((ChatScreen) (Object) this).handleChatInput(message, true);
				input.setValue("");
				chatplus$syncPrefixBox();
			}
			chatplus$focusInput();
			cir.setReturnValue(true);
			return;
		}

		if (event.key() == 256 || event.input() == 256) {
			minecraft.setScreen(null);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void chatplus$scrollMessages(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
		if (chatplus$scrollOverflow(mouseX, mouseY, verticalAmount)) {
			cir.setReturnValue(true);
			return;
		}

		if (!chatplus$isInMessageArea(mouseX, mouseY)) {
			return;
		}

		int direction = verticalAmount > 0 ? 1 : -1;
		chatplus$scrollOffset = Math.max(0, chatplus$scrollOffset + direction * 3);
		chatplus$clampScrollOffset();
		cir.setReturnValue(true);
	}

	public boolean shouldCloseOnEsc() {
		return true;
	}

	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (chatplus$draggingOpacity) {
			chatplus$setOpacityFromMouse(event.x());
			return true;
		}

		if (chatplus$resizing) {
			ChatWindowState.resizeTo(
					chatplus$resizeRight ? chatplus$resizeStartWidth + (int) event.x() - chatplus$resizeStartMouseX : chatplus$resizeStartWidth,
					chatplus$resizeBottom ? chatplus$resizeStartHeight + (int) event.y() - chatplus$resizeStartMouseY : chatplus$resizeStartHeight,
					width,
					height
			);
			chatplus$layoutWindow();
			return true;
		}

		if (chatplus$dragging) {
			ChatWindowState.moveTo((int) event.x() - chatplus$dragOffsetX, (int) event.y() - chatplus$dragOffsetY, width, height);
			chatplus$layoutWindow();
			return true;
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	public boolean mouseReleased(MouseButtonEvent event) {
		if (chatplus$draggingOpacity) {
			chatplus$draggingOpacity = false;
			return true;
		}

		if (chatplus$resizing) {
			chatplus$resizing = false;
			ChatWindowState.saveSize();
			return true;
		}

		if (chatplus$dragging) {
			chatplus$dragging = false;
			ChatWindowState.savePosition();
			return true;
		}

		return super.mouseReleased(event);
	}

	private void chatplus$layoutWindow() {
		ChatWindowState.clampToScreen(width, height);
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);

		int inputY = chatplus$getInputY();
		int contentX = panelX + PANEL_PADDING;
		int prefixBoxWidth = chatplus$getPrefixBoxWidth();
		int inputX = contentX + prefixBoxWidth + INPUT_GAP;

		input.setX(inputX);
		input.setY(chatplus$getInputY());
		input.setWidth(Math.max(20, panelWidth - PANEL_PADDING * 2 - prefixBoxWidth - INPUT_GAP));
		input.setHeight(INPUT_HEIGHT);

		for (int i = 0; i < chatplus$modeButtons.size(); i++) {
			Button button = chatplus$modeButtons.get(i);
			button.visible = false;
			button.active = false;
		}
	}

	private boolean chatplus$selectTabAt(double mouseX, double mouseY) {
		for (TabHit tab : chatplus$tabHits) {
			if (mouseX >= tab.x()
					&& mouseX < tab.x() + tab.width()
					&& mouseY >= tab.y()
					&& mouseY < tab.y() + tab.height()) {
				chatplus$selectMode(tab.mode(), tab.whisperPartner());
				return true;
			}
		}

		return false;
	}

	private boolean chatplus$closeTabAt(double mouseX, double mouseY) {
		for (TabCloseHit closeHit : chatplus$tabCloseHits) {
			if (mouseX >= closeHit.x()
					&& mouseX < closeHit.x() + closeHit.width()
					&& mouseY >= closeHit.y()
					&& mouseY < closeHit.y() + closeHit.height()) {
				ChatTabManager.getInstance().closeWhisperTab(closeHit.whisperPartner());
				if (closeHit.whisperPartner().equals(chatplus$selectedWhisperPartner)) {
					chatplus$selectMode(ChatInputMode.ALL);
				}
				return true;
			}
		}

		return false;
	}

	private void chatplus$selectMode(ChatInputMode mode) {
		chatplus$selectMode(mode, "");
	}

	private void chatplus$selectMode(ChatInputMode mode, String whisperPartner) {
		ChatInputMode.setSelected(mode);
		chatplus$selectedWhisperPartner = whisperPartner == null ? "" : whisperPartner;
		if (!chatplus$selectedWhisperPartner.isEmpty()) {
			ChatTabManager.getInstance().markWhisperRead(chatplus$selectedWhisperPartner);
		}
		chatplus$showWhisperOverflow = false;
		chatplus$scrollOffset = 0;
		chatplus$syncPrefixBox();
		chatplus$focusInput();
		chatplus$refreshButtonLabels();
	}

	private void chatplus$renderTabs(GuiGraphics graphics, int panelX, int panelY) {
		chatplus$tabHits.clear();
		chatplus$tabCloseHits.clear();
		chatplus$overflowTabHits.clear();
		chatplus$overflowHit = null;
		int x = panelX + PANEL_PADDING;
		int y = panelY + 2;
		int maxX = chatplus$getGearIconX(panelX, ChatWindowState.getWidth(width)) - ICON_GAP - TAB_BREAK_WIDTH - TAB_GAP;
		boolean compactTabs = chatplus$needsCompactTabs(maxX - x);

		for (ChatInputMode mode : ChatInputMode.values()) {
			x = chatplus$renderTab(graphics, compactTabs ? chatplus$getCompactLabel(mode) : mode.getLabel(), mode, "", x, y, maxX);
		}

		List<String> partners = ChatTabManager.getInstance().getWhisperPartners();
		if (!partners.isEmpty() && x < maxX) {
			chatplus$renderOverflowBreak(graphics, x - Math.max(2, TAB_GAP / 2), panelY);
			x += TAB_GAP;
		}
		for (int i = 0; i < partners.size(); i++) {
			String partner = partners.get(i);
			int tabWidth = chatplus$getTabWidth(partner, !partner.isEmpty());
			if (x + tabWidth > maxX) {
				chatplus$whisperOverflowStartIndex = i;
				chatplus$renderOverflowTab(graphics, x, y, maxX, partners.size() - i);
				break;
			}
			x = chatplus$renderTab(graphics, partner, ChatInputMode.ALL, partner, x, y, maxX);
		}

		chatplus$renderOverflowBreak(graphics, maxX, panelY);
		if (chatplus$showWhisperOverflow && chatplus$overflowHit != null) {
			chatplus$renderWhisperOverflowDropdown(graphics, partners);
		}
	}

	private int chatplus$renderTab(GuiGraphics graphics, String label, ChatInputMode mode, String whisperPartner, int x, int y, int maxX) {
		if (x >= maxX) {
			return x;
		}

		boolean selected = chatplus$isSelectedTab(mode, whisperPartner);
		boolean closeable = !whisperPartner.isEmpty();
		boolean flashing = !whisperPartner.isEmpty() && ChatTabManager.getInstance().hasUnreadWhisper(whisperPartner) && chatplus$flashOn();
		int tabWidth = Math.min(chatplus$getTabWidth(label, closeable), maxX - x);
		if (tabWidth < chatplus$getMinimumTabWidth(label)) {
			if (closeable || tabWidth < 8) {
				return x;
			}
			tabWidth = Math.max(8, tabWidth);
		}
		graphics.fill(x, y, x + tabWidth, y + TAB_BUTTON_HEIGHT, flashing ? 0xCC775555 : selected ? 0xCC666666 : 0xAA333333);
		graphics.renderOutline(x, y, tabWidth, TAB_BUTTON_HEIGHT, selected ? 0xFFFFFFFF : flashing ? 0xFFFF7777 : 0xAA888888);
		graphics.drawString(font, chatplus$fitLabel(label, Math.max(8, tabWidth - (closeable ? 18 : 8))), x + 5, y + 2, 0xFFFFFFFF, false);
		if (closeable) {
			int closeX = x + tabWidth - 12;
			graphics.drawString(font, "x", closeX + 3, y + 2, 0xFFFF7777, false);
			chatplus$tabCloseHits.add(new TabCloseHit(whisperPartner, closeX, y, 10, TAB_BUTTON_HEIGHT));
		}
		chatplus$tabHits.add(new TabHit(label, mode, whisperPartner, x, y, tabWidth, TAB_BUTTON_HEIGHT));
		return x + tabWidth + TAB_GAP;
	}

	private void chatplus$renderOverflowTab(GuiGraphics graphics, int x, int y, int maxX, int count) {
		if (maxX - x < 18) {
			return;
		}

		String label = "+" + count;
		int tabWidth = Math.min(Math.max(22, font.width(label) + 10), maxX - x);
		graphics.fill(x, y, x + tabWidth, y + TAB_BUTTON_HEIGHT, 0xAA333333);
		graphics.renderOutline(x, y, tabWidth, TAB_BUTTON_HEIGHT, 0xAA888888);
		graphics.drawString(font, label, x + 5, y + 2, 0xFFFFFFFF, false);
		chatplus$overflowHit = new OverflowHit(x, y, tabWidth, TAB_BUTTON_HEIGHT);
	}

	private int chatplus$getTabWidth(String label, boolean closeable) {
		return Math.max(chatplus$getMinimumTabWidth(label), font.width(label) + (closeable ? 22 : 10));
	}

	private int chatplus$getMinimumTabWidth(String label) {
		return ">".equals(label) ? Math.max(14, font.width(">") + 8) : Math.max(18, font.width(label) + 10);
	}

	private boolean chatplus$needsCompactTabs(int availableWidth) {
		int fullWidth = 0;
		for (ChatInputMode mode : ChatInputMode.values()) {
			fullWidth += chatplus$getTabWidth(mode.getLabel(), false) + TAB_GAP;
		}
		return fullWidth > availableWidth;
	}

	private String chatplus$getCompactLabel(ChatInputMode mode) {
		return switch (mode) {
			case SERVER -> ">";
			case ALL -> "A";
			case GENERAL -> "G";
			case LOCAL -> "L";
			case TEAM -> "T";
		};
	}

	private void chatplus$renderOverflowBreak(GuiGraphics graphics, int x, int panelY) {
		graphics.fill(x, panelY + 1, x + TAB_BREAK_WIDTH, panelY + TAB_HEIGHT - 1, 0xAA9A9A9A);
	}

	private String chatplus$fitLabel(String label, int maxWidth) {
		String fitted = label;
		while (font.width(fitted) > maxWidth && fitted.length() > 1) {
			fitted = fitted.substring(0, fitted.length() - 1);
		}
		return fitted;
	}

	private boolean chatplus$handleOverflowClick(double mouseX, double mouseY) {
		if (chatplus$selectOverflowTabAt(mouseX, mouseY)) {
			return true;
		}

		if (chatplus$overflowHit != null
				&& chatplus$isInBox(mouseX, mouseY, chatplus$overflowHit.x(), chatplus$overflowHit.y(), chatplus$overflowHit.width(), chatplus$overflowHit.height())) {
			chatplus$showWhisperOverflow = !chatplus$showWhisperOverflow;
			chatplus$focusInput();
			return true;
		}

		if (chatplus$showWhisperOverflow) {
			chatplus$showWhisperOverflow = false;
		}
		return false;
	}

	private boolean chatplus$selectOverflowTabAt(double mouseX, double mouseY) {
		for (TabHit tab : chatplus$overflowTabHits) {
			if (mouseX >= tab.x()
					&& mouseX < tab.x() + tab.width()
					&& mouseY >= tab.y()
					&& mouseY < tab.y() + tab.height()) {
				chatplus$selectMode(tab.mode(), tab.whisperPartner());
				return true;
			}
		}

		return false;
	}

	private boolean chatplus$scrollOverflow(double mouseX, double mouseY, double verticalAmount) {
		if (chatplus$overflowHit == null) {
			return false;
		}

		boolean onOverflowTab = chatplus$isInBox(mouseX, mouseY, chatplus$overflowHit.x(), chatplus$overflowHit.y(), chatplus$overflowHit.width(), chatplus$overflowHit.height());
		boolean onDropdown = chatplus$showWhisperOverflow && chatplus$isInBox(mouseX, mouseY, chatplus$overflowHit.x(), chatplus$overflowHit.y() + TAB_BUTTON_HEIGHT + 2, OVERFLOW_DROPDOWN_WIDTH, OVERFLOW_DROPDOWN_ROW_HEIGHT * 5 + 4);
		if (!onOverflowTab && !onDropdown) {
			return false;
		}

		int hiddenCount = Math.max(0, ChatTabManager.getInstance().getWhisperPartners().size() - chatplus$whisperOverflowStartIndex);
		int maxScroll = Math.max(0, hiddenCount - 5);
		chatplus$whisperOverflowScroll = chatplus$clamp(chatplus$whisperOverflowScroll + (verticalAmount < 0 ? 1 : -1), 0, maxScroll);
		chatplus$showWhisperOverflow = true;
		return true;
	}

	private void chatplus$renderWhisperOverflowDropdown(GuiGraphics graphics, List<String> partners) {
		int x = chatplus$overflowHit.x();
		int y = chatplus$overflowHit.y() + TAB_BUTTON_HEIGHT + 2;
		int hiddenCount = Math.max(0, partners.size() - chatplus$whisperOverflowStartIndex);
		int maxRows = Math.min(5, hiddenCount);
		int maxScroll = Math.max(0, hiddenCount - maxRows);
		chatplus$whisperOverflowScroll = chatplus$clamp(chatplus$whisperOverflowScroll, 0, maxScroll);

		graphics.fill(x, y, x + OVERFLOW_DROPDOWN_WIDTH, y + maxRows * OVERFLOW_DROPDOWN_ROW_HEIGHT + 4, 0xEE151515);
		graphics.renderOutline(x, y, OVERFLOW_DROPDOWN_WIDTH, maxRows * OVERFLOW_DROPDOWN_ROW_HEIGHT + 4, 0xCCFFFFFF);
		for (int row = 0; row < maxRows; row++) {
			String partner = partners.get(chatplus$whisperOverflowStartIndex + chatplus$whisperOverflowScroll + row);
			int rowY = y + 2 + row * OVERFLOW_DROPDOWN_ROW_HEIGHT;
			boolean unread = ChatTabManager.getInstance().hasUnreadWhisper(partner) && chatplus$flashOn();
			graphics.fill(x + 2, rowY, x + OVERFLOW_DROPDOWN_WIDTH - 2, rowY + OVERFLOW_DROPDOWN_ROW_HEIGHT, unread ? 0x88774444 : 0x66444444);
			graphics.drawString(font, chatplus$fitLabel(partner, OVERFLOW_DROPDOWN_WIDTH - 10), x + 5, rowY + 3, unread ? 0xFFFFAAAA : 0xFFFFFFFF, false);
			chatplus$overflowTabHits.add(new TabHit(partner, ChatInputMode.ALL, partner, x + 2, rowY, OVERFLOW_DROPDOWN_WIDTH - 4, OVERFLOW_DROPDOWN_ROW_HEIGHT));
		}
	}

	private boolean chatplus$isSelectedTab(ChatInputMode mode, String whisperPartner) {
		if (!whisperPartner.isEmpty()) {
			return ChatInputMode.getSelected() == ChatInputMode.ALL && whisperPartner.equals(chatplus$selectedWhisperPartner);
		}

		return ChatInputMode.getSelected() == mode && chatplus$selectedWhisperPartner.isEmpty();
	}

	private void chatplus$renderHeaderIcons(GuiGraphics graphics, int panelX, int panelY, int panelWidth) {
		int closeX = chatplus$getCloseIconX(panelX, panelWidth);
		int gearX = chatplus$getGearIconX(panelX, panelWidth);
		chatplus$renderIconButton(graphics, gearX, panelY + 2, "\u2699", chatplus$showSettings ? 0xCC777777 : 0xAA444444);
		chatplus$renderIconButton(graphics, closeX, panelY + 2, "X", 0xAA553333);
	}

	private void chatplus$renderIconButton(GuiGraphics graphics, int x, int y, String label, int color) {
		graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, color);
		graphics.renderOutline(x, y, ICON_SIZE, ICON_SIZE, 0xAAFFFFFF);
		graphics.drawString(font, label, x + 3, y + 2, 0xFFFFFFFF, false);
	}

	private boolean chatplus$handleHeaderClick(double mouseX, double mouseY) {
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);
		if (chatplus$isInBox(mouseX, mouseY, chatplus$getCloseIconX(panelX, panelWidth), panelY + 2, ICON_SIZE, ICON_SIZE)) {
			minecraft.setScreen(null);
			return true;
		}

		if (chatplus$isInBox(mouseX, mouseY, chatplus$getGearIconX(panelX, panelWidth), panelY + 2, ICON_SIZE, ICON_SIZE)) {
			chatplus$showSettings = !chatplus$showSettings;
			chatplus$focusInput();
			return true;
		}

		return false;
	}

	private int chatplus$getCloseIconX(int panelX, int panelWidth) {
		return panelX + panelWidth - PANEL_PADDING - ICON_SIZE;
	}

	private int chatplus$getGearIconX(int panelX, int panelWidth) {
		return chatplus$getCloseIconX(panelX, panelWidth) - ICON_SIZE - ICON_GAP;
	}

	private void chatplus$renderMessages(GuiGraphics graphics, int panelX, int panelY, int panelWidth, int inputY) {
		int left = panelX + PANEL_PADDING;
		int top = panelY + TAB_HEIGHT + PANEL_PADDING;
		List<ChatMessage> messages = chatplus$getSelectedMessages();
		boolean showScrollbar = messages.size() > 1;
		int right = panelX + panelWidth - PANEL_PADDING - (showScrollbar ? SCROLLBAR_WIDTH + 3 : 0);
		int bottom = inputY - PANEL_PADDING;
		int textLeft = chatplus$chatHeadsEnabled ? left + HEAD_SIZE + HEAD_GAP : left;
		int textRight = right;
		float scale = chatplus$getTextScale();
		int lineHeight = chatplus$getLineHeight();
		int lineY = bottom - lineHeight;
		chatplus$clampScrollOffset();
		chatplus$senderHits.clear();
		chatplus$linkHits.clear();

		graphics.enableScissor(left, top, right, bottom);
		for (int i = messages.size() - 1 - chatplus$scrollOffset; i >= 0 && lineY >= top; i--) {
			ChatMessage message = messages.get(i);
			Component displayMessage = chatplus$formatMessage(message);
			List<FormattedText> lineTexts = font.getSplitter().splitLines(displayMessage, Math.max(20, (int) ((textRight - textLeft) / scale)), Style.EMPTY);
			List<FormattedCharSequence> lines = Language.getInstance().getVisualOrder(lineTexts);
			for (int lineIndex = lines.size() - 1; lineIndex >= 0 && lineY >= top; lineIndex--) {
				if (chatplus$isSystemDeathMessage(message)) {
					graphics.fill(left - 2, lineY - 1, right + 2, lineY + lineHeight, DEATH_BACKGROUND_COLOR);
				}
				chatplus$drawScaledString(graphics, lines.get(lineIndex), textLeft, lineY, 0xFFFFFFFF, scale);
				chatplus$trackLinkHits(lineTexts.get(lineIndex).getString(), textLeft, lineY, lineHeight, scale);
				if (lineIndex == 0) {
					if (chatplus$chatHeadsEnabled) {
						chatplus$renderPlayerHead(graphics, message, left, lineY);
					}
					chatplus$trackSenderHit(message, textLeft, lineY, (int) ((textRight - textLeft) * scale), lineHeight);
				}
				lineY -= lineHeight;
			}
		}
		graphics.disableScissor();
		chatplus$renderScrollbar(graphics, panelX, panelY, panelWidth, inputY, messages.size());
	}

	private List<ChatMessage> chatplus$getSelectedMessages() {
		List<ChatMessage> selected;
		if (!chatplus$selectedWhisperPartner.isEmpty()) {
			selected = ChatTabManager.getInstance().getWhisperMessages(chatplus$selectedWhisperPartner);
			return chatplus$withRecentDeathAlerts(selected);
		}

		if (ChatInputMode.getSelected() == ChatInputMode.ALL) {
			return ChatTabManager.getInstance().getAllParsedMessages();
		}

		selected = ChatTabManager.getInstance().getParsedTabMessages(chatplus$getSelectedTabType());
		return chatplus$withRecentDeathAlerts(selected);
	}

	private List<ChatMessage> chatplus$withRecentDeathAlerts(List<ChatMessage> selected) {
		if (ChatInputMode.getSelected() == ChatInputMode.ALL) {
			return selected;
		}

		long cutoff = System.currentTimeMillis() - 10000L;
		List<ChatMessage> merged = new ArrayList<>(selected);
		for (ChatMessage message : ChatTabManager.getInstance().getAllParsedMessages()) {
			if (message.getTimestamp() >= cutoff
					&& chatplus$isSystemDeathMessage(message)
					&& !merged.contains(message)) {
				merged.add(message);
			}
		}
		merged.sort((first, second) -> Long.compare(first.getTimestamp(), second.getTimestamp()));
		return merged;
	}

	private ChatTabType chatplus$getSelectedTabType() {
		return switch (ChatInputMode.getSelected()) {
			case SERVER -> ChatTabType.SYSTEM;
			case ALL -> ChatTabType.GENERAL;
			case GENERAL -> ChatTabType.GENERAL;
			case LOCAL -> ChatTabType.LOCAL_CHAT;
			case TEAM -> ChatTabType.TEAM_CHAT;
		};
	}

	private int chatplus$getMessageColor(ChatMessage message) {
		return switch (message.getType()) {
			case LOCAL_CHAT -> chatplus$localColor;
			case WHISPER -> chatplus$whisperColor;
			case TEAM_CHAT -> chatplus$teamColor;
			case SYSTEM -> chatplus$systemColor;
			default -> chatplus$generalColor;
		};
	}

	private boolean chatplus$isInDragHandle(double mouseX, double mouseY) {
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);
		int handleStartX = panelX + PANEL_PADDING + chatplus$getTabsWidth() + 6;
		return mouseX >= handleStartX
				&& mouseX <= panelX + panelWidth
				&& mouseY >= panelY
				&& mouseY <= panelY + TAB_HEIGHT;
	}

	private boolean chatplus$isOnResizeEdge(double mouseX, double mouseY) {
		return chatplus$isOnRightResizeEdge(mouseX, mouseY) || chatplus$isOnBottomResizeEdge(mouseX, mouseY);
	}

	private boolean chatplus$isOnRightResizeEdge(double mouseX, double mouseY) {
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);
		int panelHeight = ChatWindowState.getHeight(height);
		return mouseX >= panelX + panelWidth - RESIZE_EDGE
				&& mouseX <= panelX + panelWidth + RESIZE_EDGE
				&& mouseY >= panelY + TAB_HEIGHT
				&& mouseY <= panelY + panelHeight + RESIZE_EDGE;
	}

	private boolean chatplus$isOnBottomResizeEdge(double mouseX, double mouseY) {
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);
		int panelHeight = ChatWindowState.getHeight(height);
		return mouseX >= panelX
				&& mouseX <= panelX + panelWidth + RESIZE_EDGE
				&& mouseY >= panelY + panelHeight - RESIZE_EDGE
				&& mouseY <= panelY + panelHeight + RESIZE_EDGE;
	}

	private int chatplus$getTabsWidth() {
		int tabsWidth = 0;
		for (ChatInputMode mode : ChatInputMode.values()) {
			tabsWidth += chatplus$getTabWidth(mode.getLabel(), false) + 2;
		}
		return tabsWidth;
	}

	private void chatplus$trackSenderHit(ChatMessage message, int x, int y, int maxWidth, int height) {
		String sender = chatplus$profileNameFor(message);
		if (sender.isBlank()) {
			return;
		}

		int width = Math.min(maxWidth, font.width(message.getSender()));
		chatplus$senderHits.add(new SenderHit(sender, message.getSender(), chatplus$senderStyleTrace(message), x, y, width, height));
	}

	private String chatplus$senderStyleTrace(ChatMessage message) {
		String sender = message.getSender();
		String fullText = message.getDisplayMessage().getString();
		int senderStart = fullText.indexOf(sender);
		if (sender.isBlank() || senderStart < 0) {
			return "";
		}

		int senderEnd = senderStart + sender.length();
		List<String> parts = new ArrayList<>();
		int[] position = {0};
		message.getDisplayMessage().visit((style, text) -> {
			int textStart = position[0];
			int textEnd = textStart + text.length();
			if (textEnd > senderStart && textStart < senderEnd) {
				chatplus$appendStyleTrace(style, parts);
			}
			position[0] = textEnd;
			return Optional.empty();
		}, Style.EMPTY);
		return String.join(",", parts);
	}

	private void chatplus$appendStyleTrace(Style style, List<String> parts) {
		if (style == null || style.isEmpty() || style.getColor() == null) {
			return;
		}

		String color = style.getColor().serialize();
		String rgb = String.format("#%06X", style.getColor().getValue());
		String colorPart = "color=" + color;
		String rgbPart = "rgb=" + rgb;
		if (!parts.contains(colorPart)) {
			parts.add(colorPart);
		}
		if (!parts.contains(rgbPart)) {
			parts.add(rgbPart);
		}
	}

	private boolean chatplus$openProfileAt(double mouseX, double mouseY) {
		for (SenderHit hit : chatplus$senderHits) {
			if (mouseX >= hit.x()
					&& mouseX < hit.x() + hit.width()
					&& mouseY >= hit.y()
					&& mouseY < hit.y() + hit.height()) {
				minecraft.setScreen(new PlayerProfileScreen(this, hit.sender(), hit.displayName(), hit.styleTrace()));
				return true;
			}
		}

		return false;
	}

	private void chatplus$trackLinkHits(String line, int x, int y, int height, float scale) {
		Matcher matcher = URL_PATTERN.matcher(line);
		while (matcher.find()) {
			int linkX = x + Math.round(font.width(line.substring(0, matcher.start())) * scale);
			int linkWidth = Math.max(4, Math.round(font.width(matcher.group()) * scale));
			chatplus$linkHits.add(new LinkHit(matcher.group(), linkX, y, linkWidth, height));
		}
	}

	private boolean chatplus$openLinkAt(double mouseX, double mouseY) {
		for (LinkHit hit : chatplus$linkHits) {
			if (mouseX >= hit.x()
					&& mouseX < hit.x() + hit.width()
					&& mouseY >= hit.y()
					&& mouseY < hit.y() + hit.height()) {
				try {
					defaultHandleClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(hit.url())), minecraft, this);
				} catch (IllegalArgumentException ignored) {
					return false;
				}
				return true;
			}
		}

		return false;
	}

	private String chatplus$profileNameFor(ChatMessage message) {
		if (message.getType() == ChatTabType.SYSTEM || message.getSender().isBlank() || message.getSender().equals("UNKNOWN")) {
			return "";
		}

		String cleaned = message.getSender();
		if (message.isOutgoing() && minecraft.getUser() != null) {
			return minecraft.getUser().getName();
		}

		if (cleaned.startsWith("You -> ")) {
			cleaned = cleaned.substring("You -> ".length());
		}

		String[] parts = cleaned.trim().split("\\s+");
		return parts.length == 0 ? cleaned.trim() : parts[parts.length - 1];
	}

	private void chatplus$renderPlayerHead(GuiGraphics graphics, ChatMessage message, int x, int y) {
		String playerName = chatplus$profileNameFor(message);
		if (playerName.isBlank() || minecraft.getConnection() == null) {
			return;
		}

		PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(playerName);
		if (playerInfo == null) {
			return;
		}

		Identifier texture = playerInfo.getSkin().body().texturePath();
		int headY = y + HEAD_Y_OFFSET;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, headY, 8.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 64, 64);
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, headY, 40.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 64, 64);
	}

	private void chatplus$renderScrollbar(GuiGraphics graphics, int panelX, int panelY, int panelWidth, int inputY, int messageCount) {
		if (messageCount <= 1) {
			return;
		}

		int trackX = panelX + panelWidth - PANEL_PADDING - SCROLLBAR_WIDTH;
		int trackTop = panelY + TAB_HEIGHT + PANEL_PADDING;
		int trackBottom = inputY - PANEL_PADDING;
		int trackHeight = Math.max(1, trackBottom - trackTop);
		int thumbHeight = Math.max(12, trackHeight / Math.max(2, messageCount / 4));
		int maxOffset = Math.max(1, messageCount - 1);
		int travel = Math.max(1, trackHeight - thumbHeight);
		int thumbY = trackBottom - thumbHeight - Math.round(chatplus$scrollOffset * travel / (float) maxOffset);
		graphics.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackBottom, 0x55333333);
		graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xCCAAAAAA);
	}

	private int chatplus$getInputY() {
		return ChatWindowState.getY(height) + ChatWindowState.getHeight(height) - INPUT_HEIGHT - PANEL_PADDING;
	}

	private boolean chatplus$isInMessageArea(double mouseX, double mouseY) {
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);
		return mouseX >= panelX
				&& mouseX <= panelX + panelWidth
				&& mouseY >= panelY + TAB_HEIGHT
				&& mouseY <= chatplus$getInputY();
	}

	private void chatplus$clampScrollOffset() {
		int maxOffset = Math.max(0, chatplus$getSelectedMessages().size() - 1);
		chatplus$scrollOffset = Math.min(chatplus$scrollOffset, maxOffset);
	}

	private void chatplus$syncPrefixBox() {
		if (!chatplus$selectedWhisperPartner.isEmpty()) {
			chatplus$setPrefixValue(chatplus$getWhisperPrefix());
			return;
		}

		chatplus$setPrefixValue(ChatInputMode.getSelected().getPrefix());
	}

	private String chatplus$getWhisperPrefix() {
		return "/tell " + chatplus$selectedWhisperPartner + " ";
	}

	private void chatplus$renderPrefixButton(GuiGraphics graphics) {
		int x = ChatWindowState.getX() + PANEL_PADDING;
		int y = chatplus$getInputY();
		String label = chatplus$getPrefixLabel();
		int width = chatplus$getPrefixBoxWidth();
		graphics.fill(x, y, x + width, y + INPUT_HEIGHT, 0xCC555555);
		graphics.renderOutline(x, y, width, INPUT_HEIGHT, 0xCCFFFFFF);
		graphics.drawString(font, label, x + 4, y + 2, 0xFFFFFFFF, false);
	}

	private boolean chatplus$isInPrefixButton(double mouseX, double mouseY) {
		return chatplus$isInBox(mouseX, mouseY, ChatWindowState.getX() + PANEL_PADDING, chatplus$getInputY(), chatplus$getPrefixBoxWidth(), INPUT_HEIGHT);
	}

	private String chatplus$getPrefixLabel() {
		if (chatplus$prefixValue.isBlank()) {
			return ChatInputMode.getSelected().getLabel();
		}

		String label = chatplus$prefixValue.trim();
		if (label.startsWith("/tell ")) {
			String partner = label.substring(6).trim();
			label = "/tell " + partner;
		}

		int maxWidth = MAX_PREFIX_BOX_WIDTH - 8;
		while (font.width(label) > maxWidth && label.length() > 1) {
			label = label.substring(0, label.length() - 1);
		}
		return label;
	}

	private int chatplus$getPrefixBoxWidth() {
		return chatplus$clamp(font.width(chatplus$getPrefixLabel()) + 8, MIN_PREFIX_BOX_WIDTH, MAX_PREFIX_BOX_WIDTH);
	}

	private void chatplus$setPrefixValue(String prefix) {
		chatplus$prefixValue = prefix == null ? "" : prefix;
	}

	private String chatplus$applyPrefix(String message) {
		if (message == null) {
			return message;
		}

		String trimmedMessage = message.trim();
		String prefix = chatplus$prefixValue;
		if (prefix == null || prefix.isBlank() || trimmedMessage.isEmpty() || trimmedMessage.startsWith("/")) {
			return message;
		}

		if (!Character.isWhitespace(prefix.charAt(prefix.length() - 1))) {
			prefix += " ";
		}

		return prefix + message;
	}

	private void chatplus$renderSettingsPanel(GuiGraphics graphics, int panelX, int panelY, int panelWidth) {
		int x = panelX + panelWidth - SETTINGS_WIDTH - PANEL_PADDING;
		int y = panelY + TAB_HEIGHT + PANEL_PADDING;
		int height = chatplus$getSettingsPanelHeight();
		graphics.fill(x, y, x + SETTINGS_WIDTH, y + height, 0xDD151515);
		graphics.renderOutline(x, y, SETTINGS_WIDTH, height, 0xCCFFFFFF);
		chatplus$renderSettingsTabs(graphics, x, y);
		int contentY = y + PANEL_PADDING + SETTINGS_ROW_HEIGHT;
		if (chatplus$settingsPage == 0) {
			chatplus$renderOptionRow(graphics, x, contentY, "Size", Integer.toString(chatplus$textSize));
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT, "Spacing", Integer.toString(chatplus$lineSpacing));
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 2, "Font", chatplus$getFontStyleLabel());
			chatplus$renderSliderRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 3, "Opacity", chatplus$backgroundOpacity);
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 4, "HUD BG", chatplus$hudBackgroundEnabled ? "On" : "Off");
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 5, "Heads", chatplus$chatHeadsEnabled ? "On" : "Off");
		} else if (chatplus$settingsPage == 1) {
			chatplus$renderColorRow(graphics, x, contentY, "General", chatplus$generalColor);
			chatplus$renderColorRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT, "Local", chatplus$localColor);
			chatplus$renderColorRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 2, "Team", chatplus$teamColor);
			chatplus$renderColorRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 3, "Whisper", chatplus$whisperColor);
			chatplus$renderColorRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 4, "System", chatplus$systemColor);
		} else {
			chatplus$renderOptionRow(graphics, x, contentY, "Clear", chatplus$getHistoryLabel());
			chatplus$renderActionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 2, "Delete old", System.currentTimeMillis() < chatplus$historyClearFlashUntil);
		}
	}

	private int chatplus$getSettingsPanelHeight() {
		return SETTINGS_ROW_HEIGHT * 7 + PANEL_PADDING * 2;
	}

	private void chatplus$renderSettingsTabs(GuiGraphics graphics, int panelX, int panelY) {
		int tabY = panelY + 4;
		int tabX = panelX + 6;
		int gap = 3;
		int tabWidth = (SETTINGS_WIDTH - 12 - gap * 2) / 3;
		chatplus$renderSettingsTab(graphics, tabX, tabY, tabWidth, "Settings", chatplus$settingsPage == 0);
		chatplus$renderSettingsTab(graphics, tabX + tabWidth + gap, tabY, tabWidth, "Colors", chatplus$settingsPage == 1);
		chatplus$renderSettingsTab(graphics, tabX + (tabWidth + gap) * 2, tabY, tabWidth, "History", chatplus$settingsPage == 2);
	}

	private void chatplus$renderSettingsTab(GuiGraphics graphics, int x, int y, int width, String label, boolean selected) {
		graphics.fill(x, y, x + width, y + 12, selected ? 0xCC777777 : 0xAA333333);
		graphics.renderOutline(x, y, width, 12, selected ? 0xFFFFFFFF : 0xAA888888);
		graphics.drawString(font, chatplus$fitLabel(label, width - 8), x + 4, y + 2, 0xFFFFFFFF, false);
	}

	private void chatplus$renderOptionRow(GuiGraphics graphics, int panelX, int rowY, String label, String value) {
		graphics.drawString(font, label, panelX + 6, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 78, rowY + 2, panelX + 90, rowY + 14, 0xAA444444);
		graphics.renderOutline(panelX + 78, rowY + 2, 12, 12, 0xAAFFFFFF);
		graphics.drawString(font, "-", panelX + 82, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 94, rowY + 2, panelX + SETTINGS_WIDTH - 22, rowY + 14, 0xAA333333);
		graphics.renderOutline(panelX + 94, rowY + 2, SETTINGS_WIDTH - 116, 12, 0xAA888888);
		graphics.drawString(font, value, panelX + 98, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + SETTINGS_WIDTH - 18, rowY + 2, panelX + SETTINGS_WIDTH - 6, rowY + 14, 0xAA444444);
		graphics.renderOutline(panelX + SETTINGS_WIDTH - 18, rowY + 2, 12, 12, 0xAAFFFFFF);
		graphics.drawString(font, "+", panelX + SETTINGS_WIDTH - 15, rowY + 4, 0xFFFFFFFF, false);
	}

	private void chatplus$renderSliderRow(GuiGraphics graphics, int panelX, int rowY, String label, int value) {
		int sliderX = panelX + 78;
		int sliderY = rowY + 6;
		int knobX = sliderX + Math.round((value - 1) * (SLIDER_WIDTH - 4) / 99.0F);
		graphics.drawString(font, label, panelX + 6, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + 3, 0xAA444444);
		graphics.fill(knobX, rowY + 3, knobX + 4, rowY + 13, 0xCCFFFFFF);
		graphics.drawString(font, Integer.toString(value), panelX + SETTINGS_WIDTH - 28, rowY + 4, 0xFFFFFFFF, false);
	}

	private void chatplus$renderColorRow(GuiGraphics graphics, int panelX, int rowY, String label, int color) {
		graphics.drawString(font, label, panelX + 6, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 78, rowY + 2, panelX + 90, rowY + 14, 0xAA444444);
		graphics.renderOutline(panelX + 78, rowY + 2, 12, 12, 0xAAFFFFFF);
		graphics.drawString(font, "-", panelX + 82, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 94, rowY + 2, panelX + SETTINGS_WIDTH - 22, rowY + 14, 0xAA333333);
		graphics.renderOutline(panelX + 94, rowY + 2, SETTINGS_WIDTH - 116, 12, 0xAA888888);
		graphics.fill(panelX + 98, rowY + 4, panelX + 108, rowY + 12, color);
		graphics.renderOutline(panelX + 98, rowY + 4, 10, 8, 0xAAFFFFFF);
		graphics.drawString(font, chatplus$getColorLabel(color), panelX + 112, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + SETTINGS_WIDTH - 18, rowY + 2, panelX + SETTINGS_WIDTH - 6, rowY + 14, 0xAA444444);
		graphics.renderOutline(panelX + SETTINGS_WIDTH - 18, rowY + 2, 12, 12, 0xAAFFFFFF);
		graphics.drawString(font, "+", panelX + SETTINGS_WIDTH - 15, rowY + 4, 0xFFFFFFFF, false);
	}

	private void chatplus$renderActionRow(GuiGraphics graphics, int panelX, int rowY, String label, boolean active) {
		graphics.drawString(font, label, panelX + 6, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 78, rowY + 2, panelX + SETTINGS_WIDTH - 6, rowY + 14, active ? 0xCC886666 : 0xAA553333);
		graphics.renderOutline(panelX + 78, rowY + 2, SETTINGS_WIDTH - 84, 12, active ? 0xFFFFFFFF : 0xAAFFFFFF);
		graphics.drawString(font, "Clear", panelX + 98, rowY + 4, 0xFFFFFFFF, false);
	}

	private boolean chatplus$handleSettingsClick(double mouseX, double mouseY) {
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);
		int x = panelX + panelWidth - SETTINGS_WIDTH - PANEL_PADDING;
		int y = panelY + TAB_HEIGHT + PANEL_PADDING;
		if (!chatplus$isInBox(mouseX, mouseY, x, y, SETTINGS_WIDTH, chatplus$getSettingsPanelHeight())) {
			return false;
		}

		int tabGap = 3;
		int tabWidth = (SETTINGS_WIDTH - 12 - tabGap * 2) / 3;
		int tabX = x + 6;
		if (chatplus$isInBox(mouseX, mouseY, tabX, y + 4, tabWidth, 12)) {
			chatplus$settingsPage = 0;
			chatplus$focusInput();
			return true;
		}

		if (chatplus$isInBox(mouseX, mouseY, tabX + tabWidth + tabGap, y + 4, tabWidth, 12)) {
			chatplus$settingsPage = 1;
			chatplus$focusInput();
			return true;
		}

		if (chatplus$isInBox(mouseX, mouseY, tabX + (tabWidth + tabGap) * 2, y + 4, tabWidth, 12)) {
			chatplus$settingsPage = 2;
			chatplus$focusInput();
			return true;
		}

		int contentY = y + PANEL_PADDING + SETTINGS_ROW_HEIGHT;
		int row = (int) ((mouseY - contentY) / SETTINGS_ROW_HEIGHT);
		if (row < 0 || row > 5) {
			return true;
		}

		if (chatplus$settingsPage == 2) {
			if (row == 0) {
				boolean decrease = chatplus$isInBox(mouseX, mouseY, x + 78, contentY + row * SETTINGS_ROW_HEIGHT + 2, 12, 12);
				boolean increase = chatplus$isInBox(mouseX, mouseY, x + SETTINGS_WIDTH - 18, contentY + row * SETTINGS_ROW_HEIGHT + 2, 12, 12);
				if (decrease || increase) {
					chatplus$historyRetentionDays = chatplus$nextHistoryDays(chatplus$historyRetentionDays, increase ? 1 : -1);
					ChatPlusConfig.setHistoryRetentionDays(chatplus$historyRetentionDays);
				}
			} else if (row == 2 && chatplus$isInBox(mouseX, mouseY, x + 78, contentY + row * SETTINGS_ROW_HEIGHT + 2, SETTINGS_WIDTH - 84, 12)) {
				ChatTabManager.getInstance().pruneHistory(chatplus$historyRetentionDays);
				chatplus$historyClearFlashUntil = System.currentTimeMillis() + 350L;
			}
			chatplus$focusInput();
			return true;
		}

		if (chatplus$settingsPage == 1) {
			boolean decrease = chatplus$isInBox(mouseX, mouseY, x + 78, contentY + row * SETTINGS_ROW_HEIGHT + 2, 12, 12);
			boolean increase = chatplus$isInBox(mouseX, mouseY, x + SETTINGS_WIDTH - 18, contentY + row * SETTINGS_ROW_HEIGHT + 2, 12, 12);
			boolean value = chatplus$isInBox(mouseX, mouseY, x + 94, contentY + row * SETTINGS_ROW_HEIGHT + 2, SETTINGS_WIDTH - 116, 12);
			if (row >= 0 && row <= 4 && (decrease || increase || value)) {
				chatplus$changeColor(row, decrease ? -1 : 1);
			}
			chatplus$focusInput();
			return true;
		}

		boolean decrease = chatplus$isInBox(mouseX, mouseY, x + 78, contentY + row * SETTINGS_ROW_HEIGHT + 2, 12, 12);
		boolean increase = chatplus$isInBox(mouseX, mouseY, x + SETTINGS_WIDTH - 18, contentY + row * SETTINGS_ROW_HEIGHT + 2, 12, 12);
		boolean value = chatplus$isInBox(mouseX, mouseY, x + 94, contentY + row * SETTINGS_ROW_HEIGHT + 2, SETTINGS_WIDTH - 116, 12);
		if (row == 3) {
			int sliderX = x + 78;
			if (chatplus$isInBox(mouseX, mouseY, sliderX, contentY + row * SETTINGS_ROW_HEIGHT + 2, SLIDER_WIDTH, 12)) {
				chatplus$draggingOpacity = true;
				chatplus$setOpacityFromMouse(mouseX);
			}
			chatplus$focusInput();
			return true;
		}

		if (!decrease && !increase && !value) {
			return true;
		}

		int delta = decrease ? -1 : 1;
		switch (row) {
			case 0 -> {
				chatplus$textSize = chatplus$clamp(chatplus$textSize + delta * 5, 50, 200);
				ChatPlusConfig.setTextSize(chatplus$textSize);
			}
			case 1 -> {
				chatplus$lineSpacing = chatplus$clamp(chatplus$lineSpacing + delta, 0, 6);
				ChatPlusConfig.setLineSpacing(chatplus$lineSpacing);
			}
			case 2 -> {
				chatplus$fontStyleIndex = (chatplus$fontStyleIndex + 1) % 2;
				ChatPlusConfig.setFontStyleIndex(chatplus$fontStyleIndex);
			}
			case 4 -> {
				chatplus$hudBackgroundEnabled = !chatplus$hudBackgroundEnabled;
				ChatPlusConfig.setHudBackgroundEnabled(chatplus$hudBackgroundEnabled);
			}
			case 5 -> {
				chatplus$chatHeadsEnabled = !chatplus$chatHeadsEnabled;
				ChatPlusConfig.setChatHeadsEnabled(chatplus$chatHeadsEnabled);
			}
			default -> {
			}
		}
		chatplus$focusInput();
		return true;
	}

	private int chatplus$nextHistoryDays(int current, int direction) {
		int[] values = {0, 1, 3, 7, 14, 30, 60, 90, 180, 365};
		int index = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i] == current) {
				index = i;
				break;
			}
		}
		return values[chatplus$clamp(index + direction, 0, values.length - 1)];
	}

	private String chatplus$getHistoryLabel() {
		return chatplus$historyRetentionDays <= 0 ? "Never" : chatplus$historyRetentionDays + "d+";
	}

	private void chatplus$changeColor(int row, int direction) {
		switch (row) {
			case 0 -> {
				chatplus$generalColor = chatplus$nextPaletteColor(chatplus$generalColor, direction);
				ChatPlusConfig.setGeneralColor(chatplus$generalColor);
			}
			case 1 -> {
				chatplus$localColor = chatplus$nextPaletteColor(chatplus$localColor, direction);
				ChatPlusConfig.setLocalColor(chatplus$localColor);
			}
			case 2 -> {
				chatplus$teamColor = chatplus$nextPaletteColor(chatplus$teamColor, direction);
				ChatPlusConfig.setTeamColor(chatplus$teamColor);
			}
			case 3 -> {
				chatplus$whisperColor = chatplus$nextPaletteColor(chatplus$whisperColor, direction);
				ChatPlusConfig.setWhisperColor(chatplus$whisperColor);
			}
			case 4 -> {
				chatplus$systemColor = chatplus$nextPaletteColor(chatplus$systemColor, direction);
				ChatPlusConfig.setSystemColor(chatplus$systemColor);
			}
			default -> {
			}
		}
	}

	private int chatplus$nextPaletteColor(int currentColor, int direction) {
		int index = 0;
		for (int i = 0; i < COLOR_PALETTE.length; i++) {
			if ((COLOR_PALETTE[i] & 0x00FFFFFF) == (currentColor & 0x00FFFFFF)) {
				index = i;
				break;
			}
		}
		return COLOR_PALETTE[chatplus$clamp(index + direction, 0, COLOR_PALETTE.length - 1)];
	}

	private String chatplus$getColorLabel(int color) {
		for (int i = 0; i < COLOR_PALETTE.length; i++) {
			if ((COLOR_PALETTE[i] & 0x00FFFFFF) == (color & 0x00FFFFFF)) {
				return COLOR_NAMES[i];
			}
		}
		return String.format("#%06X", color & 0x00FFFFFF);
	}

	private void chatplus$drawScaledString(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color, float scale) {
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		graphics.drawString(font, text, (int) (x / scale), (int) (y / scale), color, chatplus$usesTextShadow());
		graphics.pose().popMatrix();
	}

	private float chatplus$getTextScale() {
		return chatplus$textSize / 100.0F;
	}

	private int chatplus$getLineHeight() {
		return Math.max(7, Math.round(9.0F * chatplus$getTextScale()) + chatplus$lineSpacing);
	}

	private boolean chatplus$usesTextShadow() {
		return chatplus$fontStyleIndex == 0;
	}

	private Component chatplus$applySelectedFont(Component message) {
		if (chatplus$fontStyleIndex != 1) {
			return message;
		}

		return message.copy().withStyle(style -> style.withFont(LEXEND_FONT));
	}

	private Component chatplus$formatMessage(ChatMessage message) {
		return chatplus$applySelectedFont(ChatColorFormatter.linkifyUrls(ChatColorFormatter.colorMessageBody(message, chatplus$getMessageColor(message))));
	}

	private String chatplus$getFontStyleLabel() {
		return switch (chatplus$fontStyleIndex) {
			case 1 -> "Lexend";
			default -> "Default";
		};
	}

	private void chatplus$setOpacityFromMouse(double mouseX) {
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);
		int x = panelX + panelWidth - SETTINGS_WIDTH - PANEL_PADDING;
		int sliderX = x + 78;
		int relativeX = chatplus$clamp((int) mouseX - sliderX, 0, SLIDER_WIDTH);
		chatplus$backgroundOpacity = chatplus$clamp(Math.round(1 + relativeX * 99.0F / SLIDER_WIDTH), 1, 100);
		ChatPlusConfig.setBackgroundOpacity(chatplus$backgroundOpacity);
	}

	private int chatplus$opacityToAlpha(int opacity) {
		return chatplus$clamp(Math.round(chatplus$clamp(opacity, 1, 100) * 255.0F / 100.0F), 1, 255);
	}

	private int chatplus$withAlpha(int rgb, int alpha) {
		return (chatplus$clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
	}

	private boolean chatplus$isInBox(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private int chatplus$clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private boolean chatplus$flashOn() {
		return (System.currentTimeMillis() / 350L) % 2L == 0L;
	}

	private boolean chatplus$isSystemDeathMessage(ChatMessage message) {
		return message.getType() == ChatTabType.SYSTEM && ChatMessageParser.isDeathMessage(message.getRawMessage());
	}

	private void chatplus$focusInput() {
		setFocused(input);
		input.setFocused(true);
	}

	private void chatplus$refreshButtonLabels() {
		for (int i = 0; i < chatplus$modeButtons.size(); i++) {
			ChatInputMode mode = ChatInputMode.values()[i];
			chatplus$modeButtons.get(i).setMessage(chatplus$getButtonLabel(mode));
		}
	}

	private Component chatplus$getButtonLabel(ChatInputMode mode) {
		String label = mode == ChatInputMode.getSelected() ? "[" + mode.getLabel() + "]" : mode.getLabel();
		return Component.literal(label);
	}

	private int chatplus$getButtonWidth(ChatInputMode mode) {
		return switch (mode) {
			case SERVER -> 18;
			case ALL -> 34;
			case GENERAL -> 52;
			case LOCAL -> 42;
			case TEAM -> 42;
		};
	}
}
