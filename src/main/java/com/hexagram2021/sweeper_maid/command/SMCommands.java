package com.hexagram2021.sweeper_maid.command;

import com.hexagram2021.sweeper_maid.config.SMCommonConfig;
import com.hexagram2021.sweeper_maid.save.SMSavedData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;

import javax.annotation.Nullable;

public class SMCommands {
	public static LiteralArgumentBuilder<CommandSourceStack> register() {
		return Commands.literal("sweepermaid").then(
				Commands.literal("dustbin").requires(stack -> stack.hasPermission(SMCommonConfig.PERMISSION_LEVEL_DUSTBIN.get()))
						.executes(context -> dustbin(context.getSource().getPlayer(), 0)) // default the first dustbin
		).then(
				Commands.literal("dustbin").then(
						Commands.argument("index", IntegerArgumentType.integer(0))
								.executes(context -> dustbin(context.getSource().getPlayer(), IntegerArgumentType.getInteger(context, "index")))
				)
		);
	}

	private static int dustbin(@Nullable ServerPlayer player, int index) {
		if (player == null) {
			return 0;
		}

        if (index >= 0 && index < SMCommonConfig.DUSTBIN_COUNT.get()) {
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Dustbin");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player1) {
                    return ChestMenu.sixRows(id, inventory, SMSavedData.getDustbinContainer(index));
                }
            });
            return 1;
        } else {
            player.sendSystemMessage(Component.literal(SMCommonConfig.MESSAGE_WRONG_DUSTBIN.get()));
            return 0;
        }
    }
}
