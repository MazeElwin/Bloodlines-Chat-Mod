package com.example.chatplus.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.InBedChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InBedChatScreen.class)
public interface InBedChatScreenAccessor {
	@Accessor("leaveBedButton")
	Button chatplus$getLeaveBedButton();
}
