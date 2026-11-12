package io.github.defective4.cubebabel.command;

import static io.github.defective4.cubebabel.LibraryServer.*;

import java.math.BigInteger;
import java.util.Random;
import java.util.function.Consumer;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.generator.LibraryGenerator;
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
import net.minestom.server.entity.Player;

public class FindBookCommand extends Command implements CommandExecutor {
    public static enum FindMode {
        ALL, AUTHOR, CONTENT, TITLE
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
            case TITLE -> {
                openTitleSearch(player, title -> {
                    teleportPlayer(player, constructBookContents(title, null, null));
                });
            }
            case AUTHOR -> {
                openAuthorSearch(player, author -> {
                    teleportPlayer(player, constructBookContents(null, author, null));
                });
            }
            case CONTENT -> {
                openContentSearch(player, content -> {
                    teleportPlayer(player, constructBookContents(null, null, content));
                });
            }
            case ALL -> {
                openTitleSearch(player,
                        title -> openAuthorSearch(player, author -> openContentSearch(player, content -> {
                            teleportPlayer(player, constructBookContents(title, author, content));
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

    private void openAuthorSearch(Player player, Consumer<String> consumer) {
        server.getPlayerManager().openAnvilForResult(player, "Search by author", "Author", consumer);
    }

    private void openContentSearch(Player player, Consumer<String> consumer) {
        server.getPlayerManager().chatPrompt(player, Component
                .text("Enter content of the book you want to search for in the chat").color(NamedTextColor.AQUA),
                consumer);
    }

    private void openTitleSearch(Player player, Consumer<String> consumer) {
        server.getPlayerManager().openAnvilForResult(player, "Search by book title", "Title", consumer);
    }

    private void teleportPlayer(Player player, Book book) {
        BookLocation location = book.getBookLocation();
        BigInteger maxW = BigInteger.valueOf(MAX_ROOM.blockX());
        BigInteger square = maxW.multiply(BigInteger.valueOf(MAX_ROOM.blockZ()));
        server.getPlayerStorage().setDimension(player, location.room().divide(square));
        BigInteger room = location.room().mod(square);

        int z = room.divide(maxW).intValueExact();
        int x = room.mod(maxW).intValueExact();

        player.setInstance(server.getLibraryInstance(), LibraryGenerator.getRoomLocation(x, z).asPos().withY(42));

        player.sendMessage(Component.text(location.wall()));
        player.sendMessage(Component.text(location.shelf()));
        player.sendMessage(Component.text(location.book()));
    }

    private static String getPaddedString(String str, int len) {
        return str.length() == len ? str
                : str.length() > len ? str.substring(0, len) : str + " ".repeat(len - str.length());
    }

    private static String randomString(Random random, char[] charset, int len) {
        StringBuilder builder = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            builder.append(charset[random.nextInt(charset.length)]);
        }
        return builder.toString();
    }

}
