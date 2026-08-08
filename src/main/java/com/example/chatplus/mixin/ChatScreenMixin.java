package com.example.chatplus.mixin;

import com.example.chatplus.chat.ChatInputMode;
import com.example.chatplus.chat.ChatColorFormatter;
import com.example.chatplus.chat.ChatMessage;
import com.example.chatplus.chat.ChatMessageParser;
import com.example.chatplus.chat.ChatPlusConfig;
import com.example.chatplus.chat.ChatTabManager;
import com.example.chatplus.chat.ChatTabType;
import com.example.chatplus.chat.ChatWindowState;
import com.example.chatplus.chat.GhostTimerManager;
import com.example.chatplus.chat.PlayerProfileStore;
import com.example.chatplus.gui.PlayerProfileScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
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
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
	private record TabHit(String label, ChatInputMode mode, String whisperPartner, int x, int y, int width, int height) {
	}

	private record TabCloseHit(ChatInputMode mode, String tabKey, int x, int y, int width, int height) {
	}

	private record OverflowHit(int x, int y, int width, int height) {
	}

	private record SenderHit(String sender, String displayName, String styleTrace, int x, int y, int width, int height) {
	}

	private record LinkHit(String url, int x, int y, int width, int height) {
	}

	private record TextLineHit(int id, String text, int x, int y, int height, float scale) {
	}

	private record ComponentLineHit(FormattedCharSequence text, int x, int y, int height, float scale) {
	}

	private record TextSelectionPoint(int lineId, int charIndex) {
	}

	private record EmoteOption(String label, String command, String symbol) {
	}

	private static final int TAB_HEIGHT = 16;
	private static final int TAB_BUTTON_HEIGHT = 12;
	private static final int TAB_BREAK_WIDTH = 2;
	private static final int TAB_GAP = 5;
	private static final int OVERFLOW_DROPDOWN_WIDTH = 126;
	private static final int OVERFLOW_DROPDOWN_ROW_HEIGHT = 14;
	private static final int INPUT_HEIGHT = 12;
	private static final int INPUT_TEXT_Y_OFFSET = 1;
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
	private static final Pattern COORDINATE_PATTERN = Pattern.compile("(?i)(?:\\b[xyz]\\s*[:=]\\s*)?-?\\d{2,7}(?:\\.\\d+)?[,\\s]+(?:\\b[xyz]\\s*[:=]\\s*)?-?\\d{1,4}(?:\\.\\d+)?[,\\s]+(?:\\b[xyz]\\s*[:=]\\s*)?-?\\d{2,7}(?:\\.\\d+)?");
	private static final int COREPROTECT_DEFAULT_ROWS = 8;
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
	private static final int EMOTE_BUTTON_WIDTH = 16;
	private static final int EMOTE_PICKER_WIDTH = 124;
	private static final int PREFIX_MENU_WIDTH = 92;
	private static final int WHISPER_PICKER_WIDTH = 124;
	private static final int WHISPER_PICKER_ROWS = 6;
	private static final int MENU_ROW_HEIGHT = 14;
	private static final int STATUS_BANNER_HEIGHT = 14;
	private static final int TAB_CONTEXT_WIDTH = 144;
	private static final int TAB_CONTEXT_HEIGHT = 49;
	private static final int PRIVATE_INDICATOR_HEIGHT = 14;
	private static final EmoteOption[] EMOTE_OPTIONS = {
			new EmoteOption("Heart", ":Heart:", "\u2665"),
			new EmoteOption("Broken Heart", ":Broken Heart:", "\uD83D\uDC94"),
			new EmoteOption("Fire", ":fire:", "\uD83D\uDD25"),
			new EmoteOption("Eyes", ":eyes:", "\uD83D\uDC40"),
			new EmoteOption("Rose", ":rose:", "\uD83C\uDF39"),
			new EmoteOption("Check", ":check:", "\u2713"),
			new EmoteOption("X", ":x:", "\u2715"),
			new EmoteOption("Star", ":star:", "\u2605"),
			new EmoteOption("Smile", ":smile:", "\u263A"),
			new EmoteOption("Sad", ":sad:", "\u2639"),
			new EmoteOption("Shrug", ":shrug:", "\u00AF\\_(\u30C4)_/\u00AF"),
			new EmoteOption("Wilted Rose", ":wilted_rose:", "\uD83E\uDD40"),
			new EmoteOption("Skull", ":skull:", "\u2620")
	};
	private static String chatplus$lastSelectedWhisperPartner = "";

	private final List<Button> chatplus$modeButtons = new ArrayList<>();
	private final List<TabHit> chatplus$tabHits = new ArrayList<>();
	private final List<TabCloseHit> chatplus$tabCloseHits = new ArrayList<>();
	private final List<TabHit> chatplus$overflowTabHits = new ArrayList<>();
	private final List<SenderHit> chatplus$senderHits = new ArrayList<>();
	private final List<LinkHit> chatplus$linkHits = new ArrayList<>();
	private final List<TextLineHit> chatplus$textLineHits = new ArrayList<>();
	private final List<ComponentLineHit> chatplus$componentLineHits = new ArrayList<>();
	private OverflowHit chatplus$overflowHit;
	private String chatplus$prefixValue = "";
	private String chatplus$prefixLabelOverride = "";
	private String chatplus$selectedWhisperPartner = "";
	private boolean chatplus$applyingPrefix;
	private boolean chatplus$manualSendPrefix;
	private boolean chatplus$showSettings;
	private boolean chatplus$showWhisperOverflow;
	private boolean chatplus$showPrefixMenu;
	private boolean chatplus$showEmotePicker;
	private boolean chatplus$awaitingWhisperName;
	private boolean chatplus$showTabContextMenu;
	private ChatTabType chatplus$contextTabType = ChatTabType.GENERAL;
	private int chatplus$contextMenuX;
	private int chatplus$contextMenuY;
	private int chatplus$whisperOverflowScroll;
	private int chatplus$whisperOverflowStartIndex;
	private int chatplus$whisperPickerScroll;
	private int chatplus$settingsPage;
	private long chatplus$historyClearFlashUntil;
	private int chatplus$textSize = ChatPlusConfig.get().textSize;
	private int chatplus$lineSpacing = ChatPlusConfig.get().lineSpacing;
	private int chatplus$fontStyleIndex = ChatPlusConfig.get().fontStyleIndex;
	private int chatplus$backgroundOpacity = ChatPlusConfig.get().backgroundOpacity;
	private boolean chatplus$hudBackgroundEnabled = ChatPlusConfig.get().hudBackgroundEnabled;
	private boolean chatplus$chatHeadsEnabled = ChatPlusConfig.get().chatHeadsEnabled;
	private boolean chatplus$timestampsEnabled = ChatPlusConfig.get().timestampsEnabled;
	private int chatplus$chatStyleIndex = ChatPlusConfig.get().chatStyleIndex;
	private boolean chatplus$emotesEnabled = ChatPlusConfig.get().emotesEnabled;
	private boolean chatplus$coordinateWarningsEnabled = ChatPlusConfig.get().coordinateWarningsEnabled;
	private boolean chatplus$skillTrackerMessagesInAll = ChatPlusConfig.get().skillTrackerMessagesInAll;
	private int chatplus$ghostTimerMode = ChatPlusConfig.get().ghostTimerMode;
	private int chatplus$generalColor = ChatPlusConfig.get().generalColor;
	private int chatplus$localColor = ChatPlusConfig.get().localColor;
	private int chatplus$teamColor = ChatPlusConfig.get().teamColor;
	private int chatplus$whisperColor = ChatPlusConfig.get().whisperColor;
	private int chatplus$systemColor = ChatPlusConfig.get().systemColor;
	private int chatplus$historyRetentionDays = ChatPlusConfig.get().historyRetentionDays;
	private boolean chatplus$dragging;
	private boolean chatplus$resizing;
	private boolean chatplus$draggingOpacity;
	private boolean chatplus$draggingPrivateIndicator;
	private boolean chatplus$selectingText;
	private boolean chatplus$searching;
	private boolean chatplus$confirmingCoordinateSend;
	private String chatplus$pendingCoordinateMessage = "";
	private String chatplus$pendingCoordinateTab = "";
	private String chatplus$draftBeforeSearch = "";
	private int chatplus$dragOffsetX;
	private int chatplus$dragOffsetY;
	private int chatplus$resizeStartWidth;
	private int chatplus$resizeStartHeight;
	private int chatplus$resizeStartMouseX;
	private int chatplus$resizeStartMouseY;
	private int chatplus$privateIndicatorX = ChatPlusConfig.get().privateIndicatorX;
	private int chatplus$privateIndicatorY = ChatPlusConfig.get().privateIndicatorY;
	private boolean chatplus$resizeRight;
	private boolean chatplus$resizeBottom;
	private int chatplus$scrollOffset;
	private TextSelectionPoint chatplus$selectionAnchor;
	private TextSelectionPoint chatplus$selectionFocus;
	private int chatplus$nextTextLineId;
	private boolean chatplus$ghostTimerBlockedSend;
	private long chatplus$cachedMessageRevision = -1L;
	private ChatInputMode chatplus$cachedMessageMode;
	private String chatplus$cachedWhisperPartner = "";
	private boolean chatplus$cachedShowWhispers;
	private boolean chatplus$cachedShowServer;
	private List<ChatMessage> chatplus$cachedBaseMessages = List.of();

	protected ChatScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void chatplus$addModeButtons(CallbackInfo ci) {
		chatplus$modeButtons.clear();

		chatplus$layoutWindow();

		int x = ChatWindowState.getX() + PANEL_PADDING;
		int y = ChatWindowState.getY(height) + 2;
		for (ChatInputMode mode : chatplus$getVisibleModes()) {
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

		String pendingWhisper = ChatTabManager.getInstance().consumePendingWhisperOpen();
		if (!pendingWhisper.isBlank()) {
			chatplus$selectMode(ChatInputMode.ALL, pendingWhisper);
		} else if (ChatInputMode.getSelected() == ChatInputMode.ALL
				&& !chatplus$lastSelectedWhisperPartner.isBlank()
				&& ChatTabManager.getInstance().getWhisperPartners().contains(chatplus$lastSelectedWhisperPartner)) {
			chatplus$selectMode(ChatInputMode.ALL, chatplus$lastSelectedWhisperPartner);
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void chatplus$renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
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
		graphics.outline(panelX, panelY, panelWidth, panelHeight, PANEL_BORDER_COLOR);
		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + TAB_HEIGHT, 0xAA202020);
		graphics.fill(panelX, inputY - 2, panelX + panelWidth, panelY + panelHeight, 0xAA202020);

		chatplus$renderTabs(graphics, panelX, panelY);
		chatplus$renderHeaderIcons(graphics, panelX, panelY, panelWidth);
		chatplus$renderMessages(graphics, panelX, panelY, panelWidth, inputY);
		chatplus$renderPrefixButton(graphics);
		chatplus$renderEmoteButton(graphics);
		chatplus$renderPrivateIndicator(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		chatplus$renderStatusBanner(graphics, panelX, inputY, panelWidth);
		if (chatplus$showPrefixMenu) {
			chatplus$renderPrefixMenu(graphics);
		}
		if (chatplus$awaitingWhisperName) {
			chatplus$renderWhisperPicker(graphics);
		}
		if (chatplus$showEmotePicker) {
			chatplus$renderEmotePicker(graphics);
		}
		if (chatplus$showTabContextMenu) {
			chatplus$renderTabContextMenu(graphics);
		}
		if (chatplus$showSettings) {
			chatplus$renderSettingsPanel(graphics, panelX, panelY, panelWidth);
		}
		commandSuggestions.extractRenderState(graphics, mouseX, mouseY);
		ci.cancel();
	}

	@Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
	private void chatplus$prefixChatInput(String message, boolean addToRecentChat, CallbackInfo ci) {
		if (chatplus$applyingPrefix) {
			return;
		}

		if (chatplus$searching) {
			ci.cancel();
			return;
		}

		ChatTabManager manager = ChatTabManager.getInstance();
		if (manager.handleCoreProtectCommand(message)) {
			chatplus$selectMode(ChatInputMode.COREPROTECT, "");
		}
		String prefixed = chatplus$applyPrefix(chatplus$withCoreProtectDefaultRows(message));
		if (chatplus$coordinateWarningsEnabled && !chatplus$confirmingCoordinateSend && chatplus$looksLikeCoordinates(prefixed)) {
			chatplus$pendingCoordinateMessage = prefixed;
			chatplus$pendingCoordinateTab = chatplus$getSendTargetLabel();
			chatplus$confirmingCoordinateSend = true;
			ci.cancel();
			return;
		}

		if (chatplus$ghostTimerMode > 0) {
			chatplus$handleGhostTimerMessage(prefixed, addToRecentChat);
			ci.cancel();
			return;
		}

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
		if ((Object) this instanceof InBedChatScreen && event.button() == 0) {
			Button leaveBedButton = ((InBedChatScreenAccessor) this).chatplus$getLeaveBedButton();
			if (leaveBedButton.active && leaveBedButton.visible && leaveBedButton.isMouseOver(event.x(), event.y())) {
				leaveBedButton.onPress(event);
				cir.setReturnValue(true);
			}
		}
		if (cir.isCancelled()) {
			return;
		}

		if (event.button() == 1) {
			if (chatplus$openProfileAt(event.x(), event.y()) || chatplus$openTabContextMenu(event.x(), event.y())) {
				cir.setReturnValue(true);
			}
			return;
		}

		if (event.button() != 0) {
			return;
		}

		if (chatplus$handlePrefixMenuClick(event.x(), event.y())
				|| chatplus$handleWhisperPickerClick(event.x(), event.y())
				|| chatplus$handleEmotePickerClick(event.x(), event.y())
				|| chatplus$handleTabContextClick(event.x(), event.y())) {
			cir.setReturnValue(true);
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

		if (chatplus$handleComponentClickAt(event.x(), event.y())) {
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
			chatplus$showPrefixMenu = !chatplus$showPrefixMenu;
			chatplus$showEmotePicker = false;
			chatplus$showTabContextMenu = false;
			chatplus$focusInput();
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$isInEmoteButton(event.x(), event.y())) {
			chatplus$showEmotePicker = !chatplus$showEmotePicker;
			chatplus$showPrefixMenu = false;
			chatplus$showTabContextMenu = false;
			chatplus$focusInput();
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$isInPrivateIndicator(event.x(), event.y())) {
			String partner = ChatTabManager.getInstance().getLatestUnreadWhisperPartner();
			if (partner.isBlank()) {
				List<String> partners = ChatTabManager.getInstance().getWhisperPartners();
				partner = partners.isEmpty() ? "" : partners.get(partners.size() - 1);
			}
			if (!partner.isBlank()) {
				chatplus$selectMode(ChatInputMode.ALL, partner);
			}
			chatplus$draggingPrivateIndicator = true;
			chatplus$dragOffsetX = (int) event.x() - chatplus$getPrivateIndicatorX();
			chatplus$dragOffsetY = (int) event.y() - chatplus$getPrivateIndicatorY();
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

		if (chatplus$startTextSelection(event.x(), event.y())) {
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
		if ((event.key() == 67 || event.input() == 67) && chatplus$isControlDown(event) && chatplus$hasTextSelection()) {
			chatplus$copySelectedText();
			cir.setReturnValue(true);
			return;
		}

		if (event.key() == 257 || event.key() == 335 || event.input() == 257 || event.input() == 335) {
			if (chatplus$searching) {
				chatplus$jumpToNextSearchResult();
				cir.setReturnValue(true);
				return;
			}

			if (chatplus$confirmingCoordinateSend) {
				chatplus$sendPendingCoordinateMessage();
				cir.setReturnValue(true);
				return;
			}

			if (chatplus$awaitingWhisperName) {
				String partner = chatplus$getWhisperPickerSelection();
				if (!partner.isEmpty()) {
					chatplus$chooseWhisperPartner(partner);
				}
				chatplus$focusInput();
				cir.setReturnValue(true);
				return;
			}

			String message = input.getValue();
			if (!message.trim().isEmpty()) {
				chatplus$ghostTimerBlockedSend = false;
				((ChatScreen) (Object) this).handleChatInput(message, true);
				if (!chatplus$confirmingCoordinateSend && !chatplus$ghostTimerBlockedSend) {
					input.setValue("");
					chatplus$syncPrefixBox();
				}
			}
			chatplus$focusInput();
			cir.setReturnValue(true);
			return;
		}

		if (event.key() == 256 || event.input() == 256) {
			if (chatplus$confirmingCoordinateSend) {
				chatplus$confirmingCoordinateSend = false;
				chatplus$pendingCoordinateMessage = "";
				chatplus$pendingCoordinateTab = "";
				chatplus$focusInput();
				cir.setReturnValue(true);
				return;
			}
			if (chatplus$awaitingWhisperName) {
				chatplus$awaitingWhisperName = false;
				input.setValue("");
				chatplus$syncPrefixBox();
				chatplus$focusInput();
				cir.setReturnValue(true);
				return;
			}
			if (chatplus$searching) {
				chatplus$toggleSearch();
				cir.setReturnValue(true);
				return;
			}
			onClose();
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void chatplus$scrollMessages(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
		if (chatplus$scrollOverflow(mouseX, mouseY, verticalAmount)) {
			cir.setReturnValue(true);
			return;
		}

		if (chatplus$scrollWhisperPicker(mouseX, mouseY, verticalAmount)) {
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

		if (chatplus$draggingPrivateIndicator) {
			chatplus$privateIndicatorX = chatplus$clamp((int) event.x() - chatplus$dragOffsetX, 0, Math.max(0, width - chatplus$getPrivateIndicatorWidth()));
			chatplus$privateIndicatorY = chatplus$clamp((int) event.y() - chatplus$dragOffsetY, 0, Math.max(0, height - PRIVATE_INDICATOR_HEIGHT));
			return true;
		}

		if (chatplus$selectingText) {
			chatplus$selectionFocus = chatplus$textPointAt(event.x(), event.y());
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
			ChatPlusConfig.setBackgroundOpacity(chatplus$backgroundOpacity);
			return true;
		}

		if (chatplus$draggingPrivateIndicator) {
			chatplus$draggingPrivateIndicator = false;
			ChatPlusConfig.setPrivateIndicatorPosition(chatplus$privateIndicatorX, chatplus$privateIndicatorY);
			return true;
		}

		if (chatplus$selectingText) {
			chatplus$selectingText = false;
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
		input.setY(chatplus$getInputY() + INPUT_TEXT_Y_OFFSET);
		input.setWidth(Math.max(20, panelWidth - PANEL_PADDING * 2 - prefixBoxWidth - INPUT_GAP * 2 - EMOTE_BUTTON_WIDTH));
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
				if (closeHit.mode() == ChatInputMode.COREPROTECT) {
					ChatTabManager.getInstance().closeCoreProtectTab(closeHit.tabKey());
					if (ChatInputMode.getSelected() == ChatInputMode.COREPROTECT) {
						chatplus$selectMode(ChatInputMode.ALL);
					}
				} else {
					ChatTabManager.getInstance().closeWhisperTab(closeHit.tabKey());
					if (closeHit.tabKey().equals(chatplus$selectedWhisperPartner)) {
						chatplus$selectMode(ChatInputMode.ALL);
					}
					if (closeHit.tabKey().equals(chatplus$lastSelectedWhisperPartner)) {
						chatplus$lastSelectedWhisperPartner = "";
					}
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
		String tabKey = whisperPartner == null ? "" : whisperPartner;
		chatplus$selectedWhisperPartner = mode == ChatInputMode.COREPROTECT ? "" : tabKey;
		chatplus$manualSendPrefix = false;
		chatplus$prefixLabelOverride = "";
		if (!chatplus$selectedWhisperPartner.isEmpty()) {
			chatplus$lastSelectedWhisperPartner = chatplus$selectedWhisperPartner;
			ChatTabManager.getInstance().markWhisperRead(chatplus$selectedWhisperPartner);
		} else {
			chatplus$lastSelectedWhisperPartner = "";
		}
		chatplus$showWhisperOverflow = false;
		chatplus$scrollOffset = 0;
		chatplus$syncPrefixBox();
		chatplus$focusInput();
		chatplus$refreshButtonLabels();
	}

	private void chatplus$renderTabs(GuiGraphicsExtractor graphics, int panelX, int panelY) {
		chatplus$tabHits.clear();
		chatplus$tabCloseHits.clear();
		chatplus$overflowTabHits.clear();
		chatplus$overflowHit = null;
		int x = panelX + PANEL_PADDING;
		int y = panelY + 2;
		int maxX = chatplus$getSearchIconX(panelX, ChatWindowState.getWidth(width)) - ICON_GAP - TAB_BREAK_WIDTH - TAB_GAP;
		boolean compactTabs = chatplus$needsCompactTabs(maxX - x);

		for (ChatInputMode mode : chatplus$getVisibleModes()) {
			x = chatplus$renderTab(graphics, compactTabs ? chatplus$getCompactLabel(mode) : mode.getLabel(), mode, "", x, y, maxX);
		}

		ChatTabManager manager = ChatTabManager.getInstance();
		if (manager.hasCoreProtectTab()) {
			x = chatplus$renderTab(graphics, compactTabs ? "CP" : "CoreProtect", ChatInputMode.COREPROTECT, "", x, y, maxX, true);
		}

		List<String> partners = manager.getWhisperPartners();
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

	private int chatplus$renderTab(GuiGraphicsExtractor graphics, String label, ChatInputMode mode, String whisperPartner, int x, int y, int maxX) {
		return chatplus$renderTab(graphics, label, mode, whisperPartner, x, y, maxX, !whisperPartner.isEmpty());
	}

	private int chatplus$renderTab(GuiGraphicsExtractor graphics, String label, ChatInputMode mode, String tabKey, int x, int y, int maxX, boolean closeable) {
		if (x >= maxX) {
			return x;
		}

		boolean selected = chatplus$isSelectedTab(mode, tabKey);
		boolean flashing = mode != ChatInputMode.COREPROTECT && !tabKey.isEmpty() && ChatTabManager.getInstance().hasUnreadWhisper(tabKey) && chatplus$flashOn();
		int tabWidth = Math.min(chatplus$getTabWidth(label, closeable), maxX - x);
		if (tabWidth < chatplus$getMinimumTabWidth(label)) {
			if (closeable || tabWidth < 8) {
				return x;
			}
			tabWidth = Math.max(8, tabWidth);
		}
		graphics.fill(x, y, x + tabWidth, y + TAB_BUTTON_HEIGHT, flashing ? 0xCC775555 : selected ? 0xCC666666 : 0xAA333333);
		graphics.outline(x, y, tabWidth, TAB_BUTTON_HEIGHT, selected ? 0xFFFFFFFF : flashing ? 0xFFFF7777 : 0xAA888888);
		String fittedLabel = chatplus$fitLabel(label, Math.max(8, tabWidth - (closeable ? 18 : 8)));
		int labelX = closeable ? x + 5 : x + (tabWidth - font.width(fittedLabel)) / 2;
		graphics.text(font, fittedLabel, labelX, y + 2, 0xFFFFFFFF, false);
		if (closeable) {
			int closeX = x + tabWidth - 12;
			graphics.text(font, "x", closeX + 3, y + 2, 0xFFFF7777, false);
			chatplus$tabCloseHits.add(new TabCloseHit(mode, tabKey, closeX, y, 10, TAB_BUTTON_HEIGHT));
		}
		chatplus$tabHits.add(new TabHit(label, mode, tabKey, x, y, tabWidth, TAB_BUTTON_HEIGHT));
		return x + tabWidth + TAB_GAP;
	}

	private void chatplus$renderOverflowTab(GuiGraphicsExtractor graphics, int x, int y, int maxX, int count) {
		if (maxX - x < 18) {
			return;
		}

		String label = "+" + count;
		int tabWidth = Math.min(Math.max(22, font.width(label) + 10), maxX - x);
		graphics.fill(x, y, x + tabWidth, y + TAB_BUTTON_HEIGHT, 0xAA333333);
		graphics.outline(x, y, tabWidth, TAB_BUTTON_HEIGHT, 0xAA888888);
		graphics.text(font, label, x + 5, y + 2, 0xFFFFFFFF, false);
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
		for (ChatInputMode mode : chatplus$getVisibleModes()) {
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
			case COREPROTECT -> "CP";
		};
	}

	private List<ChatInputMode> chatplus$getVisibleModes() {
		List<ChatInputMode> modes = new ArrayList<>();
		for (ChatInputMode mode : ChatInputMode.values()) {
			if (mode != ChatInputMode.COREPROTECT) {
				modes.add(mode);
			}
		}
		return modes;
	}

	private void chatplus$renderOverflowBreak(GuiGraphicsExtractor graphics, int x, int panelY) {
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

	private void chatplus$renderWhisperOverflowDropdown(GuiGraphicsExtractor graphics, List<String> partners) {
		int x = chatplus$overflowHit.x();
		int y = chatplus$overflowHit.y() + TAB_BUTTON_HEIGHT + 2;
		int hiddenCount = Math.max(0, partners.size() - chatplus$whisperOverflowStartIndex);
		int maxRows = Math.min(5, hiddenCount);
		int maxScroll = Math.max(0, hiddenCount - maxRows);
		chatplus$whisperOverflowScroll = chatplus$clamp(chatplus$whisperOverflowScroll, 0, maxScroll);

		graphics.fill(x, y, x + OVERFLOW_DROPDOWN_WIDTH, y + maxRows * OVERFLOW_DROPDOWN_ROW_HEIGHT + 4, 0xEE151515);
		graphics.outline(x, y, OVERFLOW_DROPDOWN_WIDTH, maxRows * OVERFLOW_DROPDOWN_ROW_HEIGHT + 4, 0xCCFFFFFF);
		for (int row = 0; row < maxRows; row++) {
			String partner = partners.get(chatplus$whisperOverflowStartIndex + chatplus$whisperOverflowScroll + row);
			int rowY = y + 2 + row * OVERFLOW_DROPDOWN_ROW_HEIGHT;
			boolean unread = ChatTabManager.getInstance().hasUnreadWhisper(partner) && chatplus$flashOn();
			graphics.fill(x + 2, rowY, x + OVERFLOW_DROPDOWN_WIDTH - 2, rowY + OVERFLOW_DROPDOWN_ROW_HEIGHT, unread ? 0x88774444 : 0x66444444);
			graphics.text(font, chatplus$fitLabel(partner, OVERFLOW_DROPDOWN_WIDTH - 10), x + 5, rowY + 3, unread ? 0xFFFFAAAA : 0xFFFFFFFF, false);
			chatplus$overflowTabHits.add(new TabHit(partner, ChatInputMode.ALL, partner, x + 2, rowY, OVERFLOW_DROPDOWN_WIDTH - 4, OVERFLOW_DROPDOWN_ROW_HEIGHT));
		}
	}

	private boolean chatplus$isSelectedTab(ChatInputMode mode, String whisperPartner) {
		if (mode == ChatInputMode.COREPROTECT) {
			return ChatInputMode.getSelected() == ChatInputMode.COREPROTECT;
		}
		if (!whisperPartner.isEmpty()) {
			return ChatInputMode.getSelected() == ChatInputMode.ALL && whisperPartner.equals(chatplus$selectedWhisperPartner);
		}

		return ChatInputMode.getSelected() == mode && chatplus$selectedWhisperPartner.isEmpty();
	}

	private void chatplus$renderHeaderIcons(GuiGraphicsExtractor graphics, int panelX, int panelY, int panelWidth) {
		int searchX = chatplus$getSearchIconX(panelX, panelWidth);
		int mentionX = chatplus$getMentionIconX(panelX, panelWidth);
		int closeX = chatplus$getCloseIconX(panelX, panelWidth);
		int gearX = chatplus$getGearIconX(panelX, panelWidth);
		chatplus$renderIconButton(graphics, searchX, panelY + 2, "?", chatplus$searching ? 0xCC667788 : 0xAA444444);
		chatplus$renderIconButton(graphics, mentionX, panelY + 2, "!", ChatTabManager.getInstance().hasUnreadMentions() && chatplus$flashOn() ? 0xCC884444 : 0xAA444444);
		chatplus$renderIconButton(graphics, gearX, panelY + 2, "\u2699", chatplus$showSettings ? 0xCC777777 : 0xAA444444);
		chatplus$renderIconButton(graphics, closeX, panelY + 2, "X", 0xAA553333);
	}

	private void chatplus$renderIconButton(GuiGraphicsExtractor graphics, int x, int y, String label, int color) {
		graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, color);
		graphics.outline(x, y, ICON_SIZE, ICON_SIZE, 0xAAFFFFFF);
		graphics.text(font, label, x + (ICON_SIZE - font.width(label)) / 2, y + 2, 0xFFFFFFFF, false);
	}

	private boolean chatplus$handleHeaderClick(double mouseX, double mouseY) {
		int panelX = ChatWindowState.getX();
		int panelY = ChatWindowState.getY(height);
		int panelWidth = ChatWindowState.getWidth(width);
		if (chatplus$isInBox(mouseX, mouseY, chatplus$getSearchIconX(panelX, panelWidth), panelY + 2, ICON_SIZE, ICON_SIZE)) {
			chatplus$toggleSearch();
			return true;
		}

		if (chatplus$isInBox(mouseX, mouseY, chatplus$getMentionIconX(panelX, panelWidth), panelY + 2, ICON_SIZE, ICON_SIZE)) {
			chatplus$jumpToLatestMention();
			return true;
		}

		if (chatplus$isInBox(mouseX, mouseY, chatplus$getCloseIconX(panelX, panelWidth), panelY + 2, ICON_SIZE, ICON_SIZE)) {
			onClose();
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

	private int chatplus$getMentionIconX(int panelX, int panelWidth) {
		return chatplus$getGearIconX(panelX, panelWidth) - ICON_SIZE - ICON_GAP;
	}

	private int chatplus$getSearchIconX(int panelX, int panelWidth) {
		return chatplus$getMentionIconX(panelX, panelWidth) - ICON_SIZE - ICON_GAP;
	}

	private void chatplus$jumpToLatestMention() {
		List<ChatMessage> mentions = ChatTabManager.getInstance().getMentionMessages();
		if (mentions.isEmpty()) {
			chatplus$focusInput();
			return;
		}

		ChatMessage latestMention = mentions.get(mentions.size() - 1);
		chatplus$selectMode(ChatInputMode.ALL);
		List<ChatMessage> messages = ChatTabManager.getInstance().getAllParsedMessages();
		int index = messages.indexOf(latestMention);
		if (index >= 0) {
			chatplus$scrollOffset = Math.max(0, messages.size() - 1 - index);
		}
		ChatTabManager.getInstance().markMentionsRead();
		chatplus$focusInput();
	}

	private void chatplus$renderMessages(GuiGraphicsExtractor graphics, int panelX, int panelY, int panelWidth, int inputY) {
		int left = panelX + PANEL_PADDING;
		int top = panelY + TAB_HEIGHT + PANEL_PADDING;
		List<ChatMessage> messages = chatplus$getSelectedMessages();
		boolean showScrollbar = messages.size() > 1;
		int right = panelX + panelWidth - PANEL_PADDING - (showScrollbar ? SCROLLBAR_WIDTH + 3 : 0);
		int bottom = inputY - PANEL_PADDING - (chatplus$hasStatusBanner() ? STATUS_BANNER_HEIGHT + 2 : 0);
		int textLeft = chatplus$chatHeadsEnabled ? left + HEAD_SIZE + HEAD_GAP : left;
		int textRight = right;
		float scale = chatplus$getTextScale();
		int lineHeight = chatplus$getLineHeight();
		int lineY = bottom - lineHeight;
		chatplus$clampScrollOffset();
		chatplus$senderHits.clear();
		chatplus$linkHits.clear();
		chatplus$textLineHits.clear();
		chatplus$componentLineHits.clear();
		chatplus$nextTextLineId = 0;

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
				String lineText = lineTexts.get(lineIndex).getString();
				int textLineId = chatplus$nextTextLineId++;
				chatplus$renderSelectionHighlight(graphics, textLineId, lineText, textLeft, lineY, lineHeight, scale);
				chatplus$drawScaledString(graphics, lines.get(lineIndex), textLeft, lineY, 0xFFFFFFFF, scale);
				chatplus$trackTextLineHit(textLineId, lineText, textLeft, lineY, lineHeight, scale);
				chatplus$componentLineHits.add(new ComponentLineHit(lines.get(lineIndex), textLeft, lineY, lineHeight, scale));
				chatplus$trackLinkHits(lineText, textLeft, lineY, lineHeight, scale);
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
		return chatplus$filterSearchResults(chatplus$withRecentDeathAlerts(chatplus$getBaseSelectedMessages()));
	}

	private List<ChatMessage> chatplus$getBaseSelectedMessages() {
		ChatTabManager manager = ChatTabManager.getInstance();
		ChatInputMode mode = ChatInputMode.getSelected();
		String whisperPartner = chatplus$selectedWhisperPartner;
		boolean showWhispers = whisperPartner.isEmpty() && mode != ChatInputMode.ALL && chatplus$shouldShowWhispersInSelectedTab();
		boolean showServer = whisperPartner.isEmpty() && mode != ChatInputMode.ALL && chatplus$shouldShowServerMessagesInSelectedTab();
		long revision = manager.getMessageRevision();
		if (revision == chatplus$cachedMessageRevision
				&& mode == chatplus$cachedMessageMode
				&& whisperPartner.equals(chatplus$cachedWhisperPartner)
				&& showWhispers == chatplus$cachedShowWhispers
				&& showServer == chatplus$cachedShowServer) {
			return chatplus$cachedBaseMessages;
		}

		List<ChatMessage> selected;
		if (!whisperPartner.isEmpty()) {
			selected = manager.getWhisperMessages(whisperPartner);
		} else if (mode == ChatInputMode.ALL) {
			selected = manager.getAllParsedMessages();
		} else if (mode == ChatInputMode.COREPROTECT) {
			selected = manager.getCoreProtectMessages();
		} else {
			selected = manager.getParsedTabMessages(chatplus$getSelectedTabType());
			if (showWhispers) {
				selected.addAll(manager.getParsedTabMessages(ChatTabType.WHISPER));
			}
			if (showServer) {
				selected.addAll(manager.getParsedTabMessages(ChatTabType.SYSTEM));
			}
			if (showWhispers || showServer) {
				selected.sort((first, second) -> Long.compare(first.getTimestamp(), second.getTimestamp()));
			}
		}

		chatplus$cachedMessageRevision = revision;
		chatplus$cachedMessageMode = mode;
		chatplus$cachedWhisperPartner = whisperPartner;
		chatplus$cachedShowWhispers = showWhispers;
		chatplus$cachedShowServer = showServer;
		chatplus$cachedBaseMessages = selected;
		return selected;
	}

	private boolean chatplus$shouldShowWhispersInSelectedTab() {
		ChatPlusConfig.Settings settings = ChatPlusConfig.get();
		return switch (chatplus$getSelectedTabType()) {
			case GENERAL -> settings.showWhispersInGeneral;
			case LOCAL_CHAT -> settings.showWhispersInLocal;
			case TEAM_CHAT -> settings.showWhispersInTeam;
			case SYSTEM -> settings.showWhispersInSystem;
			case COREPROTECT -> false;
			default -> false;
		};
	}

	private boolean chatplus$shouldShowServerMessagesInSelectedTab() {
		ChatPlusConfig.Settings settings = ChatPlusConfig.get();
		return switch (chatplus$getSelectedTabType()) {
			case GENERAL -> settings.showServerMessagesInGeneral;
			case LOCAL_CHAT -> settings.showServerMessagesInLocal;
			case TEAM_CHAT -> settings.showServerMessagesInTeam;
			default -> false;
		};
	}

	private List<ChatMessage> chatplus$filterSearchResults(List<ChatMessage> messages) {
		String query = chatplus$getSearchQuery();
		if (query.isEmpty()) {
			return messages;
		}

		List<ChatMessage> results = new ArrayList<>();
		for (ChatMessage message : messages) {
			if (message.getRawMessage().toLowerCase(Locale.ROOT).contains(query)
					|| message.getSender().toLowerCase(Locale.ROOT).contains(query)
					|| message.getContent().toLowerCase(Locale.ROOT).contains(query)) {
				results.add(message);
			}
		}
		return results;
	}

	private String chatplus$getSearchQuery() {
		return chatplus$searching ? input.getValue().trim().toLowerCase(Locale.ROOT) : "";
	}

	private List<ChatMessage> chatplus$withRecentDeathAlerts(List<ChatMessage> selected) {
		if (ChatInputMode.getSelected() == ChatInputMode.ALL) {
			return selected;
		}

		long cutoff = System.currentTimeMillis() - 10000L;
		List<ChatMessage> merged = null;
		for (ChatMessage message : ChatTabManager.getInstance().getAllParsedMessagesSince(cutoff)) {
			if (chatplus$isSystemDeathMessage(message)
					&& !selected.contains(message)
					&& (merged == null || !merged.contains(message))) {
				if (merged == null) {
					merged = new ArrayList<>(selected);
				}
				merged.add(message);
			}
		}
		if (merged == null) {
			return selected;
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
			case COREPROTECT -> ChatTabType.COREPROTECT;
		};
	}

	private int chatplus$getMessageColor(ChatMessage message) {
		return switch (message.getType()) {
			case LOCAL_CHAT -> chatplus$localColor;
			case WHISPER -> chatplus$whisperColor;
			case TEAM_CHAT -> chatplus$teamColor;
			case SYSTEM -> chatplus$systemColor;
			case COREPROTECT -> chatplus$systemColor;
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
		for (ChatInputMode mode : chatplus$getVisibleModes()) {
			tabsWidth += chatplus$getTabWidth(mode.getLabel(), false) + 2;
		}
		return tabsWidth;
	}

	private void chatplus$trackSenderHit(ChatMessage message, int x, int y, int maxWidth, int height) {
		String sender = chatplus$profileNameFor(message);
		if (sender.isBlank()) {
			return;
		}

		String displayName = PlayerProfileStore.displayNameFor(sender, message.getSender());
		int width = Math.min(maxWidth, font.width(displayName));
		chatplus$senderHits.add(new SenderHit(sender, message.getSender(), chatplus$senderStyleTrace(message), x, y, width, height));
	}

	private void chatplus$trackTextLineHit(int id, String text, int x, int y, int height, float scale) {
		int width = Math.max(4, Math.round(font.width(text) * scale));
		chatplus$textLineHits.add(new TextLineHit(id, text, x, y, height, scale));
	}

	private boolean chatplus$startTextSelection(double mouseX, double mouseY) {
		if (chatplus$isInInputBox(mouseX, mouseY)) {
			chatplus$selectionAnchor = null;
			chatplus$selectionFocus = null;
			return false;
		}

		TextSelectionPoint point = chatplus$textPointAt(mouseX, mouseY);
		if (point == null) {
			return false;
		}

		chatplus$selectionAnchor = point;
		chatplus$selectionFocus = point;
		chatplus$selectingText = true;
		chatplus$focusInput();
		return true;
	}

	private boolean chatplus$isInInputBox(double mouseX, double mouseY) {
		return chatplus$isInBox(mouseX, mouseY, input.getX(), input.getY() - INPUT_TEXT_Y_OFFSET, input.getWidth(), input.getHeight() + INPUT_TEXT_Y_OFFSET);
	}

	private TextSelectionPoint chatplus$textPointAt(double mouseX, double mouseY) {
		TextLineHit nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (TextLineHit hit : chatplus$textLineHits) {
			if (mouseY >= hit.y() && mouseY < hit.y() + hit.height()) {
				return new TextSelectionPoint(hit.id(), chatplus$charIndexAt(hit, mouseX));
			}

			int distance = (int) Math.min(Math.abs(mouseY - hit.y()), Math.abs(mouseY - (hit.y() + hit.height())));
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = hit;
			}
		}

		if (nearest == null || nearestDistance > nearest.height() * 2) {
			return null;
		}

		return new TextSelectionPoint(nearest.id(), chatplus$charIndexAt(nearest, mouseX));
	}

	private int chatplus$charIndexAt(TextLineHit hit, double mouseX) {
		int relativeX = Math.max(0, Math.round((float) ((mouseX - hit.x()) / hit.scale())));
		String text = hit.text();
		int previousWidth = 0;
		for (int i = 1; i <= text.length(); i++) {
			int currentWidth = font.width(text.substring(0, i));
			if (relativeX < (previousWidth + currentWidth) / 2) {
				return i - 1;
			}
			previousWidth = currentWidth;
		}
		return text.length();
	}

	private void chatplus$renderSelectionHighlight(GuiGraphicsExtractor graphics, int lineId, String text, int x, int y, int height, float scale) {
		int[] range = chatplus$selectedRangeForLine(lineId, text.length());
		if (range == null || range[0] == range[1]) {
			return;
		}

		int snappedX = chatplus$scaledDrawX(x, scale);
		int startX = snappedX + (int) Math.floor(font.width(text.substring(0, range[0])) * scale);
		int endX = snappedX + (int) Math.ceil(font.width(text.substring(0, range[1])) * scale);
		graphics.fill(startX, y - 1, Math.max(startX + 1, endX), y + height, 0x886699CC);
	}

	private int[] chatplus$selectedRangeForLine(int lineId, int textLength) {
		if (!chatplus$hasTextSelection()) {
			return null;
		}

		TextSelectionPoint top = chatplus$selectionTop();
		TextSelectionPoint bottom = chatplus$selectionBottom();
		if (lineId < bottom.lineId() || lineId > top.lineId()) {
			return null;
		}

		int startIndex = lineId == top.lineId() ? top.charIndex() : 0;
		int endIndex = lineId == bottom.lineId() ? bottom.charIndex() : textLength;
		startIndex = chatplus$clamp(startIndex, 0, textLength);
		endIndex = chatplus$clamp(endIndex, 0, textLength);
		return new int[]{Math.min(startIndex, endIndex), Math.max(startIndex, endIndex)};
	}

	private boolean chatplus$hasTextSelection() {
		return chatplus$selectionAnchor != null
				&& chatplus$selectionFocus != null
				&& (chatplus$selectionAnchor.lineId() != chatplus$selectionFocus.lineId()
				|| chatplus$selectionAnchor.charIndex() != chatplus$selectionFocus.charIndex());
	}

	private boolean chatplus$isControlDown(KeyEvent event) {
		return (event.modifiers() & org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL) != 0;
	}

	private TextSelectionPoint chatplus$selectionTop() {
		if (chatplus$selectionAnchor.lineId() > chatplus$selectionFocus.lineId()
				|| (chatplus$selectionAnchor.lineId() == chatplus$selectionFocus.lineId()
				&& chatplus$selectionAnchor.charIndex() <= chatplus$selectionFocus.charIndex())) {
			return chatplus$selectionAnchor;
		}
		return chatplus$selectionFocus;
	}

	private TextSelectionPoint chatplus$selectionBottom() {
		return chatplus$selectionTop() == chatplus$selectionAnchor ? chatplus$selectionFocus : chatplus$selectionAnchor;
	}

	private void chatplus$copySelectedText() {
		TextSelectionPoint top = chatplus$selectionTop();
		TextSelectionPoint bottom = chatplus$selectionBottom();
		List<TextLineHit> selectedLines = new ArrayList<>();
		for (TextLineHit hit : chatplus$textLineHits) {
			if (hit.id() >= bottom.lineId() && hit.id() <= top.lineId()) {
				selectedLines.add(hit);
			}
		}
		selectedLines.sort((first, second) -> Integer.compare(first.y(), second.y()));

		StringBuilder selectedText = new StringBuilder();
		for (TextLineHit hit : selectedLines) {
			int[] range = chatplus$selectedRangeForLine(hit.id(), hit.text().length());
			if (range == null || range[0] == range[1]) {
				continue;
			}
			if (selectedText.length() > 0) {
				selectedText.append('\n');
			}
			selectedText.append(hit.text(), range[0], range[1]);
		}

		if (!selectedText.isEmpty()) {
			minecraft.keyboardHandler.setClipboard(selectedText.toString());
		}
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

	private int chatplus$parsePositiveInt(String value, int fallback) {
		try {
			int parsed = Integer.parseInt(value);
			return parsed > 0 ? parsed : fallback;
		} catch (NumberFormatException ignored) {
			return fallback;
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

	private boolean chatplus$handleComponentClickAt(double mouseX, double mouseY) {
		for (ComponentLineHit hit : chatplus$componentLineHits) {
			if (mouseX < hit.x() || mouseY < hit.y() || mouseY >= hit.y() + hit.height()) {
				continue;
			}

			float relativeX = (float) (mouseX - hit.x()) / hit.scale();
			float[] cursor = {0.0F};
			Style[] clickedStyle = {null};
			hit.text().accept((index, style, codePoint) -> {
				float characterWidth = Math.max(1.0F, font.width(FormattedCharSequence.codepoint(codePoint, style)));
				float start = cursor[0];
				float end = cursor[0] + characterWidth;
				if (relativeX >= start && relativeX < end) {
					clickedStyle[0] = style;
					return false;
				}
				cursor[0] = end;
				return true;
			});
			Style style = clickedStyle[0];
			if (style == null || style.getClickEvent() == null) {
				continue;
			}

			chatplus$handleClickEvent(style.getClickEvent());
			return true;
		}
		return false;
	}

	private void chatplus$handleClickEvent(ClickEvent clickEvent) {
		if (clickEvent instanceof ClickEvent.RunCommand runCommand) {
			String command = runCommand.command();
			if (minecraft != null && minecraft.getConnection() != null && command != null && !command.isBlank()) {
				if (command.startsWith("/")) {
					minecraft.getConnection().sendCommand(command.substring(1));
				} else {
					minecraft.getConnection().sendChat(command);
				}
			}
			return;
		}

		defaultHandleClickEvent(clickEvent, minecraft, this);
	}

	private void chatplus$toggleSearch() {
		chatplus$searching = !chatplus$searching;
		chatplus$confirmingCoordinateSend = false;
		if (chatplus$searching) {
			chatplus$draftBeforeSearch = input.getValue();
			input.setValue("");
		} else {
			input.setValue(chatplus$draftBeforeSearch);
			chatplus$draftBeforeSearch = "";
		}
		chatplus$scrollOffset = 0;
		chatplus$syncPrefixBox();
		chatplus$focusInput();
	}

	private void chatplus$jumpToNextSearchResult() {
		if (chatplus$getSearchQuery().isEmpty()) {
			chatplus$focusInput();
			return;
		}

		int count = chatplus$getSelectedMessages().size();
		if (count > 0) {
			chatplus$scrollOffset = count <= 1 ? 0 : (chatplus$scrollOffset + 1) % count;
		}
		chatplus$focusInput();
	}

	private boolean chatplus$looksLikeCoordinates(String message) {
		return message != null && COORDINATE_PATTERN.matcher(message).find();
	}

	private String chatplus$withCoreProtectDefaultRows(String message) {
		if (message == null || message.isBlank()) {
			return message;
		}

		String trimmed = message.trim();
		String withoutSlash = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
		String[] parts = withoutSlash.split("\\s+");
		if (parts.length < 3 || (!parts[0].equalsIgnoreCase("co") && !parts[0].equalsIgnoreCase("coreprotect"))) {
			return message;
		}
		if (!parts[1].equalsIgnoreCase("lookup") && !parts[1].equalsIgnoreCase("l")) {
			return message;
		}
		if (chatplus$hasCoreProtectRowsArgument(parts) || chatplus$isCoreProtectPageOnlyLookup(parts)) {
			return message;
		}

		return message + " rows:" + COREPROTECT_DEFAULT_ROWS;
	}

	private boolean chatplus$hasCoreProtectRowsArgument(String[] parts) {
		for (int i = 2; i < parts.length; i++) {
			if (parts[i].equalsIgnoreCase("rows:")
					|| parts[i].toLowerCase(Locale.ROOT).startsWith("rows:")) {
				return true;
			}
		}
		return false;
	}

	private boolean chatplus$isCoreProtectPageOnlyLookup(String[] parts) {
		if (parts.length != 3) {
			return false;
		}

		String page = parts[2].toLowerCase(Locale.ROOT);
		if (page.startsWith("page:")) {
			page = page.substring("page:".length());
		}
		return !page.isEmpty() && page.chars().allMatch(Character::isDigit);
	}

	private String chatplus$getSendTargetLabel() {
		if (!chatplus$selectedWhisperPartner.isEmpty()) {
			return "Whisper";
		}
		return ChatInputMode.getSelected().getLabel();
	}

	private void chatplus$sendPendingCoordinateMessage() {
		if (chatplus$pendingCoordinateMessage.isBlank()) {
			chatplus$confirmingCoordinateSend = false;
			return;
		}

		String message = chatplus$pendingCoordinateMessage;
		chatplus$confirmingCoordinateSend = false;
		chatplus$pendingCoordinateMessage = "";
		chatplus$pendingCoordinateTab = "";
		chatplus$ghostTimerBlockedSend = false;
		if (chatplus$ghostTimerMode > 0) {
			chatplus$handleGhostTimerMessage(message, true);
		} else {
			chatplus$sendChatMessageNow(message, true);
		}
		input.setValue(chatplus$ghostTimerBlockedSend ? message : "");
		if (!chatplus$ghostTimerBlockedSend) {
			chatplus$syncPrefixBox();
		}
		chatplus$focusInput();
	}

	private void chatplus$handleGhostTimerMessage(String message, boolean addToRecentChat) {
		GhostTimerManager.SubmitResult result = GhostTimerManager.submit(
				chatplus$ghostTimerMode,
				message,
				addToRecentChat,
				() -> chatplus$sendChatMessageNow(message, addToRecentChat)
		);
		chatplus$ghostTimerBlockedSend = result == GhostTimerManager.SubmitResult.BLOCKED;
	}

	private void chatplus$clearGhostMessages() {
		GhostTimerManager.clear();
	}

	private void chatplus$sendChatMessageNow(String message, boolean addToRecentChat) {
		chatplus$applyingPrefix = true;
		try {
			((ChatScreen) (Object) this).handleChatInput(message, addToRecentChat);
		} finally {
			chatplus$applyingPrefix = false;
		}
	}

	private String chatplus$profileNameFor(ChatMessage message) {
		if (message.getType() == ChatTabType.SYSTEM || message.getSender().isBlank() || message.getSender().equals("UNKNOWN")) {
			return "";
		}

		if (message.isOutgoing() && minecraft.getUser() != null) {
			return minecraft.getUser().getName();
		}

		if (!message.getSenderId().isBlank() && minecraft.getConnection() != null) {
			try {
				PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(UUID.fromString(message.getSenderId()));
				if (playerInfo != null && playerInfo.getProfile().name() != null) {
					return playerInfo.getProfile().name();
				}
			} catch (IllegalArgumentException ignored) {
			}
		}

		String cleaned = message.getSender();
		if (cleaned.startsWith("You -> ")) {
			cleaned = cleaned.substring("You -> ".length());
		}

		String onlinePlayer = chatplus$onlinePlayerSuffix(cleaned.trim());
		if (!onlinePlayer.isBlank()) {
			return onlinePlayer;
		}

		String[] parts = cleaned.trim().split("\\s+");
		return parts.length == 0 ? cleaned.trim() : parts[parts.length - 1];
	}

	private String chatplus$onlinePlayerSuffix(String displayName) {
		if (minecraft.getConnection() == null || displayName.isBlank()) {
			return "";
		}

		String bestMatch = "";
		for (PlayerInfo playerInfo : minecraft.getConnection().getOnlinePlayers()) {
			String name = playerInfo.getProfile().name();
			if (name == null || name.isBlank()) {
				continue;
			}
			if (displayName.endsWith(name) && name.length() > bestMatch.length()) {
				bestMatch = name;
			}
		}
		return bestMatch;
	}

	private void chatplus$renderPlayerHead(GuiGraphicsExtractor graphics, ChatMessage message, int x, int y) {
		if (minecraft.getConnection() == null) {
			return;
		}

		PlayerInfo playerInfo = chatplus$playerInfoFor(message);
		if (playerInfo == null) {
			return;
		}

		Identifier texture = playerInfo.getSkin().body().texturePath();
		int headY = y + HEAD_Y_OFFSET;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, headY, 8.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 64, 64);
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, headY, 40.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 64, 64);
	}

	private PlayerInfo chatplus$playerInfoFor(ChatMessage message) {
		if (!message.getSenderId().isBlank()) {
			try {
				PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(UUID.fromString(message.getSenderId()));
				if (playerInfo != null) {
					return playerInfo;
				}
			} catch (IllegalArgumentException ignored) {
			}
		}

		String playerName = chatplus$profileNameFor(message);
		return playerName.isBlank() ? null : minecraft.getConnection().getPlayerInfo(playerName);
	}

	private void chatplus$renderScrollbar(GuiGraphicsExtractor graphics, int panelX, int panelY, int panelWidth, int inputY, int messageCount) {
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
		if (chatplus$searching) {
			chatplus$setPrefixValue("Search");
			return;
		}

		if (chatplus$awaitingWhisperName) {
			chatplus$setPrefixValue("To?");
			return;
		}

		if (chatplus$manualSendPrefix) {
			return;
		}

		if (!chatplus$selectedWhisperPartner.isEmpty()) {
			chatplus$setPrefixValue(chatplus$getWhisperPrefix());
			return;
		}

		chatplus$setPrefixValue(ChatInputMode.getSelected().getPrefix());
	}

	private String chatplus$getWhisperPrefix() {
		return "/tell " + chatplus$selectedWhisperPartner + " ";
	}

	private void chatplus$renderPrefixButton(GuiGraphicsExtractor graphics) {
		int x = ChatWindowState.getX() + PANEL_PADDING;
		int y = chatplus$getInputY();
		String label = chatplus$getPrefixLabel();
		int width = chatplus$getPrefixBoxWidth();
		graphics.fill(x, y, x + width, y + INPUT_HEIGHT, 0xCC555555);
		graphics.outline(x, y, width, INPUT_HEIGHT, 0xCCFFFFFF);
		graphics.text(font, label, x + 4, y + 2, 0xFFFFFFFF, false);
	}

	private void chatplus$renderEmoteButton(GuiGraphicsExtractor graphics) {
		int x = chatplus$getEmoteButtonX();
		int y = chatplus$getInputY();
		graphics.fill(x, y, x + EMOTE_BUTTON_WIDTH, y + INPUT_HEIGHT, chatplus$showEmotePicker ? 0xCC777777 : 0xCC555555);
		graphics.outline(x, y, EMOTE_BUTTON_WIDTH, INPUT_HEIGHT, 0xCCFFFFFF);
		String label = ":)";
		graphics.text(font, label, x + (EMOTE_BUTTON_WIDTH - font.width(label)) / 2, y + 2, 0xFFFFFFFF, false);
	}

	private void chatplus$renderPrefixMenu(GuiGraphicsExtractor graphics) {
		int x = ChatWindowState.getX() + PANEL_PADDING;
		int y = chatplus$getInputY() - MENU_ROW_HEIGHT * 4 - 3;
		String[] labels = {"Global chat", "/local", "/team", "/w..."};
		graphics.fill(x, y, x + PREFIX_MENU_WIDTH, y + MENU_ROW_HEIGHT * labels.length + 4, 0xEE151515);
		graphics.outline(x, y, PREFIX_MENU_WIDTH, MENU_ROW_HEIGHT * labels.length + 4, 0xCCFFFFFF);
		for (int i = 0; i < labels.length; i++) {
			int rowY = y + 2 + i * MENU_ROW_HEIGHT;
			graphics.fill(x + 2, rowY, x + PREFIX_MENU_WIDTH - 2, rowY + MENU_ROW_HEIGHT, 0x66444444);
			graphics.text(font, labels[i], x + 6, rowY + 3, 0xFFFFFFFF, false);
		}
	}

	private void chatplus$renderEmotePicker(GuiGraphicsExtractor graphics) {
		int width = EMOTE_PICKER_WIDTH;
		int rows = EMOTE_OPTIONS.length;
		int x = chatplus$getEmoteButtonX() + EMOTE_BUTTON_WIDTH - width;
		int y = chatplus$getInputY() - MENU_ROW_HEIGHT * rows - 5;
		graphics.fill(x, y, x + width, y + MENU_ROW_HEIGHT * rows + 4, 0xEE151515);
		graphics.outline(x, y, width, MENU_ROW_HEIGHT * rows + 4, 0xCCFFFFFF);
		for (int i = 0; i < EMOTE_OPTIONS.length; i++) {
			EmoteOption option = EMOTE_OPTIONS[i];
			int rowY = y + 2 + i * MENU_ROW_HEIGHT;
			graphics.fill(x + 2, rowY, x + width - 2, rowY + MENU_ROW_HEIGHT, 0x66333333);
			graphics.fill(x + 5, rowY + 2, x + 19, rowY + MENU_ROW_HEIGHT - 2, 0xAA222222);
			graphics.outline(x + 5, rowY + 2, 14, MENU_ROW_HEIGHT - 4, 0xAA777777);
			graphics.text(font, option.symbol(), x + 8, rowY + 3, 0xFFFFFFFF, false);
			graphics.text(font, chatplus$fitLabel(option.command(), width - 29), x + 25, rowY + 3, 0xFFDDDDDD, false);
		}
	}

	private void chatplus$renderPrivateIndicator(GuiGraphicsExtractor graphics) {
		int x = chatplus$getPrivateIndicatorX();
		int y = chatplus$getPrivateIndicatorY();
		int indicatorWidth = chatplus$getPrivateIndicatorWidth();
		boolean unread = ChatTabManager.getInstance().hasUnreadWhispers();
		String label = "PM";
		graphics.fill(x, y, x + indicatorWidth, y + PRIVATE_INDICATOR_HEIGHT, unread && chatplus$flashOn() ? 0xCC884444 : 0xAA333333);
		graphics.outline(x, y, indicatorWidth, PRIVATE_INDICATOR_HEIGHT, unread ? 0xFFFFAAAA : 0xAAFFFFFF);
		graphics.text(font, label, x + (indicatorWidth - font.width(label)) / 2, y + 3, unread ? 0xFFFFDDDD : 0xFFFFFFFF, false);
	}

	private void chatplus$renderTabContextMenu(GuiGraphicsExtractor graphics) {
		boolean showPrivate = chatplus$getShowWhispersInTab(chatplus$contextTabType);
		boolean showServer = chatplus$getShowServerMessagesInTab(chatplus$contextTabType);
		graphics.fill(chatplus$contextMenuX, chatplus$contextMenuY, chatplus$contextMenuX + TAB_CONTEXT_WIDTH, chatplus$contextMenuY + TAB_CONTEXT_HEIGHT, 0xEE151515);
		graphics.outline(chatplus$contextMenuX, chatplus$contextMenuY, TAB_CONTEXT_WIDTH, TAB_CONTEXT_HEIGHT, 0xCCFFFFFF);
		graphics.text(font, chatplus$contextTabType.getDisplayName(), chatplus$contextMenuX + 6, chatplus$contextMenuY + 5, 0xFFFFFFFF, false);
		graphics.fill(chatplus$contextMenuX + 6, chatplus$contextMenuY + 19, chatplus$contextMenuX + 16, chatplus$contextMenuY + 29, 0xAA333333);
		graphics.outline(chatplus$contextMenuX + 6, chatplus$contextMenuY + 19, 10, 10, 0xAAFFFFFF);
		if (showPrivate) {
			graphics.text(font, "x", chatplus$contextMenuX + 8, chatplus$contextMenuY + 20, 0xFFFFFFFF, false);
		}
		graphics.text(font, "Show private here", chatplus$contextMenuX + 21, chatplus$contextMenuY + 20, 0xFFFFFFFF, false);
		graphics.fill(chatplus$contextMenuX + 6, chatplus$contextMenuY + 34, chatplus$contextMenuX + 16, chatplus$contextMenuY + 44, 0xAA333333);
		graphics.outline(chatplus$contextMenuX + 6, chatplus$contextMenuY + 34, 10, 10, 0xAAFFFFFF);
		if (showServer) {
			graphics.text(font, "x", chatplus$contextMenuX + 8, chatplus$contextMenuY + 35, 0xFFFFFFFF, false);
		}
		graphics.text(font, "Show server here", chatplus$contextMenuX + 21, chatplus$contextMenuY + 35, 0xFFFFFFFF, false);
	}

	private void chatplus$renderStatusBanner(GuiGraphicsExtractor graphics, int panelX, int inputY, int panelWidth) {
		String text = "";
		int color = 0xCC333333;
		if (chatplus$confirmingCoordinateSend) {
			text = "Coordinates detected. Enter sends to " + chatplus$pendingCoordinateTab + ", Esc cancels.";
			color = 0xDD663333;
		} else if (chatplus$ghostTimerMode > 0 && GhostTimerManager.isCoolingDown()) {
			float seconds = GhostTimerManager.remainingMillis() / 1000.0F;
			text = "Ghost Timer " + chatplus$getGhostTimerLabel() + ": " + String.format(Locale.ROOT, "%.1fs", seconds);
			if (GhostTimerManager.queuedCount() > 1) {
				text += " (" + GhostTimerManager.queuedCount() + " queued)";
			} else if (GhostTimerManager.queuedCount() == 1) {
				text += " (1 queued)";
			}
			color = 0xCC3E314A;
		} else if (chatplus$searching) {
			text = "Search results: " + chatplus$getSelectedMessages().size();
			color = 0xCC26384A;
		}
		if (text.isEmpty()) {
			return;
		}

		int x = panelX + PANEL_PADDING;
		int y = inputY - STATUS_BANNER_HEIGHT - 2;
		int width = panelWidth - PANEL_PADDING * 2;
		graphics.fill(x, y, x + width, y + STATUS_BANNER_HEIGHT, color);
		graphics.outline(x, y, width, STATUS_BANNER_HEIGHT, 0xAAFFFFFF);
		graphics.text(font, chatplus$fitLabel(text, width - 8), x + 4, y + 3, 0xFFFFFFFF, false);
	}

	private boolean chatplus$hasStatusBanner() {
		return chatplus$confirmingCoordinateSend
				|| (chatplus$ghostTimerMode > 0 && GhostTimerManager.isCoolingDown())
				|| chatplus$searching;
	}

	private boolean chatplus$isInPrefixButton(double mouseX, double mouseY) {
		return chatplus$isInBox(mouseX, mouseY, ChatWindowState.getX() + PANEL_PADDING, chatplus$getInputY(), chatplus$getPrefixBoxWidth(), INPUT_HEIGHT);
	}

	private boolean chatplus$handlePrefixMenuClick(double mouseX, double mouseY) {
		if (!chatplus$showPrefixMenu) {
			return false;
		}

		int x = ChatWindowState.getX() + PANEL_PADDING;
		int y = chatplus$getInputY() - MENU_ROW_HEIGHT * 4 - 3;
		if (!chatplus$isInBox(mouseX, mouseY, x, y, PREFIX_MENU_WIDTH, MENU_ROW_HEIGHT * 4 + 4)) {
			chatplus$showPrefixMenu = false;
			return false;
		}

		int row = chatplus$clamp((int) ((mouseY - y - 2) / MENU_ROW_HEIGHT), 0, 3);
		chatplus$awaitingWhisperName = false;
		switch (row) {
			case 0 -> {
				chatplus$manualSendPrefix = true;
				chatplus$prefixLabelOverride = "Global";
				chatplus$setPrefixValue("");
			}
			case 1 -> {
				chatplus$manualSendPrefix = true;
				chatplus$prefixLabelOverride = "/lc";
				chatplus$setPrefixValue("/lc ");
			}
			case 2 -> {
				chatplus$manualSendPrefix = true;
				chatplus$prefixLabelOverride = "/tc";
				chatplus$setPrefixValue("/tc ");
			}
			case 3 -> {
				chatplus$manualSendPrefix = true;
				chatplus$prefixLabelOverride = "";
				chatplus$awaitingWhisperName = true;
				chatplus$whisperPickerScroll = 0;
				chatplus$setPrefixValue("To?");
				input.setValue("");
			}
			default -> {
			}
		}
		chatplus$showPrefixMenu = false;
		chatplus$focusInput();
		return true;
	}

	private void chatplus$renderWhisperPicker(GuiGraphicsExtractor graphics) {
		List<String> players = chatplus$getFilteredOnlinePlayers();
		int rows = Math.max(1, Math.min(WHISPER_PICKER_ROWS, players.isEmpty() ? 1 : players.size()));
		int x = ChatWindowState.getX() + PANEL_PADDING;
		int y = chatplus$getInputY() - MENU_ROW_HEIGHT * rows - 17;
		int height = MENU_ROW_HEIGHT * rows + 16;
		int maxScroll = Math.max(0, players.size() - WHISPER_PICKER_ROWS);
		chatplus$whisperPickerScroll = chatplus$clamp(chatplus$whisperPickerScroll, 0, maxScroll);

		graphics.fill(x, y, x + WHISPER_PICKER_WIDTH, y + height, 0xEE151515);
		graphics.outline(x, y, WHISPER_PICKER_WIDTH, height, 0xCCFFFFFF);
		String header = input.getValue().isBlank() ? "Online players" : "Matches: " + players.size();
		graphics.text(font, chatplus$fitLabel(header, WHISPER_PICKER_WIDTH - 10), x + 5, y + 4, 0xFFCCCCCC, false);
		if (players.isEmpty()) {
			graphics.text(font, "Type a name", x + 6, y + 17, 0xFFAAAAAA, false);
			return;
		}

		for (int row = 0; row < rows; row++) {
			int index = chatplus$whisperPickerScroll + row;
			if (index >= players.size()) {
				break;
			}
			String player = players.get(index);
			int rowY = y + 15 + row * MENU_ROW_HEIGHT;
			graphics.fill(x + 2, rowY, x + WHISPER_PICKER_WIDTH - 2, rowY + MENU_ROW_HEIGHT, row == 0 ? 0x88666666 : 0x66444444);
			graphics.text(font, chatplus$fitLabel(player, WHISPER_PICKER_WIDTH - 12), x + 6, rowY + 3, 0xFFFFFFFF, false);
		}
	}

	private boolean chatplus$handleWhisperPickerClick(double mouseX, double mouseY) {
		if (!chatplus$awaitingWhisperName) {
			return false;
		}

		List<String> players = chatplus$getFilteredOnlinePlayers();
		int rows = Math.max(1, Math.min(WHISPER_PICKER_ROWS, players.isEmpty() ? 1 : players.size()));
		int x = ChatWindowState.getX() + PANEL_PADDING;
		int y = chatplus$getInputY() - MENU_ROW_HEIGHT * rows - 17;
		int height = MENU_ROW_HEIGHT * rows + 16;
		if (!chatplus$isInBox(mouseX, mouseY, x, y, WHISPER_PICKER_WIDTH, height)) {
			return false;
		}

		if (players.isEmpty() || mouseY < y + 15) {
			chatplus$focusInput();
			return true;
		}

		int row = (int) ((mouseY - y - 15) / MENU_ROW_HEIGHT);
		int index = chatplus$whisperPickerScroll + chatplus$clamp(row, 0, WHISPER_PICKER_ROWS - 1);
		if (index >= 0 && index < players.size()) {
			chatplus$chooseWhisperPartner(players.get(index));
		}
		chatplus$focusInput();
		return true;
	}

	private boolean chatplus$scrollWhisperPicker(double mouseX, double mouseY, double verticalAmount) {
		if (!chatplus$awaitingWhisperName) {
			return false;
		}

		List<String> players = chatplus$getFilteredOnlinePlayers();
		int rows = Math.max(1, Math.min(WHISPER_PICKER_ROWS, players.isEmpty() ? 1 : players.size()));
		int x = ChatWindowState.getX() + PANEL_PADDING;
		int y = chatplus$getInputY() - MENU_ROW_HEIGHT * rows - 17;
		int height = MENU_ROW_HEIGHT * rows + 16;
		if (!chatplus$isInBox(mouseX, mouseY, x, y, WHISPER_PICKER_WIDTH, height)) {
			return false;
		}

		int maxScroll = Math.max(0, players.size() - WHISPER_PICKER_ROWS);
		chatplus$whisperPickerScroll = chatplus$clamp(chatplus$whisperPickerScroll + (verticalAmount < 0 ? 1 : -1), 0, maxScroll);
		return true;
	}

	private String chatplus$getWhisperPickerSelection() {
		String typed = input.getValue().trim();
		List<String> players = chatplus$getFilteredOnlinePlayers();
		if (!players.isEmpty()) {
			for (String player : players) {
				if (player.equalsIgnoreCase(typed)) {
					return player;
				}
			}
			if (!typed.isEmpty() && players.size() == 1) {
				return players.get(0);
			}
		}
		return typed;
	}

	private void chatplus$chooseWhisperPartner(String partner) {
		chatplus$awaitingWhisperName = false;
		chatplus$manualSendPrefix = true;
		chatplus$whisperPickerScroll = 0;
		chatplus$setPrefixValue("/tell " + partner + " ");
		input.setValue("");
	}

	private List<String> chatplus$getFilteredOnlinePlayers() {
		List<String> players = new ArrayList<>();
		if (minecraft.getConnection() == null) {
			return players;
		}

		String query = input.getValue().trim().toLowerCase(Locale.ROOT);
		String localName = minecraft.getUser() == null ? "" : minecraft.getUser().getName();
		for (PlayerInfo playerInfo : minecraft.getConnection().getOnlinePlayers()) {
			String name = playerInfo.getProfile().name();
			if (name == null || name.isBlank() || name.equalsIgnoreCase(localName)) {
				continue;
			}
			if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)) {
				players.add(name);
			}
		}
		players.sort(String.CASE_INSENSITIVE_ORDER);
		return players;
	}

	private boolean chatplus$handleEmotePickerClick(double mouseX, double mouseY) {
		if (!chatplus$showEmotePicker) {
			return false;
		}

		int width = EMOTE_PICKER_WIDTH;
		int x = chatplus$getEmoteButtonX() + EMOTE_BUTTON_WIDTH - width;
		int y = chatplus$getInputY() - MENU_ROW_HEIGHT * EMOTE_OPTIONS.length - 5;
		if (!chatplus$isInBox(mouseX, mouseY, x, y, width, MENU_ROW_HEIGHT * EMOTE_OPTIONS.length + 4)) {
			chatplus$showEmotePicker = false;
			return false;
		}

		int row = chatplus$clamp((int) ((mouseY - y - 2) / MENU_ROW_HEIGHT), 0, EMOTE_OPTIONS.length - 1);
		String value = input.getValue();
		String command = EMOTE_OPTIONS[row].command();
		input.setValue(value + (value.isEmpty() || value.endsWith(" ") ? "" : " ") + command + " ");
		chatplus$showEmotePicker = false;
		chatplus$focusInput();
		return true;
	}

	private boolean chatplus$openTabContextMenu(double mouseX, double mouseY) {
		for (TabHit tab : chatplus$tabHits) {
			if (chatplus$isInBox(mouseX, mouseY, tab.x(), tab.y(), tab.width(), tab.height())) {
				if (tab.mode() == ChatInputMode.COREPROTECT || !tab.whisperPartner().isEmpty()) {
					return false;
				}
				chatplus$contextTabType = chatplus$tabTypeForMode(tab.mode());
				chatplus$contextMenuX = chatplus$clamp((int) mouseX, 0, Math.max(0, width - TAB_CONTEXT_WIDTH));
				chatplus$contextMenuY = chatplus$clamp((int) mouseY, 0, Math.max(0, height - TAB_CONTEXT_HEIGHT));
				chatplus$showTabContextMenu = true;
				chatplus$showPrefixMenu = false;
				chatplus$showEmotePicker = false;
				chatplus$focusInput();
				return true;
			}
		}
		return false;
	}

	private boolean chatplus$handleTabContextClick(double mouseX, double mouseY) {
		if (!chatplus$showTabContextMenu) {
			return false;
		}

		if (!chatplus$isInBox(mouseX, mouseY, chatplus$contextMenuX, chatplus$contextMenuY, TAB_CONTEXT_WIDTH, TAB_CONTEXT_HEIGHT)) {
			chatplus$showTabContextMenu = false;
			return false;
		}

		if (mouseY >= chatplus$contextMenuY + 32) {
			boolean show = !chatplus$getShowServerMessagesInTab(chatplus$contextTabType);
			ChatPlusConfig.setShowServerMessagesInTab(chatplus$contextTabType, show);
		} else if (mouseY >= chatplus$contextMenuY + 17) {
			boolean show = !chatplus$getShowWhispersInTab(chatplus$contextTabType);
			ChatPlusConfig.setShowWhispersInTab(chatplus$contextTabType, show);
		}
		chatplus$showTabContextMenu = false;
		chatplus$focusInput();
		return true;
	}

	private boolean chatplus$getShowWhispersInTab(ChatTabType tabType) {
		ChatPlusConfig.Settings settings = ChatPlusConfig.get();
		return switch (tabType) {
			case GENERAL -> settings.showWhispersInGeneral;
			case LOCAL_CHAT -> settings.showWhispersInLocal;
			case TEAM_CHAT -> settings.showWhispersInTeam;
			case SYSTEM -> settings.showWhispersInSystem;
			case COREPROTECT -> false;
			default -> false;
		};
	}

	private boolean chatplus$getShowServerMessagesInTab(ChatTabType tabType) {
		ChatPlusConfig.Settings settings = ChatPlusConfig.get();
		return switch (tabType) {
			case GENERAL -> settings.showServerMessagesInGeneral;
			case LOCAL_CHAT -> settings.showServerMessagesInLocal;
			case TEAM_CHAT -> settings.showServerMessagesInTeam;
			default -> false;
		};
	}

	private ChatTabType chatplus$tabTypeForMode(ChatInputMode mode) {
		return switch (mode) {
			case SERVER -> ChatTabType.SYSTEM;
			case LOCAL -> ChatTabType.LOCAL_CHAT;
			case TEAM -> ChatTabType.TEAM_CHAT;
			case COREPROTECT -> ChatTabType.COREPROTECT;
			default -> ChatTabType.GENERAL;
		};
	}

	private String chatplus$getPrefixLabel() {
		if (chatplus$searching) {
			int count = chatplus$getSelectedMessages().size();
			return input.getValue().isBlank() ? "Search" : "Search " + count;
		}

		if (chatplus$awaitingWhisperName) {
			return "To?";
		}

		if (!chatplus$prefixLabelOverride.isBlank()) {
			return chatplus$prefixLabelOverride;
		}

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

	private int chatplus$getEmoteButtonX() {
		return input.getX() + input.getWidth() + INPUT_GAP;
	}

	private boolean chatplus$isInEmoteButton(double mouseX, double mouseY) {
		return chatplus$isInBox(mouseX, mouseY, chatplus$getEmoteButtonX(), chatplus$getInputY(), EMOTE_BUTTON_WIDTH, INPUT_HEIGHT);
	}

	private boolean chatplus$isInPrivateIndicator(double mouseX, double mouseY) {
		return chatplus$isInBox(mouseX, mouseY, chatplus$getPrivateIndicatorX(), chatplus$getPrivateIndicatorY(), chatplus$getPrivateIndicatorWidth(), PRIVATE_INDICATOR_HEIGHT);
	}

	private int chatplus$getPrivateIndicatorX() {
		return chatplus$clamp(chatplus$privateIndicatorX, 0, Math.max(0, width - chatplus$getPrivateIndicatorWidth()));
	}

	private int chatplus$getPrivateIndicatorY() {
		return chatplus$clamp(chatplus$privateIndicatorY, 0, Math.max(0, height - PRIVATE_INDICATOR_HEIGHT));
	}

	private int chatplus$getPrivateIndicatorWidth() {
		return font.width("PM") + 10;
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

	private void chatplus$renderSettingsPanel(GuiGraphicsExtractor graphics, int panelX, int panelY, int panelWidth) {
		int x = panelX + panelWidth - SETTINGS_WIDTH - PANEL_PADDING;
		int y = panelY + TAB_HEIGHT + PANEL_PADDING;
		int height = chatplus$getSettingsPanelHeight();
		graphics.fill(x, y, x + SETTINGS_WIDTH, y + height, 0xDD151515);
		graphics.outline(x, y, SETTINGS_WIDTH, height, 0xCCFFFFFF);
		chatplus$renderSettingsTabs(graphics, x, y);
		int contentY = y + PANEL_PADDING + SETTINGS_ROW_HEIGHT;
		if (chatplus$settingsPage == 0) {
			chatplus$renderOptionRow(graphics, x, contentY, "Font Size", Integer.toString(chatplus$textSize));
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT, "Line spacing", Integer.toString(chatplus$lineSpacing));
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 2, "Font", chatplus$getFontStyleLabel());
			chatplus$renderSliderRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 3, "BG Opacity", chatplus$backgroundOpacity);
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 4, "HUD BG", chatplus$hudBackgroundEnabled ? "On" : "Off");
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 5, "Heads", chatplus$chatHeadsEnabled ? "On" : "Off");
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 6, "Timestamps", chatplus$timestampsEnabled ? "On" : "Off");
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 7, "Coords", chatplus$coordinateWarningsEnabled ? "Warn" : "Off");
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 8, "Ghost Timer", chatplus$getGhostTimerLabel());
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 9, "Emotes", chatplus$emotesEnabled ? "On" : "Off");
			chatplus$renderOptionRow(graphics, x, contentY + SETTINGS_ROW_HEIGHT * 10, "Skill Text", chatplus$skillTrackerMessagesInAll ? "All" : "System");
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
		return SETTINGS_ROW_HEIGHT * 12 + PANEL_PADDING * 2;
	}

	private void chatplus$renderSettingsTabs(GuiGraphicsExtractor graphics, int panelX, int panelY) {
		int tabY = panelY + 4;
		int tabX = panelX + 6;
		int gap = 3;
		int tabWidth = (SETTINGS_WIDTH - 12 - gap * 2) / 3;
		chatplus$renderSettingsTab(graphics, tabX, tabY, tabWidth, "Settings", chatplus$settingsPage == 0);
		chatplus$renderSettingsTab(graphics, tabX + tabWidth + gap, tabY, tabWidth, "Colors", chatplus$settingsPage == 1);
		chatplus$renderSettingsTab(graphics, tabX + (tabWidth + gap) * 2, tabY, tabWidth, "History", chatplus$settingsPage == 2);
	}

	private void chatplus$renderSettingsTab(GuiGraphicsExtractor graphics, int x, int y, int width, String label, boolean selected) {
		graphics.fill(x, y, x + width, y + 12, selected ? 0xCC777777 : 0xAA333333);
		graphics.outline(x, y, width, 12, selected ? 0xFFFFFFFF : 0xAA888888);
		graphics.text(font, chatplus$fitLabel(label, width - 8), x + 4, y + 2, 0xFFFFFFFF, false);
	}

	private void chatplus$renderOptionRow(GuiGraphicsExtractor graphics, int panelX, int rowY, String label, String value) {
		graphics.text(font, label, panelX + 6, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 78, rowY + 2, panelX + 90, rowY + 14, 0xAA444444);
		graphics.outline(panelX + 78, rowY + 2, 12, 12, 0xAAFFFFFF);
		graphics.text(font, "-", panelX + 82, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 94, rowY + 2, panelX + SETTINGS_WIDTH - 22, rowY + 14, 0xAA333333);
		graphics.outline(panelX + 94, rowY + 2, SETTINGS_WIDTH - 116, 12, 0xAA888888);
		graphics.text(font, value, panelX + 98, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + SETTINGS_WIDTH - 18, rowY + 2, panelX + SETTINGS_WIDTH - 6, rowY + 14, 0xAA444444);
		graphics.outline(panelX + SETTINGS_WIDTH - 18, rowY + 2, 12, 12, 0xAAFFFFFF);
		graphics.text(font, "+", panelX + SETTINGS_WIDTH - 15, rowY + 4, 0xFFFFFFFF, false);
	}

	private void chatplus$renderSliderRow(GuiGraphicsExtractor graphics, int panelX, int rowY, String label, int value) {
		int sliderX = panelX + 78;
		int sliderY = rowY + 6;
		int knobX = sliderX + Math.round((value - 1) * (SLIDER_WIDTH - 4) / 99.0F);
		graphics.text(font, label, panelX + 6, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + 3, 0xAA444444);
		graphics.fill(knobX, rowY + 3, knobX + 4, rowY + 13, 0xCCFFFFFF);
		graphics.text(font, Integer.toString(value), panelX + SETTINGS_WIDTH - 28, rowY + 4, 0xFFFFFFFF, false);
	}

	private void chatplus$renderColorRow(GuiGraphicsExtractor graphics, int panelX, int rowY, String label, int color) {
		graphics.text(font, label, panelX + 6, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 78, rowY + 2, panelX + 90, rowY + 14, 0xAA444444);
		graphics.outline(panelX + 78, rowY + 2, 12, 12, 0xAAFFFFFF);
		graphics.text(font, "-", panelX + 82, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 94, rowY + 2, panelX + SETTINGS_WIDTH - 22, rowY + 14, 0xAA333333);
		graphics.outline(panelX + 94, rowY + 2, SETTINGS_WIDTH - 116, 12, 0xAA888888);
		graphics.fill(panelX + 98, rowY + 4, panelX + 108, rowY + 12, color);
		graphics.outline(panelX + 98, rowY + 4, 10, 8, 0xAAFFFFFF);
		graphics.text(font, chatplus$getColorLabel(color), panelX + 112, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + SETTINGS_WIDTH - 18, rowY + 2, panelX + SETTINGS_WIDTH - 6, rowY + 14, 0xAA444444);
		graphics.outline(panelX + SETTINGS_WIDTH - 18, rowY + 2, 12, 12, 0xAAFFFFFF);
		graphics.text(font, "+", panelX + SETTINGS_WIDTH - 15, rowY + 4, 0xFFFFFFFF, false);
	}

	private void chatplus$renderActionRow(GuiGraphicsExtractor graphics, int panelX, int rowY, String label, boolean active) {
		graphics.text(font, label, panelX + 6, rowY + 4, 0xFFFFFFFF, false);
		graphics.fill(panelX + 78, rowY + 2, panelX + SETTINGS_WIDTH - 6, rowY + 14, active ? 0xCC886666 : 0xAA553333);
		graphics.outline(panelX + 78, rowY + 2, SETTINGS_WIDTH - 84, 12, active ? 0xFFFFFFFF : 0xAAFFFFFF);
		graphics.text(font, "Clear", panelX + 98, rowY + 4, 0xFFFFFFFF, false);
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
		if (row < 0 || row > 10) {
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
				chatplus$fontStyleIndex = (chatplus$fontStyleIndex + 1) % 3;
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
			case 6 -> {
				chatplus$timestampsEnabled = !chatplus$timestampsEnabled;
				ChatPlusConfig.setTimestampsEnabled(chatplus$timestampsEnabled);
			}
			case 7 -> {
				chatplus$coordinateWarningsEnabled = !chatplus$coordinateWarningsEnabled;
				ChatPlusConfig.setCoordinateWarningsEnabled(chatplus$coordinateWarningsEnabled);
			}
			case 8 -> {
				chatplus$ghostTimerMode = (chatplus$ghostTimerMode + 1) % 3;
				ChatPlusConfig.setGhostTimerMode(chatplus$ghostTimerMode);
				if (chatplus$ghostTimerMode != 1) {
					chatplus$clearGhostMessages();
				}
			}
			case 9 -> {
				chatplus$emotesEnabled = !chatplus$emotesEnabled;
				ChatPlusConfig.setEmotesEnabled(chatplus$emotesEnabled);
			}
			case 10 -> {
				chatplus$skillTrackerMessagesInAll = !chatplus$skillTrackerMessagesInAll;
				ChatPlusConfig.setSkillTrackerMessagesInAll(chatplus$skillTrackerMessagesInAll);
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

	private void chatplus$drawScaledString(GuiGraphicsExtractor graphics, FormattedCharSequence text, int x, int y, int color, float scale) {
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, (int) (x / scale), (int) (y / scale), color, chatplus$usesTextShadow());
		graphics.pose().popMatrix();
	}

	private void chatplus$drawScaledString(GuiGraphicsExtractor graphics, String text, int x, int y, int color, float scale) {
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, (int) (x / scale), (int) (y / scale), color, chatplus$usesTextShadow());
		graphics.pose().popMatrix();
	}

	private int chatplus$scaledDrawX(int x, float scale) {
		return Math.round((int) (x / scale) * scale);
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

	private Component chatplus$applySelectedFont(ChatMessage chatMessage, Component message) {
		if (chatMessage.getType() == ChatTabType.COREPROTECT || (chatplus$fontStyleIndex != 1 && chatplus$fontStyleIndex != 2)) {
			return message;
		}

		return message.copy().withStyle(style -> style.withFont(LEXEND_FONT));
	}

	private Component chatplus$formatMessage(ChatMessage message) {
		String playerName = chatplus$profileNameFor(message);
		Component formatted = chatplus$fontStyleIndex == 2 && message.getType() != ChatTabType.COREPROTECT
				? ChatColorFormatter.discordStyle(message, chatplus$getMessageColor(message), playerName)
				: ChatColorFormatter.colorMessageBody(message, chatplus$getMessageColor(message), playerName);
		if (chatplus$emotesEnabled && message.getType() != ChatTabType.COREPROTECT) {
			formatted = ChatColorFormatter.applyEmotes(formatted);
		}
		formatted = ChatColorFormatter.linkifyUrls(formatted);
		if (chatplus$timestampsEnabled && chatplus$fontStyleIndex != 2) {
			formatted = ChatColorFormatter.prependTimestamp(formatted, message.getTimestamp());
		}
		return chatplus$applySelectedFont(message, formatted);
	}

	private String chatplus$getFontStyleLabel() {
		return switch (chatplus$fontStyleIndex) {
			case 1 -> "Lexend";
			case 2 -> "Discord";
			default -> "Default";
		};
	}

	private String chatplus$getChatStyleLabel() {
		return chatplus$chatStyleIndex == 1 ? "Discord" : "Default";
	}

	private String chatplus$getGhostTimerLabel() {
		return switch (chatplus$ghostTimerMode) {
			case 1 -> "On Que'd";
			case 2 -> "On Strict";
			default -> "Off";
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
			case COREPROTECT -> 72;
		};
	}
}
