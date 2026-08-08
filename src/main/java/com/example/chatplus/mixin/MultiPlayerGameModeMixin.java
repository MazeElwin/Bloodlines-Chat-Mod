package com.example.chatplus.mixin;

import com.example.chatplus.chat.SkillProgressTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "handleInventoryButtonClick", at = @At("HEAD"))
	private void chatplus$trackEnchantButton(int containerId, int buttonId, CallbackInfo ci) {
		SkillProgressTracker.scheduleEnchantingButtonAction(containerId, buttonId);
	}

	@Inject(method = "interact", at = @At("HEAD"))
	private void chatplus$suppressToolBreakAfterItemFramePlacement(Player player, Entity entity, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir) {
		chatplus$suppressIfPlacingInItemFrame(player, entity, hand);
	}

	private static void chatplus$suppressIfPlacingInItemFrame(Player player, Entity entity, InteractionHand hand) {
		if (!(entity instanceof ItemFrame itemFrame) || player == null || hand == null || !itemFrame.getItem().isEmpty()) {
			return;
		}

		ItemStack stack = player.getItemInHand(hand);
		if (!stack.isEmpty() && stack.isDamageableItem() && stack.getCount() == 1) {
			SkillProgressTracker.suppressItemFramePlacement(stack);
		}
	}
}
