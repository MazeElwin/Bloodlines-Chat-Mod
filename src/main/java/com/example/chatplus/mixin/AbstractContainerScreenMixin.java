package com.example.chatplus.mixin;

import com.example.chatplus.chat.SkillProgressTracker;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
	@Shadow
	protected AbstractContainerMenu menu;

	protected AbstractContainerScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "slotClicked", at = @At("HEAD"))
	private void chatplus$scheduleSkillCheckAfterCraft(Slot slot, int slotId, int mouseButton, ContainerInput type, CallbackInfo ci) {
		SkillProgressTracker.scheduleContainerAction(this, menu, slot, type);
	}
}
