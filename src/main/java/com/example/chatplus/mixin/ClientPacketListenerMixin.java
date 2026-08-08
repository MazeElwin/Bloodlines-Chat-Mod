package com.example.chatplus.mixin;

import com.example.chatplus.chat.SkillProgressTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.multiplayer.ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(
			method = "handleContainerSetSlot",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/Minecraft;getTutorial()Lnet/minecraft/client/tutorial/Tutorial;",
					shift = At.Shift.BEFORE
			)
	)
	private void chatplus$scheduleCraftingCheckAfterToolBreak(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
		if (packet.getContainerId() != 0 || !packet.getItem().isEmpty()) {
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || packet.getSlot() < 0 || packet.getSlot() >= player.inventoryMenu.slots.size()) {
			return;
		}

		ItemStack previousStack = player.inventoryMenu.getSlot(packet.getSlot()).getItem();
		scheduleIfBrokenTool(previousStack);
	}

	@Inject(
			method = "handleSetPlayerInventory",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/Minecraft;getTutorial()Lnet/minecraft/client/tutorial/Tutorial;",
					shift = At.Shift.BEFORE
			)
	)
	private void chatplus$scheduleCraftingCheckAfterPlayerInventoryToolBreak(ClientboundSetPlayerInventoryPacket packet,
			CallbackInfo ci) {
		if (!packet.contents().isEmpty()) {
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || packet.slot() < 0 || packet.slot() >= player.getInventory().getContainerSize()) {
			return;
		}

		ItemStack previousStack = player.getInventory().getItem(packet.slot());
		scheduleIfBrokenTool(previousStack);
	}

	private static void scheduleIfBrokenTool(ItemStack previousStack) {
		if (previousStack.isEmpty() || !previousStack.isDamageableItem() || previousStack.getCount() != 1) {
			return;
		}

		SkillProgressTracker.schedulePotentialToolBreakCraftingAction(previousStack.copy());
	}
}
