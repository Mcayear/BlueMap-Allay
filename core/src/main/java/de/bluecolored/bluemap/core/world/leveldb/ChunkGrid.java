package de.bluecolored.bluemap.core.world.leveldb;

import com.flowpowered.math.vector.Vector2i;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.Vector2iCache;
import de.bluecolored.bluemap.core.world.Region;

import lombok.RequiredArgsConstructor;
import org.iq80.leveldb.DB;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class ChunkGrid<T> {
    // 区块网格默认大小为16
    private static final Grid CHUNK_GRID = new Grid(16);
    // LevelDB中区域大小可能与MCA不同，这里假设是16x16的区块组成一个区域
    private static final Grid REGION_GRID = new Grid(16).multiply(CHUNK_GRID);

    private static final Vector2iCache VECTOR_2_I_CACHE = new Vector2iCache();

    private final ChunkLoader<T> chunkLoader;
    private final DB db;

    private final LoadingCache<Vector2i, Region<T>> regionCache = Caffeine.newBuilder()
            .executor(BlueMap.THREAD_POOL)
            .softValues()
            .maximumSize(32)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(this::loadRegion);
    
    private final LoadingCache<Vector2i, T> chunkCache = Caffeine.newBuilder()
            .executor(BlueMap.THREAD_POOL)
            .softValues()
            .maximumSize(10240) // 缓存10个区域的区块
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(this::loadChunk);

    public Grid getChunkGrid() {
        return CHUNK_GRID;
    }

    public Grid getRegionGrid() {
        return REGION_GRID;
    }

    public T getChunk(int x, int z) {
        return getChunk(VECTOR_2_I_CACHE.get(x, z));
    }

    private T getChunk(Vector2i pos) {
        return chunkCache.get(pos);
    }

    public Region<T> getRegion(int x, int z) {
        return getRegion(VECTOR_2_I_CACHE.get(x, z));
    }

    private Region<T> getRegion(Vector2i pos) {
        return regionCache.get(pos);
    }

    public void preloadRegionChunks(int x, int z, Predicate<Vector2i> chunkFilter) {
        try {
            Region<T> region = getRegion(x, z);
            // 预加载区域内的区块
            // 具体实现取决于LevelDBRegion的实现
        } catch (Exception ex) {
            Logger.global.logDebug("Unexpected exception trying to preload region (x:%d, z:%d): %s".formatted(x, z, ex));
        }
    }

    public Collection<Vector2i> listRegions() {
        // LevelDB中可能没有明确的区域文件
        // 这里需要根据LevelDB的特点实现区域列表
        return Collections.emptyList();
    }

    public void invalidateChunkCache() {
        regionCache.invalidateAll();
        chunkCache.invalidateAll();
    }

    public void invalidateChunkCache(int x, int z) {
        // LevelDB中区域大小可能不同于MCA
        regionCache.invalidate(VECTOR_2_I_CACHE.get(x >> 4, z >> 4));
        chunkCache.invalidate(VECTOR_2_I_CACHE.get(x, z));
    }

    private Region<T> loadRegion(Vector2i regionPos) {
        return loadRegion(regionPos.getX(), regionPos.getY());
    }

    private Region<T> loadRegion(int x, int z) {
        // 创建一个LevelDB区域实现
        return new LevelDBRegion<>(chunkLoader, db, x, z);
    }

    private T loadChunk(Vector2i chunkPos) {
        return loadChunk(chunkPos.getX(), chunkPos.getY());
    }

    private T loadChunk(int x, int z) {
        final int tries = 3;
        final int tryInterval = 1000;

        Exception loadException = null;
        for (int i = 0; i < tries; i++) {
            try {
                // LevelDB中区域大小可能不同于MCA
                return getRegion(x >> 4, z >> 4)
                        .loadChunk(x, z);
            } catch (IOException | RuntimeException e) {
                if (loadException != null && loadException != e)
                    e.addSuppressed(loadException);

                loadException = e;

                if (i + 1 < tries) {
                    try {
                        Thread.sleep(tryInterval);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        Logger.global.logDebug("Unexpected exception trying to load chunk (x:%d, z:%d): %s".formatted(x, z, loadException));
        return chunkLoader.erroredChunk();
    }
}