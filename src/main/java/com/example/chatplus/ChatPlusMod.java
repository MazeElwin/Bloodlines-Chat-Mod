package com.example.chatplus;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatPlusMod implements ModInitializer {
	public static final String MOD_ID = "bloodline-chat";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Bloodline Chat mod initialized!");
	}
}
