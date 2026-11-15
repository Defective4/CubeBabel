package io.github.defective4.cubebabel.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class GamemodeCommand extends Command implements CommandExecutor {

    private final ArgumentEnum<GameMode> gm;

    public GamemodeCommand() {
        super("gamemode");
        gm = ArgumentType.Enum("gamemode", GameMode.class);
        addSyntax(this, gm);
    }

    @Override
    public void apply(CommandSender sender, CommandContext ctx) {
        Player player = (Player) sender;
        GameMode gamemode = ctx.get(gm);
        player.setGameMode(gamemode);
        player.sendMessage(Component.translatable("commands.gamemode.success.self")
                .arguments(Component.text(gamemode.name().toLowerCase())));
    }

}
