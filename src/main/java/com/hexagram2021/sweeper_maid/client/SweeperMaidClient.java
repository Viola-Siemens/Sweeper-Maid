package com.hexagram2021.sweeper_maid.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端专用初始化喵~
 * <p>
 * 本类只会在物理客户端被加载，避免把客户端类带到专用服务器，从而保持核心模组“仅服务端”的特性喵~
 * </p>
 * <p>
 * 注册 NeoForge 内置配置界面，使玩家可在“模组列表 → Sweeper Maid → 配置”中直接编辑配置，
 * 其中包括各条通知语句（{@code $1}/{@code $2} 等占位符会原样保留）——这也是本模组的多语言方案：
 * 由能够访问服务器的管理员自行填写所需语言的语句喵~
 * </p>
 * <p>
 * 单人游戏或玩家开设的局域网世界中，宿主客户端与集成服务器共用同一份配置，因此在界面中的修改会直接作用于
 * 正在运行的服务器；连入的其他玩家只能改到自己本地那份配置，服务器不会采用，因此无需额外的权限控制喵~
 * </p>
 */
public final class SweeperMaidClient {
	private SweeperMaidClient() {
	}

	/**
	 * 注册配置界面工厂喵~
	 * <p>
	 * {@link ConfigurationScreen} 会依据已注册的 {@code ModConfigSpec} 自动生成可编辑界面，无需自定义 UI 喵~
	 * </p>
	 *
	 * @param modContainer 模组容器实例喵~
	 */
	public static void init(ModContainer modContainer) {
		modContainer.registerExtensionPoint(IConfigScreenFactory.class,
				(container, parent) -> new ConfigurationScreen(container, parent));
	}
}
