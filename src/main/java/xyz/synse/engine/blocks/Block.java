package xyz.synse.engine.blocks;

import xyz.synse.engine.data.DataHolder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Block implements DataHolder {
    private final String name;

    public Block(String name) {
        this.name = name;
    }

    @Override
    public void read(DataInputStream in) throws IOException {

    }

    @Override
    public void write(DataOutputStream out) throws IOException {

    }

    public String getName() {
        return name;
    }
}
