package com.example.chatplus.chat;

import com.example.chatplus.ChatPlusMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillProgressTracker {
	private enum SkillAction {
		CRAFTING("Crafting"),
		ENCHANTING("Enchanting");

		private final String skillName;

		SkillAction(String skillName) {
			this.skillName = skillName;
		}
	}

	private static final Pattern SAME_LINE_SKILL_PATTERN = Pattern.compile(
			"(?i)\\b(craft(?:ing)?|enchant(?:ing)?)\\b.*?\\b(?:level|lvl|lv)\\s*[:=-]?\\s*([ivxlcdm]+|\\d+).*?(\\d+(?:\\.\\d+)?)\\s*%");
	private static final Pattern SAME_LINE_PERCENT_FIRST_PATTERN = Pattern.compile(
			"(?i)\\b(craft(?:ing)?|enchant(?:ing)?)\\b.*?(\\d+(?:\\.\\d+)?)\\s*%.*?\\b(?:level|lvl|lv)\\s*[:=-]?\\s*([ivxlcdm]+|\\d+)");
	private static final Pattern COMPACT_SKILL_PATTERN = Pattern.compile(
			"(?i)\\b(craft(?:ing)?|enchant(?:ing)?)\\b\\s*[:=-]\\s*([ivxlcdm]+|\\d+)\\s*[,/| -]+\\s*(\\d+(?:\\.\\d+)?)\\s*%");
	private static final Pattern SKILLS_PLAINTEXT_HEADER_PATTERN = Pattern.compile("(?i)^\\s*skills\\s+in\\s+plaintext\\s*$");
	private static final Pattern SKILLS_PLAINTEXT_SEPARATOR_PATTERN = Pattern.compile("^\\s*-{3,}\\s*$");
	private static final Pattern SKILLS_PLAINTEXT_UNKNOWN_PATTERN = Pattern.compile("^\\s*\\?{3,}\\s*$");
	private static final Pattern ANY_SKILL_PLAINTEXT_LINE_PATTERN = Pattern.compile(
			"(?i)^\\s*[\\p{L}][\\p{L} '\\-/]*:\\s*(?:[ivxlcdm]+|\\d+)\\s*\\|\\s*\\d+(?:\\.\\d+)?\\s*%\\s*$");
	private static final long AUTO_CHECK_DELAY_MILLIS = 900L;
	private static final long AUTO_CHECK_TIMEOUT_MILLIS = 5000L;
	private static final long TOOL_BREAK_CONFIRM_DELAY_MILLIS = 150L;
	private static final long ITEM_FRAME_PLACEMENT_SUPPRESS_MILLIS = 3000L;
	private static final long SKILLS_PLAINTEXT_RESPONSE_MILLIS = 8000L;
	private static final ZoneId EASTERN_TIME = ZoneId.of("America/New_York");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path STORE_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(ChatPlusMod.MOD_ID)
			.resolve("skill-tracker.json");

	private static SkillStore store;
	private static long autoCheckAtMillis;
	private static long autoCheckTimeoutAtMillis;
	private static boolean waitingForSkills;
	private static PendingAction pendingAction;
	private static int scheduledContainerId = -1;
	private static long skillsPlaintextResponseUntilMillis;
	private static ItemStack suppressedItemFrameStack = ItemStack.EMPTY;
	private static long suppressItemFramePlacementUntilMillis;
	private static final Queue<PendingToolBreak> pendingToolBreaks = new ArrayDeque<>();
	private static final Queue<SkillSnapshot> recentSnapshots = new ArrayDeque<>();
	private static String popupMessage = "";
	private static long popupUntilMillis;

	private SkillProgressTracker() {
	}

	public static void handleIncomingMessage(Component message) {
		if (message == null) {
			return;
		}

		for (SkillSnapshot snapshot : parseSnapshots(message.getString())) {
			recordSnapshot(snapshot);
		}
	}

	public static boolean isSkillPlaintextMessage(String message) {
		if (message == null || message.isBlank()) {
			return false;
		}

		String trimmed = message.trim();
		if (SKILLS_PLAINTEXT_HEADER_PATTERN.matcher(trimmed).matches()
				|| SKILLS_PLAINTEXT_SEPARATOR_PATTERN.matcher(trimmed).matches()
				|| ANY_SKILL_PLAINTEXT_LINE_PATTERN.matcher(trimmed).matches()) {
			markSkillsPlaintextResponseActive();
			return true;
		}

		if (SKILLS_PLAINTEXT_UNKNOWN_PATTERN.matcher(trimmed).matches()
				&& System.currentTimeMillis() <= skillsPlaintextResponseUntilMillis) {
			return true;
		}

		return !parseSnapshots(message).isEmpty();
	}

	public static void scheduleContainerAction(Screen screen, AbstractContainerMenu menu, Slot slot, ContainerInput type) {
		if (slot == null || menu == null || (type != ContainerInput.PICKUP && type != ContainerInput.QUICK_MOVE)) {
			return;
		}

		SkillAction action = actionFor(screen, menu, slot);
		if (action == null) {
			return;
		}

		ItemStack stack = slot.getItem();
		if (stack.isEmpty()) {
			return;
		}

		pendingAction = new PendingAction(
				action,
				stack.getHoverName().getString(),
				snapshotFor(action.skillName),
				System.currentTimeMillis()
		);
		scheduledContainerId = menu.containerId;
		autoCheckAtMillis = System.currentTimeMillis() + AUTO_CHECK_DELAY_MILLIS;
		autoCheckTimeoutAtMillis = 0L;
		addSkillTrackerMessage(String.format(Locale.ROOT,
				"Skill tracker saw %s: %s",
				action.skillName,
				stack.getHoverName().getString()));
	}

	public static void scheduleEnchantingButtonAction(int containerId, int buttonId) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.player.containerMenu == null
				|| minecraft.player.containerMenu.containerId != containerId
				|| !isEnchantingContainer(minecraft.screen, minecraft.player.containerMenu)) {
			return;
		}

		ItemStack targetItem = findEnchantTarget(minecraft.player.containerMenu);
		if (targetItem.isEmpty()) {
			return;
		}

		pendingAction = new PendingAction(
				SkillAction.ENCHANTING,
				targetItem.getHoverName().getString(),
				snapshotFor(SkillAction.ENCHANTING.skillName),
				System.currentTimeMillis()
		);
		scheduledContainerId = containerId;
		autoCheckAtMillis = System.currentTimeMillis() + AUTO_CHECK_DELAY_MILLIS;
		autoCheckTimeoutAtMillis = 0L;
		addSkillTrackerMessage(String.format(Locale.ROOT,
				"Skill tracker saw Enchanting button %d: %s",
				buttonId,
				targetItem.getHoverName().getString()));
	}

	public static void scheduleToolBreakCraftingAction(ItemStack brokenStack) {
		if (brokenStack == null || brokenStack.isEmpty()) {
			return;
		}

		schedulePotentialToolBreakCraftingAction(brokenStack);
	}

	public static void schedulePotentialToolBreakCraftingAction(ItemStack brokenStack) {
		if (brokenStack == null || brokenStack.isEmpty()) {
			return;
		}
		if (isSuppressedItemFramePlacement(brokenStack)) {
			return;
		}

		pendingToolBreaks.add(new PendingToolBreak(brokenStack.copy(), System.currentTimeMillis() + TOOL_BREAK_CONFIRM_DELAY_MILLIS));
	}

	public static void suppressItemFramePlacement(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}

		suppressedItemFrameStack = stack.copy();
		suppressItemFramePlacementUntilMillis = System.currentTimeMillis() + ITEM_FRAME_PLACEMENT_SUPPRESS_MILLIS;
	}

	private static boolean isSuppressedItemFramePlacement(ItemStack stack) {
		if (System.currentTimeMillis() > suppressItemFramePlacementUntilMillis || !sameStack(stack, suppressedItemFrameStack)) {
			return false;
		}

		suppressedItemFrameStack = ItemStack.EMPTY;
		suppressItemFramePlacementUntilMillis = 0L;
		return true;
	}

	private static void scheduleConfirmedToolBreakCraftingAction(ItemStack brokenStack) {
		pendingAction = new PendingAction(
				SkillAction.CRAFTING,
				brokenStack.getHoverName().getString(),
				snapshotFor(SkillAction.CRAFTING.skillName),
				System.currentTimeMillis()
		);
		scheduledContainerId = -1;
		autoCheckAtMillis = System.currentTimeMillis() + AUTO_CHECK_DELAY_MILLIS;
		autoCheckTimeoutAtMillis = 0L;
		addSkillTrackerMessage(String.format(Locale.ROOT,
				"Skill tracker saw broken tool: %s",
				brokenStack.getHoverName().getString()));
	}

	public static void tickAutoCheck() {
		tickPendingToolBreaks();

		if (pendingAction == null && !waitingForSkills) {
			return;
		}

		long now = System.currentTimeMillis();
		if (waitingForSkills) {
			if (autoCheckTimeoutAtMillis > 0L && now >= autoCheckTimeoutAtMillis) {
				waitingForSkills = false;
				autoCheckTimeoutAtMillis = 0L;
				pendingAction = null;
				scheduledContainerId = -1;
			}
			return;
		}

		if (autoCheckAtMillis <= 0L || now < autoCheckAtMillis) {
			return;
		}

		autoCheckAtMillis = 0L;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.player.connection == null) {
			return;
		}

		waitingForSkills = true;
		autoCheckTimeoutAtMillis = now + AUTO_CHECK_TIMEOUT_MILLIS;
		markSkillsPlaintextResponseActive();
		addSkillTrackerMessage("Skill tracker checking /skills plaintext");
		minecraft.player.connection.sendCommand("skills plaintext");
	}

	private static void markSkillsPlaintextResponseActive() {
		skillsPlaintextResponseUntilMillis = System.currentTimeMillis() + SKILLS_PLAINTEXT_RESPONSE_MILLIS;
	}

	private static void tickPendingToolBreaks() {
		if (pendingToolBreaks.isEmpty()) {
			return;
		}

		long now = System.currentTimeMillis();
		while (!pendingToolBreaks.isEmpty() && pendingToolBreaks.peek().dueAtMillis() <= now) {
			PendingToolBreak pendingBreak = pendingToolBreaks.poll();
			if (!isStackStillInPlayerInventory(pendingBreak.stack())) {
				scheduleConfirmedToolBreakCraftingAction(pendingBreak.stack());
			}
		}
	}

	private static boolean isStackStillInPlayerInventory(ItemStack stack) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || stack == null || stack.isEmpty()) {
			return false;
		}

		for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
			ItemStack currentStack = minecraft.player.getInventory().getItem(slot);
			if (sameStack(currentStack, stack)) {
				return true;
			}
		}

		if (minecraft.player.containerMenu != null && sameStack(minecraft.player.containerMenu.getCarried(), stack)) {
			return true;
		}

		return false;
	}

	private static boolean sameStack(ItemStack first, ItemStack second) {
		return first != null
				&& second != null
				&& !first.isEmpty()
				&& !second.isEmpty()
				&& first.getCount() == second.getCount()
				&& ItemStack.isSameItemSameComponents(first, second);
	}

	private static void recordSnapshot(SkillSnapshot snapshot) {
		SkillStore currentStore = store();
		SkillSnapshot previous = currentStore.skills.put(snapshot.name, snapshot);
		currentStore.dailyKey = todayKey();
		recentSnapshots.add(snapshot);
		while (recentSnapshots.size() > 12) {
			recentSnapshots.poll();
		}

		if (waitingForSkills && pendingAction != null && pendingAction.action.skillName.equals(snapshot.name)) {
			recordPendingGain(snapshot);
		} else if (previous == null || Math.abs(snapshot.totalProgress() - previous.totalProgress()) > 0.0001D) {
			save();
		}
	}

	private static void recordPendingGain(SkillSnapshot afterSnapshot) {
		PendingAction action = pendingAction;
		waitingForSkills = false;
		pendingAction = null;
		autoCheckTimeoutAtMillis = 0L;
		scheduledContainerId = -1;

		SkillSnapshot beforeSnapshot = action.beforeSnapshot;
		if (beforeSnapshot == null) {
			save();
			showPopup(String.format(Locale.ROOT,
					"%s baseline",
					action.action.skillName));
			addSkillTrackerMessage(String.format(Locale.ROOT,
					"%s baseline saved at level %d %.2f%%. Craft/enchant once more to measure gain.",
					afterSnapshot.name,
					afterSnapshot.level,
					afterSnapshot.percent));
			return;
		}

		double gain = afterSnapshot.totalProgress() - beforeSnapshot.totalProgress();
		if (gain <= 0.0001D) {
			save();
			showPopup(String.format(Locale.ROOT,
					"%s no XP: %s",
					action.action.skillName,
					shortItemName(action.itemName)));
			addSkillTrackerMessage(String.format(Locale.ROOT,
					"%s showed no visible EXP gain from %s. Level %d %.2f%%",
					afterSnapshot.name,
					action.itemName,
					afterSnapshot.level,
					afterSnapshot.percent));
			return;
		}

		SkillStore currentStore = store();
		String day = todayKey();
		if (!day.equals(currentStore.dailyKey)) {
			currentStore.dailyKey = day;
			currentStore.dailyItems.clear();
		}

		String itemKey = dailyItemKey(afterSnapshot.name, action.itemName);
		DailyItemStats dailyStats = currentStore.dailyItems.computeIfAbsent(itemKey, ignored -> new DailyItemStats());
		dailyStats.itemName = action.itemName;
		dailyStats.skillName = afterSnapshot.name;
		dailyStats.count++;
		dailyStats.totalGain += gain;
		dailyStats.lastGain = gain;
		dailyStats.updatedAt = Instant.now().toString();

		SkillEvent event = new SkillEvent();
		event.timestamp = Instant.now().toString();
		event.day = day;
		event.skillName = afterSnapshot.name;
		event.itemName = action.itemName;
		event.gain = gain;
		event.levelBefore = beforeSnapshot.level;
		event.percentBefore = beforeSnapshot.percent;
		event.levelAfter = afterSnapshot.level;
		event.percentAfter = afterSnapshot.percent;
		event.dailyItemCrafts = dailyStats.count;
		currentStore.events.add(event);
		while (currentStore.events.size() > 1000) {
			currentStore.events.remove(0);
		}

		save();
		showPopup(String.format(Locale.ROOT,
				"%s +%.2f%%: %s",
				action.action.skillName,
				gain,
				shortItemName(action.itemName)));
		addSkillTrackerMessage(String.format(Locale.ROOT,
				"%s +%.2f%% from %s. Level %d %.2f%%. Today: %dx, avg %.2f%%",
				afterSnapshot.name,
				gain,
				action.itemName,
				afterSnapshot.level,
				afterSnapshot.percent,
				dailyStats.count,
				dailyStats.totalGain / Math.max(1, dailyStats.count)));
	}

	private static void addSkillTrackerMessage(String message) {
		ChatTabManager.getInstance().addLocalSystemMessage(message, ChatPlusConfig.get().skillTrackerMessagesInAll);
	}

	private static void showPopup(String message) {
		if (message == null || message.isBlank()) {
			return;
		}

		popupMessage = message;
		popupUntilMillis = System.currentTimeMillis() + 3500L;
	}

	public static void renderPopup(GuiGraphicsExtractor graphics, Font font, int screenWidth) {
		if (popupMessage.isBlank() || System.currentTimeMillis() >= popupUntilMillis) {
			return;
		}

		int popupWidth = Math.min(screenWidth - 24, font.width(popupMessage) + 18);
		int x = (screenWidth - popupWidth) / 2;
		int y = 12;
		int textX = x + (popupWidth - font.width(popupMessage)) / 2;
		graphics.nextStratum();
		graphics.fill(x, y, x + popupWidth, y + 18, 0xDD111111);
		graphics.outline(x, y, popupWidth, 18, 0xCC55FF55);
		graphics.text(font, Component.literal(popupMessage), textX, y + 5, 0xFFFFFFFF, false);
	}

	private static String shortItemName(String itemName) {
		if (itemName == null) {
			return "";
		}

		String trimmed = itemName.trim();
		return trimmed.length() <= 24 ? trimmed : trimmed.substring(0, 23) + "...";
	}

	private static SkillSnapshot snapshotFor(String skillName) {
		SkillSnapshot stored = store().skills.get(skillName);
		if (stored != null) {
			return stored;
		}
		for (SkillSnapshot snapshot : recentSnapshots) {
			if (snapshot.name.equals(skillName)) {
				return snapshot;
			}
		}
		return null;
	}

	private static SkillAction actionFor(Screen screen, AbstractContainerMenu menu, Slot slot) {
		if (slot instanceof ResultSlot && isCraftingContainer(screen, menu)) {
			return SkillAction.CRAFTING;
		}
		return null;
	}

	private static boolean isEnchantingContainer(Screen screen, AbstractContainerMenu menu) {
		String title = screen == null ? "" : screen.getTitle().getString().toLowerCase(Locale.ROOT);
		if (title.contains("enchant")) {
			return true;
		}

		String menuName = menu == null ? "" : menu.getClass().getSimpleName().toLowerCase(Locale.ROOT);
		return menuName.contains("enchantmentmenu");
	}

	private static ItemStack findEnchantTarget(AbstractContainerMenu menu) {
		for (Slot slot : menu.slots) {
			ItemStack stack = slot.getItem();
			if (!stack.isEmpty() && !isLapis(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean isLapis(ItemStack stack) {
		String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
		return itemId.equals("minecraft:lapis_lazuli");
	}

	private static boolean isCraftingContainer(Screen screen, AbstractContainerMenu menu) {
		String title = screen == null ? "" : screen.getTitle().getString().toLowerCase(Locale.ROOT);
		if (title.contains("craft") || title.contains("workbench")) {
			return true;
		}

		String menuName = menu == null ? "" : menu.getClass().getSimpleName().toLowerCase(Locale.ROOT);
		return menuName.contains("craftingmenu") || menuName.contains("inventorymenu");
	}

	private static List<SkillSnapshot> parseSnapshots(String text) {
		List<SkillSnapshot> snapshots = new ArrayList<>();
		if (text == null || text.isBlank()) {
			return snapshots;
		}

		for (String line : text.split("\\R")) {
			parseLine(line, snapshots);
		}
		return snapshots;
	}

	private static void parseLine(String line, List<SkillSnapshot> snapshots) {
		addMatch(line, SAME_LINE_SKILL_PATTERN.matcher(line), snapshots, false);
		addMatch(line, SAME_LINE_PERCENT_FIRST_PATTERN.matcher(line), snapshots, true);
		addMatch(line, COMPACT_SKILL_PATTERN.matcher(line), snapshots, false);
	}

	private static void addMatch(String line, Matcher matcher, List<SkillSnapshot> snapshots, boolean percentBeforeLevel) {
		while (matcher.find()) {
			String skillName = normalizeSkillName(matcher.group(1));
			if (skillName.isBlank()) {
				continue;
			}

			try {
				int level = parseLevel(percentBeforeLevel ? matcher.group(3) : matcher.group(2));
				double percent = Double.parseDouble(percentBeforeLevel ? matcher.group(2) : matcher.group(3));
				snapshots.add(new SkillSnapshot(skillName, level, percent, Instant.now().toString(), line));
			} catch (IllegalArgumentException ignored) {
			}
		}
	}

	private static int parseLevel(String rawLevel) {
		if (rawLevel == null || rawLevel.isBlank()) {
			throw new IllegalArgumentException("missing level");
		}

		String level = rawLevel.trim().toUpperCase(Locale.ROOT);
		if (level.chars().allMatch(Character::isDigit)) {
			return Integer.parseInt(level);
		}

		int total = 0;
		int previous = 0;
		for (int index = level.length() - 1; index >= 0; index--) {
			int value = romanValue(level.charAt(index));
			if (value <= 0) {
				throw new IllegalArgumentException("invalid roman level");
			}
			if (value < previous) {
				total -= value;
			} else {
				total += value;
				previous = value;
			}
		}
		return total;
	}

	private static int romanValue(char character) {
		return switch (character) {
			case 'I' -> 1;
			case 'V' -> 5;
			case 'X' -> 10;
			case 'L' -> 50;
			case 'C' -> 100;
			case 'D' -> 500;
			case 'M' -> 1000;
			default -> 0;
		};
	}

	private static String normalizeSkillName(String rawSkill) {
		String lower = rawSkill.toLowerCase(Locale.ROOT);
		if (lower.startsWith("craft")) {
			return "Crafting";
		}
		if (lower.startsWith("enchant")) {
			return "Enchanting";
		}
		return "";
	}

	private static String dailyItemKey(String skillName, String itemName) {
		return skillName.toLowerCase(Locale.ROOT) + ":" + itemName.toLowerCase(Locale.ROOT);
	}

	private static SkillStore store() {
		if (store != null) {
			rollDailyStoreIfNeeded(store);
			return store;
		}

		if (!Files.exists(STORE_PATH)) {
			store = new SkillStore();
			store.dailyKey = todayKey();
			return store;
		}

		try (Reader reader = Files.newBufferedReader(STORE_PATH, StandardCharsets.UTF_8)) {
			store = GSON.fromJson(reader, SkillStore.class);
			if (store == null) {
				store = new SkillStore();
			}
		} catch (IOException | JsonParseException exception) {
			ChatPlusMod.LOGGER.warn("Failed to read skill tracker store", exception);
			store = new SkillStore();
		}

		store.sanitize();
		rollDailyStoreIfNeeded(store);
		return store;
	}

	private static void rollDailyStoreIfNeeded(SkillStore currentStore) {
		String today = todayKey();
		if (currentStore.dailyKey == null || currentStore.dailyKey.isBlank()) {
			currentStore.dailyKey = today;
		} else if (!today.equals(currentStore.dailyKey)) {
			currentStore.dailyKey = today;
			currentStore.dailyItems.clear();
			save();
		}
	}

	private static void save() {
		try {
			Files.createDirectories(STORE_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(STORE_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(store == null ? new SkillStore() : store, writer);
			}
		} catch (IOException exception) {
			ChatPlusMod.LOGGER.warn("Failed to write skill tracker store", exception);
		}
	}

	private static String todayKey() {
		return LocalDate.now(EASTERN_TIME).toString();
	}

	private record PendingAction(SkillAction action, String itemName, SkillSnapshot beforeSnapshot, long timestamp) {
	}

	private record PendingToolBreak(ItemStack stack, long dueAtMillis) {
	}

	private record SkillSnapshot(String name, int level, double percent, String updatedAt, String sourceLine) {
		private double totalProgress() {
			return level * 100.0D + percent;
		}
	}

	private static final class SkillStore {
		private Map<String, SkillSnapshot> skills = new LinkedHashMap<>();
		private List<SkillEvent> events = new ArrayList<>();
		private Map<String, DailyItemStats> dailyItems = new LinkedHashMap<>();
		private String dailyKey = "";

		private void sanitize() {
			if (skills == null) {
				skills = new LinkedHashMap<>();
			}
			if (events == null) {
				events = new ArrayList<>();
			}
			if (dailyItems == null) {
				dailyItems = new LinkedHashMap<>();
			}
			if (dailyKey == null) {
				dailyKey = "";
			}
			rebuildDailyItemsFromEvents();
		}

		private void rebuildDailyItemsFromEvents() {
			if (dailyKey.isBlank() || events.isEmpty()) {
				return;
			}

			Map<String, DailyItemStats> rebuiltDailyItems = new LinkedHashMap<>();
			for (SkillEvent event : events) {
				if (event == null || !dailyKey.equals(event.day)) {
					continue;
				}

				String itemKey = dailyItemKey(event.skillName, event.itemName);
				DailyItemStats dailyStats = rebuiltDailyItems.computeIfAbsent(itemKey, ignored -> new DailyItemStats());
				dailyStats.itemName = event.itemName;
				dailyStats.skillName = event.skillName;
				dailyStats.count++;
				dailyStats.totalGain += event.gain;
				dailyStats.lastGain = event.gain;
				dailyStats.updatedAt = event.timestamp;
			}

			dailyItems = rebuiltDailyItems;
		}
	}

	private static final class SkillEvent {
		private String timestamp = "";
		private String day = "";
		private String skillName = "";
		private String itemName = "";
		private double gain;
		private int levelBefore;
		private double percentBefore;
		private int levelAfter;
		private double percentAfter;
		private int dailyItemCrafts;
	}

	private static final class DailyItemStats {
		private String itemName = "";
		private String skillName = "";
		private int count;
		private double totalGain;
		private double lastGain;
		private String updatedAt = "";
	}
}
