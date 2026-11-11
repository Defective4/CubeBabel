package io.github.defective4.cubebabel;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import io.github.defective4.cubebabel.book.BookManager;
import io.github.defective4.cubebabel.generator.HubGenerator;
import io.github.defective4.cubebabel.generator.LibraryGenerator;
import io.github.defective4.cubebabel.player.PlayerManager;
import io.github.defective4.cubebabel.room.HexagonManager;
import io.github.defective4.cubebabel.schematic.SchematicManager;
import io.github.defective4.jbabel.BabelCatalogue;
import io.github.defective4.jbabel.BabelLibrary;
import io.github.defective4.jbabel.Book;
import io.github.defective4.jbabel.BookLocation;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.world.DimensionType;

public class LibraryServer {
    public static final BlockVec CENTER_ROOM;
    public static final BlockVec CENTER_ROOM_LOCATION;
    public static final BlockVec MAX_ROOM = new BlockVec(2727270, 2142855);
    private final BookManager bookManager;
    private final BabelCatalogue catalogue;
    private BigInteger dimension = BigInteger.ZERO; // TODO proper dimension managements
    private final InstanceContainer hubInstance;
    private final BabelLibrary library;

    private final InstanceContainer libraryInstance;

    private final SchematicManager schematicManager = new SchematicManager();
    private final MinecraftServer server;

    static {
        CENTER_ROOM = new BlockVec(MAX_ROOM.blockX() / 2, MAX_ROOM.blockZ() / 2);
        CENTER_ROOM_LOCATION = HexagonManager.getRoomLocation(CENTER_ROOM.blockX(), CENTER_ROOM.blockZ());
    }

    public LibraryServer(BabelLibrary library) {
        this.library = Objects.requireNonNull(library);
        server = MinecraftServer.init();
        libraryInstance = initInstance("babel:library", new LibraryGenerator(this));
        hubInstance = initInstance("babel:hub", new HubGenerator(schematicManager.getHub(), this));
        catalogue = new BabelCatalogue.Builder(library).create();
        bookManager = new BookManager(this);
        initEvents();
    }

    public BabelCatalogue getCatalogue() {
        return catalogue;
    }

    // TODO
    public BigInteger getDimension() {
        return dimension;
    }

    public InstanceContainer getHubInstance() {
        return hubInstance;
    }

    public BabelLibrary getLibrary() {
        return library;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public void start(String host, int port) {
        server.start(host, port);
    }

    private void initEvents() {
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class,
                e -> e.setSpawningInstance(hubInstance));

        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoadedEvent.class, event -> {
            PlayerManager.initPlayer(event);
        });

        MinecraftServer.getCommandManager().register(new Command("find") {
            {
                setDefaultExecutor((sender, ctx) -> {
                    String[] args = ctx.getInput().split(" ");
                    String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    Book book = catalogue.findBook(msg);
                    BookLocation location = book.getBookLocation();
                    BigInteger maxW = BigInteger.valueOf(MAX_ROOM.blockX());
                    BigInteger square = maxW.multiply(BigInteger.valueOf(MAX_ROOM.blockZ()));
                    dimension = location.room().divide(square);
                    BigInteger room = location.room().mod(square);

                    System.out.println(location.wall());
                    System.out.println(location.shelf());
                    System.out.println(location.book());

                    int z = room.divide(maxW).intValueExact();
                    int x = room.mod(maxW).intValueExact();

                    ((Player) sender).teleport(HexagonManager.getRoomLocation(x, z).asPos().withY(64));
                });
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, e -> {
            ItemStack item = e.getItemStack();
            if (item != null && item.material() == Material.WRITTEN_BOOK) {
                BookManager.openBook(e.getPlayer(), item);
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockBreakEvent.class, e -> {
            e.setCancelled(true);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockPlaceEvent.class, e -> {
            e.setCancelled(true);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockInteractEvent.class, e -> {
            e.setCancelled(true);
            if (e.getPlayer().isSneaking()) return;
            BlockVec pos = e.getBlockPosition();
            if (!e.getBlock().key().equals(Block.CHISELED_BOOKSHELF.key())) return;
            BlockFace shelfFace = BookManager.getBlockFace(e.getBlock());
            if (e.getBlockFace() != shelfFace) return;

            ItemStack book = bookManager.getBookAt(pos, e.getCursorPosition(), e.getBlockFace(), e.getBlock(),
                    e.getInstance());
            e.getPlayer().getInventory().addItemStack(book);
        });
    }

    private static InstanceContainer initInstance(String dimension, Generator generator) {
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(MinecraftServer
                .getDimensionTypeRegistry().register(dimension, DimensionType.builder().ambientLight(1.0f).build()));
        instance.setChunkSupplier(LightingChunk::new);
        instance.setGenerator(generator);
        return instance;

    }
}
