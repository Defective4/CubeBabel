package io.github.defective4.cubebabel.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.block.Facing;
import io.github.defective4.cubebabel.room.HexagonManager;
import io.github.defective4.cubebabel.room.Side;
import io.github.defective4.cubebabel.room.Sides;
import io.github.defective4.cubebabel.schematic.Schematic;
import io.github.defective4.cubebabel.schematic.SchematicDefinition;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

public class LibraryGenerator implements Generator {

    public static final double HIGH_THRESHOLD = 122.47119229084849;
    public static final double LOW_THRESHOLD = 57.52880770915151;
    private static final int GROUND_LEVEL = 41;
    private final LibraryServer server;

    public LibraryGenerator(LibraryServer server) {
        this.server = server;
    }

    @Override
    public void generate(GenerationUnit unit) {
        unit.modifier().fillHeight(GROUND_LEVEL - 1, GROUND_LEVEL, Block.OAK_PLANKS);

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

    private void placeHexagon(int x, int z, GenerationUnit unit) {
        BlockVec start = unit.absoluteStart();
        BlockVec index = HexagonManager.getRoomIndex(new BlockVec(x, z));
        int ix = index.blockX();
        int iz = index.blockZ();
        Sides entrances = HexagonManager.calculateRoomEntrances(ix, iz);

        Map<BlockVec, Block> blocks = new HashMap<>();
        BlockVec vec;

        for (int v = 0; v < 5; v++) {
            int y = GROUND_LEVEL + v;
            for (int h = -2; h <= 2; h++) {
                if (!entrances.has(Side.N)) {
                    vec = new BlockVec(x + h, y, z - 6);
                    blocks.put(vec, createBookshelf(Facing.SOUTH));
                }
                if (!entrances.has(Side.S)) {
                    vec = new BlockVec(x + h, y, z + 6);
                    blocks.put(vec, createBookshelf(Facing.NORTH));
                    blocks.put(vec.add(0, 0, 1), Block.OAK_PLANKS);
                }
            }

            for (int d = 0; d <= 5; d++) {
                // north-east
                boolean shelf = d < 5;
                if (!entrances.has(Side.NE)) {
                    vec = new BlockVec(x + 3 + d, y, z - 5 + d);
                    blocks.put(vec, shelf ? createBookshelf(d < 3 ? Facing.SOUTH : Facing.WEST) : Block.OAK_PLANKS);
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 2; j++) {
                            if (i == j) {
                                continue;
                            }
                            blocks.put(vec.add(i, 0, -j), Block.OAK_PLANKS);
                        }
                    }
                }
                // north-west
                if (!entrances.has(Side.NW)) {
                    vec = new BlockVec(x - 3 - d, y, z - 5 + d);
                    blocks.put(vec, shelf ? createBookshelf(d < 3 ? Facing.SOUTH : Facing.EAST) : Block.OAK_PLANKS);
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 2; j++) {
                            if (i == j) {
                                continue;
                            }
                            blocks.put(vec.add(-i, 0, -j), Block.OAK_PLANKS);
                        }
                    }
                }
                // south-east
                if (!entrances.has(Side.SE)) {
                    vec = new BlockVec(x + 3 + d, y, z + 5 - d);
                    blocks.put(vec, shelf ? createBookshelf(d < 3 ? Facing.NORTH : Facing.WEST) : Block.OAK_PLANKS);
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 2; j++) {
                            if (i == j) {
                                continue;
                            }
                            blocks.put(vec.add(i, 0, j), Block.OAK_PLANKS);
                        }
                    }
                }
                // south-west
                if (!entrances.has(Side.SW)) {
                    vec = new BlockVec(x - 3 - d, y, z + 5 - d);
                    blocks.put(vec, shelf ? createBookshelf(d < 3 ? Facing.NORTH : Facing.EAST) : Block.OAK_PLANKS);
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 2; j++) {
                            if (i == j) {
                                continue;
                            }
                            blocks.put(vec.add(-i, 0, j), Block.OAK_PLANKS);
                        }
                    }
                }
            }
        }

        Optional<SchematicDefinition> schemOpt = server.getSchematicManager().getSchematic(ix, iz);
        if (schemOpt.isPresent()) {
            Random rand = server.getSchematicManager().getRandomForRoom(ix, iz);
            SchematicDefinition def = schemOpt.get();
            Schematic schematic = def.schematic();
            BlockVec hexStart = new BlockVec(x - schematic.getX() / 2, GROUND_LEVEL - 1 - schematic.getY() / 2,
                    z - schematic.getZ() / 2).sub(0, 0, 0);
            for (Entry<BlockVec, Block> entry : schematic.getBlocks().entrySet()) {
                BlockVec pos = hexStart.add(entry.getKey());
                if (blocks.containsKey(pos)) {
                    continue;
                }
                if (!entry.getValue().air()) {
                    Block block = entry.getValue();
                    String blkKey = block.key().asString();
                    if (def.ignoredBlockTypes().contains(blkKey)) {
                        continue;
                    }
                    if (block.key().equals(Block.REDSTONE_WIRE.key())) {
                        block = block.defaultState();
                    }
                    if (def.chanceIntact() == 100 || def.nonRandomTypes().contains(blkKey)
                            || rand.nextInt(100) < def.chanceIntact()) {
                        blocks.put(pos, block);
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

    private static Block createBookshelf(Facing facing) {
        Block shelf = Block.CHISELED_BOOKSHELF.withProperty("facing", facing.name().toLowerCase());
        for (int i = 0; i < 6; i++) {
            shelf = shelf.withProperty("slot_%s_occupied".formatted(i), "true");
        }
        return shelf;
    }

}
