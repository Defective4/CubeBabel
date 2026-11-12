package io.github.defective4.cubebabel.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.block.Facing;
import io.github.defective4.cubebabel.room.Side;
import io.github.defective4.cubebabel.room.Sides;
import io.github.defective4.cubebabel.schematic.Schematic;
import io.github.defective4.cubebabel.schematic.SchematicDefinition;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;

public class LibraryGenerator implements Generator {

    public static final int GROUND_LEVEL = 41;
    public static final double HIGH_THRESHOLD = 122.47119229084849;
    public static final double LOW_THRESHOLD = 57.52880770915151;
    private final LibraryServer server;

    public LibraryGenerator(LibraryServer server) {
        this.server = server;
    }

    @Override
    public void generate(GenerationUnit unit) {
        unit.modifier().fillHeight(GROUND_LEVEL - 1, GROUND_LEVEL, Block.OAK_PLANKS);
        BlockVec start = unit.absoluteStart();
        int chunkX = start.chunkX();
        int chunkZ = start.chunkZ();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                start = unit.absoluteStart();
                chunkX = start.chunkX();
                chunkZ = start.chunkZ();
                start = start.add(i * 16, 0, j * 16);
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
        BlockVec index = LibraryGenerator.getRoomIndex(new BlockVec(x, z));
        int ix = index.blockX();
        int iz = index.blockZ();
        Sides entrances = LibraryGenerator.calculateRoomEntrances(ix, iz);

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

        Optional<SchematicDefinition> schemOpt = server.getSchematicManager().getRoomSchematic(ix, iz);
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

    public static io.github.defective4.cubebabel.room.Sides calculateRoomEntrances(int absX, int absZ) {
        int rx = absX - LibraryServer.CENTER_ROOM.blockX();
        int rz = absZ - LibraryServer.CENTER_ROOM.blockZ();
        if (rx == 0) {
            if (rz == 0) return Sides.NS;
            if (rz == 1)
                return Sides.NNE;
            else if (rz == -1)
                return Sides.SSW;
            else
                return rz < 0 ? Sides.SESW : Sides.NENW;
        }

        BlockVec centerRoomLocation = getRoomLocation(absX, absZ).sub(LibraryServer.CENTER_ROOM_LOCATION.blockX(), 0,
                LibraryServer.CENTER_ROOM_LOCATION.blockZ());
        BlockVec offsetRoomLocation = getRoomLocation(absX, absZ + (rx > 0 ? 1 : -1))
                .sub(LibraryServer.CENTER_ROOM_LOCATION.blockX(), 0, LibraryServer.CENTER_ROOM_LOCATION.blockZ());

        double deg = Math.toDegrees(Math.atan2(centerRoomLocation.blockX(), centerRoomLocation.blockZ()));
        if (deg == -HIGH_THRESHOLD)
            return Sides.SNE;
        else if (deg == LOW_THRESHOLD) return Sides.NSW;

        double offsetDeg = Math.toDegrees(Math.atan2(offsetRoomLocation.blockX(), offsetRoomLocation.blockZ()));
        if (offsetDeg == HIGH_THRESHOLD)
            return Sides.SNW;
        else if (offsetDeg == -LOW_THRESHOLD)
            return Sides.NSE;
        else {
            if (offsetDeg < -LOW_THRESHOLD && deg > -HIGH_THRESHOLD
                    || deg > LOW_THRESHOLD && offsetDeg < HIGH_THRESHOLD)
                return Sides.NS;
            if (deg < -HIGH_THRESHOLD) return Sides.NESW;
            if (offsetDeg > HIGH_THRESHOLD) return Sides.NWSE;
            if (offsetDeg > -LOW_THRESHOLD && offsetDeg < LOW_THRESHOLD) return rx > 0 ? Sides.NESW : Sides.NWSE;
        }

        return new io.github.defective4.cubebabel.room.Sides();
    }

    public static BlockVec getRoomIndex(BlockVec pos) {
        int z = pos.blockZ() + 7;
        boolean b = false;
        if ((pos.blockX() + 7) / 11 % 2 == 1) {
            z -= 7;
            b = true;
        }

        int cx = (pos.blockX() + 7) / 11 * 11;
        int cz = z / 14 * 14;
        if (b) {
            cz += 7;
        }

        int y = pos.blockY();

        BlockVec[] vectors = new BlockVec[] { new BlockVec(cx, y, cz), new BlockVec(cx + 11, y, cz + 7),
                new BlockVec(cx + 11, y, cz - 7), new BlockVec(cx - 11, y, cz + 7), new BlockVec(cx - 11, y, cz - 7),
                new BlockVec(cx, y, cz + 14), new BlockVec(cx, y, cz - 14), };

        BlockVec closest = Arrays.stream(vectors).sorted((v1, v2) -> {
            double diff = v1.distance(pos) - v2.distance(pos);
            return diff > 0 ? 1 : diff < 0 ? -1 : 0;
        }).findFirst().get();

        int roomX = closest.blockX() / 11;
        int roomZ = closest.blockZ() / 14;

        return new BlockVec(roomX, roomZ);
    }

    public static BlockVec getRoomLocation(int x, int z) {
        int roomX = x * 11;
        int roomZ = z * 14 + roomX % 2 * 7;

        return new BlockVec(roomX, roomZ);
    }

    private static Block createBookshelf(Facing facing) {
        Block shelf = Block.CHISELED_BOOKSHELF.withProperty("facing", facing.name().toLowerCase());
        for (int i = 0; i < 6; i++) {
            shelf = shelf.withProperty("slot_%s_occupied".formatted(i), "true");
        }
        return shelf;
    }

}
