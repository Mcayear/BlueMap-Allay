package de.bluecolored.bluemap.allay;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import org.allaymc.api.network.ProtocolInfo;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private final LoadingCache<Dimension, ServerWorld> worlds;

    public AllayPlugin() {
        Logger.global.clear();
        Logger.global.put(new JavaLogger(log));

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
                .build(AllayDimension::new);

        AllayPlugin.INSTANCE = this;
    }

    @Override
    public void onEnable() {
        //register events
        org.allaymc.api.server.Server.getInstance().getEventBus().registerListener(this);
        org.allaymc.api.server.Server.getInstance().getEventBus().registerListener(eventForwarder);

        //register commands
        for (var command : commands.getRootCommands()) {
            org.allaymc.api.server.Server.getInstance().getCommandRegistry().register(command.getName(), command);
        }

        //update online-player collections
        this.onlinePlayerList.clear();
        this.onlinePlayerMap.clear();
        for (var player : getServer().getOnlinePlayers().values()) {
            var allayPlayer = new AllayPlayer(player.getUUID());
            onlinePlayerMap.put(player.getUUID(), allayPlayer);
            onlinePlayerList.add(allayPlayer);
        }

        //load bluemap
        getServer().getScheduler().runLaterAsync(this, () -> {
            try {
                Logger.global.logInfo("Loading...");
                this.pluginInstance.load();
                if (pluginInstance.isLoaded()) Logger.global.logInfo("Loaded!");

                //start updating players
                getServer().getScheduler().scheduleRepeating(this, () -> {
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
        if (world instanceof Dimension allayDim)
            return Optional.of(worlds.get(allayDim));
        return Optional.empty();
    }

    public ServerWorld getServerWorld(Dimension dimension) {
        return worlds.get(dimension);
    }

    public static AllayPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    public Collection<ServerWorld> getLoadedServerWorlds() {
        return List.of();
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
