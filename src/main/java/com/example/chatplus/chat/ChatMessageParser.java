package com.example.chatplus.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses chat messages and extracts metadata
 */
public class ChatMessageParser {
	private static final Pattern OUTGOING_WHISPER_COMMAND_PATTERN = Pattern.compile("^/(w|whisper|msg|tell|dm)\\s+(\\S+)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern OUTGOING_LOCAL_COMMAND_PATTERN = Pattern.compile("^/(lc|local)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern OUTGOING_TEAM_COMMAND_PATTERN = Pattern.compile("^/(tc|team|teamchat)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern SENT_WHISPER_CONFIRMATION_PATTERN = Pattern.compile("^You\\s+whispers?\\s+to\\s+(.+?):\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern INCOMING_WHISPER_PATTERN = Pattern.compile("^(?:\\[(?:w|whisper|msg|tell|dm|pm)\\]\\s*)?(?:(?:from|to)\\s+)?(.+?)\\s+(?:whispers?|msgs?|tells?)\\s*(?:to\\s+you)?:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern INCOMING_TEAM_CHAT_PATTERN = Pattern.compile("^(?:\\[(?:tc|team|teamchat)\\]|\\((?:tc|team|teamchat)\\)|(?:team)\\s*>?)\\s*(?:<)?(.+?)(?:>|:|\\s\\u00BB\\s|\\s>\\s)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern INCOMING_LOCAL_CHAT_PATTERN = Pattern.compile("^(?:\\[(?:lc|local|nearby)\\]|\\((?:lc|local|nearby)\\)|(?:local|nearby)\\s*>?)\\s*(?:<)?(.+?)(?:>|:|\\s\\u00BB\\s|\\s>\\s)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern SERVER_CHAT_PATTERN = Pattern.compile("^(.+?)\\s>\\s(.+)$");
	private static final Pattern PLAYER_MESSAGE_PATTERN = Pattern.compile("^<(.+?)>\\s*(.+)$");
	private static final Pattern SYSTEM_MESSAGE_PATTERN = Pattern.compile("^\\[.+\\]|^[*].+");
	private static final Pattern COREPROTECT_INSPECTOR_ENABLED_PATTERN = Pattern.compile("(?i)\\binspector\\s+(?:is\\s+)?now\\s+enabled\\b|\\binspector\\s+enabled\\b");
	private static final Pattern COREPROTECT_INSPECTOR_DISABLED_PATTERN = Pattern.compile("(?i)\\binspector\\s+(?:is\\s+)?now\\s+disabled\\b|\\binspector\\s+disabled\\b");
	
	/**
	 * Parse a chat message and return a ChatMessage object
	 */
	public static ChatMessage parse(String rawMessage) {
		return parse(rawMessage, false);
	}

	public static ChatMessage parse(String rawMessage, boolean outgoing) {
		return parse(rawMessage, outgoing, "");
	}

	public static ChatMessage parse(String rawMessage, boolean outgoing, String styleTrace) {
		if (rawMessage == null || rawMessage.isEmpty()) {
			return null;
		}

		if (outgoing) {
			return parseOutgoing(rawMessage);
		}
		
		Matcher sentWhisperMatcher = SENT_WHISPER_CONFIRMATION_PATTERN.matcher(rawMessage);
		if (sentWhisperMatcher.find()) {
			String recipient = sentWhisperMatcher.group(1).trim();
			String content = sentWhisperMatcher.group(2).trim();
			String partner = extractPlayerName(recipient);
			return new ChatMessage(rawMessage, Component.literal(rawMessage), ChatTabType.WHISPER, "You -> " + recipient, content, true, styleTrace, partner);
		}

		Matcher whisperMatcher = INCOMING_WHISPER_PATTERN.matcher(rawMessage);
		if (whisperMatcher.find()) {
			String sender = whisperMatcher.group(1).trim();
			String content = whisperMatcher.group(2).trim();
			String partner = extractPlayerName(sender);
			return new ChatMessage(rawMessage, Component.literal(rawMessage), ChatTabType.WHISPER, sender, content, false, styleTrace, partner);
		}

		Matcher teamMatcher = INCOMING_TEAM_CHAT_PATTERN.matcher(rawMessage);
		if (teamMatcher.find()) {
			String sender = teamMatcher.group(1).trim();
			String content = teamMatcher.group(2).trim();
			return new ChatMessage(rawMessage, ChatTabType.TEAM_CHAT, sender, content, false, styleTrace);
		}
		
		Matcher localMatcher = INCOMING_LOCAL_CHAT_PATTERN.matcher(rawMessage);
		if (localMatcher.find()) {
			String sender = localMatcher.group(1).trim();
			String content = localMatcher.group(2).trim();
			ChatTabType type = hasTeamChatBodyColor(styleTrace) ? ChatTabType.TEAM_CHAT : hasLocalChatBodyColor(styleTrace) ? ChatTabType.LOCAL_CHAT : ChatTabType.GENERAL;
			return new ChatMessage(rawMessage, type, sender, content, false, styleTrace);
		}

		Matcher serverChatMatcher = SERVER_CHAT_PATTERN.matcher(rawMessage);
		if (serverChatMatcher.find()) {
			String sender = serverChatMatcher.group(1).trim();
			String content = serverChatMatcher.group(2).trim();
			ChatTabType type = hasTeamChatBodyColor(styleTrace) ? ChatTabType.TEAM_CHAT : hasLocalChatBodyColor(styleTrace) ? ChatTabType.LOCAL_CHAT : ChatTabType.GENERAL;
			return new ChatMessage(rawMessage, type, sender, content, false, styleTrace);
		}
		
		if (SYSTEM_MESSAGE_PATTERN.matcher(rawMessage).find()) {
			return createServerMessage(rawMessage, styleTrace);
		}
		
		Matcher playerMatcher = PLAYER_MESSAGE_PATTERN.matcher(rawMessage);
		if (playerMatcher.find()) {
			String sender = playerMatcher.group(1).trim();
			String content = playerMatcher.group(2).trim();
			ChatTabType type = hasTeamChatBodyColor(styleTrace) ? ChatTabType.TEAM_CHAT : ChatTabType.GENERAL;
			return new ChatMessage(rawMessage, type, sender, content, false, styleTrace);
		}
		
		return createServerMessage(rawMessage, styleTrace);
	}

	private static ChatMessage createServerMessage(String rawMessage, String styleTrace) {
		return new ChatMessage(
				rawMessage,
				Component.literal(rawMessage).withStyle(ChatFormatting.YELLOW),
				ChatTabType.SYSTEM,
				"SERVER",
				rawMessage,
				false,
				styleTrace
		);
	}

	private static boolean hasLocalChatBodyColor(String styleTrace) {
		String styles = styleTrace.toLowerCase();
		return styles.endsWith("color=aqua,rgb=#55ffff")
				|| styles.endsWith("color=dark_aqua,rgb=#00aaaa")
				|| styles.endsWith("rgb=#55ffff")
				|| styles.endsWith("rgb=#00ffff")
				|| styles.endsWith("rgb=#00aaaa");
	}

	private static boolean hasTeamChatBodyColor(String styleTrace) {
		String styles = styleTrace.toLowerCase();
		return styles.endsWith("color=yellow,rgb=#ffff55")
				|| styles.endsWith("color=gold,rgb=#ffaa00")
				|| styles.endsWith("rgb=#ffff55")
				|| styles.endsWith("rgb=#ffaa00");
	}

	public static String extractPlayerName(String displayName) {
		String cleaned = displayName == null ? "" : displayName.trim();
		if (cleaned.startsWith("You -> ")) {
			cleaned = cleaned.substring("You -> ".length()).trim();
		}

		String[] parts = cleaned.split("\\s+");
		return parts.length == 0 ? cleaned : parts[parts.length - 1];
	}

	private static ChatMessage parseOutgoing(String rawMessage) {
		Matcher whisperMatcher = OUTGOING_WHISPER_COMMAND_PATTERN.matcher(rawMessage);
		if (whisperMatcher.find()) {
			String recipient = whisperMatcher.group(2).trim();
			String content = whisperMatcher.group(3).trim();
			return new ChatMessage(rawMessage, Component.literal(rawMessage), ChatTabType.WHISPER, "You -> " + recipient, content, true, "", recipient);
		}

		Matcher localMatcher = OUTGOING_LOCAL_COMMAND_PATTERN.matcher(rawMessage);
		if (localMatcher.find()) {
			return new ChatMessage(rawMessage, ChatTabType.LOCAL_CHAT, "You", localMatcher.group(2).trim(), true);
		}

		Matcher teamMatcher = OUTGOING_TEAM_COMMAND_PATTERN.matcher(rawMessage);
		if (teamMatcher.find()) {
			return new ChatMessage(rawMessage, ChatTabType.TEAM_CHAT, "You", teamMatcher.group(2).trim(), true);
		}

		return new ChatMessage(rawMessage, ChatTabType.GENERAL, "You", rawMessage, true);
	}
	
	/**
	 * Check if a message is a whisper command
	 */
	public static boolean isWhisper(String message) {
		return message != null && (OUTGOING_WHISPER_COMMAND_PATTERN.matcher(message).find() || INCOMING_WHISPER_PATTERN.matcher(message).find());
	}
	
	/**
	 * Check if a message is a local chat command
	 */
	public static boolean isLocalChat(String message) {
		return message != null && (OUTGOING_LOCAL_COMMAND_PATTERN.matcher(message).find() || INCOMING_LOCAL_CHAT_PATTERN.matcher(message).find());
	}

	public static boolean isTeamChat(String message) {
		return message != null && (OUTGOING_TEAM_COMMAND_PATTERN.matcher(message).find() || INCOMING_TEAM_CHAT_PATTERN.matcher(message).find());
	}

	public static boolean isDeathMessage(String message) {
		if (message == null) {
			return false;
		}

		String text = message.toLowerCase();
		return text.contains(" died")
				|| text.contains(" was slain")
				|| text.contains(" was shot")
				|| text.contains(" was fireballed")
				|| text.contains(" was pricked")
				|| text.contains(" walked into")
				|| text.contains(" drowned")
				|| text.contains(" suffocated")
				|| text.contains(" burned to death")
				|| text.contains(" went up in flames")
				|| text.contains(" fell ")
				|| text.contains(" hit the ground")
				|| text.contains(" blew up")
				|| text.contains(" was blown up")
				|| text.contains(" was killed")
				|| text.contains(" froze to death")
				|| text.contains(" starved to death")
				|| text.contains(" withered away")
				|| text.contains(" experienced kinetic energy")
				|| text.contains(" was impaled");
	}
	
	/**
	 * Check if a message is a system message
	 */
	public static boolean isSystemMessage(String message) {
		return message != null && SYSTEM_MESSAGE_PATTERN.matcher(message).find();
	}

	public static boolean isCoreProtectInspectorEnabled(String message) {
		return message != null && COREPROTECT_INSPECTOR_ENABLED_PATTERN.matcher(message).find();
	}

	public static boolean isCoreProtectInspectorDisabled(String message) {
		return message != null && COREPROTECT_INSPECTOR_DISABLED_PATTERN.matcher(message).find();
	}
}
