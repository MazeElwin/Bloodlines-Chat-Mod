package com.example.chatplus.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manages different chat tabs and message routing
 */
public class ChatTabManager {
	private static ChatTabManager instance;
	private final Map<ChatTabType, List<ChatMessage>> chatTabs = new HashMap<>();
	private final List<ChatMessage> allMessages = Collections.synchronizedList(new ArrayList<>());
	private final Map<String, List<ChatMessage>> whisperMessages = new LinkedHashMap<>();
	private final Set<String> openWhisperTabs = new LinkedHashSet<>();
	private final Set<String> unreadWhispers = new HashSet<>();
	private final List<ChatMessage> mentionMessages = Collections.synchronizedList(new ArrayList<>());
	private final List<ChatMessage> coreProtectMessages = Collections.synchronizedList(new ArrayList<>());
	private final Set<String> openCoreProtectTabs = new LinkedHashSet<>();
	private final Set<ChatMessage> unreadMentions = Collections.newSetFromMap(new IdentityHashMap<>());
	private final List<ChatTabListener> listeners = new ArrayList<>();
	private String pendingWhisperOpen = "";
	private long messageRevision;
	
	private ChatTabManager() {
		for (ChatTabType type : ChatTabType.values()) {
			chatTabs.put(type, Collections.synchronizedList(new ArrayList<>()));
		}
		for (ChatMessage message : RawChatLog.loadHistory(ChatPlusConfig.get().historyRetentionDays)) {
			handleParsedMessage(message, false, false);
		}
	}
	
	public static ChatTabManager getInstance() {
		if (instance == null) {
			instance = new ChatTabManager();
		}
		return instance;
	}
	
	/**
	 * Parse and route a chat message to the appropriate tab
	 */
	public void handleChatMessage(String message) {
		handleIncomingMessage(message);
	}

	public void handleIncomingMessage(String message) {
		handleParsedMessage(ChatMessageParser.parse(message));
	}

	public void handleIncomingMessage(Component message) {
		handleIncomingMessage(message, "", "");
	}

	public void handleIncomingMessage(Component message, String senderId, String localPlayerName) {
		ChatMessage chatMessage = ChatMessageParser.parse(message.getString(), false, createStyleTrace(message));
		if (chatMessage != null) {
			chatMessage = chatMessage.withDisplayMessage(message);
		}
		if (chatMessage != null) {
			if (senderId != null && !senderId.isBlank()) {
				chatMessage = chatMessage.withSenderId(senderId);
			}
			if (!chatMessage.isOutgoing() && isMentioned(chatMessage.getRawMessage(), localPlayerName)) {
				chatMessage = chatMessage.withMentionedPlayer(localPlayerName);
			}
		}
		handleParsedMessage(chatMessage);
	}

	public void handleOutgoingMessage(String message) {
		handleParsedMessage(ChatMessageParser.parse(message, true));
	}

	public boolean handleCoreProtectCommand(String message) {
		String command = message == null ? "" : message.trim();
		if (command.startsWith("/")) {
			command = command.substring(1);
		}

		String[] parts = command.split("\\s+");
		if (parts.length == 0 || (!parts[0].equalsIgnoreCase("co") && !parts[0].equalsIgnoreCase("coreprotect"))) {
			return false;
		}

		openCoreProtectTabs.add("");
		messageRevision++;
		return true;
	}

	public void addLocalSystemMessage(String message) {
		addLocalSystemMessage(message, true);
	}

	public void addLocalSystemMessage(String message, boolean includeInAll) {
		handleParsedMessage(new ChatMessage(
				message,
				Component.literal(message),
				ChatTabType.SYSTEM,
				"SERVER",
				message,
				false,
				""
		), false, true, includeInAll);
	}

	private void handleParsedMessage(ChatMessage chatMessage) {
		handleParsedMessage(chatMessage, true, true);
	}

	private void handleParsedMessage(ChatMessage chatMessage, boolean logMessage, boolean notify) {
		handleParsedMessage(chatMessage, logMessage, notify, true);
	}

