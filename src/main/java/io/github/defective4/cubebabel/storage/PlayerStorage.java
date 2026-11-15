package io.github.defective4.cubebabel.storage;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import io.github.defective4.cubebabel.book.BookManager;
import io.github.defective4.jbabel.BookLocation;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerLoadedEvent;

public class PlayerStorage {
    public static record HighlightedBook(BlockVec block, BookLocation location, Point particlesLocation) {
    }

    private final Map<UUID, BigInteger> dimensions = new HashMap<>();
    private final Map<Player, HighlightedBook> highlightedBooks = new HashMap<>();
    private final Map<Player, HighlightedBook> resolvedHighlightedBooks = new HashMap<>();

    public BigInteger getDimension(Player player) {
        return dimensions.getOrDefault(player.getUuid(), BigInteger.ZERO);
    }

    public Map<Player, HighlightedBook> getHighlightedBooks() {
        return highlightedBooks;
    }

    public Map<Player, HighlightedBook> getResolvedHighlightedBooks() {
        return resolvedHighlightedBooks;
    }

    public void highlightBook(Player player, BookLocation book, BlockVec center) {
        BlockVec bookshelf = BookManager.calculateBookshelfCoordinates(center, book.wall(), book.shelf(), book.book());
        highlightedBooks.put(player, new HighlightedBook(bookshelf, book, null));
    }

    public void resolveHighlight(Player player, HighlightedBook particlePos) {
        resolvedHighlightedBooks.put(player, particlePos);
    }

    public void setDimension(Player player, BigInteger dimension) {
        dimensions.put(player.getUuid(), Objects.requireNonNull(dimension));
    }

    public static void initPlayer(PlayerLoadedEvent event) {
        Player p = event.getPlayer();
        p.setGameMode(GameMode.SURVIVAL);
    }
}
