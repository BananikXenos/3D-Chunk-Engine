package xyz.synse.engine.world;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class World {
    private final File directory;
    private final Map<Long, Region> regions = new HashMap<>();

    public World(File directory) {
        this.directory = directory;
    }

    public World(Path directory) {
        this.directory = directory.toFile();
    }

    public World(String directory) {
        this.directory = new File(directory);
    }

    public Region getRegion(int x, int z) {
        long key = getRegionKey(x, z);
        return regions.get(key);
    }

    public boolean isRegionLoaded(int x, int z) {
        return getRegion(x, z) != null;
    }

    public void tryLoadRegion(int x, int z) {
        getOrCreateRegion(x, z);
    }

    public void reloadRegion(int x, int z) {
        regions.remove(getRegionKey(x, z));
        tryLoadRegion(x, z);
    }

    private List<Region> allRegions() {
        return new ArrayList<>(regions.values());
    }

    public void save() {
        allRegions().forEach(region -> {
            try {
                region.save(this.directory);
            } catch (InterruptedException | IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private synchronized void getOrCreateRegion(int x, int z) {
        regions.computeIfAbsent(getRegionKey(x, z), key -> {
            Region region = new Region(x, z);
            region.load(this.directory);
            return region;
        });
    }

    private long getRegionKey(int regionX, int regionZ) {
        return (long) regionX & 0xFFFFFFFFL | ((long) regionZ & 0xFFFFFFFFL) << 32;
    }
}
