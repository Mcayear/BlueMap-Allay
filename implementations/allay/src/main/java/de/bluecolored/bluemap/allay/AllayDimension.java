package de.bluecolored.bluemap.allay;

import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.util.Key;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.storage.NativeFileWorldStorage;

import java.lang.ref.WeakReference;
import java.nio.file.Path;

/**
 * BlueMap Project 2024/7/6
 *
 * @author daoge_cmd
 */
public class AllayDimension implements ServerWorld {
    private final WeakReference<Dimension> delegate;
    private final Path worldFolder;
    private final Key dimension;

    public AllayDimension(Dimension delegate) {
        this.delegate = new WeakReference<>(delegate);
        this.dimension = switch(delegate.getDimensionInfo().dimensionId()) {
            case 0 -> DataPack.DIMENSION_OVERWORLD;
            case 1 -> DataPack.DIMENSION_THE_NETHER;
            case 2 -> DataPack.DIMENSION_THE_END;
            default -> throw new IllegalStateException("Unexpected value: " + delegate.getDimensionInfo().dimensionId());
        };
        var storage = delegate.getWorld().getWorldStorage();
        if (storage instanceof NativeFileWorldStorage fileStorage) {
            this.worldFolder = fileStorage.getWorldFolderPath();
        } else {
            this.worldFolder = Path.of("");
        }
    }

    @Override
    public boolean persistWorldChanges() {
        // TODO
        return false;
    }

    @Override
    public Path getWorldFolder() {
        return worldFolder;
    }

    @Override
    public Key getDimension() {
        return dimension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        var that = (AllayDimension) o;
        Object world = delegate.get();
        return world != null && world.equals(that.delegate.get());
    }

    @Override
    public int hashCode() {
        Object world = delegate.get();
        return world != null ? world.hashCode() : 0;
    }
}
