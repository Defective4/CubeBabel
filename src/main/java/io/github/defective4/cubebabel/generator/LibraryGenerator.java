package io.github.defective4.cubebabel.generator;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.Block.Setter;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

public class LibraryGenerator implements Generator {

    @Override
    public void generate(GenerationUnit unit) {
        unit.modifier().fillHeight(0, 40, Block.DIRT);
        unit.modifier().fillHeight(40, 41, Block.GRASS_BLOCK);

        BlockVec start = unit.absoluteStart();
        int chunkX = start.chunkX();
        int chunkZ = start.chunkZ();
        if (chunkX < 0 || chunkZ < 0) return;

        int offsetX = Math.ceilDiv(start.blockX(), 11) * 11 % 16;

        unit.fork(setter -> {
            for (int x = offsetX; x < 16; x += 11) {
                int offsetZ = Math.ceilDiv(start.blockZ(), 14) * 14 % 16;
                offsetZ += (x + start.blockX()) / 11 * 7;
                offsetZ %= 14;
                for (int z = offsetZ; z < 16; z += 14) {
                    placeHexagon(setter, x, z, unit);
                }
            }
        });
    }

    private static void placeHexagon(Setter setter, int x, int z, GenerationUnit unit) {
        BlockVec start = unit.absoluteStart();
        setter.setBlock(x + start.blockX(), 42, z + start.blockZ(), Block.DIAMOND_BLOCK);
    }

}
