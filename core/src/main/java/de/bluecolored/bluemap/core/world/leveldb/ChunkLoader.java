package de.bluecolored.bluemap.core.world.leveldb;

import java.io.IOException;

/**
 * 负责从LevelDB数据库加载区块数据
 */
public interface ChunkLoader<T> {

    /**
     * 从LevelDB数据库加载指定坐标的区块
     *
     * @param x 区块X坐标
     * @param z 区块Z坐标
     * @return 加载的区块
     * @throws IOException 如果加载过程中发生I/O错误
     */
    T loadChunk(int x, int z) throws IOException;

    /**
     * 返回一个表示错误区块的对象
     *
     * @return 错误区块
     */
    T erroredChunk();

}