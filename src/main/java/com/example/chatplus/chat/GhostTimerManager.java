package com.example.chatplus.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.Queue;

public final class GhostTimerManager {
	public enum SubmitResult {
		SENT,
		QUEUED,
		BLOCKED
	}

	private record PendingMessage(String message, boolean addToRecentChat) {
	}

	private static final long DELAY_MS = 5100L;
	private static final Queue<PendingMessage> PENDING_MESSAGES = new ArrayDeque<>();
	private static long nextSendAt;
	private static ClientPacketListener activeConnection;

	private GhostTimerManager() {
	}

	public static SubmitResult submit(int mode, String message, boolean addToRecentChat, Runnable immediateSender) {
		if (message == null || message.trim().isEmpty()) {
			return SubmitResult.BLOCKED;
		}

		long now = System.currentTimeMillis();
		if (now >= nextSendAt && PENDING_MESSAGES.isEmpty()) {
			activeConnection = Minecraft.getInstance().getConnection();
			immediateSender.run();
			nextSendAt = now + DELAY_MS;
			return SubmitResult.SENT;
		}

		if (mode == 1) {
			activeConnection = Minecraft.getInstance().getConnection();
			PENDING_MESSAGES.add(new PendingMessage(message, addToRecentChat));
			return SubmitResult.QUEUED;
		}

		return SubmitResult.BLOCKED;
	}

	public static void tick() {
		if (nextSendAt <= 0L && PENDING_MESSAGES.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getConnection() == null || minecraft.getConnection() != activeConnection || minecraft.player == null) {
			clear();
			return;
		}

		if (PENDING_MESSAGES.isEmpty() || System.currentTimeMillis() < nextSendAt) {
			return;
		}

		if (ChatPlusConfig.get().ghostTimerMode != 1) {
			clear();
			return;
		}

		PendingMessage pending = PENDING_MESSAGES.poll();
		sendNow(minecraft, pending);
		nextSendAt = System.currentTimeMillis() + DELAY_MS;
	}

	public static void clear() {
		PENDING_MESSAGES.clear();
		nextSendAt = 0L;
		activeConnection = null;
	}

	public static int queuedCount() {
		return PENDING_MESSAGES.size();
	}

	public static long remainingMillis() {
		return Math.max(0L, nextSendAt - System.currentTimeMillis());
	}

	public static boolean isCoolingDown() {
		return remainingMillis() > 0L;
	}

	private static void sendNow(Minecraft minecraft, PendingMessage pending) {
		String message = StringUtil.filterText(StringUtils.normalizeSpace(pending.message().trim()));
		if (message.isEmpty()) {
			return;
		}

		if (pending.addToRecentChat()) {
			minecraft.gui.getChat().addRecentChat(message);
		}
		if (message.startsWith("/")) {
			minecraft.getConnection().sendCommand(message.substring(1));
		} else {
			minecraft.getConnection().sendChat(message);
		}
	}
}
