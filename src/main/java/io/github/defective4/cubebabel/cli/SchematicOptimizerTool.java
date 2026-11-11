package io.github.defective4.cubebabel.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class SchematicOptimizerTool {
    public static void main(String[] args) throws IOException {
        for (String file : args) {
            optimizeSchematic(new File(file));
        }
    }

    private static void optimizeSchematic(File file) throws FileNotFoundException, IOException {
        CompoundBinaryTag tag;
        try (InputStream in = new GZIPInputStream(new FileInputStream(file))) {
            tag = BinaryTagIO.reader().read(in);
        }

        CompoundBinaryTag schem = tag.getCompound("Schematic");
        schem = schem.remove("Offset").remove("DataVersion").remove("Version").remove("Metadata");
        tag = CompoundBinaryTag.builder()
                .put("Schematic",
                        schem.remove("Blocks").put("Blocks", schem.getCompound("Blocks").remove("BlockEntities")))
                .build();

        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(file))) {
            BinaryTagIO.writer().write(tag, out);
        }
    }
}
