package xyz.synse.engine.world;

import lombok.Getter;
import me.tongfei.progressbar.ProgressBar;
import xyz.synse.engine.data.Compression;
import xyz.synse.engine.blocks.Block;
import xyz.synse.engine.blocks.Blocks;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

public class Region {
    public static final int CHUNKS_SIZE = 32;
    @Getter
    private final int x;
    @Getter
    private final int z;
    private final Chunk[][] chunks = new Chunk[CHUNKS_SIZE][CHUNKS_SIZE];
    private boolean hasUnsavedChanges = false;

    public Region(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public void updateChunk(int x, int z, Chunk chunk) {
        chunks[x][z] = chunk;
        hasUnsavedChanges = true;
    }

    public Chunk getChunk(int x, int z) {
        return chunks[x][z];
    }

    public void load(File directory) {
        File regionFile = getRegionFile(directory, x, z);
        if (!regionFile.exists()) {
            return;
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(regionFile, "r")) {
            // Read the positions header
            randomAccessFile.seek(0);
            int[] positionsHeader = new int[1024]; // Assuming 1024 ints

            for (int i = 0; i < positionsHeader.length; i++) {
                positionsHeader[i] = randomAccessFile.readInt();
            }

            // Load the chunks
            ProgressBar pb = new ProgressBar("Loading chunks", (Region.CHUNKS_SIZE * Region.CHUNKS_SIZE));
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            for (int position : positionsHeader) {
                executor.execute(() -> {
                    try {
                        byte[] chunkData;
                        synchronized (randomAccessFile) {
                            randomAccessFile.seek(position);

                            int dataLength = randomAccessFile.readInt();
                            chunkData = new byte[dataLength];
                            randomAccessFile.read(chunkData);
                        }


                        byte[] decompressedData = Compression.decompress(chunkData, true);

                        deserializeChunk(decompressedData);
                    } catch (IOException | InvocationTargetException | NoSuchMethodException |
                             InstantiationException | IllegalAccessException | DataFormatException e) {
                        e.printStackTrace();
                        System.out.println(position);
                    }
                    pb.step();
                });
            }

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            pb.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.gc();
    }


    private void deserializeChunk(final byte[] chunkData) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(chunkData);
             DataInputStream in = new DataInputStream(byteIn)) {

            int x = in.readInt();
            int z = in.readInt();

            long timestamp = in.readLong();

            // Create a new chunk and populate it with data
            Chunk chunk = new Chunk(x, z, timestamp, new Block[Chunk.CHUNK_SIZE_X][Chunk.CHUNK_SIZE_Y][Chunk.CHUNK_SIZE_Z]);

            while (in.available() > 0) {
                long combined = in.readLong();

                int bX = (int) (combined >> 42);
                int bY = (int) ((combined >> 21) & 0x1FFFFF);
                int bZ = (int) (combined & 0x1FFFFF);

                String blockName = in.readUTF();

                Block block = Blocks.createBlock(blockName);
                block.read(in);

                chunk.setBlock(bX, bY, bZ, block);
            }

            updateChunk(chunk.getX(), chunk.getZ(), chunk);
        }
    }

    public void save(File directory) throws InterruptedException, IOException {
        if (!hasUnsavedChanges) return;

        if (!directory.exists())
            directory.mkdirs();

        ProgressBar pb = new ProgressBar("Saving chunks", (Region.CHUNKS_SIZE * Region.CHUNKS_SIZE));
        ExecutorService executor = Executors.newFixedThreadPool(12);
        List<byte[]> serializedChunks = new ArrayList<>();

        for (int i = 0; i < CHUNKS_SIZE * CHUNKS_SIZE; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    int chunkX = finalI % CHUNKS_SIZE;
                    int chunkZ = finalI / CHUNKS_SIZE;

                    byte[] chunkData = Compression.compress(serializeChunk(chunkX, chunkZ), Deflater.BEST_SPEED, true);

                    synchronized (serializedChunks) {
                        serializedChunks.add(chunkData);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }

        executor.shutdown();

        File regionFile = getRegionFile(directory, x, z);
        if (!regionFile.exists())
            regionFile.createNewFile();

        ExecutorService writingExecutor = Executors.newFixedThreadPool(6);
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(regionFile, "rw")) {
            AtomicLong currentPosition = new AtomicLong(0); // Start writing position after the reserved space for positions
            AtomicLong currentChunk = new AtomicLong(0);

            // Reserve space for the positions header
            randomAccessFile.seek(0);
            byte[] positionsHeader = new byte[1024 * Integer.BYTES]; // Assuming 1024 ints, each int occupies 4 bytes
            randomAccessFile.write(positionsHeader);
            currentPosition.addAndGet(positionsHeader.length);

            while (!executor.isTerminated() || !serializedChunks.isEmpty()) {
                if (serializedChunks.isEmpty()) {
                    continue;
                }

                byte[] data = serializedChunks.remove(0);

                if (data == null) {
                    System.out.println("Empty chunk");
                    pb.step();
                    continue;
                }

                // Execute a write operation in a separate thread
                writingExecutor.execute(() -> {
                    try {
                        // Acquire a lock for writing to the file
                        synchronized (randomAccessFile) {
                            // Write the data length and data to the file
                            long finalCurrentPosition = currentPosition.getAndAdd(Integer.BYTES + data.length);
                            randomAccessFile.seek(finalCurrentPosition);
                            randomAccessFile.writeInt(data.length);
                            randomAccessFile.write(data);

                            // Update the position in the positions header
                            int finalCurrentChunk = (int) currentChunk.getAndIncrement();
                            randomAccessFile.seek((long) finalCurrentChunk * Integer.BYTES); // Write the position in the header
                            randomAccessFile.writeInt((int) finalCurrentPosition);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    pb.step();
                });
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            writingExecutor.shutdown();
            writingExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            // Truncate
            long fileSize = currentPosition.get();
            randomAccessFile.setLength(fileSize);

            pb.close();

            hasUnsavedChanges = false;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.gc();
    }

    private byte[] serializeChunk(int x, int z) throws IOException {
        Chunk chunk = chunks[x][z];

        if (chunk == null) return null;

        try (
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteOut);
        ) {
            out.writeInt(x);
            out.writeInt(z);

            out.writeLong(chunk.getTimestamp());

            for (int bX = 0; bX < Chunk.CHUNK_SIZE_X; bX++) {
                for (int bY = 0; bY < Chunk.CHUNK_SIZE_Y; bY++) {
                    for (int bZ = 0; bZ < Chunk.CHUNK_SIZE_X; bZ++) {
                        Block block = chunk.getBlock(bX, bY, bZ);
                        if (block == null) continue;

                        long combined = ((long) bX << 42) | ((long) bY << 21) | bZ;
                        out.writeLong(combined);

                        out.writeUTF(block.getName());

                        block.write(out);
                    }
                }
            }
            return byteOut.toByteArray();
        }
    }

    private static File getRegionFile(File directory, int x, int z) {
        return new File(directory, "r." + x + "." + z + ".rg");
    }
}
