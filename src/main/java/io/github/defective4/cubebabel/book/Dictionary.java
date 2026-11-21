package io.github.defective4.cubebabel.book;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.SchedulerManager;

public class Dictionary {
    private final Map<String, String> definitions;
    private final List<String> delimiters;
    private final ExecutorService highlighterService = Executors.newFixedThreadPool(1);
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    private Dictionary(Map<String, String> definitions) throws IOException {
        delimiters = definitions.keySet().stream().sorted((a1, a2) -> a2.length() - a1.length()).toList();
        definitions = new HashMap<>(definitions);
        List<String> emptyKeys = definitions.entrySet().stream().filter(e -> e.getValue() == null)
                .map(Map.Entry::getKey).toList();
        emptyKeys.forEach(definitions::remove);
        this.definitions = Collections.unmodifiableMap(definitions);
    }

    public String getRandomWord(Random rand) {
        return delimiters.get(rand.nextInt(delimiters.size()));
    }

    public Future<?> highlightWords(Book book, Consumer<Book> callback, Consumer<Float> progressCallback) {
        return highlighterService.submit(() -> {
            List<String> pages = book.pages().stream().map(serializer::serialize).toList();
            List<Component> formatted = new ArrayList<>();

            int index = 0;
            SchedulerManager scheduler = MinecraftServer.getSchedulerManager();
            for (String page : pages) {
                List<String> split = splitWithDelimiter(page, delimiters);

                Component cpt = Component.text("");
                for (int i = 0; i < split.size(); i++) {
                    String part = split.get(i);
                    if (part.isEmpty()) {
                        continue;
                    }
                    Component partCpt = Component.text(part);
                    if (i % 2 == 1) {
                        partCpt = partCpt.color(DARK_RED).decorate(UNDERLINED)
                                .hoverEvent(HoverEvent.showText(Component.text(definitions.getOrDefault(part, ""))));
                    } else {
                        partCpt = partCpt.color(BLACK).decoration(UNDERLINED, false);
                    }

                    cpt = cpt.append(partCpt);
                }
                if (cpt != null) {
                    formatted.add(cpt);
                }
                float progress = ++index / (float) pages.size();
                scheduler.scheduleNextTick(() -> progressCallback.accept(progress));
            }

            scheduler.scheduleNextTick(() -> callback.accept(Book.book(book.title(), book.author(), formatted)));
        });
    }

    public static Dictionary fromDictionaryFile(File file) throws IOException {
        Map<String, String> definitions;
        if (!file.exists()) {
            definitions = Map.of();
        } else {
            try (Reader reader = new FileReader(file)) {
                definitions = new HashMap<>();
                for (Entry<String, JsonElement> entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                    definitions.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        return new Dictionary(definitions);
    }

    public static Dictionary fromWordsListFile(File file) throws IOException {
        Map<String, String> definitions;
        if (!file.exists()) {
            definitions = Map.of();
        } else {
            definitions = new HashMap<>();
            for (String line : Files.readAllLines(file.toPath())) {
                definitions.put(line, null);
            }
        }
        return new Dictionary(definitions);
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
