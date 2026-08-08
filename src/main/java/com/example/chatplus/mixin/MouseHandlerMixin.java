package com.example.chatplus.mixin;

import com.example.chatplus.chat.ChatPlusConfig;
import com.example.chatplus.chat.ChatTabManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.MouseHandler.class)
public abstract class MouseHandlerMixin {
	private static final int PRIVATE_INDICATOR_HEIGHT = 14;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	public abstract double getScaledXPos(com.mojang.blaze3d.platform.Window window);

	@Shadow
	public abstract double getScaledYPos(com.mojang.blaze3d.platform.Window window);

	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void chatplus$openUnreadPrivateMessage(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
		if (minecraft.screen != null || action != 1 || buttonInfo.button() != 0 || !ChatTabManager.getInstance().hasUnreadWhispers()) {
			return;
		}

		int width = minecraft.font.width("PM") + 10;
		ChatPlusConfig.Settings settings = ChatPlusConfig.get();
		int x = Math.max(0, Math.min(settings.privateIndicatorX, minecraft.getWindow().getGuiScaledWidth() - width));
		int y = Math.max(0, Math.min(settings.privateIndicatorY, minecraft.getWindow().getGuiScaledHeight() - PRIVATE_INDICATOR_HEIGHT));
		double mouseX = getScaledXPos(minecraft.getWindow());
		double mouseY = getScaledYPos(minecraft.getWindow());
		if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + PRIVATE_INDICATOR_HEIGHT) {
			return;
		}

		ChatTabManager.getInstance().requestOpenLatestUnreadWhisper();
		minecraft.setScreen(new ChatScreen("", false));
		ci.cancel();
	}
}
