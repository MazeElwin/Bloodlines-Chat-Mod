package com.example.chatplus.chat;

public final class ChatWindowState {
	private static final int DEFAULT_WIDTH = 520;
	private static final int DEFAULT_HEIGHT = 170;
	private static final int MIN_WIDTH = 150;
	private static final int MIN_HEIGHT = 70;
	private static int x = ChatPlusConfig.get().windowX;
	private static int y = ChatPlusConfig.get().windowY;
	private static int width = ChatPlusConfig.get().windowWidth;
	private static int height = ChatPlusConfig.get().windowHeight;

	private ChatWindowState() {
	}

	public static int getX() {
		return x;
	}

	public static int getY(int screenHeight) {
		if (y < 0) {
			y = Math.max(4, screenHeight - DEFAULT_HEIGHT - 18);
		}
		return y;
	}

	public static int getWidth(int screenWidth) {
		return Math.min(width, Math.max(MIN_WIDTH, screenWidth - 8));
	}

	public static int getHeight(int screenHeight) {
		return Math.min(height, Math.max(MIN_HEIGHT, screenHeight - 8));
	}

	public static void moveTo(int nextX, int nextY, int screenWidth, int screenHeight) {
		int clampedWidth = getWidth(screenWidth);
		int clampedHeight = getHeight(screenHeight);
		x = clamp(nextX, 0, Math.max(0, screenWidth - clampedWidth));
		y = clamp(nextY, 0, Math.max(0, screenHeight - clampedHeight));
	}

	public static void clampToScreen(int screenWidth, int screenHeight) {
		moveTo(x, getY(screenHeight), screenWidth, screenHeight);
	}

	public static void resizeTo(int nextWidth, int nextHeight, int screenWidth, int screenHeight) {
		width = clamp(nextWidth, MIN_WIDTH, Math.max(MIN_WIDTH, screenWidth - x));
		height = clamp(nextHeight, MIN_HEIGHT, Math.max(MIN_HEIGHT, screenHeight - getY(screenHeight)));
	}

	public static void savePosition() {
		ChatPlusConfig.setWindowPosition(x, y);
	}

	public static void saveSize() {
		ChatPlusConfig.setWindowSize(width, height);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
