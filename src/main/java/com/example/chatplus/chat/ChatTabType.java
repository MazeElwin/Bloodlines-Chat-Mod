package com.example.chatplus.chat;

/**
 * Enumeration of different chat tab types
 */
public enum ChatTabType {
	GENERAL("General", "All chat"),
	LOCAL_CHAT("Local", "/lc - Local chat only"),
	TEAM_CHAT("Team", "Team chat"),
	WHISPER("Whisper", "/w - Direct messages"),
	SYSTEM("System", "System messages and alerts");
	
	private final String displayName;
	private final String description;
	
	ChatTabType(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public String getDescription() {
		return description;
	}
}
