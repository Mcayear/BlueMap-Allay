package de.bluecolored.bluemap.core.world.leveldb;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.*;
import de.bluecolored.bluemap.core.world.leveldb.data.LevelDBData;
import de.bluecolored.bluemap.core.world.mca.data.LevelData;
import lombok.Getter;
import lombok.ToString;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
@ToString
public class LevelDBWorld implements World {

    private final String id;
    private static final String FILE_LEVEL_DAT = "level.dat";
    private final DimensionType dimensionType;
    private final Vector3i spawnPoint;
    private final LevelDBData levelData;

    public LevelDBWorld(Path worldFolder, Key dimension, LevelDBData levelData) {
        this.id = World.id(worldFolder, dimension);
        this.levelData = levelData;
        // TODO:辨别各个世界(world)的维度类型是什么
        this.dimensionType = new LevelData.Dimension(DimensionType.OVERWORLD).getType();
        this.spawnPoint = new Vector3i(
                levelData.getData().getSpawnX(),
                levelData.getData().getSpawnY(),
                levelData.getData().getSpawnZ()
        );
    }

    @Override
    public String getName() {
        return levelData.getData().getLevelName();
    }

    @Override
    public Grid getChunkGrid() {
        return null;
    }

    @Override
    public Grid getRegionGrid() {
        return null;
    }

    /**
     * Returns the {@link Chunk} on the specified block-position
     *
     * @param x
     * @param z
     */
    @Override
    public Chunk getChunkAtBlock(int x, int z) {
        return null;
    }

    /**
     * Returns the {@link Chunk} on the specified chunk-position
     *
     * @param x
     * @param z
     */
    @Override
    public Chunk getChunk(int x, int z) {
        return null;
    }

    /**
     * Returns the {@link Region} on the specified region-position
     *
     * @param x
     * @param z
     */
    @Override
    public Region<Chunk> getRegion(int x, int z) {
        return null;
    }

    /**
     * Returns a collection of all regions in this world.
     * <i>(Be aware that the collection is not cached and recollected each time from the world-files!)</i>
     */
    @Override
    public Collection<Vector2i> listRegions() {
        return List.of();
    }

    /**
     * Loads the filtered chunks from the specified region into the chunk cache (if there is a cache)
     *
     * @param x
     * @param z
     * @param chunkFilter
     */
    @Override
    public void preloadRegionChunks(int x, int z, Predicate<Vector2i> chunkFilter) {

    }

    /**
     * Invalidates the complete chunk cache (if there is a cache), so that every chunk has to be reloaded from disk
     */
    @Override
    public void invalidateChunkCache() {

    }

    /**
     * Invalidates the chunk from the chunk-cache (if there is a cache), so that the chunk has to be reloaded from disk
     *
     * @param x
     * @param z
     */
    @Override
    public void invalidateChunkCache(int x, int z) {

    }

    @Override
    public void iterateEntities(int minX, int minZ, int maxX, int maxZ, Consumer<Entity> entityConsumer) {

    }

    public static LevelDBWorld load(Path worldFolder, Key dimension) throws IOException, InterruptedException {
        // load level.dat
        var levelDat = worldFolder.resolve(FILE_LEVEL_DAT).toFile();
        if (!levelDat.exists()) {
            // 不存在 level.dat
            return null;
        }

        LevelDBData levelData;
        try (var input = new FileInputStream(levelDat)) {
            // current_version + data length
            // noinspection ResultOfMethodCallIgnored
            input.skip(8);
            NBTInputStream readerLE = NbtUtils.createReaderLE(new ByteArrayInputStream(input.readAllBytes()));
            NbtMap nbt = (NbtMap) readerLE.readTag();
            readerLE.close();
            levelData = new LevelDBData(nbt);
        }

        // create world
        return new LevelDBWorld(worldFolder, dimension, levelData);
    }
}
