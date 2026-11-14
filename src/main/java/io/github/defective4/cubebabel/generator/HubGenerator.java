package io.github.defective4.cubebabel.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.block.Facing;
import io.github.defective4.cubebabel.schematic.EntityDefinition;
import io.github.defective4.cubebabel.schematic.Schematic;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

public class HubGenerator implements Generator {

    private final Schematic hub;
    private final LibraryServer server;

    public HubGenerator(Schematic hub, LibraryServer server) {
        this.hub = hub;
        this.server = server;
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
        Map<BlockVec, Block> map = new HashMap<>(hub.getBlocks());
        map.put(new BlockVec(9, 1, 6),
                Block.CHISELED_BOOKSHELF.withProperty("facing", Facing.WEST.name().toLowerCase()));
        for (Entry<BlockVec, Block> entry : map.entrySet()) {
            BlockVec location = start.add(entry.getKey());
            if (location.sameChunk(unit.absoluteStart())) {
                unit.modifier().setBlock(location, entry.getValue());
            }
        }

        for (Entry<Pos, EntityDefinition> entry : hub.getEntities().entrySet()) {
            EntityDefinition def = entry.getValue();
            Pos location = start.asPos().add(entry.getKey()).withYaw(def.rotation().yaw())
                    .withPitch(def.rotation().pitch());
            if (location.sameChunk(unit.absoluteStart())) {
                Entity entity = new Entity(EntityType.fromKey(def.id()));
                entity.setInstance(server.getHubInstance(), location);
            }
        }
    }

}
