package io.github.defective4.cubebabel.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

public class LibraryGenerator implements Generator {

    @Override
    public void generate(GenerationUnit unit) {
        unit.modifier().fillHeight(0, 40, Block.DIRT);
        unit.modifier().fillHeight(40, 41, Block.GRASS_BLOCK);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockVec start = unit.absoluteStart();
                start = start.add(i * 16, 0, j * 16);
                int chunkX = start.chunkX();
                int chunkZ = start.chunkZ();
                if (chunkX < 0 || chunkZ < 0) return;

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
        if (new BlockVec(x, 40, z).sameChunk(start)) {
            unit.modifier().setBlock(x, 42, z, Block.DIAMOND_BLOCK);
        }

        Map<BlockVec, Block> blocks = new HashMap<>();
        BlockVec vec;

        for (int h = -2; h <= 2; h++) {
            vec = new BlockVec(x + h, 41, z - 6);
            blocks.put(vec, Block.OAK_PLANKS);
            vec = new BlockVec(x + h, 41, z + 6);
            blocks.put(vec, Block.OAK_PLANKS);
        }

        for (int d = 0; d <= 5; d++) {
            vec = new BlockVec(x + 3 + d, 41, z - 5 + d);
            blocks.put(vec, Block.OAK_PLANKS); // north-east
            vec = new BlockVec(x - 3 - d, 41, z - 5 + d);
            blocks.put(vec, Block.OAK_PLANKS); // north-west
            vec = new BlockVec(x + 3 + d, 41, z + 5 - d);
            blocks.put(vec, Block.OAK_PLANKS); // south-east
            vec = new BlockVec(x - 3 - d, 41, z + 5 - d);
            blocks.put(vec, Block.OAK_PLANKS); // south-west
        }

        for (Entry<BlockVec, Block> entry : blocks.entrySet()) {
            BlockVec vector = entry.getKey();
            if (vector.sameChunk(start)) {
                unit.modifier().setBlock(vector, entry.getValue());
            }
        }
    }

}
