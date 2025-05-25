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
import org.iq80.leveldb.DB;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
@ToString
public class LevelDBWorld implements World {

    private final String id;
    private static final String FILE_LEVEL_DAT = "level.dat";
    private static final String DIR_DB = "db_";

    private final DimensionType dimensionType;
    private final Vector3i spawnPoint;
    private final LevelDBData levelData;
    private final DB db;
    private final ChunkGrid<Chunk> chunkGrid;

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
        updateDbDirectory(worldFolder);
        var dbFolder = worldFolder.resolve(DIR_DB).toFile();
        try {
            if (!dbFolder.exists() && !dbFolder.mkdirs()) {
                throw new WorldStorageException("Failed to create world database directory!");
            }
            org.iq80.leveldb.Options options = new org.iq80.leveldb.Options()
                    .createIfMissing(false)
                    .compressionType(org.iq80.leveldb.CompressionType.ZLIB_RAW).blockSize(64 * 1024);
            this.db = new Iq80DBFactory().open(dbFolder, options);
            this.chunkGrid = new ChunkGrid<>(new LevelDBChunkLoader(), this.db);
        } catch (IOException e) {
            throw new WorldStorageException(e);
        }
    }

    @Override
    public String getName() {
        return levelData.getData().getLevelName();
    }

    @Override
    public Grid getChunkGrid() {
        return chunkGrid.getChunkGrid();
    }

    @Override
    public Grid getRegionGrid() {
        return chunkGrid.getRegionGrid();
    }

    /**
     * Returns the {@link Chunk} on the specified block-position
     *
     * @param x
     * @param z
     */
    @Override
    public Chunk getChunkAtBlock(int x, int z) {
        return getChunk(x >> 4, z >> 4);
    }

    /**
     * Returns the {@link Chunk} on the specified chunk-position
     *
     * @param x
     * @param z
     */
    @Override
    public Chunk getChunk(int x, int z) {
        return chunkGrid.getChunk(x, z);
    }

    /**
     * Returns the {@link Region} on the specified region-position
     *
     * @param x
     * @param z
     */
    @Override
    public Region<Chunk> getRegion(int x, int z) {
        return chunkGrid.getRegion(x, z);
    }

    /**
     * Returns a collection of all regions in this world.
     * <i>(Be aware that the collection is not cached and recollected each time from the world-files!)</i>
     */
    @Override
    public Collection<Vector2i> listRegions() {
        return chunkGrid.listRegions();
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
        chunkGrid.preloadRegionChunks(x, z, chunkFilter);
    }

    /**
     * Invalidates the complete chunk cache (if there is a cache), so that every chunk has to be reloaded from disk
     */
    @Override
    public void invalidateChunkCache() {
        chunkGrid.invalidateChunkCache();
    }

    /**
     * Invalidates the chunk from the chunk-cache (if there is a cache), so that the chunk has to be reloaded from disk
     *
     * @param x
     * @param z
     */
    @Override
    public void invalidateChunkCache(int x, int z) {
        chunkGrid.invalidateChunkCache(x, z);
    }

    @Override
    public void iterateEntities(int minX, int minZ, int maxX, int maxZ, Consumer<Entity> entityConsumer) {
        // 实现实体迭代，需要从LevelDB读取实体数据
        // 这可能需要特定的LevelDB格式知识
    }

    public static LevelDBWorld load(Path worldFolder, Key dimension) {
        // load level.dat
        var levelDat = worldFolder.resolve(FILE_LEVEL_DAT).toFile();
        if (!levelDat.exists()) {
            throw new WorldStorageException("level.dat not found!");
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
        } catch (IOException e) {
            throw new WorldStorageException(e);
        }

        // create world
        return new LevelDBWorld(worldFolder, dimension, levelData);
    }
    
    /**
     * LevelDB世界的区块加载器实现
     */
    private class LevelDBChunkLoader implements ChunkLoader<Chunk> {
        
        // LevelDB键类型前缀
        private static final byte CHUNK_DATA_PREFIX = 47;
        
        /**
         * 创建区块数据键
         */
        private byte[] createChunkKey(int x, int z) {
            byte[] key = new byte[9];
            key[0] = CHUNK_DATA_PREFIX;
            
            // 写入X坐标（小端序）
            key[1] = (byte) (x & 0xFF);
            key[2] = (byte) ((x >> 8) & 0xFF);
            key[3] = (byte) ((x >> 16) & 0xFF);
            key[4] = (byte) ((x >> 24) & 0xFF);
            
            // 写入Z坐标（小端序）
            key[5] = (byte) (z & 0xFF);
            key[6] = (byte) ((z >> 8) & 0xFF);
            key[7] = (byte) ((z >> 16) & 0xFF);
            key[8] = (byte) ((z >> 24) & 0xFF);
            
            return key;
        }
        
        @Override
        public Chunk loadChunk(int x, int z) throws IOException {
            // 从LevelDB中读取区块数据
            byte[] chunkKey = createChunkKey(x, z);
            byte[] chunkData = db.get(chunkKey);
            
            if (chunkData == null) {
                // 区块不存在
                return erroredChunk();
            }
            
            // 创建区块对象并解析数据
            return new LevelDBChunk(x, z, db);
        }
        
        @Override
        public Chunk erroredChunk() {
            // 返回一个表示错误的区块
            return new LevelDBErrorChunk();
        }
    }

    private void updateDbDirectory(Path worldFolder) {
        var sourceDb = worldFolder.resolve("db");
        var targetDb = worldFolder.resolve("db_");

        try {
            if (!Files.exists(sourceDb) || !Files.isDirectory(sourceDb)) {
                return;
            }

            // delete directory
            if (Files.exists(targetDb)) {
                try (var walk = Files.walk(targetDb)) {
                    walk.sorted(Comparator.reverseOrder())
                            .parallel()
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    throw new UncheckedIOException("Failed to delete: " + path, e);
                                }
                            });
                }
            }

            // copy directory
            try (var walk = Files.walk(sourceDb)) {
                walk.parallel()
                        .forEach(source -> {
                            var target = targetDb.resolve(sourceDb.relativize(source));
                            try {
                                if (Files.isDirectory(source)) {
                                    Files.createDirectories(target);
                                } else {
                                    Files.createDirectories(target.getParent());
                                    Files.copy(source, target,
                                            StandardCopyOption.REPLACE_EXISTING,
                                            StandardCopyOption.COPY_ATTRIBUTES);
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException("Failed to copy: " + source, e);
                            }
                        });
            }

        } catch (IOException | UncheckedIOException e) {
            throw new WorldStorageException("Failed to copy database directory", e);
        }
    }
}
