package com.example.chatplus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.KeyboardHandler.class)
public class KeyboardHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void chatplus$closeChatScreenOnEscape(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (minecraft.screen == null && action == 1 && (event.key() == 257 || event.key() == 335 || event.input() == 257 || event.input() == 335)) {
			minecraft.setScreen(new ChatScreen("", false));
			ci.cancel();
			return;
		}

		if (minecraft.screen instanceof ChatScreen
				&& action == 1
				&& (event.key() == 256 || event.input() == 256)) {
			minecraft.screen.onClose();
			ci.cancel();
		}
	}
}
