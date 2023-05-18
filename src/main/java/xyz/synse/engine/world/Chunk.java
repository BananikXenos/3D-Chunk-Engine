package xyz.synse.engine.world;

import lombok.Getter;
import xyz.synse.engine.blocks.Block;

public class Chunk {
    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 256;
    public static final int CHUNK_SIZE_Z = 16;
    @Getter
    private final int x;
    @Getter
    private final int z;
    @Getter
    private final long timestamp;

    private Block[][][] blocks = new Block[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];

    public Chunk(int x, int z, long timestamp) {
        this.x = x;
        this.z = z;
        this.timestamp = timestamp;
    }

    public Chunk(int x, int z, long timestamp, Block[][][] blocks) {
        this.x = x;
        this.z = z;
        this.timestamp = timestamp;
        this.blocks = blocks;
    }

    public Block getBlock(int x, int y, int z) {
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, Block block) {
        blocks[x][y][z] = block;
    }
}
