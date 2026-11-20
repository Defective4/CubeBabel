package io.github.defective4.cubebabel.command;

import static io.github.defective4.cubebabel.LibraryServer.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.book.Dictionary;
import io.github.defective4.cubebabel.generator.LibraryGenerator;
import io.github.defective4.cubebabel.util.Tuple;
import io.github.defective4.jbabel.BabelCatalogue;
import io.github.defective4.jbabel.BabelLibrary;
import io.github.defective4.jbabel.Book;
import io.github.defective4.jbabel.BookLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

public class FindBookCommand extends Command implements CommandExecutor {
    public static enum ContentMode {
        PAGE, PARTIAL, WHOLE, WORDS
    }

    public static enum FindMode {
        ALL, AUTHOR, CONTENT, TITLE, WORDS
    }

    private final ArgumentEnum<FindMode> arg = ArgumentType.Enum("mode", FindMode.class);
    private final LibraryServer server;

    public FindBookCommand(LibraryServer server) {
        super("find");
        this.server = server;

        addSyntax(this, arg);
    }

    @Override
    public void apply(CommandSender sender, CommandContext ctx) {
        Player player = (Player) sender;
        FindMode mode = ctx.get(arg);

        switch (mode) {
            case WORDS -> {
                teleportPlayer(player, getRandomWordsBook(), -1);
            }
            case TITLE -> {
                openTitleSearch(player, title -> {
                    teleportPlayer(player, constructBookContents(title, null, null), -1);
                });
            }
            case AUTHOR -> {
                openAuthorSearch(player, author -> {
                    teleportPlayer(player, constructBookContents(null, author, null), -1);
                });
            }
            case CONTENT -> {
                openContentSearch(player, content -> {
                    teleportPlayer(player, constructBookContents(null, null, content.getA()), content.getB());
                });
            }
            case ALL -> {
                openTitleSearch(player,
                        title -> openAuthorSearch(player, author -> openContentSearch(player, content -> {
                            teleportPlayer(player, constructBookContents(title, author, content.getA()),
                                    content.getB());
                        })));
            }
            default -> {
            }
        }
    }

    private Book constructBookContents(String title, String author, String content) {
        BabelCatalogue catalogue = server.getCatalogue();
        BabelLibrary library = server.getLibrary();

        Random random = catalogue.getRandom();
        char[] charset = server.getLibrary().getChars();

        StringBuilder bookContent = new StringBuilder(library.getCharactersPerBook());
        bookContent.append(title == null ? randomString(random, charset, 15) : getPaddedString(title, 15));
        bookContent.append(author == null ? randomString(random, charset, 15) : getPaddedString(author, 15));
        bookContent.append(" ".repeat(library.getCharactersPerPage() - 30));
        int len = library.getCharactersPerBook() - library.getCharactersPerPage();
        bookContent.append(content == null ? randomString(random, charset, len) : getPaddedString(content, len));

        return catalogue.findBook(bookContent.toString());
    }

    private String generateRandomWordTitleAuthor(Random random, Dictionary dictionary) {
        StringBuilder builder;
        builder = new StringBuilder(15);

        fillBuilderWithRandomWords(random, dictionary, builder);
        String title = getPaddedString(builder.toString().trim(), 15);

        builder.delete(0, builder.capacity());
        fillBuilderWithRandomWords(random, dictionary, builder);
        String author = builder.toString().trim();

        return getPaddedString(title + author, server.getLibrary().getCharactersPerPage());
    }

    private Book getRandomWordsBook() {
        BabelLibrary library = server.getLibrary();
        BabelCatalogue catalogue = server.getCatalogue();
        Random random = catalogue.getRandom();
        Dictionary dictionary = server.getDictionary();

        String titleAuthor = generateRandomWordTitleAuthor(random, dictionary);

        int contentsSize = library.getCharactersPerPage() * (library.getPagesPerBook() - 1);
        StringBuilder builder = new StringBuilder(contentsSize);
        fillBuilderWithRandomWords(random, dictionary, builder);
        String content = getPaddedString(builder.toString(), contentsSize);

        String fullContent = titleAuthor + content;
        return catalogue.findBook(fullContent);
    }

    private Tuple<String, Integer> getRandomWordsBook(String content) {
        BabelLibrary library = server.getLibrary();
        BabelCatalogue catalogue = server.getCatalogue();
        Random random = catalogue.getRandom();
        Dictionary dictionary = server.getDictionary();
        int contentsSize = library.getCharactersPerPage() * (library.getPagesPerBook() - 1);
        List<String> words = new ArrayList<>();
        while (getCombinedLength(words) < contentsSize - content.length()) {
            words.add(dictionary.getRandomWord(random));
        }

        List<String> split = List.of(content.split(" "));
        words.addAll(random.nextInt(words.size()), split);
        while (getCombinedLength(words) > contentsSize) {
            words.remove(words.size() - 1);
        }

        String finalContent = String.join(" ", words.toArray(String[]::new));
        int page = 0;
        int index = finalContent.indexOf(content);
        if (index >= 0) {
            page = index / library.getCharactersPerPage();
        }
        return new Tuple<>(getPaddedString(finalContent, contentsSize), page);
    }

