package de.bluecolored.bluemap.allay;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.core.world.World;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.math.location.Location3fc;

import java.util.Optional;

/**
 * BlueMap Project 2024/7/6
 *
 * @author daoge_cmd
 */
public class AllayCommandSource implements CommandSource {

    private final Plugin plugin;
    private final CommandSender delegate;

    public AllayCommandSource(Plugin plugin, CommandSender delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
    }


    @Override
    public void sendMessage(Text text) {
        delegate.sendText(text.toPlainString());
    }

    @Override
    public boolean hasPermission(String permission) {
        return delegate.hasPerm(permission);
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
    public Optional<World> getWorld() {
        var location = getLocation();

        if (location != null) {
            var serverWorld = AllayPlugin.getInstance().getServerWorld(location.dimension());
            return Optional.ofNullable(plugin.getWorld(serverWorld));
        }

        return Optional.empty();
    }

    private Location3fc getLocation() {
        Location3fc location = null;
        // Return non-null value only if the sender is an entity
        if (delegate.isEntity()) location = delegate.getCmdExecuteLocation();
        return location;
    }
}
