package io.github.defective4.cubebabel.command;

import java.math.BigInteger;

import io.github.defective4.cubebabel.LibraryServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.entity.Player;

public class CenterCommand extends Command implements CommandExecutor {

    private final LibraryServer server;

    public CenterCommand(LibraryServer server) {
        super("center");
        this.server = server;
        setDefaultExecutor(this);
    }

    @Override
    public void apply(CommandSender sender, CommandContext ctx) {
        Player player = (Player) sender;
        server.getUIManager().openAnvilForResult(player, "Dimension hex", "0", str -> {
            BigInteger hex;
            try {
                hex = new BigInteger(str, 32);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Entered dimension hex is invalid").color(NamedTextColor.RED));
                return;
            }
            server.getPlayerStorage().setDimension(player, hex);
            LibraryServer.teleportPlayer(player, server.getLibraryInstance(),
                    LibraryServer.CENTER_ROOM_LOCATION.asPos());
        });
    }

}
