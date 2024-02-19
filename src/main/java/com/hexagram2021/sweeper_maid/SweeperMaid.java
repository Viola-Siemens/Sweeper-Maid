package com.hexagram2021.sweeper_maid;

import com.hexagram2021.sweeper_maid.command.SMCommands;
import com.hexagram2021.sweeper_maid.config.SMCommonConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Mod(SweeperMaid.MODID)
public class SweeperMaid {
	public static final String MODID = "sweeper_maid";
	public static final String MODNAME = "Sweeper Maid";
	public static final String VERSION = ModList.get().getModFileById(MODID).versionString();

	public SweeperMaid() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SMCommonConfig.getConfig());
		MinecraftForge.EVENT_BUS.register(this);
	}

	public static final SimpleContainer dustbin = new SimpleContainer(54);
	private int sweepTickRemain = 0;
	private boolean toSweep = false;
	private boolean firstTick = true;

	private static final RandomSource randomSource = RandomSource.create();

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(SMCommands.register());
	}

	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event) {
		if(SMCommonConfig.ITEM_SWEEP_INTERVAL.get() == 0) {
			return;
		}
		switch (event.phase) {
			case START -> {
				this.sweepTickRemain -= 1;
				if(this.sweepTickRemain <= 0) {
					this.toSweep = true;
					this.sweepTickRemain = SMCommonConfig.ITEM_SWEEP_INTERVAL.get() * SharedConstants.TICKS_PER_SECOND;
				} else if(this.sweepTickRemain == 15 * SharedConstants.TICKS_PER_SECOND || this.sweepTickRemain == 30 * SharedConstants.TICKS_PER_SECOND || this.sweepTickRemain == 60 * SharedConstants.TICKS_PER_SECOND) {
					event.getServer().getPlayerList().getPlayers().forEach(player -> {
						try {
							player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
									createCommandSourceStack(player, player.level(), player.blockPosition()),
									Component.literal(SMCommonConfig.MESSAGE_BEFORE_SWEEP_15_30_60.get().replaceAll("\\$1", String.valueOf(this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GRAY),
									player, 0
							)));
						} catch (CommandSyntaxException ignored) {
						}
					});
				} else if(this.sweepTickRemain % SharedConstants.TICKS_PER_SECOND == 0 && this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND <= 10) {
					event.getServer().getPlayerList().getPlayers().forEach(player -> {
						try {
							player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
									createCommandSourceStack(player, player.level(), player.blockPosition()),
									Component.literal(SMCommonConfig.MESSAGE_BEFORE_SWEEP_1_10.get().replaceAll("\\$1", String.valueOf(this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GOLD),
									player, 0
							)));
						} catch (CommandSyntaxException ignored) {
						}
					});
				}
			}
			case END -> {
				if(this.firstTick) {
					this.firstTick = false;
					this.toSweep = false;
				} else if(this.toSweep) {
					this.toSweep = false;
					SimpleContainer oldBin = new SimpleContainer(dustbin.getContainerSize());
					for(int i = 0; i < dustbin.getContainerSize(); ++i) {
						oldBin.setItem(i, dustbin.getItem(i));
						dustbin.setItem(i, ItemStack.EMPTY);
					}
					AtomicInteger droppedItems = new AtomicInteger();
					AtomicInteger extraEntities = new AtomicInteger();
					event.getServer().getAllLevels().forEach(serverLevel -> {
						for(Entity entity: serverLevel.getAllEntities()) {
							if(entity instanceof ItemEntity itemEntity) {
								dustbin.addItem(itemEntity.getItem());
								droppedItems.addAndGet(1);
								itemEntity.kill();
							} else if(entity != null) {
								ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
								if (typeKey != null) {
									String type = typeKey.toString();
									if(SMCommonConfig.EXTRA_ENTITY_TYPES.get().contains(type)) {
										extraEntities.addAndGet(1);
										entity.kill();
									}
								}
							}
						}
					});
					for(int i = 0; i < oldBin.getContainerSize(); ++i) {
						dustbin.addItem(oldBin.getItem(i));
					}
					event.getServer().getPlayerList().getPlayers().forEach(player -> {
						try {
							player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
									createCommandSourceStack(player, player.level(), player.blockPosition()),
									Component.literal(SMCommonConfig.MESSAGE_AFTER_SWEEP.get().replaceAll("\\$1", droppedItems.toString()).replaceAll("\\$2", extraEntities.toString())).withStyle(ChatFormatting.AQUA),
									player, 0
							)));
						} catch (CommandSyntaxException ignored) {
						}
					});
					event.getServer().getPlayerList().broadcastSystemMessage(
							Component.literal(SMCommonConfig.CHAT_MESSAGE_AFTER_SWEEP.get()).append(Component.literal("/sweepermaid dustbin").withStyle(style ->
									style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sweepermaid dustbin")))), false
					);
				}
			}
		}
	}

	private static CommandSourceStack createCommandSourceStack(Player player, Level level, BlockPos blockPos) {
		return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(blockPos), Vec2.ZERO, (ServerLevel)level, 2, player.getName().getString(), player.getDisplayName(), level.getServer(), player);
	}
}
