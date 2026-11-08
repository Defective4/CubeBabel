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

    private final Random rand = new Random();

    private final List<SchematicDefinition> schematics;

    public SchematicManager() {
        schematics = Collections.unmodifiableList(Arrays.asList(//
                new SchematicDefinition(loadSchematic("void1"), 100, Set.of()), //
                new SchematicDefinition(loadSchematic("blood"), 10), //
                new SchematicDefinition(loadSchematic("enchanting"), 50), //
                new SchematicDefinition(loadSchematic("gates1"), 90), //
                new SchematicDefinition(loadSchematic("deco2"), 10), //
                new SchematicDefinition(loadSchematic("floor1"), 100, Set.of()), //
                new SchematicDefinition(loadSchematic("floor2"), 10, Set.of(), Set.of(getBlockKey(Block.GRASS_BLOCK))) //
        ));
    }

    public Random getRandomForRoom(int roomX, int roomZ) {
        synchronized (rand) {
            rand.setSeed(roomX + roomZ * roomX);
            return rand;
        }
    }

    public Optional<SchematicDefinition> getSchematic(int roomX, int roomZ) {
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
