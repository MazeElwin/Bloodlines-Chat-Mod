package com.example.chatplus.chat;

import net.minecraft.network.chat.Component;

/**
 * Represents a parsed chat message with metadata
 */
public class ChatMessage {
	private final String rawMessage;
	private final Component displayMessage;
	private final ChatTabType type;
	private final String sender;
	private final String content;
	private final String whisperPartner;
	private final boolean outgoing;
	private final String styleTrace;
	private final long timestamp;
	
	public ChatMessage(String rawMessage, ChatTabType type, String sender, String content) {
		this(rawMessage, type, sender, content, false);
	}

	public ChatMessage(String rawMessage, ChatTabType type, String sender, String content, boolean outgoing) {
		this(rawMessage, type, sender, content, outgoing, "");
	}

	public ChatMessage(String rawMessage, ChatTabType type, String sender, String content, boolean outgoing, String styleTrace) {
		this(rawMessage, Component.literal(rawMessage), type, sender, content, outgoing, styleTrace);
	}

	public ChatMessage(String rawMessage, Component displayMessage, ChatTabType type, String sender, String content, boolean outgoing, String styleTrace) {
		this(rawMessage, displayMessage, type, sender, content, outgoing, styleTrace, "");
	}

	public ChatMessage(String rawMessage, Component displayMessage, ChatTabType type, String sender, String content, boolean outgoing, String styleTrace, String whisperPartner) {
		this(rawMessage, displayMessage, type, sender, content, outgoing, styleTrace, whisperPartner, System.currentTimeMillis());
	}

	public ChatMessage(String rawMessage, Component displayMessage, ChatTabType type, String sender, String content, boolean outgoing, String styleTrace, String whisperPartner, long timestamp) {
		this.rawMessage = rawMessage;
		this.displayMessage = displayMessage == null ? Component.literal(rawMessage) : displayMessage;
		this.type = type;
		this.sender = sender;
		this.content = content;
		this.whisperPartner = whisperPartner == null ? "" : whisperPartner;
		this.outgoing = outgoing;
		this.styleTrace = styleTrace == null ? "" : styleTrace;
		this.timestamp = timestamp;
	}
	
	public String getRawMessage() {
		return rawMessage;
	}

	public Component getDisplayMessage() {
		return displayMessage;
	}

	public ChatMessage withDisplayMessage(Component displayMessage) {
		return new ChatMessage(rawMessage, displayMessage, type, sender, content, outgoing, styleTrace, whisperPartner, timestamp);
	}
	
	public ChatTabType getType() {
		return type;
	}
	
	public String getSender() {
		return sender;
	}
	
	public String getContent() {
		return content;
	}

	public String getWhisperPartner() {
		return whisperPartner;
	}

	public boolean isOutgoing() {
		return outgoing;
	}

	public String getStyleTrace() {
		return styleTrace;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public String toString() {
		return String.format("[%s] %s: %s", type.getDisplayName(), sender, content);
	}
}