    private void openAuthorSearch(Player player, Consumer<String> consumer) {
        server.getUIManager().openAnvilForResult(player, "Search by author", "Author", consumer);
    }

    private void openContentSearch(Player player, Consumer<Tuple<String, Integer>> consumer) {
        server.getUIManager().openBookDialog(player, server.getHubCatalogue().getContentSearchModeBook(),
                modeString -> {
                    ContentMode localMode;
                    try {
                        localMode = ContentMode.valueOf(modeString.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        localMode = ContentMode.WHOLE;
                    }
                    ContentMode mode = localMode;
                    server.getUIManager().chatPrompt(
                            player, Component.text("Enter content of the book you want to search for in the chat")
                                    .color(NamedTextColor.AQUA),
                            content -> consumer.accept(prepareContent(mode, content)));
                });
    }

    private void openTitleSearch(Player player, Consumer<String> consumer) {
        server.getUIManager().openAnvilForResult(player, "Search by book title", "Title", consumer);
    }

    private Tuple<String, Integer> prepareContent(ContentMode mode, String content) {
        StringBuilder actualContent = new StringBuilder();
        int page = -2;
        BabelLibrary library = server.getLibrary();
        switch (mode) {
            case PAGE -> {
                String randomBookContent = server.getCatalogue().getRandomBook().getContent();
                int cpp = library.getCharactersPerPage();
                String pageContent = getPaddedString(content, cpp);
                page = server.getCatalogue().getRandom().nextInt(library.getPagesPerBook());
                int index = page * cpp;
                actualContent.append(insertString(pageContent, randomBookContent, index));
            }
            case PARTIAL -> {
                String randomBookContent = server.getCatalogue().getRandomBook().getContent();
                int cpb = library.getCharactersPerBook();
                int cpp = library.getCharactersPerPage();
                int index = server.getCatalogue().getRandom().nextInt(cpb - content.length());
                page = index / cpp;
                actualContent.append(insertString(content, randomBookContent, index));
            }
            case WORDS -> {
                Tuple<String, Integer> tuple = getRandomWordsBook(content);
                actualContent.append(tuple.getA());
                page = tuple.getB();
            }
            default -> actualContent.append(content);
        }
        return new Tuple<>(actualContent.toString(), page + 1);
    }

    private void teleportPlayer(Player player, Book book, int page) {
        BookLocation location = book.getBookLocation();
        BigInteger maxW = BigInteger.valueOf(MAX_ROOM.blockX());
        BigInteger square = maxW.multiply(BigInteger.valueOf(MAX_ROOM.blockZ()));
        server.getPlayerStorage().setDimension(player, location.room().divide(square));
        BigInteger room = location.room().mod(square);

        int z = room.divide(maxW).intValueExact();
        int x = room.mod(maxW).intValueExact();

        BlockVec center = LibraryGenerator.getRoomLocation(x, z).withBlockY(41);
        BlockVec coordinates = server.getPlayerStorage().highlightBook(player, location, center, page);
        double xD = coordinates.blockX() + 0.5 - center.blockX() + 0.5;
        double zD = coordinates.blockZ() + 0.5 - center.blockZ() + 0.5;
        float yaw = (float) Math.toDegrees(Math.atan2(zD, xD)) - 90;

        Pos pos = center.asPos().withYaw(yaw);

        LibraryServer.teleportPlayer(player, server.getLibraryInstance(), pos);
    }

    private static void fillBuilderWithRandomWords(Random random, Dictionary dictionary, StringBuilder builder) {
        while (true) {
            String word = dictionary.getRandomWord(random);
            if (builder.length() + word.length() > builder.capacity()) {
                break;
            }
            builder.append(word + " ");
        }
    }

    private static int getCombinedLength(List<String> words) {
        return words.stream().mapToInt(str -> str.length() + 1).sum() - 1;
    }

    private static String getPaddedString(String str, int len) {
        return str.length() == len ? str
                : str.length() > len ? str.substring(0, len) : str + " ".repeat(len - str.length());
    }

    private static String insertString(String contents, String bookContent, int index) {
        bookContent = bookContent.substring(0, index) + contents + bookContent.substring(index + contents.length());
        return bookContent;
    }

    private static String randomString(Random random, char[] charset, int len) {
        StringBuilder builder = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            builder.append(charset[random.nextInt(charset.length)]);
        }
        return builder.toString();
    }

}
