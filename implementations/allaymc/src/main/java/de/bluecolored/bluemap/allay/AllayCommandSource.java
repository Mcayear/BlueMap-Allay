package de.bluecolored.bluemap.allay;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.math.location.Location3dc;

import java.util.Optional;

/**
 * BlueMap Project 2024/7/6
 *
 * @author daoge_cmd
 */
public class AllayCommandSource implements CommandSource {

    private final CommandSender delegate;

    public AllayCommandSource(CommandSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public void sendMessage(Component text) {
        delegate.sendText(LegacyComponentSerializer.legacySection().serialize(text));
    }

    @Override
    public boolean hasPermission(String permission) {
        return delegate.hasPermission(permission);
    }

    @Override
    public Optional<Vector3d> getPosition() {
        var location = getLocation();

        if (location != null) {
            return Optional.of(new Vector3d(location.x(), location.y(), location.z()));
        }

        return Optional.empty();
    }

    @Override
    public Optional<ServerWorld> getWorld() {
        var location = getLocation();

        if (location != null) {
            var serverWorld = AllayPlugin.getInstance().getServerWorld(location.dimension());
            return Optional.ofNullable(serverWorld);
        }

        return Optional.empty();
    }

    private Location3dc getLocation() {
        Location3dc location = null;
        // Return non-null value only if the sender is an entity
        if (delegate.isEntity()) location = delegate.getCmdExecuteLocation();
        return location;
    }
}