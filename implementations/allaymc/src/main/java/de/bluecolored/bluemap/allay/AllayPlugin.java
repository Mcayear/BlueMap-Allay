/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.allay;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.JavaLogger;
import de.bluecolored.bluemap.core.logger.Logger;
import org.allaymc.api.network.ProtocolInfo;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.world.Dimension;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * BlueMap Project 2024/7/6
 *
 * @author daoge_cmd
 */
public class AllayPlugin extends Plugin implements Server {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger("BlueMap");
    private static AllayPlugin INSTANCE;

    private final de.bluecolored.bluemap.common.plugin.Plugin pluginInstance;
    private final EventForwarder eventForwarder;
    private final AllayCommands commands;
    private final String minecraftVersion;

    private int playerUpdateIndex = 0;
    private final Map<UUID, Player> onlinePlayerMap;
    private final List<AllayPlayer> onlinePlayerList;

    private final LoadingCache<org.allaymc.api.world.World, ServerWorld> worlds;

    public AllayPlugin() {
        Logger.global.clear();

        // 1. 创建一个 java.util.logging.Logger (JUL Logger) 实例作为桥接
        java.util.logging.Logger julLoggerBridge = java.util.logging.Logger.getLogger("BlueMapAllayBridge");
        julLoggerBridge.setUseParentHandlers(false); // 防止日志重复输出到父级 handlers (如控制台)

        // 2. 创建并添加我们的自定义 Slf4jJulHandler 到 JUL Logger
        //    这将把所有发送到 julLoggerBridge 的日志转发给 SLF4J 的 'log' 实例
        Slf4jJulHandler slf4jHandler = new Slf4jJulHandler(log); // 'log' 是我们顶部的 SLF4J logger
        slf4jHandler.setLevel(Level.ALL); // Handler本身也需要设置级别以处理所有日志
        julLoggerBridge.addHandler(slf4jHandler);
        julLoggerBridge.setLevel(Level.ALL); // JUL Logger 也需要设置级别以捕获所有日志记录

        // 3. 使用配置好的 julLoggerBridge 创建 BlueMap 的 JavaLogger
        //    这里我们使用 BlueMapJavaLogger 作为 de.bluecolored.bluemap.core.logger.JavaLogger 的别名
        JavaLogger blueMapCoreLogger = new JavaLogger(julLoggerBridge);

        // 4. 将这个 BlueMap JavaLogger 实例添加到 BlueMap 的全局 Logger 中
        //    这里的 .put() 方法是基于您原始代码的推测，请确认 MultiLogger 是否有此方法或类似方法如 addLogger()
        //    如果 MultiLogger 不支持动态添加，此行可能需要调整，或者说明BlueMap的日志系统可能不如预期的那样灵活。
        Logger.global.put(blueMapCoreLogger);

        this.minecraftVersion = ProtocolInfo.getMinecraftVersionStr();

        this.onlinePlayerMap = new ConcurrentHashMap<>();
        this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

        this.eventForwarder = new EventForwarder();
        this.pluginInstance = new de.bluecolored.bluemap.common.plugin.Plugin("allay", this);
        this.commands = new AllayCommands(this.pluginInstance);

        this.worlds = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .maximumSize(1000)
                .build(AllayWorld::new);

        AllayPlugin.INSTANCE = this;
    }

    @Override
    public void onEnable() {
        //register events
        org.allaymc.api.server.Server.getInstance().getEventBus().registerListener(this);
        org.allaymc.api.server.Server.getInstance().getEventBus().registerListener(eventForwarder);

        //register commands
        for (var command : commands.getRootCommands()) {
            Registries.COMMANDS.register(command.getName(), command);
        }

        //update online-player collections
        this.onlinePlayerList.clear();
        this.onlinePlayerMap.clear();
        for (var player : org.allaymc.api.server.Server.getInstance().getPlayerService().getPlayers().values()) {
            var allayPlayer = new AllayPlayer(player.getUUID());
            onlinePlayerMap.put(player.getUUID(), allayPlayer);
            onlinePlayerList.add(allayPlayer);
        }

        //load bluemap
        org.allaymc.api.server.Server.getInstance().getScheduler().runLaterAsync(this, () -> {
            try {
                Logger.global.logInfo("Loading...");
                this.pluginInstance.load();
                if (pluginInstance.isLoaded()) Logger.global.logInfo("Loaded!");

                //start updating players
                org.allaymc.api.server.Server.getInstance().getScheduler().scheduleRepeating(this, () -> {
                    updateSomePlayers();
                    return true;
                }, 1);
            } catch (IOException | RuntimeException e) {
                Logger.global.logError("Failed to load!", e);
                this.pluginInstance.unload();
            }
        });
    }

    @Override
    public @Nullable String getMinecraftVersion() {
        return minecraftVersion;
    }

    @Override
    public Path getConfigFolder() {
        return getPluginContainer().dataFolder();
    }

    @Override
    public Optional<Path> getModsFolder() {
        return Optional.of(Path.of("mods"));
    }

    @Override
    public Optional<ServerWorld> getServerWorld(Object world) {
        // TODO: impl it.
//        if (world instanceof Dimension allayDim)
//            return Optional.of(worlds.get(allayDim));
        return Optional.empty();
    }

    public ServerWorld getServerWorld(Dimension dimension) {
        // TODO: impl it.
//        return worlds.get(dimension);
        return null;
    }

    public static AllayPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    public Collection<ServerWorld> getLoadedServerWorlds() {
        Collection<ServerWorld> loadedWorlds = new ArrayList<>(3);
        for (org.allaymc.api.world.World world : org.allaymc.api.server.Server.getInstance().getWorldPool().getWorlds().values()) {
            loadedWorlds.add(worlds.get(world));
        }
        return loadedWorlds;
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return onlinePlayerMap.values();
    }

    @Override
    public void registerListener(ServerEventListener listener) {
        eventForwarder.addListener(listener);
    }

    @Override
    public void unregisterAllListeners() {
        eventForwarder.removeAllListeners();
    }

    /**
     * Only update some of the online players each tick to minimize performance impact on the server-thread.
     * Only call this method on the server-thread.
     */
    private void updateSomePlayers() {
        int onlinePlayerCount = onlinePlayerList.size();
        if (onlinePlayerCount == 0) return;

        int playersToBeUpdated = onlinePlayerCount / 20; //with 20 tps, each player is updated once a second
        if (playersToBeUpdated == 0) playersToBeUpdated = 1;

        for (int i = 0; i < playersToBeUpdated; i++) {
            playerUpdateIndex++;
            if (playerUpdateIndex >= 20 && playerUpdateIndex >= onlinePlayerCount) playerUpdateIndex = 0;

            if (playerUpdateIndex < onlinePlayerCount) {
                onlinePlayerList.get(playerUpdateIndex).update();
            }
        }

        return;
    }
}