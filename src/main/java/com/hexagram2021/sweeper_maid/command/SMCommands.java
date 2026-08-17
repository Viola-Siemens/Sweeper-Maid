package com.hexagram2021.sweeper_maid.command;

import com.hexagram2021.sweeper_maid.SweeperMaid;
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

/**
 * 清洁女仆模组命令注册类喵~
 * <p>
 * 提供以下命令：
 * <ul>
 *     <li>{@code /sweepermaid dustbin [index]} - 打开指定索引的垃圾箱界面喵~</li>
 *     <li>{@code /sweepermaid dustbin clear <index>} - 清空指定索引的垃圾箱喵~</li>
 *     <li>{@code /sweepermaid clean} - 立即执行清理操作喵~</li>
 * </ul>
 * </p>
 *
 * @author liudongyu
 */
public final class SMCommands {
	/**
	 * 注册清洁女仆命令喵~
	 *
	 * @return 命令构建器喵~
	 */
	public static LiteralArgumentBuilder<CommandSourceStack> register() {
		return Commands.literal("sweepermaid").then(
				Commands.literal("dustbin").requires(stack -> stack.hasPermission(SMCommonConfig.PERMISSION_LEVEL_DUSTBIN.get()))
						.executes(context -> dustbin(context.getSource().getPlayer(), 0)) // default the first dustbin
						.then(
								Commands.literal("clear")
										.requires(stack -> stack.hasPermission(SMCommonConfig.PERMISSION_LEVEL_CLEAN.get()))
										.then(
												Commands.argument("index", IntegerArgumentType.integer(0))
														.executes(context -> clearDustbin(
																context.getSource(),
																IntegerArgumentType.getInteger(context, "index")
														))
										)
						)
						.then(
								Commands.argument("index", IntegerArgumentType.integer(0))
										.executes(context -> dustbin(context.getSource().getPlayer(), IntegerArgumentType.getInteger(context, "index")))
						)
		).then(
				Commands.literal("clean").requires(stack -> stack.hasPermission(SMCommonConfig.PERMISSION_LEVEL_CLEAN.get()))
						.executes(context -> clean())
		);
	}

	/**
	 * 打开垃圾箱界面喵~
	 * <p>
	 * 根据索引打开对应的垃圾箱容器界面，如果索引无效则发送错误消息喵~
	 * </p>
	 *
	 * @param player 玩家实例喵~
	 * @param index 垃圾箱索引喵~
	 * @return 命令执行结果码喵~
	 */
	private static int dustbin(@Nullable ServerPlayer player, int index) {
		if (player == null) {
			return 0;
		}

		if (index >= 0 && index < SMCommonConfig.DUSTBIN_COUNT.get()) {
			player.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal(SMCommonConfig.DUSTBIN_NAME.get() + index);
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player1) {
					return ChestMenu.sixRows(id, inventory, SMSavedData.getDustbinContainer(index));
				}
			});
			return 1;
		}
		player.sendSystemMessage(Component.literal(SMCommonConfig.MESSAGE_WRONG_DUSTBIN.get()));
		return 0;
	}

	/**
	 * 清空指定索引的垃圾箱喵~
	 * <p>
	 * 清空操作需要与立即清理相同的权限等级。执行成功后会反馈被清除的物品总数量；
	 * 若垃圾箱索引无效，则向命令执行者发送错误提示喵~
	 * </p>
	 *
	 * @param source 命令源喵~
	 * @param index 垃圾箱索引喵~
	 * @return 命令执行结果码喵~
	 */
	private static int clearDustbin(CommandSourceStack source, int index) {
		if (index >= 0 && index < SMCommonConfig.DUSTBIN_COUNT.get()) {
			int itemCount = SMSavedData.clearDustbin(index);
			source.sendSuccess(() -> Component.literal(
					SMCommonConfig.MESSAGE_AFTER_CLEAR_DUSTBIN.get()
							.replace("$1", String.valueOf(index))
							.replace("$2", String.valueOf(itemCount))
			), true);
			return 1;
		}
		source.sendFailure(Component.literal(SMCommonConfig.MESSAGE_WRONG_DUSTBIN.get()));
		return 0;
	}

	/**
	 * 立即执行清理操作喵~
	 * <p>
	 * 触发清洁女仆的清理回调函数，重置清理倒计时喵~
	 * </p>
	 *
	 * @return 命令执行结果码喵~
	 */
	private static int clean() {
		SweeperMaid.clean.run();
		return 1;
	}

	/**
	 * 私有构造方法，防止实例化喵~
	 */
	private SMCommands() {
	}
}
