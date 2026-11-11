package io.github.defective4.cubebabel.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import net.minestom.server.instance.block.Block;

public class SchematicManager {
    private final Schematic hub;
    private final Random rand = new Random();
    private final List<SchematicDefinition> schematics;

    public SchematicManager() {
        schematics = Collections.unmodifiableList(Arrays.asList(//
                new SchematicDefinition(loadSchematic("room/void1"), 100, Set.of()), //
                new SchematicDefinition(loadSchematic("room/blood"), 10), //
                new SchematicDefinition(loadSchematic("room/enchanting"), 50), //
                new SchematicDefinition(loadSchematic("room/gates1"), 90), //
                new SchematicDefinition(loadSchematic("room/deco2"), 10), //
                new SchematicDefinition(loadSchematic("room/floor1"), 100, Set.of()), //
                new SchematicDefinition(loadSchematic("room/floor2"), 10, Set.of(),
                        Set.of(getBlockKey(Block.GRASS_BLOCK))) //
        ));
        hub = loadSchematic("fixed/hub");
    }

    public Schematic getHub() {
        return hub;
    }

    public Random getRandomForRoom(int roomX, int roomZ) {
        synchronized (rand) {
            rand.setSeed(roomX + roomZ * roomX);
            return rand;
        }
    }

    public Optional<SchematicDefinition> getRoomSchematic(int roomX, int roomZ) {
        return Optional.empty();
    }

    private Schematic loadSchematic(String name) {
        try (InputStream in = new GZIPInputStream(
                getClass().getResourceAsStream("/schematics/%s.schem".formatted(name)))) {
            return new Schematic(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String getBlockKey(Block block) {
        return block.key().toString();
    }
}
