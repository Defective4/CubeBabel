package io.github.defective4.cubebabel.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;

public class Schematic {

    private final Map<BlockVec, Block> blocks;
    private final Map<Pos, EntityDefinition> entities;
    private final int x, y, z;

    public Schematic(InputStream in) throws IOException {
        CompoundBinaryTag tag = BinaryTagIO.reader().read(in).getCompound("Schematic");
        x = tag.getInt("Width");
        y = tag.getInt("Height");
        z = tag.getInt("Length");
        Map<Integer, Block> blockPalette = new HashMap<>();
        CompoundBinaryTag blocks = tag.getCompound("Blocks");
        CompoundBinaryTag palette = blocks.getCompound("Palette");
        palette.forEach(entry -> blockPalette.put(((IntBinaryTag) entry.getValue()).intValue(),
                Block.fromState(entry.getKey())));

        byte[] blockdata = blocks.getByteArray("Data");
        int index = 0;
        int i = 0;
        int value = 0;
        int varint_length = 0;
        Map<BlockVec, Block> blockmap = new HashMap<>();
        while (i < blockdata.length) {
            value = 0;
            varint_length = 0;

            while (true) {
                value |= (blockdata[i] & 127) << varint_length++ * 7;
                if (varint_length > 5) throw new RuntimeException("VarInt too big (probably corrupted data)");
                if ((blockdata[i] & 128) != 128) {
                    i++;
                    break;
                }
                i++;
            }
            int by = index / (x * z);
            int bz = index % (x * z) / x;
            int bx = index % (x * z) % x;

            blockmap.put(new BlockVec(bx, by, bz), blockPalette.get(value));
            index++;
        }
        this.blocks = Collections.unmodifiableMap(blockmap);

        if (tag.contains("Entities")) {
            Map<Pos, EntityDefinition> entities = new HashMap<>();
            ListBinaryTag list = tag.getList("Entities");
            for (BinaryTag entityBinTag : list) {
                if (entityBinTag instanceof CompoundBinaryTag entityTag) {
                    String id = entityTag.getString("Id");
                    ListBinaryTag posList = entityTag.getList("Pos");
                    double x = posList.getDouble(0);
                    double y = posList.getDouble(1);
                    double z = posList.getDouble(2);

                    Pos rotation;

                    if (entityTag.contains("Data")) {
                        CompoundBinaryTag data = entityTag.getCompound("Data");
                        ListBinaryTag rot = data.getList("Rotation");
                        rotation = new Pos(0, 0, 0, rot.getFloat(0), rot.getFloat(1));
                    } else {
                        rotation = new Pos(0, 0, 0, 0, 0);
                    }

                    entities.put(new Pos(x, y, z), new EntityDefinition(id, rotation));
                }
            }
            this.entities = Collections.unmodifiableMap(entities);
        } else {
            entities = Collections.emptyMap();
        }
    }

    public Map<BlockVec, Block> getBlocks() {
        return blocks;
    }

    public Map<Pos, EntityDefinition> getEntities() {
        return entities;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

}
