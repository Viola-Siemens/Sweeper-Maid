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
						.executes(context -> dustbin(context.getSource().getPlayer(), 0)) // 默认展示第一个垃圾桶
		).then(
				Commands.literal("dustbin").then(
						Commands.argument("index", IntegerArgumentType.integer(0)) // 可以选择查看第几个垃圾桶
								.executes(context -> dustbin(context.getSource().getPlayer(), IntegerArgumentType.getInteger(context, "index")))
				)
		);
	}

	private static int dustbin(@Nullable ServerPlayer player, int index) {
		if (player == null) {
			return 0;
		}

		// 获取垃圾桶列表并判断 index 是否有效
        if (index >= 0 && index < SMSavedData.getDustbins().size()) {
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Dustbin");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player1) {
// 打开第 index 个垃圾桶
                    return ChestMenu.sixRows(id, inventory, SMSavedData.getDustbins().get(index));
                }
            });
            return 1;
        } else {
            player.sendSystemMessage(Component.literal(SMCommonConfig.MESSAGE_WRONG_DUSTBIN.get()));
            return 0;
        }
    }
}
