# Sweeper Maid - Custom Commands

This is a modified Forge 1.20.1 version of Sweeper Maid.

# 清扫女仆 - 自定义命令

本项目是基于 Sweeper Maid 修改的 Forge 1.20.1 版本。

## Original Project

- Original author: Hexagram
- Original repository: https://github.com/Viola-Siemens/Sweeper-Maid

## 原项目信息

- 原作者：Hexagram
- 原项目仓库：https://github.com/Viola-Siemens/Sweeper-Maid

## Changes in This Version

- Added `/sweepermaid dustbin clear <index>` for clearing a specified dustbin.
- The clear command uses `PERMISSION_LEVEL_CLEAN`, which defaults to permission level 2.
- Added `MESSAGE_AFTER_CLEAR_DUSTBIN` so the clear-result message can be customized in the common configuration.
- Preserved the original `SweeperMaid-SavedData` storage format, so existing dustbin contents remain compatible.

## 本版本的改动

- 新增 `/sweepermaid dustbin clear <index>` 命令，用于清空指定的垃圾箱。
- 清空命令使用 `PERMISSION_LEVEL_CLEAN` 权限，默认为权限等级 2。
- 新增 `MESSAGE_AFTER_CLEAR_DUSTBIN` 配置项，可在通用配置中自定义清空操作的提示消息。
- 保留了原有的 `SweeperMaid-SavedData` 存储格式，因此已有的垃圾箱内容保持兼容。

## Build

Run:

```text
gradlew.bat build
```

The built mod JAR is written to `build/libs/`.

## 构建方法

运行：

```text
gradlew.bat build
```

构建生成的 Mod JAR 文件将输出到 `build/libs/` 目录。

## Download

- **Current version**: sweeper_maid-1.1.2.jar
- **Direct download**: [sweeper_maid-1.1.2.jar](https://raw.githubusercontent.com/QXjusta/Sweeper-Maid-dev-Forge-1.20.1-release/main/releases/sweeper_maid-1.1.2.jar)
- Or browse the `releases/` directory on GitHub to download manually.

## 下载

- **当前版本**：sweeper_maid-1.1.2.jar
- **直接下载**：[sweeper_maid-1.1.2.jar](https://raw.githubusercontent.com/QXjusta/Sweeper-Maid-dev-Forge-1.20.1-release/main/releases/sweeper_maid-1.1.2.jar)
- 也可以在 GitHub 上浏览 `releases/` 目录手动下载。

> **提示**：除了直接下载，你还可以在 GitHub 仓库页面点击 **Releases** 标签，发布正式版本并附上 JAR 文件作为 Release Asset，这样更便于版本管理和用户下载。

## License

This modified work remains licensed under the GNU Affero General Public License v3.0. See `LICENSE`.

## 许可证

本修改版作品仍采用 GNU Affero General Public License v3.0 许可证。详见 `LICENSE` 文件。
