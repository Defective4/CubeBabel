package io.github.defective4.cubebabel.command;

import static io.github.defective4.cubebabel.LibraryServer.*;

import java.math.BigInteger;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.room.HexagonManager;
import io.github.defective4.jbabel.BabelCatalogue;
import io.github.defective4.jbabel.Book;
import io.github.defective4.jbabel.BookLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

public class RandomBookCommand extends Command implements CommandExecutor {

    private final LibraryServer server;

    public RandomBookCommand(LibraryServer server) {
        this.server = server;
        super("random");
        setDefaultExecutor(this);
    }

    @Override
    public void apply(CommandSender sender, CommandContext ctx) {
        Player player = (Player) sender;
        player.sendMessage(Component.text("Preparing for teleport...").color(NamedTextColor.YELLOW));
        BabelCatalogue catalogue = server.getCatalogue();
        Book book = catalogue.getRandomBook();
        BookLocation location = book.getBookLocation();

        BigInteger maxW = BigInteger.valueOf(MAX_ROOM.blockX());
        BigInteger square = maxW.multiply(BigInteger.valueOf(MAX_ROOM.blockZ()));
        BigInteger dimension = location.room().divide(square);
        BigInteger room = location.room().mod(square);

        int z = room.divide(maxW).intValueExact();
        int x = room.mod(maxW).intValueExact();

        server.setDimension(dimension);
        Pos pos = HexagonManager.getRoomLocation(x, z).asPos().withY(42);
        if (player.getInstance() == server.getLibraryInstance()) {
            player.teleport(pos);
        } else {
            player.setInstance(server.getLibraryInstance(), pos);
        }
    }

}
