package xyz.synse.engine.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface DataHolder {
    void read(DataInputStream in) throws IOException;
    void write(DataOutputStream out) throws IOException;
}
