package io.github.defective4.cubebabel.generator;

import java.util.Map.Entry;

import io.github.defective4.cubebabel.schematic.Schematic;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

public class HubGenerator implements Generator {

    private final Schematic hub;

    public HubGenerator(Schematic hub) {
        this.hub = hub;
    }

    @Override
    public void generate(GenerationUnit unit) {
        BlockVec start = unit.absoluteStart();
        int chunkX = start.chunkX();
        int chunkZ = start.chunkZ();
        if (chunkX == 0 && (chunkZ == 0 || chunkZ == 1)) {
            placeHub(unit);
        }
    }

    private void placeHub(GenerationUnit unit) {
        BlockVec start = new BlockVec(0, 64, 0);
        for (Entry<BlockVec, Block> entry : hub.getBlocks().entrySet()) {
            BlockVec location = start.add(entry.getKey());
            if (location.sameChunk(unit.absoluteStart())) {
                unit.modifier().setBlock(location, entry.getValue());
            }
        }
    }

}
