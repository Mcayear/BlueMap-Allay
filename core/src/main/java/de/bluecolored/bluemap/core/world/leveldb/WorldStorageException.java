package de.bluecolored.bluemap.core.world.leveldb;

/**
 * 表示LevelDB世界存储相关的异常
 */
public class WorldStorageException extends RuntimeException {

    public WorldStorageException(String message) {
        super(message);
    }

    public WorldStorageException(Throwable cause) {
        super(cause);
    }

    public WorldStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
