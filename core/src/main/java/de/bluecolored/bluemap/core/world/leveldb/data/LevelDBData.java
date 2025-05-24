package de.bluecolored.bluemap.core.world.leveldb.data;

import de.bluecolored.bluemap.core.world.DimensionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cloudburstmc.nbt.NbtMap;

import java.util.HashMap;
import java.util.Map;

public class LevelDBData {

    @Getter
    private LevelDBData.Data data = new LevelDBData.Data();

    public LevelDBData(NbtMap nbtMap) {
        data.levelName = nbtMap.getString("LevelName");
        data.spawnX = nbtMap.getInt("SpawnX");
        data.spawnY = nbtMap.getInt("SpawnY");
        data.spawnZ = nbtMap.getInt("SpawnZ");
//        data.worldGenSettings.dimensions.put();
    }


    @Getter
    public static class Data {

        private String levelName = "world";

        private int spawnX = 0;

        private int spawnY = 0;

        private int spawnZ = 0;

        private LevelDBData.WGSettings worldGenSettings = new LevelDBData.WGSettings();

    }

    @Getter
    public static class WGSettings {
        private Map<String, LevelDBData.Dimension> dimensions = new HashMap<>();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dimension {
        private DimensionType type = DimensionType.OVERWORLD;
    }

}
