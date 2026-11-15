package io.github.defective4.cubebabel.command;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.storage.PlayerStorage;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class DebugCommand extends Command implements CommandExecutor {

    public static enum Debug {
        storage
    }

    private final LibraryServer server;
    private final ArgumentEnum<Debug> storage;

    public DebugCommand(LibraryServer server) {
        super("debug");
        this.server = server;

        storage = ArgumentType.Enum("mode", Debug.class);

        addSyntax(this, storage);
    }

    @Override
    public void apply(CommandSender sender, CommandContext ctx) {
        switch (ctx.get(storage)) {
            case storage -> {
                PlayerStorage storage = server.getPlayerStorage();
                sender.sendMessage("Pending highlighted blocks");
                sender.sendMessage(Integer.toString(storage.getHighlightedBooks().size()));
                sender.sendMessage("");
                sender.sendMessage("Resolved highlighted blocks");
                sender.sendMessage(Integer.toString(storage.getResolvedHighlightedBooks().size()));
                sender.sendMessage("");
                sender.sendMessage("Modified bookshelves");
                sender.sendMessage(storage.getPlayerBookshelves().toString());
            }
            default -> {
            }
        }
    }
}
