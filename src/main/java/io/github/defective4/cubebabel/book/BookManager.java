package io.github.defective4.cubebabel.book;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.generator.LibraryGenerator;
import io.github.defective4.cubebabel.room.HexagonManager;
import io.github.defective4.cubebabel.room.Sides;
import io.github.defective4.jbabel.BabelLibrary;
import io.github.defective4.jbabel.Book;
import io.github.defective4.jbabel.BookLocation;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.WrittenBookContent;

public class BookManager {
    private final LibraryServer server;

    public BookManager(LibraryServer server) {
        this.server = server;
    }

    public ItemStack getBookAt(BlockVec pos, Point cursor, BlockFace blockFace, Block block, Instance instance,
            BigInteger dimension) {
        BookLocation location = getBookCoordinates(pos, cursor, blockFace, dimension);
        instance.setBlock(pos, setShelfBook(block, location.book() % 6, false));

        Book babelBook = server.getCatalogue().getBookAt(location);
        BabelLibrary library = server.getLibrary();

        String content = babelBook.getContent();

        String title = content.substring(0, 15).trim();
        String author = content.substring(15, 15 + 15).trim();

        List<Component> components = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            int start = library.getCharactersPerPage() * (i + 1);
            int end = start + library.getCharactersPerPage();
            components.add(Component.text(content.substring(start, end)));
        }

        ItemStack book = ItemStack.builder(Material.WRITTEN_BOOK)
                .set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(title, author, components)).build();
        return book;
    }

    public static BlockFace getBlockFace(Block block) {
        return BlockFace.valueOf(block.getProperty("facing").toUpperCase());
    }

    public static void openBook(Player player, ItemStack item) {
        WrittenBookContent content = item.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content != null) {
            net.kyori.adventure.inventory.Book book = net.kyori.adventure.inventory.Book.builder()
                    .pages(content.pages().stream().map(ft -> ft.text()).toList())
                    .title(Component.text(content.title().text())).author(Component.text(content.author())).build();

            player.openBook(book);
        }
    }

    private static BookLocation getBookCoordinates(BlockVec pos, Point cursor, BlockFace clickedFace,
            BigInteger dimension) {
        BlockVec index = HexagonManager.getRoomIndex(pos);
        BlockVec center = HexagonManager.getRoomLocation(index.blockX(), index.blockZ());
        BlockVec diff = pos.sub(center);

        int vIndex = 0;
        int wall = 0;
        int shelf = pos.blockY() - LibraryGenerator.GROUND_LEVEL;
        int book = -1;

        double h = clickedFace == BlockFace.EAST || clickedFace == BlockFace.WEST ? cursor.z() : cursor.x();
        if (clickedFace == BlockFace.NORTH || clickedFace == BlockFace.EAST) {
            h = 1 - h;
        }

        book = (int) (h / (1d / 3d));
        if (cursor.y() < 0.5) {
            book += 3;
        }

        if (Math.abs(diff.blockZ()) == 6) {
            if (Math.abs(diff.blockX()) <= 2) {
                int diffX = diff.blockX();
                if (diff.blockZ() > 0) {
                    diffX *= -1;
                }
                vIndex = diffX + 2;
                wall = diff.blockZ() > 0 ? 3 : 0;
            }
        } else if (Math.abs(diff.blockX()) + Math.abs(diff.blockZ()) == 8) {
            int wx = diff.blockX();
            int wz = diff.blockZ();
            vIndex = (Math.abs(wx) - Math.abs(wz) + 2) / 2;

            if (wx > 0 && wz < 0) {
                wall = 1;
            } else if (wx > 0 && wz > 0) {
                wall = 2;
                vIndex = 4 - vIndex;
            } else if (wx < 0 && wz > 0) {
                wall = 4;
            } else if (wx < 0 && wz < 0) {
                wall = 5;
                vIndex = 4 - vIndex;
            }
        }
        Sides entrances = HexagonManager.calculateRoomEntrances(index.blockX(), index.blockZ());
        if (entrances.first() != null && wall >= entrances.first().getWallIndex()) {
            wall--;
        }
        if (entrances.second() != null && wall >= entrances.second().getWallIndex()) {
            wall--;
        }

        BigInteger maxW = BigInteger.valueOf(LibraryServer.MAX_ROOM.blockX());
        BigInteger square = maxW.multiply(BigInteger.valueOf(LibraryServer.MAX_ROOM.blockZ()));

        BigInteger room = BigInteger.valueOf(index.blockX()).add(BigInteger.valueOf(index.blockZ()).multiply(maxW))
                .add(square.multiply(dimension));
        BookLocation loc = new BookLocation(room, wall, shelf, book + vIndex * 6);
        return loc;
    }

    private static Block setShelfBook(Block shelf, int slot, boolean has) {
        return shelf.withProperty("slot_%s_occupied".formatted(slot), Boolean.toString(has));
    }
}
