package com.witch.module;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Poe a hora [HH:mm] antes de cada mensagem do chat (o trabalho e feito pelo ChatComponentMixin). */
public class ChatTimestampsModule extends Module {
	private static ChatTimestampsModule instance;
	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

	public ChatTimestampsModule() {
		super("Chat Timestamps", Category.UTILITY);
		instance = this;
	}

	public static Component stamp(Component original) {
		if (instance == null || !instance.isEnabled()) return original;
		return Component.empty()
				.append(Component.literal("[" + LocalTime.now().format(FMT) + "] ").withStyle(ChatFormatting.GRAY))
				.append(original);
	}
}
