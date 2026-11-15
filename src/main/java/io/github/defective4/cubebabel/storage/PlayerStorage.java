package io.github.defective4.cubebabel.storage;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import io.github.defective4.cubebabel.book.BookManager;
import io.github.defective4.jbabel.BookLocation;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

public class PlayerStorage {
    public static record HighlightedBook(BlockVec block, BookLocation location, Point particlesLocation) {
    }

    private final Map<UUID, BigInteger> dimensions = new HashMap<>();
    private final Map<Player, HighlightedBook> highlightedBooks = new HashMap<>();
    private final Map<Player, Map<BlockVec, Block>> playerBookshelves = new HashMap<>();
    private final Map<Player, HighlightedBook> resolvedHighlightedBooks = new HashMap<>();

    public PlayerStorage() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, e -> {
            Player player = e.getPlayer();
            dimensions.remove(player.getUuid());
            highlightedBooks.remove(player);
            playerBookshelves.remove(player);
            resolvedHighlightedBooks.remove(player);
        });
    }

    public Block getBookshelf(Player player, BlockVec pos) {
        return playerBookshelves.computeIfAbsent(player, _ -> new HashMap<>()).getOrDefault(pos,
                player.getInstance().getBlock(pos));
    }

    public BigInteger getDimension(Player player) {
        return dimensions.getOrDefault(player.getUuid(), BigInteger.ZERO);
    }

    public Map<Player, HighlightedBook> getHighlightedBooks() {
        return highlightedBooks;
    }

    public Map<Player, Map<BlockVec, Block>> getPlayerBookshelves() {
        return playerBookshelves;
    }

    public Map<Player, HighlightedBook> getResolvedHighlightedBooks() {
        return resolvedHighlightedBooks;
    }

    public BlockVec highlightBook(Player player, BookLocation book, BlockVec center) {
        BlockVec bookshelf = BookManager.calculateBookshelfCoordinates(center, book.wall(), book.shelf(), book.book());
        highlightedBooks.put(player, new HighlightedBook(bookshelf, book, null));
        return bookshelf;
    }

    public void putBookshelf(Player player, BlockVec pos, Block block) {
        Map<BlockVec, Block> map = playerBookshelves.computeIfAbsent(player, _ -> new HashMap<>());
        player.sendPacket(new BlockChangePacket(pos, block));
        for (int i = 0; i < 6; i++)
            if (!BookManager.isBookSlotOccupied(block, i)) {
                map.put(pos, block);
                return;
            }
        map.remove(pos);
        clean(player, map);
    }

    public void removeBookshelves(Player player) {
        playerBookshelves.remove(player);
    }

    public void removeBookshelves(Player player, int chunkX, int chunkZ) {
        if (!playerBookshelves.containsKey(player)) return;
        Map<BlockVec, Block> map = playerBookshelves.get(player);
        List<BlockVec> list = map.keySet().stream().filter(vec -> {
            return vec.chunkX() == chunkX && vec.chunkZ() == chunkZ;
        }).toList();
        list.forEach(map::remove);
        clean(player, map);
    }

    public void resolveHighlight(Player player, HighlightedBook particlePos) {
        resolvedHighlightedBooks.put(player, particlePos);
    }

    public void setDimension(Player player, BigInteger dimension) {
        dimensions.put(player.getUuid(), Objects.requireNonNull(dimension));
    }

    private void clean(Player player, Map<BlockVec, Block> map) {
        if (map.isEmpty()) {
            playerBookshelves.remove(player);
        }
    }

    public static void initPlayer(PlayerLoadedEvent event) {
        Player p = event.getPlayer();
        p.setGameMode(GameMode.SURVIVAL);
    }
}
