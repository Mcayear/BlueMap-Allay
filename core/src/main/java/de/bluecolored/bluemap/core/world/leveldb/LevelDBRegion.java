package de.bluecolored.bluemap.core.world.leveldb;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.world.ChunkConsumer;
import de.bluecolored.bluemap.core.world.Region;
import de.bluecolored.bluemap.core.logger.Logger;
import org.iq80.leveldb.DB;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

/**
 * LevelDB区域实现
 * 在LevelDB格式中，区域是逻辑上的概念，实际存储在单个数据库中
 */
public class LevelDBRegion<T> implements Region<T> {

    // 区域坐标
    private final int regionX;
    private final int regionZ;
    
    // 每个区域包含的区块数量（边长）
    private static final int REGION_SIZE = 16;
    
    private final ChunkLoader<T> chunkLoader;
    private final DB db;
    
    // 缓存区域内已知存在的区块坐标
    private Set<Vector2i> existingChunks;
    
    // LevelDB键类型
    private enum LevelDBKey {
        // 区块数据前缀 (47)
        CHUNK_DATA((byte) 47),
        // 区块版本 (118)
        VERSION((byte) 118);
        
        private final byte prefix;
        
        LevelDBKey(byte prefix) {
            this.prefix = prefix;
        }
        
        public byte[] createKey(int chunkX, int chunkZ) {
            ByteBuffer buffer = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(prefix);
            buffer.putInt(chunkX);
            buffer.putInt(chunkZ);
            return buffer.array();
        }
    }
    
    public LevelDBRegion(ChunkLoader<T> chunkLoader, DB db, int regionX, int regionZ) {
        this.chunkLoader = chunkLoader;
        this.db = db;
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.existingChunks = new HashSet<>();
        
        // 这里可以从LevelDB中预先检测区域内的区块
        // 实际实现可能需要读取LevelDB的索引信息
    }
    
    @Override
    public T loadChunk(int chunkX, int chunkZ) throws IOException {
        // 调用ChunkLoader加载指定区块
        return chunkLoader.loadChunk(chunkX, chunkZ);
    }
    
    @Override
    public T emptyChunk() {
        // 返回一个空的区块实例
        return chunkLoader.erroredChunk(); // 使用错误区块作为空区块
    }
    
    @Override
    public void iterateAllChunks(ChunkConsumer<T> consumer) throws IOException {
        // 遍历区域内所有区块
        // 在LevelDB中，需要找到该区域范围内的所有区块
        
        int startX = regionX * REGION_SIZE;
        int startZ = regionZ * REGION_SIZE;
        int endX = startX + REGION_SIZE;
        int endZ = startZ + REGION_SIZE;
        
        for (int z = startZ; z < endZ; z++) {
            for (int x = startX; x < endX; x++) {
                Vector2i chunkPos = new Vector2i(x, z);
                
                // 检查区块是否存在
                // 这是一个简化的实现，实际上需要从LevelDB中检查
                if (existingChunks.contains(chunkPos) || isChunkExistInDB(x, z)) {
                    // 如果区块存在并且通过过滤器
                    if (consumer.filter(x, z, 0)) {
                        try {
                            T chunk = loadChunk(x, z);
                            consumer.accept(x, z, chunk);
                        } catch (IOException e) {
                            consumer.fail(x, z, e);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 检查LevelDB中是否存在指定坐标的区块
     * 
     * @param x 区块X坐标
     * @param z 区块Z坐标
     * @return 如果区块存在则返回true
     */
    private boolean isChunkExistInDB(int x, int z) {
        // 检查区块数据是否存在
        try {
            // 首先检查区块版本数据是否存在
            byte[] versionKey = LevelDBKey.VERSION.createKey(x, z);
            byte[] versionValue = db.get(versionKey);
            
            if (versionValue != null) {
                // 版本存在，说明区块数据应该存在
                existingChunks.add(new Vector2i(x, z));
                return true;
            }
            
            // 如果版本不存在，检查区块数据是否存在
            byte[] dataKey = LevelDBKey.CHUNK_DATA.createKey(x, z);
            byte[] dataValue = db.get(dataKey);
            
            if (dataValue != null) {
                // 区块数据存在
                existingChunks.add(new Vector2i(x, z));
                return true;
            }
            
            return false;
        } catch (Exception e) {
            Logger.global.logDebug("Error checking chunk existence at (%d, %d): %s".formatted(x, z, e.getMessage()));
            return false;
        }
    }
} 