package com.hexagram2021.sweeper_maid.command;

import com.hexagram2021.sweeper_maid.SweeperMaid;
import com.hexagram2021.sweeper_maid.config.SMCommonConfig;
import com.hexagram2021.sweeper_maid.save.SMSavedData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * 扫帚女仆模组命令注册类喵~
 * <p>
 * 提供以下命令：
 * <ul>
 *     <li>{@code /sweepermaid dustbin [index]} - 打开指定索引的垃圾箱界面喵~</li>
 *     <li>{@code /sweepermaid clean} - 立即执行清理操作喵~</li>
 * </ul>
 * </p>
 *
 * @author liudongyu
 */
public final class SMCommands {
	/**
	 * 注册扫帚女仆命令喵~
	 *
	 * @return 命令构建器喵~
	 */
	public static LiteralArgumentBuilder<CommandSourceStack> register() {
		return Commands.literal("sweepermaid").then(
				Commands.literal("dustbin").requires(stack -> stack.hasPermission(SMCommonConfig.PERMISSION_LEVEL_DUSTBIN.get()))
						.executes(context -> dustbin(context.getSource().getPlayer(), 0)) // default the first dustbin
						.then(
								Commands.argument("index", IntegerArgumentType.integer(0))
										.suggests(SMCommands::suggestDustbinIndices)
										.executes(context -> dustbin(context.getSource().getPlayer(), IntegerArgumentType.getInteger(context, "index")))
						)
		).then(
				// 打开回收箱界面。
				Commands.literal("recycle").requires(stack -> stack.hasPermission(SMCommonConfig.PERMISSION_LEVEL_DUSTBIN.get()))
						.executes(context -> recycle(context.getSource().getPlayer()))
		).then(
				Commands.literal("clean").requires(stack -> stack.hasPermission(SMCommonConfig.PERMISSION_LEVEL_CLEAN.get()))
						.executes(context -> clean())
		).then(
				// 手动清空：empty all [force] | empty recycle | empty <index> [force]。
				Commands.literal("empty").requires(stack -> stack.hasPermission(SMCommonConfig.PERMISSION_LEVEL_EMPTY.get()))
						.then(Commands.literal("all")
								.executes(context -> emptyAll(context.getSource(), false))
								.then(Commands.literal("force").executes(context -> emptyAll(context.getSource(), true))))
						.then(Commands.literal("recycle")
								.executes(context -> emptyRecycle(context.getSource())))
						.then(Commands.argument("index", IntegerArgumentType.integer(0))
								.suggests(SMCommands::suggestDustbinIndices)
								.executes(context -> emptyDustbin(context.getSource(), IntegerArgumentType.getInteger(context, "index"), false))
								.then(Commands.literal("force").executes(context -> emptyDustbin(context.getSource(), IntegerArgumentType.getInteger(context, "index"), true))))
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

		if (index >= 0 && index < SMSavedData.totalDustbinCount()) {
			openContainer(player, SMCommonConfig.DUSTBIN_NAME.get() + index, SMSavedData.getDustbinContainer(index));
			return 1;
		}
		player.sendSystemMessage(Component.literal(SMCommonConfig.MESSAGE_WRONG_DUSTBIN.get()));
		return 0;
	}

	// 打开回收箱界面；未启用回收箱时发送错误消息。
	private static int recycle(@Nullable ServerPlayer player) {
		if (player == null) {
			return 0;
		}
		SimpleContainer recycle = SMSavedData.getInstance().getRecycle();
		if (recycle != null) {
			openContainer(player, SMCommonConfig.RECYCLE_NAME.get(), recycle);
			return 1;
		}
		player.sendSystemMessage(Component.literal(SMCommonConfig.MESSAGE_WRONG_DUSTBIN.get()));
		return 0;
	}

	// 为玩家打开一个六行箱子界面，展示给定容器。
	private static void openContainer(ServerPlayer player, String title, Container container) {
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return Component.literal(title);
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player1) {
				return ChestMenu.sixRows(id, inventory, container);
			}
		});
	}

	/**
	 * 立即执行清理操作喵~
	 * <p>
	 * 触发扫帚女仆的清理回调函数，重置清理倒计时喵~
	 * </p>
	 *
	 * @return 命令执行结果码喵~
	 */
	private static int clean() {
		SweeperMaid.clean.run();
		return 1;
	}

	// 清空所有轮换垃圾箱；force 为 true 时连同受保护物品一并清除。
	private static int emptyAll(CommandSourceStack source, boolean force) {
		int moved = SMSavedData.getInstance().emptyAllDustbins(force);
		sendEmptyFeedback(source, "All dustbins", moved);
		return 1;
	}

	// 清空回收箱。
	private static int emptyRecycle(CommandSourceStack source) {
		SMSavedData.getInstance().emptyRecycle();
		sendEmptyFeedback(source, SMCommonConfig.RECYCLE_NAME.get(), 0);
		return 1;
	}

	// 清空指定索引的轮换垃圾箱；force 为 true 时连同受保护物品一并清除。
	private static int emptyDustbin(CommandSourceStack source, int index, boolean force) {
		if (index < 0 || index >= SMSavedData.totalDustbinCount()) {
			source.sendFailure(Component.literal(SMCommonConfig.MESSAGE_WRONG_DUSTBIN.get()));
			return 0;
		}
		int moved = SMSavedData.getInstance().emptyDustbin(index, force);
		sendEmptyFeedback(source, SMCommonConfig.DUSTBIN_NAME.get() + index, moved);
		return 1;
	}

	// 发送清空反馈：先说明清空的目标，仅当确有物品移入回收箱时，再单独补一行移入数量。
	private static void sendEmptyFeedback(CommandSourceStack source, String target, int movedToRecycle) {
		source.sendSuccess(() -> Component.literal(SMCommonConfig.MESSAGE_DUSTBIN_EMPTIED.get().replace("$1", target)), true);
		if (movedToRecycle > 0) {
			source.sendSuccess(() -> Component.literal(SMCommonConfig.MESSAGE_RECYCLE_MOVED.get().replace("$1", String.valueOf(movedToRecycle))), true);
		}
	}

	// 为 index 参数提供有效垃圾箱序号的补全建议（整数参数默认不会出现在 Tab 补全中）。
	private static CompletableFuture<Suggestions> suggestDustbinIndices(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		int total = SMSavedData.totalDustbinCount();
		for (int i = 0; i < total; ++i) {
			builder.suggest(i);
		}
		return builder.buildFuture();
	}

	/**
	 * 私有构造方法，防止实例化喵~
	 */
	private SMCommands() {
	}
}
