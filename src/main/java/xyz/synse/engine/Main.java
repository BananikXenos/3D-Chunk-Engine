package xyz.synse.engine;

import me.tongfei.progressbar.ProgressBar;
import xyz.synse.engine.blocks.Blocks;
import xyz.synse.engine.blocks.defaults.GrassBlock;
import xyz.synse.engine.world.Chunk;
import xyz.synse.engine.world.Region;
import xyz.synse.engine.world.World;

import java.util.Random;

public class Main {

    public static void main(String[] args) {
        World world = new World("world");

        // Load region at coordinates (0, 0)
        world.tryLoadRegion(0, 0);
        Region region = world.getRegion(0, 0);

        // Generate missing chunks in the region
        generateMissingChunks(region);

        // Save the world
        world.save();
    }

    public static void generateMissingChunks(Region region) {
        // Count the number of null chunks
        int nullChunks = getNullChunks(region);

        // Create a progress bar to track chunk generation
        try (ProgressBar pb = new ProgressBar("Generating chunks", nullChunks)) {
            // Iterate over the chunks in the region
            for (int x = 0; x < Region.CHUNKS_SIZE; x++) {
                for (int z = 0; z < Region.CHUNKS_SIZE; z++) {
                    // Check if the chunk already exists
                    Chunk chunk = region.getChunk(x, z);
                    if (chunk != null) {
                        pb.step();
                        continue;
                    }

                    // Generate a new chunk and update the region
                    chunk = createChunk(x, z);
                    region.updateChunk(x, z, chunk);
                    pb.step();
                }
            }
        }
    }

    public static int getNullChunks(Region region) {
        int nullChunks = 0;
        // Iterate over the chunks in the region
        for (int x = 0; x < Region.CHUNKS_SIZE; x++) {
            for (int z = 0; z < Region.CHUNKS_SIZE; z++) {
                // Check if the chunk is null
                Chunk chunk = region.getChunk(x, z);
                if (chunk == null) {
                    nullChunks++;
                }
            }
        }
        return nullChunks;
    }

    public static Chunk createChunk(int x, int z) {
        Chunk chunk = new Chunk(x, z, System.currentTimeMillis());
        // Iterate over the blocks in the chunk
        for (int bX = 0; bX < Chunk.CHUNK_SIZE_X; bX++) {
            for (int bY = 0; bY < Chunk.CHUNK_SIZE_Y; bY++) {
                for (int bZ = 0; bZ < Chunk.CHUNK_SIZE_X; bZ++) {
                    // Set block types based on position
                    if (bY <= 5) {
                        chunk.setBlock(bX, bY, bZ, Blocks.BEDROCK);
                    } else {
                        boolean randomBlock = new Random().nextBoolean();
                        chunk.setBlock(bX, bY, bZ, randomBlock ? new GrassBlock(new Random().nextFloat()) : Blocks.STONE);
                    }
                }
            }
        }
        return chunk;
    }
}
