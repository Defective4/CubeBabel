package io.github.defective4.cubebabel;

import java.util.Objects;

import io.github.defective4.cubebabel.book.BookManager;
import io.github.defective4.cubebabel.book.HubCatalogue;
import io.github.defective4.cubebabel.command.FindBookCommand;
import io.github.defective4.cubebabel.command.RandomBookCommand;
import io.github.defective4.cubebabel.command.SpawnCommand;
import io.github.defective4.cubebabel.generator.HubGenerator;
import io.github.defective4.cubebabel.generator.LibraryGenerator;
import io.github.defective4.cubebabel.player.PlayerManager;
import io.github.defective4.cubebabel.player.PlayerStorage;
import io.github.defective4.cubebabel.room.HexagonManager;
import io.github.defective4.cubebabel.schematic.SchematicManager;
import io.github.defective4.jbabel.BabelCatalogue;
import io.github.defective4.jbabel.BabelLibrary;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
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
import net.minestom.server.world.DimensionType.Skybox;

public class LibraryServer {
    public static final BlockVec CENTER_ROOM;
    public static final BlockVec CENTER_ROOM_LOCATION;
    public static final BlockVec MAX_ROOM = new BlockVec(2727270, 2142855);
    public static final Pos SPAWN_POINT = new Pos(6.5, 67, 3.5);
    private final BookManager bookManager;
    private final BabelCatalogue catalogue;
    private final HubCatalogue hubCatalogue;
    private final InstanceContainer hubInstance;

    private final BabelLibrary library;

    private final InstanceContainer libraryInstance;
    private final PlayerManager playerManager;
    private final PlayerStorage playerStorage = new PlayerStorage();
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
        catalogue = new BabelCatalogue.Builder(library).withRandomBookBits(1024 * 16).create();
        bookManager = new BookManager(this);
        hubCatalogue = new HubCatalogue(this);
        playerManager = new PlayerManager();
        initEvents();
        initCommands();
    }

    public BabelCatalogue getCatalogue() {
        return catalogue;
    }

    public InstanceContainer getHubInstance() {
        return hubInstance;
    }

    public BabelLibrary getLibrary() {
        return library;
    }

    public InstanceContainer getLibraryInstance() {
        return libraryInstance;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public PlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public void start(String host, int port) {
        server.start(host, port);
    }

    private void initCommands() {
        CommandManager mgr = MinecraftServer.getCommandManager();

        mgr.register(new RandomBookCommand(this));
        mgr.register(new SpawnCommand(this));
        mgr.register(new FindBookCommand(this));
    }

    private void initEvents() {
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> {
            Pos vec = SPAWN_POINT;
            e.getPlayer().setRespawnPoint(vec);
            e.setSpawningInstance(hubInstance);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoadedEvent.class, event -> {
            PlayerManager.initPlayer(event);
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

        libraryInstance.eventNode().addListener(PlayerBlockInteractEvent.class, e -> {
            e.setCancelled(true);
            if (e.getPlayer().isSneaking()) return;
            BlockVec pos = e.getBlockPosition();
            if (!e.getBlock().key().equals(Block.CHISELED_BOOKSHELF.key())) return;
            BlockFace shelfFace = BookManager.getBlockFace(e.getBlock());
            if (e.getBlockFace() != shelfFace) return;

            ItemStack book = bookManager.getBookAt(pos, e.getCursorPosition(), e.getBlockFace(), e.getBlock(),
                    e.getInstance(), playerStorage.getDimension(e.getPlayer()));
            e.getPlayer().getInventory().addItemStack(book);
        });

        hubCatalogue.initEvents();
    }

    private static InstanceContainer initInstance(String dimension, Generator generator) {
        InstanceContainer instance = MinecraftServer.getInstanceManager()
                .createInstanceContainer(MinecraftServer.getDimensionTypeRegistry().register(dimension,
                        DimensionType.builder().skybox(Skybox.END).ambientLight(1.0f).build()));
        instance.setChunkSupplier(LightingChunk::new);
        instance.setGenerator(generator);
        return instance;

    }
}
