package com.hexagram2021.sweeper_maid;

import com.hexagram2021.sweeper_maid.platform.Services;
import com.google.common.collect.Lists;
import com.hexagram2021.sweeper_maid.command.SMCommands;
import com.hexagram2021.sweeper_maid.save.SMSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class SweeperMaidCommon {
	public static final String MODID = "sweeper_maid";
	public static final String MODNAME = "Sweeper Maid";

	private static int sweepTickRemain = 0;
	private static boolean toSweep = false;
	private static boolean firstTick = true;

	public static <A> void registerCommands(@NotNull Supplier<CommandDispatcher<CommandSourceStack>> commands) {
		final CommandDispatcher<CommandSourceStack> dispatcher = commands.get();
		dispatcher.register(SMCommands.register());
	}

	public static void onServerPreTick(MinecraftServer server) {
		if (Services.CONFIG.getItemSweepInterval().get() == 0) return;

		sweepTickRemain -= 1;

		if (sweepTickRemain <= 0) {
			toSweep = true;
			sweepTickRemain = Services.CONFIG.getItemSweepInterval().get() * SharedConstants.TICKS_PER_SECOND;
			return;
		}

		if (sweepTickRemain == 15 * SharedConstants.TICKS_PER_SECOND || sweepTickRemain == 30 * SharedConstants.TICKS_PER_SECOND || sweepTickRemain == 60 * SharedConstants.TICKS_PER_SECOND) {
			server.getPlayerList().getPlayers().forEach(player -> {
				try {
					player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
							createCommandSourceStack(player, player.level(), player.blockPosition()),
							Component.literal(Services.CONFIG.getMessageBeforeSweep15_30_60().get().replaceAll("\\$1", String.valueOf(sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GRAY),
							player, 0
					)));
				} catch (CommandSyntaxException ignored) {
				}
			});

			return;
		}

		if (sweepTickRemain % SharedConstants.TICKS_PER_SECOND == 0 && sweepTickRemain / SharedConstants.TICKS_PER_SECOND <= 10) {
			server.getPlayerList().getPlayers().forEach(player -> {
				try {
					player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
							createCommandSourceStack(player, player.level(), player.blockPosition()),
							Component.literal(Services.CONFIG.getMessageBeforeSweep1_10().get().replaceAll("\\$1", String.valueOf(sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GOLD),
							player, 0
					)));
				} catch (CommandSyntaxException ignored) {

				}
			});
		}
	}

	public static void onServerPostTick(MinecraftServer server) {
		if (Services.CONFIG.getItemSweepInterval().get() == 0) return;

		SimpleContainer dustbin = SMSavedData.getDustbin();

		if (firstTick) {
			firstTick = false;
			toSweep   = false;
			return;
		}

		if (toSweep) {
			toSweep = false;
			SimpleContainer oldBin = new SimpleContainer(dustbin.getContainerSize());
			for (int i = 0; i < dustbin.getContainerSize(); ++i) {
				oldBin.setItem(i, dustbin.getItem(i));
				dustbin.setItem(i, ItemStack.EMPTY);
			}
			AtomicInteger droppedItems = new AtomicInteger();
			AtomicInteger extraEntities = new AtomicInteger();

			server.getAllLevels().forEach(serverLevel -> {
				Iterable<Entity> entities = serverLevel.getAllEntities();
				List<Entity> killedEntities = Lists.newArrayList();
				for (Entity entity: entities) {
					if(entity instanceof ItemEntity itemEntity) {
						dustbin.addItem(itemEntity.getItem());
						droppedItems.addAndGet(1);
						killedEntities.add(itemEntity);
					} else if (entity != null) {
						ResourceLocation typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
						if (typeKey != null) {
							String type = typeKey.toString();
							if(Services.CONFIG.getExtraEntityTypes().get().contains(type)) {
								extraEntities.addAndGet(1);
								killedEntities.add(entity);
							}
						}
					}
				}
				killedEntities.forEach(Entity::kill);
			});

			for (int i = 0; i < oldBin.getContainerSize(); ++i) {
				dustbin.addItem(oldBin.getItem(i));
			}

			server.getPlayerList().getPlayers().forEach(player -> {
				try {
					player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
							createCommandSourceStack(player, player.level(), player.blockPosition()),
							Component.literal(Services.CONFIG.getMessageAfterSweep().get().replaceAll("\\$1", droppedItems.toString()).replaceAll("\\$2", extraEntities.toString())).withStyle(ChatFormatting.AQUA),
							player, 0
					)));
				} catch (CommandSyntaxException ignored) {
				}
			});

			server.getPlayerList().broadcastSystemMessage(
					Component.literal(Services.CONFIG.getChatMessageAfterSweep().get()).append(Component.literal("/sweepermaid dustbin").withStyle(style ->
							style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sweepermaid dustbin")))), false
			);
			dustbin.setChanged();
		}
	}

	public static void onServerStarted(MinecraftServer server) {
		var world = server.getLevel(Level.OVERWORLD);

		assert world != null;
		// SMSavedData::new, SMSavedData::new, SMSavedData.SAVED_DATA_NAME
		if (!world.isClientSide) {
			SMSavedData worldData = world.getDataStorage().computeIfAbsent(new SavedData.Factory<>(SMSavedData::new, SMSavedData::new, null), SMSavedData.SAVED_DATA_NAME);
			SMSavedData.setInstance(worldData);
		}
	}

	private static CommandSourceStack createCommandSourceStack(Player player, Level level, BlockPos blockPos) {
		return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(blockPos), Vec2.ZERO, (ServerLevel)level, 2, player.getName().getString(), player.getDisplayName(), level.getServer(), player);
	}
}