	private void handleParsedMessage(ChatMessage chatMessage, boolean logMessage, boolean notify, boolean includeInAll) {
		if (chatMessage != null) {
			if (!openCoreProtectTabs.isEmpty() && chatMessage.getType() == ChatTabType.SYSTEM) {
				chatMessage = chatMessage.withType(ChatTabType.COREPROTECT);
			}
			List<ChatMessage> tab = chatTabs.get(chatMessage.getType());
			if (tab != null) {
				boolean effectiveIncludeInAll = includeInAll && shouldIncludeInAll(chatMessage);
				if (effectiveIncludeInAll) {
					allMessages.add(chatMessage);
				}
				tab.add(chatMessage);
				if (chatMessage.getType() == ChatTabType.COREPROTECT) {
					coreProtectMessages.add(chatMessage);
				}
				if (chatMessage.getType() == ChatTabType.WHISPER && !chatMessage.getWhisperPartner().isEmpty()) {
					String partner = chatMessage.getWhisperPartner();
					whisperMessages.computeIfAbsent(partner, ignored -> Collections.synchronizedList(new ArrayList<>())).add(chatMessage);
					if (notify) {
						openWhisperTabs.add(partner);
						if (!chatMessage.isOutgoing()) {
							unreadWhispers.add(partner);
						}
					}
				}
				if (chatMessage.isMention()) {
					mentionMessages.add(chatMessage);
					if (notify && !chatMessage.isOutgoing()) {
						unreadMentions.add(chatMessage);
					}
				}
				if (logMessage) {
					RawChatLog.append(chatMessage);
				}
				if (notify) {
					notifyListeners(chatMessage);
				}
				messageRevision++;
			}
		}
	}

	private boolean shouldIncludeInAll(ChatMessage chatMessage) {
		return chatMessage.getType() != ChatTabType.SYSTEM
				|| ChatPlusConfig.get().skillTrackerMessagesInAll
				|| !SkillProgressTracker.isSkillPlaintextMessage(chatMessage.getRawMessage());
	}
	
	/**
	 * Get all messages from a specific tab
	 */
	public List<String> getTabMessages(ChatTabType tabType) {
		List<ChatMessage> messages = getParsedTabMessages(tabType);
		List<String> rawMessages = new ArrayList<>(messages.size());
		for (ChatMessage message : messages) {
			rawMessages.add(message.getRawMessage());
		}
		return rawMessages;
	}

	public List<ChatMessage> getParsedTabMessages(ChatTabType tabType) {
		return snapshot(chatTabs.getOrDefault(tabType, Collections.emptyList()));
	}

	public List<ChatMessage> getParsedTabMessagesSince(ChatTabType tabType, long cutoff) {
		return snapshotSince(chatTabs.getOrDefault(tabType, Collections.emptyList()), cutoff);
	}

	public List<ChatMessage> getAllParsedMessages() {
		return snapshot(allMessages);
	}

	public List<ChatMessage> getAllParsedMessagesSince(long cutoff) {
		return snapshotSince(allMessages, cutoff);
	}

	public long getMessageRevision() {
		return messageRevision;
	}

	public List<String> getWhisperPartners() {
		return new ArrayList<>(openWhisperTabs);
	}

	public List<ChatMessage> getWhisperMessages(String partner) {
		return snapshot(whisperMessages.getOrDefault(partner, Collections.emptyList()));
	}

	public List<ChatMessage> getMentionMessages() {
		return snapshot(mentionMessages);
	}

	public List<ChatMessage> getCoreProtectMessages() {
		return snapshot(coreProtectMessages);
	}

	public List<ChatMessage> getCoreProtectMessagesSince(long cutoff) {
		return snapshotSince(coreProtectMessages, cutoff);
	}

	public boolean hasCoreProtectTab() {
		return openCoreProtectTabs.contains("");
	}

	public void closeCoreProtectTab(String lookup) {
		openCoreProtectTabs.remove("");
		messageRevision++;
	}

	public boolean hasUnreadMentions() {
		return !unreadMentions.isEmpty();
	}

	public void markMentionsRead() {
		unreadMentions.clear();
	}

	public void closeWhisperTab(String partner) {
		openWhisperTabs.remove(partner);
		unreadWhispers.remove(partner);
	}

	public boolean hasUnreadWhisper(String partner) {
		return unreadWhispers.contains(partner);
	}

	public boolean hasUnreadWhispers() {
		return !unreadWhispers.isEmpty();
	}

	public String getLatestUnreadWhisperPartner() {
		String latestPartner = "";
		long latestTimestamp = Long.MIN_VALUE;
		for (String partner : unreadWhispers) {
			List<ChatMessage> messages = whisperMessages.getOrDefault(partner, Collections.emptyList());
			if (!messages.isEmpty()) {
				long timestamp = messages.get(messages.size() - 1).getTimestamp();
				if (timestamp > latestTimestamp) {
					latestTimestamp = timestamp;
					latestPartner = partner;
				}
			}
		}
		return latestPartner;
	}

	public void requestOpenLatestUnreadWhisper() {
		String partner = getLatestUnreadWhisperPartner();
		if (!partner.isBlank()) {
			pendingWhisperOpen = partner;
		}
	}

	public String consumePendingWhisperOpen() {
		String partner = pendingWhisperOpen;
		pendingWhisperOpen = "";
		return partner;
	}

