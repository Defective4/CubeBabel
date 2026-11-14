package io.github.defective4.cubebabel.book;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.generator.LibraryGenerator;
import io.github.defective4.cubebabel.room.Side;
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
    private static final String BOOKSHELF_SLOT = "slot_%s_occupied";
    private final LibraryServer server;

    public BookManager(LibraryServer server) {
        this.server = server;
    }

    public ItemStack getBookFrom(BookLocation location, Block block, Instance instance) {

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

    public static BlockVec calculateBookshelfCoordinates(BlockVec roomIndex, int wall) {
        BlockVec center = LibraryGenerator.getRoomLocation(roomIndex.blockX(), roomIndex.blockZ());
        Sides entrances = LibraryGenerator.calculateRoomEntrances(roomIndex.blockX(), roomIndex.blockZ());
        int index = wall;
        for (Side side : entrances.sides()) {
            if (side.getWallIndex() <= index) {
                index++;
            }
        }

        BlockVec horizontal;
        int xd, zd;

        switch (index) {
            case 0, 3 -> {
                int mult = index == 3 ? 1 : -1;
                xd = 2 * mult;
                zd = 6 * mult;
            }
            case 2, 5 -> {
                xd = index == 2 ? 7 : -7;
                zd = index == 2 ? 1 : -1;
            }
            case 1, 4 -> {
                xd = index == 1 ? 3 : -3;
                zd = index == 1 ? -5 : 5;
            }
            default -> {
                xd = 0;
                zd = 0;
            }
        }

        System.out.println("%s = %s".formatted(wall, index));

        horizontal = center.add(xd, 0, zd);
        return horizontal;
    }

    public static BlockFace getBlockFace(Block block) {
        return BlockFace.valueOf(block.getProperty("facing").toUpperCase());
    }

    public static BookLocation getBookCoordinates(BlockVec pos, int bookIndex, BigInteger dimension) {
        BlockVec index = LibraryGenerator.getRoomIndex(pos);
        BlockVec center = LibraryGenerator.getRoomLocation(index.blockX(), index.blockZ());
        BlockVec diff = pos.sub(center);

        int vIndex = 0;
        int wall = 0;
        int shelf = pos.blockY() - LibraryGenerator.GROUND_LEVEL;

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
        Sides entrances = LibraryGenerator.calculateRoomEntrances(index.blockX(), index.blockZ());
        for (Side side : entrances.sides()) {
            if (wall >= side.getWallIndex()) {
                wall--;
            }
        }

        BigInteger maxW = BigInteger.valueOf(LibraryServer.MAX_ROOM.blockX());
        BigInteger square = maxW.multiply(BigInteger.valueOf(LibraryServer.MAX_ROOM.blockZ()));

        BigInteger room = BigInteger.valueOf(index.blockX()).add(BigInteger.valueOf(index.blockZ()).multiply(maxW))
                .add(square.multiply(dimension));
        BookLocation loc = new BookLocation(room, wall, shelf, bookIndex + vIndex * 6);
        return loc;
    }

    public static int getShelfBookIndex(Point cursor, BlockFace clickedFace) {
        int book;
        double h = clickedFace == BlockFace.EAST || clickedFace == BlockFace.WEST ? cursor.z() : cursor.x();
        if (clickedFace == BlockFace.NORTH || clickedFace == BlockFace.EAST) {
            h = 1 - h;
        }

        book = (int) (h / (1d / 3d));
        if (cursor.y() < 0.5) {
            book += 3;
        }
        return book;
    }

    public static boolean isBookSlotOccupied(Block block, int slot) {
        String property = block.getProperty(BOOKSHELF_SLOT.formatted(slot));
        return property != null && property.equals("true");
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

    public static Block setShelfBook(Block shelf, int slot, boolean has) {
        return shelf.withProperty(BOOKSHELF_SLOT.formatted(slot), Boolean.toString(has));
    }
}
