package io.github.defective4.cubebabel.command;

import io.github.defective4.cubebabel.LibraryServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

public class BookDialogCommand extends Command implements CommandExecutor {

    private final ArgumentString response;

    public BookDialogCommand() {
        super("bookdialog");

        response = ArgumentType.String("response");
        addSyntax(this, response);
    }

    @Override
    public void apply(CommandSender sender, CommandContext ctx) {
        String response = ctx.get(this.response);
        LibraryServer.getInstance().getUIManager().executeBookCallback((Player) sender, response);
    }

}
