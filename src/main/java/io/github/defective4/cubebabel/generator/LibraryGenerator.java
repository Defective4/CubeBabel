package io.github.defective4.cubebabel.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

public class LibraryGenerator implements Generator {

    private static final int GROUND_LEVEL = 41;

    @Override
    public void generate(GenerationUnit unit) {
        unit.modifier().fillHeight(0, GROUND_LEVEL - 1, Block.DIRT);
        unit.modifier().fillHeight(40, GROUND_LEVEL, Block.GRASS_BLOCK);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockVec start = unit.absoluteStart();
                start = start.add(i * 16, 0, j * 16);
                int chunkX = start.chunkX();
                int chunkZ = start.chunkZ();
                if (chunkX < 0 || chunkZ < 0) {
                    continue;
                }

                int offsetX = Math.ceilDiv(start.blockX(), 11) * 11 % 16;

                for (int x = offsetX; x < 16; x += 11) {
                    int offsetZ = Math.ceilDiv(start.blockZ(), 14) * 14 % 16;
                    offsetZ += (x + start.blockX()) / 11 * 7;
                    offsetZ %= 14;
                    for (int z = offsetZ; z < 16; z += 14) {
                        placeHexagon(x + start.blockX(), z + start.blockZ(), unit);
                    }
                }
            }
        }
    }

    private static void placeHexagon(int x, int z, GenerationUnit unit) {
        BlockVec start = unit.absoluteStart();

        Map<BlockVec, Block> blocks = new HashMap<>();
        BlockVec vec;

        for (int v = 0; v < 1; v++) {
            int y = GROUND_LEVEL + v;
            for (int h = -2; h <= 2; h++) {
                vec = new BlockVec(x + h, y, z - 6);
                blocks.put(vec, Block.OAK_PLANKS);
                vec = new BlockVec(x + h, y, z + 6);
                blocks.put(vec, Block.OAK_PLANKS);
                blocks.put(vec.add(0, 0, 1), Block.OAK_PLANKS);
            }

            for (int d = 0; d <= 5; d++) {
                // north-east
                vec = new BlockVec(x + 3 + d, y, z - 5 + d);
                blocks.put(vec, Block.OAK_PLANKS);
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        blocks.put(vec.add(i, 0, -j), Block.OAK_PLANKS);
                    }
                }
                // north-west
                vec = new BlockVec(x - 3 - d, y, z - 5 + d);
                blocks.put(vec, Block.OAK_PLANKS);
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        blocks.put(vec.add(-i, 0, -j), Block.OAK_PLANKS);
                    }
                }
                // south-east
                vec = new BlockVec(x + 3 + d, y, z + 5 - d);
                blocks.put(vec, Block.OAK_PLANKS);
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        blocks.put(vec.add(i, 0, j), Block.OAK_PLANKS);
                    }
                }
                // south-west
                vec = new BlockVec(x - 3 - d, y, z + 5 - d);
                blocks.put(vec, Block.OAK_PLANKS);
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        blocks.put(vec.add(-i, 0, j), Block.OAK_PLANKS);
                        blocks.put(vec, Block.OAK_PLANKS);
                    }
                }
            }
        }

        for (Entry<BlockVec, Block> entry : blocks.entrySet()) {
            BlockVec vector = entry.getKey();
            if (vector.sameChunk(start)) {
                unit.modifier().setBlock(vector, entry.getValue());
            }
        }
    }

}
