package io.github.defective4.cubebabel.command;

import io.github.defective4.cubebabel.LibraryServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.entity.Player;

public class SpawnCommand extends Command implements CommandExecutor {

    private final LibraryServer server;

    public SpawnCommand(LibraryServer server) {
        super("spawn");
        this.server = server;
        setDefaultExecutor(this);
    }

    @Override
    public void apply(CommandSender sender, CommandContext ctx) {
        Player player = (Player) sender;
        if (player.getInstance() == server.getHubInstance()) {
            player.teleport(LibraryServer.SPAWN_POINT);
        } else {
            player.setInstance(server.getHubInstance(), LibraryServer.SPAWN_POINT);
        }
        player.sendMessage(Component.text("Teleported to spawn").color(NamedTextColor.GREEN));
    }
}
