package io.github.defective4.cubebabel.schematic;

import java.util.Objects;
import java.util.Set;

import net.minestom.server.instance.block.Block;

public record SchematicDefinition(Schematic schematic, int chanceIntact, Set<String> ignoredBlockTypes) {

    private static final Set<String> DEFAULT_IGNORED = Set.of(Block.OAK_PLANKS.key().asString());

    public SchematicDefinition(Schematic schematic) {
        this(schematic, 100, DEFAULT_IGNORED);
    }

    public SchematicDefinition(Schematic schematic, int chanceIntact) {
        this(schematic, chanceIntact, DEFAULT_IGNORED);
    }

    public SchematicDefinition(Schematic schematic, int chanceIntact, Set<String> ignoredBlockTypes) {
        this.schematic = Objects.requireNonNull(schematic);
        this.chanceIntact = chanceIntact;
        this.ignoredBlockTypes = ignoredBlockTypes;
    }
}
