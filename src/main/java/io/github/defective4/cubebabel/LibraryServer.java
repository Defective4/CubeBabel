package io.github.defective4.cubebabel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.github.defective4.cubebabel.generator.LibraryGenerator;
import io.github.defective4.cubebabel.room.HexagonManager;
import io.github.defective4.cubebabel.room.Sides;
import io.github.defective4.cubebabel.schematic.SchematicManager;
import io.github.defective4.jbabel.BabelCatalogue;
import io.github.defective4.jbabel.BabelLibrary;
import io.github.defective4.jbabel.Book;
import io.github.defective4.jbabel.BookLocation;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.item.component.WrittenBookContent;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

public class LibraryServer {
    public static final BlockVec CENTER_ROOM;
    public static final BlockVec CENTER_ROOM_LOCATION;
    public static final BlockVec MAX_ROOM = new BlockVec(2727270, 2142855);
    private final BabelCatalogue catalogue;
    private final BabelLibrary library;
    private final RegistryKey<DimensionType> libraryDimension;
    private final InstanceContainer libraryInstance;
    private final SchematicManager schematicManager = new SchematicManager();
    private final MinecraftServer server;

    static {
        CENTER_ROOM = new BlockVec(MAX_ROOM.blockX() / 2, MAX_ROOM.blockZ() / 2);
        CENTER_ROOM_LOCATION = HexagonManager.getRoomLocation(CENTER_ROOM.blockX(), CENTER_ROOM.blockZ());
    }

    public LibraryServer(BabelLibrary library) {
        server = MinecraftServer.init();
        libraryDimension = MinecraftServer.getDimensionTypeRegistry().register("minecraft:library",
                DimensionType.builder().ambientLight(1.0f).build());

        libraryInstance = MinecraftServer.getInstanceManager().createInstanceContainer(libraryDimension);
        this.library = library;
        catalogue = new BabelCatalogue.Builder(library).create();

        libraryInstance.setChunkSupplier(LightingChunk::new);
        libraryInstance.setGenerator(new LibraryGenerator(this));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class,
                e -> e.setSpawningInstance(libraryInstance));

        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoadedEvent.class, event -> {
            Player p = event.getPlayer();
            p.setGameMode(GameMode.SURVIVAL);
            BlockVec vec = new BlockVec(MAX_ROOM.blockX() / 2, MAX_ROOM.blockZ() / 2);
            p.teleport(HexagonManager.getRoomLocation(vec.blockX(), vec.blockZ()).withY(64).asPos());
        });

        MinecraftServer.getCommandManager().register(new Command("tp") {
            {
                setDefaultExecutor((sender, ctx) -> {
                    String[] args = ctx.getInput().split(" ");
                    double x = Double.parseDouble(args[2]);
                    double y = Double.parseDouble(args[3]);
                    double z = Double.parseDouble(args[4]);

                    Player player = (Player) sender;
                    player.teleport(new Pos(x, y, z));
                });
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, e -> {
            ItemStack item = e.getItemStack();
            if (item != null && item.material() == Material.WRITTEN_BOOK) {
                CustomData custom = item.get(DataComponents.CUSTOM_DATA);
                if (custom != null) {
                    CompoundBinaryTag data = custom.nbt();
                    if (data != null) {
                        BookLocation location = new BookLocation(data.getString("room"), data.getInt("wall"),
                                data.getInt("shelf"), data.getInt("book"));
                        Book book = catalogue.getBookAt(location);
                        List<Component> pages = new ArrayList<>();
                        String content = book.getContent();
                        for (int i = 0; i < 100; i++) {
                            int start = library.getCharactersPerPage() * i + 1;
                            int end = start + library.getCharactersPerPage();
                            pages.add(Component.text(content.substring(start, end)));
                        }
                        e.getPlayer().openBook(net.kyori.adventure.inventory.Book.book(pages));
                    }
                }
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockInteractEvent.class, e -> {
            if (e.getPlayer().isSneaking()) return;
            BlockVec pos = e.getBlockPosition();
            if (!e.getBlock().key().equals(Block.CHISELED_BOOKSHELF.key())) return;
            BlockFace shelfFace = BlockFace.valueOf(e.getBlock().getProperty("facing").toUpperCase());
            if (e.getBlockFace() != shelfFace) return;

            BookLocation location = getBookCoordinates(pos, e.getCursorPosition(), e.getBlockFace());
            e.getInstance().setBlock(pos, setShelfBook(e.getBlock(), location.book() % 6, false));

            Book babelBook = catalogue.getBookAt(location);

            String title = babelBook.getPage(0).substring(0, 15).trim();
            String author = babelBook.getPage(0).substring(15, 15 + 15).trim();

            CompoundBinaryTag babelTag = CompoundBinaryTag.builder().putString("room", location.base32Room())
                    .putInt("wall", location.wall()).putInt("shelf", location.shelf()).putInt("book", location.book())
                    .build();

            CustomData data = new CustomData(babelTag);
            ItemStack book = ItemStack.builder(Material.WRITTEN_BOOK)
                    .set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(title, author, List.of()))
                    .set(DataComponents.CUSTOM_DATA, data).build();
            e.getPlayer().getInventory().addItemStack(book);
        });
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public void start(String host, int port) {
        server.start(host, port);
    }

    private static BookLocation getBookCoordinates(BlockVec pos, Point cursor, BlockFace clickedFace) {
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

        BigInteger room = BigInteger.valueOf(index.blockX())
                .add(BigInteger.valueOf(index.blockZ()).multiply(BigInteger.valueOf(MAX_ROOM.blockX())));

        return new BookLocation(room, wall, shelf, book + vIndex * 6);
    }

    private static Block setShelfBook(Block shelf, int slot, boolean has) {
        return shelf.withProperty("slot_%s_occupied".formatted(slot), Boolean.toString(has));
    }
}
