package com.example.chatplus.chat;

/**
 * Interface for listening to chat tab updates
 */
public interface ChatTabListener {
	void onMessageReceived(ChatMessage message);
}
