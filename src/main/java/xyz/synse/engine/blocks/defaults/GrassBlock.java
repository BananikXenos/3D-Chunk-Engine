package xyz.synse.engine.blocks.defaults;

import xyz.synse.engine.blocks.Block;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GrassBlock extends Block {
    private float growth;
    public GrassBlock(float growth) {
        super("grass");
        this.growth = growth;
    }

    public GrassBlock() {
        super("grass");
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeFloat(growth);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        growth = in.readFloat();
    }
}