	public void markWhisperRead(String partner) {
		unreadWhispers.remove(partner);
	}

	public void pruneHistory(int retentionDays) {
		if (retentionDays <= 0) {
			return;
		}

		long cutoff = System.currentTimeMillis() - Duration.ofDays(retentionDays).toMillis();
		allMessages.removeIf(message -> message.getTimestamp() < cutoff);
		for (List<ChatMessage> tab : chatTabs.values()) {
			tab.removeIf(message -> message.getTimestamp() < cutoff);
		}
		whisperMessages.entrySet().removeIf(entry -> {
			entry.getValue().removeIf(message -> message.getTimestamp() < cutoff);
			if (entry.getValue().isEmpty()) {
				unreadWhispers.remove(entry.getKey());
				openWhisperTabs.remove(entry.getKey());
				return true;
			}
			return false;
		});
		mentionMessages.removeIf(message -> message.getTimestamp() < cutoff);
		coreProtectMessages.removeIf(message -> message.getTimestamp() < cutoff);
		unreadMentions.removeIf(message -> message.getTimestamp() < cutoff);
		RawChatLog.prune(retentionDays);
		messageRevision++;
	}
	
	/**
	 * Clear a specific tab
	 */
	public void clearTab(ChatTabType tabType) {
		List<ChatMessage> tab = chatTabs.get(tabType);
		if (tab != null) {
			tab.clear();
			messageRevision++;
		}
	}
	
	/**
	 * Clear all tabs
	 */
	public void clearAllTabs() {
		allMessages.clear();
		whisperMessages.clear();
		openWhisperTabs.clear();
		unreadWhispers.clear();
		mentionMessages.clear();
		coreProtectMessages.clear();
		openCoreProtectTabs.clear();
		unreadMentions.clear();
		for (List<ChatMessage> tab : chatTabs.values()) {
			tab.clear();
		}
		messageRevision++;
	}
	
	/**
	 * Get tab statistics
	 */
	public Map<ChatTabType, Integer> getTabCounts() {
		Map<ChatTabType, Integer> counts = new LinkedHashMap<>();
		for (ChatTabType type : ChatTabType.values()) {
			counts.put(type, chatTabs.get(type).size());
		}
		return counts;
	}
	
	public void addListener(ChatTabListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(ChatTabListener listener) {
		listeners.remove(listener);
	}
	
	private void notifyListeners(ChatMessage message) {
		for (ChatTabListener listener : listeners) {
			listener.onMessageReceived(message);
		}
	}

	private static String createStyleTrace(Component message) {
		List<String> parts = new ArrayList<>();
		collectStyleTrace(message, parts);
		return String.join(",", parts);
	}

	private static void collectStyleTrace(Component message, List<String> parts) {
		appendStyle(message.getStyle(), parts);

		for (Component sibling : message.getSiblings()) {
			collectStyleTrace(sibling, parts);
		}

	}

	private static void appendStyle(Style style, List<String> parts) {
		if (style == null || style.isEmpty()) {
			return;
		}

		if (style.getColor() != null) {
			parts.add("color=" + style.getColor().serialize());
			parts.add(String.format("rgb=#%06X", style.getColor().getValue()));
		}
	}

	private static boolean isMentioned(String rawMessage, String playerName) {
		if (rawMessage == null || playerName == null || playerName.isBlank()) {
			return false;
		}

		String needle = "@" + playerName.toLowerCase(Locale.ROOT);
		String lower = rawMessage.toLowerCase(Locale.ROOT);
		int index = lower.indexOf(needle);
		while (index >= 0) {
			int end = index + needle.length();
			boolean startsCleanly = index == 0 || !isMentionNameChar(lower.charAt(index - 1));
			boolean endsCleanly = end >= lower.length() || !isMentionNameChar(lower.charAt(end));
			if (startsCleanly && endsCleanly) {
				return true;
			}
			index = lower.indexOf(needle, index + 1);
		}
		return false;
	}

	private static boolean isMentionNameChar(char character) {
		return Character.isLetterOrDigit(character) || character == '_';
	}

	private static List<ChatMessage> snapshotSince(List<ChatMessage> messages, long cutoff) {
		List<ChatMessage> recent = new ArrayList<>();
		synchronized (messages) {
			for (int index = messages.size() - 1; index >= 0; index--) {
				ChatMessage message = messages.get(index);
				if (message.getTimestamp() < cutoff) {
					break;
				}
				recent.add(message);
			}
		}
		Collections.reverse(recent);
		return recent;
	}

	private static List<ChatMessage> snapshot(List<ChatMessage> messages) {
		synchronized (messages) {
			return new ArrayList<>(messages);
		}
	}
}
