package io.github.defective4.cubebabel.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.zip.GZIPInputStream;

public class SchematicManager {

    private final Random rand = new Random();

    private final List<SchematicDefinition> schematics;

    public SchematicManager() {
        schematics = Collections.unmodifiableList(Arrays.asList(//
                new SchematicDefinition(loadSchematic("void1")), //
                new SchematicDefinition(loadSchematic("blood"), 10), //
                new SchematicDefinition(loadSchematic("enchanting"), 50) //
        ));
    }

    public Random getRandomForRoom(int roomX, int roomZ) {
        synchronized (rand) {
            rand.setSeed(roomX + roomZ * roomX);
            return rand;
        }
    }

    public Optional<SchematicDefinition> getSchematic(int roomX, int roomZ) {
        return Optional.ofNullable(schematics.get(2));
    }

    private Schematic loadSchematic(String name) {
        try (InputStream in = new GZIPInputStream(
                getClass().getResourceAsStream("/schematics/%s.schem".formatted(name)))) {
            return new Schematic(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
