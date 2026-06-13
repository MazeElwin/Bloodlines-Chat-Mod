package com.example.chatplus.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.time.Duration;
import java.util.*;

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
	private final List<ChatTabListener> listeners = new ArrayList<>();
	
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
		ChatMessage chatMessage = ChatMessageParser.parse(message.getString(), false, createStyleTrace(message));
		if (chatMessage != null && chatMessage.getType() != ChatTabType.SYSTEM) {
			chatMessage = chatMessage.withDisplayMessage(message);
		}
		handleParsedMessage(chatMessage);
	}

	public void handleOutgoingMessage(String message) {
		handleParsedMessage(ChatMessageParser.parse(message, true));
	}

	private void handleParsedMessage(ChatMessage chatMessage) {
		handleParsedMessage(chatMessage, true, true);
	}

	private void handleParsedMessage(ChatMessage chatMessage, boolean logMessage, boolean notify) {
		if (chatMessage != null) {
			List<ChatMessage> tab = chatTabs.get(chatMessage.getType());
			if (tab != null) {
				allMessages.add(chatMessage);
				tab.add(chatMessage);
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
				if (logMessage) {
					RawChatLog.append(chatMessage);
				}
				if (notify) {
					notifyListeners(chatMessage);
				}
			}
		}
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
		return new ArrayList<>(chatTabs.getOrDefault(tabType, Collections.emptyList()));
	}

	public List<ChatMessage> getAllParsedMessages() {
		return new ArrayList<>(allMessages);
	}

	public List<String> getWhisperPartners() {
		return new ArrayList<>(openWhisperTabs);
	}

	public List<ChatMessage> getWhisperMessages(String partner) {
		return new ArrayList<>(whisperMessages.getOrDefault(partner, Collections.emptyList()));
	}

	public void closeWhisperTab(String partner) {
		openWhisperTabs.remove(partner);
		unreadWhispers.remove(partner);
	}

	public boolean hasUnreadWhisper(String partner) {
		return unreadWhispers.contains(partner);
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
		RawChatLog.prune(retentionDays);
	}
	
	/**
	 * Clear a specific tab
	 */
	public void clearTab(ChatTabType tabType) {
		List<ChatMessage> tab = chatTabs.get(tabType);
		if (tab != null) {
			tab.clear();
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
		for (List<ChatMessage> tab : chatTabs.values()) {
			tab.clear();
		}
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
}
