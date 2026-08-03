# 扫地女仆 (Sweeper Maid)

[English](README.md) | **简体中文**

一个轻量、**纯服务端**的 Minecraft 模组：定期清扫地面上的掉落物（以及箭矢等可配置的额外实体），
让服务器保持流畅——同时把清扫掉的物品暂存到可轮换的 **垃圾箱** 中以便找回，并用一个不参与轮换的
**回收箱** 保护贵重物品。

- **Minecraft：** 1.21.1 &nbsp;•&nbsp; **加载器：** NeoForge 21.1.x &nbsp;•&nbsp; **Java：** 21
- **运行端：** 仅服务端。原版 / 未装模组的客户端也能进入装有本模组的服务器；
  只有当你想使用游戏内配置界面时，才需要在客户端也安装本模组。

## 功能

- **定期清扫** 掉落物，间隔可配置，并通过物品栏上方（ActionBar）发送倒计时提示
  （60 / 30 / 15 秒，以及最后 10 秒内每秒一次）。
- **轮换垃圾箱** —— 清扫的物品存入 `DUSTBINS_PER_ROTATION`（x）× `ROTATION_COUNT`（y）个容器。
  每次清扫写入一个由 `x` 个垃圾箱组成的“轮换代”；某一代只有在经过 `y` 次清扫后才会被清空并复用，
  因此物品在被自动清除前可找回 `y` 次清扫的时间。
- **回收箱** —— 在清空某一代之前，*受保护* 的物品（按稀有度阈值、是否附魔、是否有自定义名称，
  或明确的物品清单判定）会被移入不参与轮换的回收箱，而不是被删除。
- **白名单 / 黑名单** —— 白名单物品永不清扫（留在地面）；黑名单物品会被清扫但不入箱（直接丢弃）。
- **区块过载警告** —— 当单个区块内的掉落物超过阈值时，向管理员发送提示。
- **性能优先** —— 清扫在开始时对目标拍摄快照，随后每刻只处理有限数量的实体
  （`SWEEP_ENTITIES_PER_TICK`）以避免卡顿；宽限时间（`MIN_ITEM_AGE_SECONDS`）会跳过玩家刚丢下的物品。
- **自定义语言的通知** —— 所有展示给玩家的文本都是你自己填写的配置字符串，可用任意语言。

## 安装

**服务端（主要目标）：**
1. 安装 **NeoForge 1.21.1**（任意 `21.1.x`）。
2. 把 `sweeper_maid-<版本>.jar` 放入服务器的 `mods/` 目录。
3. 启动服务器，配置文件会生成在 `config/sweeper_maid-common.toml`。

原版客户端、以及装了 NeoForge 但未装本模组的客户端都可正常连入——本模组已声明为“客户端可选”。

**客户端（可选）：** 安装同一个 jar 即可获得游戏内配置界面（*模组列表 → Sweeper Maid → 配置*）。
在单人游戏或你自己开设局域网世界时很有用。

## 指令

主指令：`/sweepermaid`

| 指令 | 默认权限 | 说明 |
| --- | --- | --- |
| `/sweepermaid clean` | 2（OP） | 立即执行一次清扫。 |
| `/sweepermaid dustbin [序号]` | 0（所有人） | 打开某个垃圾箱界面（默认 `0`）。 |
| `/sweepermaid recycle` | 0（所有人） | 打开回收箱。 |
| `/sweepermaid empty all [force]` | 2（OP） | 清空所有垃圾箱。 |
| `/sweepermaid empty <序号> [force]` | 2（OP） | 清空指定垃圾箱。 |
| `/sweepermaid empty recycle` | 2（OP） | 清空回收箱。 |

`force` 会跳过回收箱救援、直接删除受保护物品。权限等级可在配置中调整
（`PERMISSION_LEVEL_DUSTBIN` / `_CLEAN` / `_EMPTY`）。

## 轮换与回收箱的工作方式

默认 `x = 8`、`y = 2` 时共有 **16 个垃圾箱**（`0–15`）：第 0 代 = 箱 `0–7`，第 1 代 = 箱 `8–15`。

- 第 1 次清扫写入第 0 代（箱 `0–7`）。
- 第 2 次清扫写入第 1 代（箱 `8–15`）。
- 第 3 次清扫复用第 0 代：会先清空其中（第 1 次清扫留下的）物品——受保护的物品移入回收箱，其余移除。
  因此物品可找回 **`y` 次清扫** 的时间。

每次清扫后，玩家会收到一条可点击的聊天列表，列出所有非空垃圾箱（连续的会合并为区间，如
`[Dustbin 0~3]`），并附带一条“下次清扫将清空哪些垃圾箱”的提醒。

## 配置

配置文件为 `config/sweeper_maid-common.toml`。部分选项：

| 选项 | 默认值 | 含义 |
| --- | --- | --- |
| `ITEM_SWEEP_INTERVAL` | `600` | 两次清扫的间隔（秒）。`0` 表示关闭自动清扫（`/clean` 仍可用）。 |
| `MIN_ITEM_AGE_SECONDS` | `5` | 存在时间小于该值的物品本次不清扫。 |
| `SWEEP_ENTITIES_PER_TICK` | `200` | 清扫时每刻处理的实体数。`0` 表示单刻完成。 |
| `DUSTBINS_PER_ROTATION` | `8` | 每个轮换代的垃圾箱数量（x）。 |
| `ROTATION_COUNT` | `2` | 轮换代数量（y）——物品可存活的清扫次数。 |
| `ENABLE_RECYCLE` | `true` | 将受保护物品移入回收箱而非删除。 |
| `RECYCLE_PROTECT_RARITY` | `RARE` | 保护稀有度不低于该值的物品（`COMMON`/`UNCOMMON`/`RARE`/`EPIC`）。 |
| `RECYCLE_PROTECT_ENCHANTED` / `_NAMED` | `true` | 保护带附魔 / 带自定义名称的物品。 |
| `RECYCLE_PROTECT_ITEMS` | `[]` | 始终受保护的物品 ID。 |
| `ITEM_WHITELIST` | `nether_star, heavy_core` | 永不清扫。 |
| `ITEM_BLACKLIST` | `cobblestone, sand` | 会清扫但不入箱。 |
| `EXTRA_ENTITY_TYPES` | `arrow, spectral_arrow, …` | 一并清理的非掉落物实体类型。 |
| `ITEM_OVERLOAD_THRESHOLD` | `640` | 区块掉落物超过该数量时提醒管理员。 |
| `MESSAGE_*`、`DUSTBIN_NAME`、`RECYCLE_NAME` | —— | 所有面向玩家的文本，可用任意语言填写；`$1`/`$2` 等占位符会被替换。 |

**修改配置：**
- **专用 / 无界面服务器：** 直接编辑 `config/sweeper_maid-common.toml`，改动会 **实时生效**，无需重启。
- **单人游戏或局域网房主（客户端已装本模组）：** *模组列表 → Sweeper Maid → 配置*。

`config-examples/` 中提供了中文示例配置：[`config-examples/zh_cn.toml`](config-examples/zh_cn.toml)。

## 从源码构建

```bash
./gradlew build
```

生成的 jar 位于 `build/libs/`。

## 免责声明

> ⚠️ 本分支的功能开发大量借助 AI 完成（“vibe coded / 氛围编程”），按“原样”提供，
> **不含任何形式的担保**。在正式环境使用前，请先在备份或测试世界中验证。

## 许可证与鸣谢

以 **AGPL-3.0** 授权。原始模组作者 **Hexagram**——参见
[Viola-Siemens/Sweeper-Maid](https://github.com/Viola-Siemens/Sweeper-Maid)。
