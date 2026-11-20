package io.github.defective4.cubebabel.book;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
        this.words = words.stream().sorted((a1, a2) -> a2.length() - a1.length()).toList();
    }

    public Dictionary(List<String> words) {
        this.words = List.copyOf(words);
    }

    // TODO OPTIMIZE
    public Book highlightWords(Book book) {
        List<String> pages = book.pages().stream().map(serializer::serialize).toList();
        List<Component> formatted = new ArrayList<>();

        for (String page : pages) {
            List<String> split = splitWithDelimiter(page, words);

            Component cpt = null;
            for (int i = 0; i < split.size(); i++) {
                String part = split.get(i);
                if (part.isEmpty()) {
                    continue;
                }
                Component partCpt = Component.text(part);
                boolean underlined = i % 2 == 1;
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

    private static List<String> splitWithDelimiter(String text, Collection<String> delimiters) {
        AtomicReference<String> textRef = new AtomicReference<>(text);
        List<String> parts = new ArrayList<>();
        while (true) {
            Optional<String> opt = delimiters.stream().filter(textRef.get()::contains).min((a1, a2) -> {
                String txt = textRef.get();
                return txt.indexOf(a1) - txt.indexOf(a2);
            });
            if (!opt.isPresent()) {
                parts.add(textRef.get());
                break;
            }
            String delimiter = opt.get();
            String currentText = textRef.get();
            int index = currentText.indexOf(delimiter);
            parts.add(currentText.substring(0, index));
            textRef.set(textRef.get().substring(index + delimiter.length()));
            parts.add(delimiter);
        }
        return parts;
    }

}
