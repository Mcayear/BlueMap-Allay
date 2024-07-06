package de.bluecolored.bluemap.allay;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.serverinterface.Gamemode;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import org.allaymc.api.entity.effect.type.EffectInvisibilityType;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.server.Server;
import org.cloudburstmc.protocol.bedrock.data.GameType;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * BlueMap Project 2024/7/6
 *
 * @author daoge_cmd
 */
public class AllayPlayer implements Player {
    private static final Map<GameType, Gamemode> GAMEMODE_MAP = new EnumMap<>(GameType.class);
    static {
        GAMEMODE_MAP.put(GameType.ADVENTURE, Gamemode.ADVENTURE);
        GAMEMODE_MAP.put(GameType.SURVIVAL, Gamemode.SURVIVAL);
        GAMEMODE_MAP.put(GameType.CREATIVE, Gamemode.CREATIVE);
        GAMEMODE_MAP.put(GameType.SPECTATOR, Gamemode.SPECTATOR);
    }

    private final UUID uuid;
    private Text name;
    private ServerWorld world;
    private Vector3d position;
    private Vector3d rotation;
    private int skyLight;
    private int blockLight;
    private boolean sneaking;
    private boolean invisible;
    private Gamemode gamemode;

    public AllayPlayer(UUID playerUUID) {
        this.uuid = playerUUID;
        update();
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public Text getName() {
        return this.name;
    }

    @Override
    public ServerWorld getWorld() {
        return this.world;
    }

    @Override
    public Vector3d getPosition() {
        return this.position;
    }

    @Override
    public Vector3d getRotation() {
        return rotation;
    }

    @Override
    public int getSkyLight() {
        return skyLight;
    }

    @Override
    public int getBlockLight() {
        return blockLight;
    }

    @Override
    public boolean isSneaking() {
        return this.sneaking;
    }

    @Override
    public boolean isInvisible() {
        return this.invisible;
    }

    @Override
    public Gamemode getGamemode() {
        return this.gamemode;
    }

    /**
     * API access, only call on server thread!
     */
    public void update() {
        EntityPlayer player = Server.getInstance().getOnlinePlayers().get(uuid);
        if (player == null) return;

        this.gamemode = GAMEMODE_MAP.get(player.getGameType());
        if (this.gamemode == null) this.gamemode = Gamemode.SURVIVAL;

        this.invisible = player.hasEffect(EffectInvisibilityType.INVISIBILITY_TYPE);

        this.name = Text.of(player.getOriginName());

        var location = player.getLocation();
        this.position = new Vector3d(location.x(), location.y(), location.z());
        this.rotation = new Vector3d(location.pitch(), location.yaw(), 0);
        this.sneaking = player.isSneaking();

        // TODO
//        this.skyLight = player.getLocation().getBlock().getLightFromSky();
//        this.blockLight = player.getLocation().getBlock().getLightFromBlocks();

        this.world = AllayPlugin.getInstance().getServerWorld(player.getDimension());
    }
}
