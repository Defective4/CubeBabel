package io.github.defective4.cubebabel.book;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Dictionary {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final List<String> words;

    public Dictionary(File file) throws IOException {
        List<String> words;
        if (!file.exists()) {
            words = List.of();
        } else {
            words = Files.readAllLines(file.toPath());
        }
        this.words = words;
    }

    public Dictionary(List<String> words) {
        this.words = List.copyOf(words);
    }

    public Book highlightWords(Book book) {
        List<String> pages = book.pages().stream().map(serializer::serialize).toList();
        List<Component> formatted = new ArrayList<>();

        for (String page : pages) {
            List<String> split = new ArrayList<>();
            split.add(page);

            for (String word : words) {
                List<String> splitCopy = List.copyOf(split);
                split.clear();
                for (String part : splitCopy) {
                    Collections.addAll(split, part.splitWithDelimiters(word, 0));
                }
            }

            Component cpt = null;
            for (String part : split) {
                if (part.isEmpty()) {
                    continue;
                }
                Component partCpt = Component.text(part);
                boolean underlined = words.contains(part);
                partCpt = partCpt.color(underlined ? DARK_RED : BLACK).decoration(UNDERLINED, underlined);

                if (cpt == null) {
                    cpt = partCpt;
                } else {
                    cpt = cpt.append(partCpt);
                }
            }
            if (cpt != null) {
                formatted.add(cpt);
            }
        }

        return Book.book(book.title(), book.author(), formatted);
    }

}
