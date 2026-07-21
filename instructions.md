# 从 Forge 1.19.4 移植到 NeoForge 1.21.1 (ModDevGradle) 步骤指南

> 项目：Sanity: Descent Into Madness (`sanitydim`)  
> 当前平台：Minecraft 1.19.4 + Forge 45.1.0 + Parchment + Mixin + GeckoLib 4.2  
> 目标平台：Minecraft 1.21.1 + NeoForge + ModDevGradle

---

## 目录

1. [构建系统迁移](#1-构建系统迁移)
2. [主类与注册系统迁移](#2-主类与注册系统迁移)
3. [能力(Capability)系统迁移](#3-能力capability系统迁移)
4. [网络通信迁移](#4-网络通信迁移)
5. [配置系统迁移](#5-配置系统迁移)
6. [Mixin 迁移](#6-mixin-迁移)
7. [客户端渲染迁移](#7-客户端渲染迁移)
8. [实体、物品与音效注册迁移](#8-实体物品与音效注册迁移)
9. [事件系统迁移](#9-事件系统迁移)
10. [资源文件迁移](#10-资源文件迁移)
11. [依赖更新](#11-依赖更新)
12. [API 变更速查表](#12-api-变更速查表)

---

## 1. 构建系统迁移

### 1.1 `gradle.properties`

将版本属性从 1.19.4 更新为 1.21.1：

```properties
org.gradle.jvmargs=-Xmx4G
org.gradle.daemon=false

mod_id=sanitydim
spec_version=1
mod_version=1.1.0
developer=croissantnova
archives_base_name=sanitydim

minecraft_version=1.21.1
mc_display_version=mc1.21.1
neo_version=21.1.0   # NeoForge 版本号

geckolib_version=4.7  # 查 NeoForge 1.21.1 对应 GeckoLib 版本
```

移除：
- `forge_version` → 替换为 `neo_version`
- `parchment_version` → NeoForge 1.21.1 的 ModDevGradle 使用官方映射，若需 Parchment 需额外配置

### 1.2 `settings.gradle`

从 ForgeGradle 风格改为 ModDevGradle 风格：

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}
```

### 1.3 `build.gradle` — 完整重构

从 ForgeGradle 5.1+ 风格改为 ModDevGradle/NeoGradle 风格：

```groovy
plugins {
    id 'java-library'
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '1.0.+'     // ModDevGradle 插件
    id 'net.neoforged.moddev.repositories' version '1.0.+'
}

version = mod_version
group = "${developer}.${mod_id}"
base {
    archivesName = "${archives_base_name}-${mc_display_version}"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)   // Java 17 → 21

neoForge {
    version = neo_version

    parchment {
        // 如需 Parchment 映射，查阅 ModDevGradle 文档配置方式
    }

    runs {
        client {
            client()
        }
        server {
            server()
        }
        gameTestServer {
            gameTestServer()
        }
        data {
            data()
            programArguments = ['--mod', 'sanitydim', '--all',
                '--output', file('src/generated/resources/').absolutePath,
                '--existing', file('src/main/resources/').absolutePath]
        }
    }

    mods {
        sanitydim {
            sourceSet sourceSets.main
        }
    }
}

sourceSets.main.resources { srcDir 'src/generated/resources' }

// Mixin 配置（在 mods.toml 或 build.gradle 中声明）
// NeoForge 需要 mixin 配置在 neoforge.mods.toml 中声明

repositories {
    maven { url 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }
    // 注意：GeckoLib 的 NeoForge 版本可能在另一个仓库
    maven { url 'https://maven.blamejared.com/' }
}

dependencies {
    // GeckoLib NeoForge 1.21.1 版本（需确认坐标）
    implementation "software.bernie.geckolib:geckolib-neoforge-${minecraft_version}:${geckolib_version}"
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

// ModDevGradle 自动处理 reobf，无需手动 finalizedBy('reobfJar')

publishing {
    publications {
        mavenJava(MavenPublication) {
            artifact jar
        }
    }
    repositories {
        maven {
            url "file://${project.projectDir}/mcmodsrepo"
        }
    }
}
```

**关键变化：**
- `net.minecraftforge.gradle` → `net.neoforged.moddev`
- 移除 `buildscript` 块（不再需要）
- 移除 `apply plugin:` 语句
- `minecraft {}` → `neoForge {}`
- `mappings channel: 'parchment', ...` → 通过 `parchment {}` 子块配置
- Java 17 → Java 21
- 移除 `mixin {}` 块（改用 NeoForge 的 mixin 声明方式）
- 移除 `jar {}` 大部分内容（ModDevGradle 自动生成）
- 移除 `jar.finalizedBy('reobfJar')`（自动处理）

---

## 2. 主类与注册系统迁移

### 2.1 `SanityMod.java`

```diff
- import net.minecraftforge.api.distmarker.Dist;
- import net.minecraftforge.api.distmarker.OnlyIn;
- import net.minecraftforge.common.MinecraftForge;
- import net.minecraftforge.eventbus.api.IEventBus;
- import net.minecraftforge.fml.common.Mod;
- import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
- import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
- import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
+ import net.neoforged.api.distmarker.Dist;
+ import net.neoforged.api.distmarker.OnlyIn;
+ import net.neoforged.neoforge.common.NeoForge;
+ import net.neoforged.bus.api.IEventBus;
+ import net.neoforged.fml.common.Mod;
+ import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
+ import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
+ import net.neoforged.fml.ModLoadingContext;
```

| Forge 1.19.4 | NeoForge 1.21.1 |
|---|---|
| `MinecraftForge.EVENT_BUS` | `NeoForge.EVENT_BUS` |
| `FMLJavaModLoadingContext.get().getModEventBus()` | `ModLoadingContext.get().getModEventBus()`（包变更） |
| `net.minecraftforge.fml.common.Mod` | `net.neoforged.fml.common.Mod` |
| `net.minecraftforge.eventbus.api.IEventBus` | `net.neoforged.bus.api.IEventBus` |
| `@OnlyIn(Dist.CLIENT)` | `@OnlyIn(Dist.CLIENT)`（包路径变更） |

**注意：** 静态字段 `@OnlyIn` 可能会有编译警告，建议重构为仅在客户端初始化。

### 2.2 DeferredRegister

`DeferredRegister` 在 NeoForge 中包路径有变化：

```diff
- import net.minecraftforge.registries.DeferredRegister;
- import net.minecraftforge.registries.ForgeRegistries;
- import net.minecraftforge.registries.RegistryObject;
+ import net.neoforged.neoforge.registries.DeferredRegister;
+ import net.minecraft.core.registries.Registries;  // 改用 Vanilla 的 Registries
+ import net.neoforged.neoforge.registries.DeferredHolder;  // RegistryObject → DeferredHolder
```

| Forge | NeoForge |
|---|---|
| `ForgeRegistries.ENTITY_TYPES` | `Registries.ENTITY_TYPE` |
| `ForgeRegistries.ITEMS` | `Registries.ITEM` |
| `ForgeRegistries.SOUND_EVENTS` | `Registries.SOUND_EVENT` |
| `RegistryObject<T>` | `DeferredHolder<T, T>`（或仍可用 `RegistryObject`） |

**`EntityRegistry.java` 示例：**

```java
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> DEFERRED_REGISTER =
        DeferredRegister.create(Registries.ENTITY_TYPE, SanityMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RottingStalker>> ROTTING_STALKER =
        DEFERRED_REGISTER.register("rotting_stalker",
            () -> EntityType.Builder.of(RottingStalker::new, MobCategory.MONSTER)
                .sized(1f, 2.9f).fireImmune().build("rotting_stalker"));
    // ...
}
```

**`ItemRegistry.java` 和 `SoundRegistry.java` 同理。**

---

## 3. 能力(Capability)系统迁移

NeoForge 1.21.1 的能力系统与 Forge 有较大差异。

### 3.1 Capability 注册

```diff
- import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
+ import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
```

在 `EventHandler.registerCaps()` 中：

```java
// NeoForge 1.21.1 不再需要 RegisterCapabilitiesEvent 注册 Capability 类
// 改为直接使用 Capability 常量 + attach 事件
```

### 3.2 Capability Provider

`SanityProvider.java` 现在改为实现 `ICapabilityProvider`（注意包路径变化）：

```diff
- import net.minecraftforge.common.capabilities.*;
- import net.minecraftforge.common.util.LazyOptional;
+ import net.neoforged.neoforge.capabilities.ICapabilityProvider;
+ import net.neoforged.neoforge.common.util.LazyOptional;
```

**注意：** NeoForge 1.21.1 中 `CapabilityToken` 已被移除，改为直接使用 `Capability` 接口的静态工厂方法。建议查阅最新的 NeoForge Capability 文档确认具体 API。

### 3.3 `AttachCapabilitiesEvent`

```diff
- import net.minecraftforge.event.AttachCapabilitiesEvent;
+ import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
```

事件处理逻辑基本一致，但包路径变更。

---

## 4. 网络通信迁移

### 4.1 `PacketHandler.java`

```diff
- import net.minecraftforge.network.NetworkRegistry;
- import net.minecraftforge.network.simple.SimpleChannel;
+ import net.neoforged.neoforge.network.NetworkRegistry;
+ import net.neoforged.neoforge.network.simple.SimpleChannel;
```

`SimpleChannel` 的 API 在 NeoForge 中基本保持兼容，但如有变化请查阅 NeoForge 网络文档。

**注意：** `PROTOCOL_VERSION` 建议更新为新的版本号以反映新平台。

---

## 5. 配置系统迁移

`ForgeConfigSpec` 在 NeoForge 中保持兼容，包路径变更：

```diff
- import net.minecraftforge.common.ForgeConfigSpec;
- import net.minecraftforge.fml.ModLoadingContext;
- import net.minecraftforge.fml.config.ModConfig;
- import net.minecraftforge.fml.event.config.ModConfigEvent;
+ import net.neoforged.neoforge.common.ModConfigSpec;  // 或仍为 ForgeConfigSpec
+ import net.neoforged.fml.ModLoadingContext;
+ import net.neoforged.fml.config.ModConfig;
+ import net.neoforged.fml.event.config.ModConfigEvent;
```

**关键类：**
- `ConfigManager.java` — 约 24KB，包含大量配置项
- `ConfigDefault.java`、`ConfigProxy.java`、`ConfigItem.java` 等辅助类

**建议：** NeoForge 1.21.1 可能仍使用 `ForgeConfigSpec`（包路径可能不同），以及 `ModConfigSpec` 共存。请查阅 NeoForge 最新文档确认配置 API。

---

## 6. Mixin 迁移

### 6.1 `sanitydim.mixins.json`

```diff
  {
-   "minVersion": "0.7.11",
-   "compatibilityLevel": "JAVA_17",
-   "package": "croissantnova.sanitydim.mixin",
-   "refmap": "sanitydim.refmap.json",
+   "minVersion": "0.8.5",
+   "compatibilityLevel": "JAVA_21",
+   "package": "croissantnova.sanitydim.mixin",
+   "refmap": "sanitydim.refmap.json",
    "mixins": [...],
    "client": [...],
    "server": []
  }
```

### 6.2 声明 Mixin 到 NeoForge

在 `META-INF/neoforge.mods.toml`（见 10.1 节）中添加：

```toml
[[mixins]]
config="sanitydim.mixins.json"
```

或在 `build.gradle` 中配置（取决于 ModDevGradle 版本）。

### 6.3 Mixin 目标变化

**高风险的 Mixin（需要验证 1.21.1 中的目标方法是否变更）：**

| Mixin 类 | 注入目标 | 风险 |
|---|---|---|
| `MixinEntity.move()` | `Entity.move(MoverType, Vec3)` | 中 |
| `MixinGameRenderer.render()` | `GameRenderer.render(FJZ)V` | 高 — 渲染管线变化 |
| `MixinGameRenderer.resize()` | `GameRenderer.resize(II)V` | 中 |
| `MixinClientLevel.playSeededSound()` | `ClientLevel.playSeededSound(...)` | 高 — 方法签名可能变化 |
| `MixinChickenRenderer` | `ChickenRenderer` | 中 — 实体渲染器变化 |
| `MixinCowRenderer`/`MixinPigRenderer`/`MixinSheepRenderer` | 实体渲染器 | 中 |
| `MixinSheepFurLayer` | `SheepFurLayer` | 中 |
| `MixinServerPlayerGameMode` | `ServerPlayerGameMode` | 中 |
| `MixinAnimal`/`MixinChicken`/`MixinCow`/`MixinPig`/`MixinSheep` | 实体类 | 低 |
| `MixinEnderMan`/`MixinVillager`/`MixinZombifiedPiglin` | 实体类 | 低 |
| `MixinFlowerPotBlock`/`MixinJukeboxBlockEntity` | 方块类 | 低 |
| `MixinShearsItem`/`MixinThrownEgg`/`MixinTemptGoal` | 物品/AI | 低 |

**Mixin 代码中需要全局检查的模式：**
- `BlockPos.m_274561_(x, y, z)` → 在 1.21.1 中应改为 `BlockPos.containing(x, y, z)` 或 `new BlockPos((int)x, (int)y, (int)z)`
- 所有 `@Shadow` 方法签名需对照 1.21.1 确认

---

## 7. 客户端渲染迁移

### 7.1 HUD 覆盖层

Forge 的 `RegisterGuiOverlaysEvent` 在 NeoForge 1.21.1 中变更为 `RegisterGuiLayersEvent`：

```diff
- import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
- import net.minecraftforge.client.gui.overlay.ForgeGui;
- import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
+ import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
+ import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;  // 可能变化
```

在 `ModEventHandler.registerOverlaysEvent()` 和 `GuiHandler.initOverlays()` 中：
- `event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), ...)` → API 可能变化
- `event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), ...)` → API 可能变化

**注意：** `ForgeGui.blit()` 静态方法在 NeoForge 中可能不存在，需改用 `GuiGraphics.blit()`。

### 7.2 GUI 渲染 (`GuiHandler.java`)

- `PoseStack` → 在新版 Minecraft 1.21.1 中使用 `GuiGraphics`（多数 draw 方法已迁移到 `GuiGraphics`）
- `ForgeGui.blit(poseStack, ...)` → `guiGraphics.blit(...)`
- `gui.getFont().drawShadow(poseStack, ...)` → `guiGraphics.drawString(font, ...)`
- `RenderSystem.setShaderTexture(0, ...)` → 使用 `RenderSystem.setShaderTexture(0, ...)` 仍然可用，但需确认

### 7.3 后处理渲染 (`PostProcessor.java`)

`PostPass` 构造函数在 1.21.1 中可能变化。需要检查：

```java
// 旧代码
new PostPass(mc.getResourceManager(), "insanity", mc.getMainRenderTarget(), m_swapBuffer);
// 新版本可能更改为：
new PostPass(mc.getResourceManager(), ResourceLocation.parse("shaders/program/insanity.json"), ...);
```

### 7.4 实体渲染器 (`render/` 目录)

- `EntityRenderersEvent.RegisterRenderers` 仍然可用（包路径变更）
- GeckoLib 的 `GeoEntityRenderer` 需要更新到 NeoForge 1.21.1 版本
- `RendererRottingStalker`、`RendererSneakingTerror` 继承 GeckoLib 的渲染器，API 可能变化
- `CustomGlowingGeoLayer` 需要验证兼容性
- `BlackoutEyesLayer` 系列类中的渲染逻辑需对照新 API

### 7.5 顶点渲染

`GuiHandler.renderFullscreen()` 中的缓冲区渲染：

```diff
- bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
+ // 检查 1.21.1 中是否变化
```

---

## 8. 实体、物品与音效注册迁移

### 8.1 实体 (`EntityRegistry.java`)

已在上文（2.2 节）说明。额外注意：
- `RottingStalker.java` 和 `SneakingTerror.java` 如果使用 GeckoLib，需更新到 NeoForge 版本
- `InnerEntity.java` / `InnerEntitySpawner.java` 可能需要更新 API

### 8.2 物品 (`ItemRegistry.java`)

- `GarlandItem.java` — 实现 `FlowerArmorMaterial` 接口，需检查 `ArmorMaterial` API 变化
- `FlowerArmorMaterial.java` — 在 1.21.1 中 `ArmorMaterial` 可能使用 `Holder` 模式

### 8.3 音效 (`SoundRegistry.java`)

```diff
- SoundEvent.createVariableRangeEvent(new ResourceLocation(...))
+ SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(...))
```

或：
```diff
- new ResourceLocation(SanityMod.MODID, name)
+ ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, name)
```

---

## 9. 事件系统迁移

### 9.1 Forge 事件包变更

全局替换规则：

| 旧包 (Forge) | 新包 (NeoForge) |
|---|---|
| `net.minecraftforge.common.MinecraftForge` | `net.neoforged.neoforge.common.NeoForge` |
| `net.minecraftforge.eventbus.api.SubscribeEvent` | `net.neoforged.bus.api.SubscribeEvent` |
| `net.minecraftforge.eventbus.api.IEventBus` | `net.neoforged.bus.api.IEventBus` |
| `net.minecraftforge.event.TickEvent` | `net.neoforged.neoforge.event.tick.LevelTickEvent` / `PlayerTickEvent` |
| `net.minecraftforge.event.level.LevelEvent` | `net.neoforged.neoforge.event.level.LevelEvent` |
| `net.minecraftforge.event.level.BlockEvent` | `net.neoforged.neoforge.event.level.BlockEvent` |
| `net.minecraftforge.event.entity.*` | `net.neoforged.neoforge.event.entity.*` |
| `net.minecraftforge.event.entity.player.*` | `net.neoforged.neoforge.event.entity.player.*` |
| `net.minecraftforge.event.entity.living.*` | `net.neoforged.neoforge.event.entity.living.*` |

### 9.2 TickEvent 分解

NeoForge 1.21.1 中 `TickEvent` 被拆分为独立的事件类：

```diff
- TickEvent.PlayerTickEvent + event.side + event.phase
+ PlayerTickEvent.Post / PlayerTickEvent.Pre
```

`EventHandler.tickPlayer()`：
```java
// 旧
@SubscribeEvent
public void tickPlayer(final TickEvent.PlayerTickEvent event) {
    if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END && ...)
}

// 新（大概形式，需确认 NeoForge 1.21.1 精确 API）
@SubscribeEvent
public void tickPlayer(final PlayerTickEvent.Post event) { ... }
```

### 9.3 其他事件

- `RegisterCommandsEvent` → 包变更为 `net.neoforged.neoforge.event.RegisterCommandsEvent`
- `VanillaGameEvent` → 包变更
- `AdvancementEvent.AdvancementEarnEvent` → 包变更
- `EntityAttributeCreationEvent` → 包变更
- `RegisterGuiOverlaysEvent` → `RegisterGuiLayersEvent`

### 9.4 `ModEventHandler.java` 静态方法

所有 `@SubscribeEvent` 静态方法在 NeoForge 中需要注册到 mod bus 时，使用 `@Mod.EventBusSubscriber` 注解或手动注册。

---

## 10. 资源文件迁移

### 10.1 `mods.toml` → `neoforge.mods.toml`

创建 `src/main/resources/META-INF/neoforge.mods.toml`（NeoForge 使用不同的文件名）：

```toml
modLoader="javafml"
loaderVersion="[4,)"         # NeoForge 的 FML 版本

license="All rights reserved"
issueTrackerURL="https://github.com/croissantnova/SanityDescentIntoMadness/issues"

[[mods]]
modId="sanitydim"
version="${file.jarVersion}"
displayName="Sanity: Descent Into Madness"
updateJSONURL="https://raw.githubusercontent.com/croissantnova/SanityDescentIntoMadness/main/update.json"
displayURL="https://modrinth.com/mod/sanity-descent-into-madness"
logoFile="sanitydim512.png"
credits="toujourspareil, Zapsplat"
authors="croissantnova"
description='''Brings the sanity mechanic from Don't Starve into Minecraft'''

[[dependencies.sanitydim]]
    modId="geckolib"
    mandatory=true
    versionRange="[4.7,)"
    side="BOTH"

[[dependencies.sanitydim]]
    modId="neoforge"
    mandatory=true
    versionRange="[21.1,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.sanitydim]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.21.1,1.22)"
    ordering="NONE"
    side="BOTH"

# Mixin 声明
[[mixins]]
config="sanitydim.mixins.json"
```

**主要变化：**
- 文件名从 `mods.toml` → `neoforge.mods.toml`
- `loaderVersion` 从 `[45,)` → `[4,)`（NeoForge FML 版本）
- 依赖 `forge` → `neoforge`
- Minecraft 版本范围更新为 `[1.21.1,1.22)`

### 10.2 `pack.mcmeta`

```diff
  {
      "pack": {
          "description": "sanitymod resources",
-         "pack_format": 12,
-         "forge:resource_pack_format": 12,
-         "forge:data_pack_format": 10
+         "pack_format": 34,  // 1.21.1 的资源包格式号
      }
  }
```

**注意：** Minecraft 1.21.1 的资源包格式号请以 Mojang 官方文档为准（可能在 34 左右）。NeoForge 不再需要 `forge:resource_pack_format` 和 `forge:data_pack_format` 字段。

### 10.3 Shader 文件

`assets/minecraft/shaders/program/insanity.json` 和 `chromatical.json`：
- `_comment` 字段中的 "targets" → 可能在 1.21.1 中有变化
- 确认后处理 shader 的 JSON 格式是否与 1.21.1 兼容

### 10.4 语言文件

`assets/sanitydim/lang/en_us.json` — 正常不需要改动，但确保所有翻译 key 与代码中的 `Component.translatable()` 调用匹配。

### 10.5 `sounds.json`

位于 `assets/sanitydim/sounds.json`，格式通常兼容，但建议与 1.21.1 模板比对确认。

### 10.6 GeckoLib 动画/模型

`assets/sanitydim/geo/` 和 `assets/sanitydim/animations/` — GeckoLib 模型和动画文件格式跨版本兼容较好，但更新到新版 GeckoLib 后建议验证。

---

## 11. 依赖更新

### 11.1 GeckoLib

| 项目 | Forge 1.19.4 | NeoForge 1.21.1 |
|---|---|---|
| Maven 坐标 | `software.bernie.geckolib:geckolib-forge-1.19.4:4.2` | `software.bernie.geckolib:geckolib-neoforge-1.21.1:4.7`（待确认） |
| Maven 仓库 | `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/` | 可能变更，确认最新 |

**需要更新的 GeckoLib API：**
- 实体动画状态机（如有）
- `GeoEntityRenderer` 构造参数可能变化
- `IAnimatable` 接口方法

### 11.2 Mixin

| 项目 | 旧版 | 新版 |
|---|---|---|
| Mixin 版本 | 0.8.5 (annotationProcessor) | 内置于 NeoForge/ModDevGradle |
| MixinGradle 插件 | `org.spongepowered.mixin` 0.7+ | **不再需要** — ModDevGradle 内置 Mixin 支持 |

移除 `build.gradle` 中的：
```groovy
// 不再需要
classpath 'org.spongepowered:mixingradle:0.7-SNAPSHOT'
id 'org.spongepowered.mixin' version '0.7.+'
apply plugin: 'org.spongepowered.mixin'

mixin {
    add sourceSets.main, project.mod_id + '.refmap.json'
    config project.mod_id + '.mixins.json'
}
```

### 11.3 Parchment

如需 Parchment 映射，查阅 ModDevGradle 文档的 `parchment {}` 配置方式。NeoForge 1.21.1 默认使用官方映射，Parchment 为可选附加。

---

## 12. API 变更速查表

### 常见的 `new ResourceLocation()` 替换

```diff
- new ResourceLocation(SanityMod.MODID, "sanity")
+ ResourceLocation.fromNamespaceAndPath(SanityMod.MODID, "sanity")
```

### BlockPos 方法

```diff
- BlockPos.m_274561_(x, y, z)
+ BlockPos.containing(x, y, z)
```

### 包路径速查

| 类 | Forge 1.19.4 包 | NeoForge 1.21.1 包 |
|---|---|---|
| `@Mod` | `net.minecraftforge.fml.common.Mod` | `net.neoforged.fml.common.Mod` |
| `@SubscribeEvent` | `net.minecraftforge.eventbus.api.SubscribeEvent` | `net.neoforged.bus.api.SubscribeEvent` |
| `IEventBus` | `net.minecraftforge.eventbus.api.IEventBus` | `net.neoforged.bus.api.IEventBus` |
| `Dist` / `@OnlyIn` | `net.minecraftforge.api.distmarker` | `net.neoforged.api.distmarker` |
| `MinecraftForge` | `net.minecraftforge.common.MinecraftForge` | `net.neoforged.neoforge.common.NeoForge` |
| `DeferredRegister` | `net.minecraftforge.registries.DeferredRegister` | `net.neoforged.neoforge.registries.DeferredRegister` |
| `ForgeRegistries` | `net.minecraftforge.registries.ForgeRegistries` | → 使用 `net.minecraft.core.registries.Registries` |
| `ForgeConfigSpec` | `net.minecraftforge.common.ForgeConfigSpec` | `net.neoforged.neoforge.common.ModConfigSpec` |
| `SimpleChannel` | `net.minecraftforge.network.simple.SimpleChannel` | `net.neoforged.neoforge.network.simple.SimpleChannel` |

---

## 建议的迁移顺序

1. **构建系统**（步骤 1）— 先搭好 Gradle 环境，确保可编译
2. **资源文件**（步骤 10）— mods.toml、pack.mcmeta、mixins.json
3. **主类和注册**（步骤 2 + 8）— 修复编译错误
4. **事件系统**（步骤 9）— 批量修改 import 和事件类名
5. **能力系统**（步骤 3）— 适配新 Capability API
6. **网络通信**（步骤 4）— 更新网络包
7. **配置系统**（步骤 5）— 更新 ConfigManager
8. **Mixin**（步骤 6）— 验证每个 Mixin 目标在 1.21.1 中是否存在
9. **客户端渲染**（步骤 7）— 最后处理 GUI、渲染器和后处理

---

> **注意：** 本指南基于项目当前结构分析生成，实际迁移时请以 NeoForge 1.21.1 和 ModDevGradle 的最新官方文档为准。部分 API 细节（特别是 Capability 系统和事件 Tick 拆分）可能在 NeoForge 正式版中有调整，建议在迁移每个步骤时查阅对应文档。
>
> NeoForge 文档：https://docs.neoforged.net/
>
> ModDevGradle 文档：https://github.com/neoforged/ModDevGradle
