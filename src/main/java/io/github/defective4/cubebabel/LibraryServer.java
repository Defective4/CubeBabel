package io.github.defective4.cubebabel;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

import java.util.Objects;

import io.github.defective4.cubebabel.book.BookManager;
import io.github.defective4.cubebabel.book.HubCatalogue;
import io.github.defective4.cubebabel.command.FindBookCommand;
import io.github.defective4.cubebabel.command.RandomBookCommand;
import io.github.defective4.cubebabel.command.SpawnCommand;
import io.github.defective4.cubebabel.generator.HubGenerator;
import io.github.defective4.cubebabel.generator.LibraryGenerator;
import io.github.defective4.cubebabel.schematic.SchematicManager;
import io.github.defective4.cubebabel.storage.PlayerStorage;
import io.github.defective4.cubebabel.ui.UIManager;
import io.github.defective4.jbabel.BabelCatalogue;
import io.github.defective4.jbabel.BabelLibrary;
import io.github.defective4.jbabel.BookLocation;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.WrittenBookContent;
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
    private final UIManager playerManager;
    private final PlayerStorage playerStorage = new PlayerStorage();
    private final SchematicManager schematicManager = new SchematicManager();
    private final MinecraftServer server;

    static {
        CENTER_ROOM = new BlockVec(MAX_ROOM.blockX() / 2, MAX_ROOM.blockZ() / 2);
        CENTER_ROOM_LOCATION = LibraryGenerator.getRoomLocation(CENTER_ROOM.blockX(), CENTER_ROOM.blockZ());
    }

    public LibraryServer(BabelLibrary library) {
        this.library = Objects.requireNonNull(library);
        server = MinecraftServer.init();
        libraryInstance = initInstance("babel:library", new LibraryGenerator(this));
        hubInstance = initInstance("babel:hub", new HubGenerator(schematicManager.getHub(), this));
        catalogue = new BabelCatalogue.Builder(library).withRandomBookBits(1024 * 16).create();
        bookManager = new BookManager(this);
        hubCatalogue = new HubCatalogue(this);
        playerManager = new UIManager();
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

    public UIManager getPlayerManager() {
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
            PlayerStorage.initPlayer(event);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, e -> {
            ItemStack item = e.getItemStack();
            if (item != null && item.material() == Material.WRITTEN_BOOK) {
                BookManager.openBook(e.getPlayer(), item);
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockBreakEvent.class, e -> e.setCancelled(true));

        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockPlaceEvent.class, e -> e.setCancelled(true));

        MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent.class, e -> {
            Player player = e.getPlayer();
            BlockVec pos = player.getPosition().asBlockVec();
            BlockVec index = LibraryGenerator.getRoomIndex(pos);

            for (int i = 0; i < 4; i++) {
                BlockVec loc = BookManager.calculateBookshelfCoordinates(index, i);
                player.sendMessage("%s = %s".formatted(i, loc));
                if (loc != null) {
                    player.getInstance().setBlock(loc.withBlockY(41), switch (i) {
                        case 1 -> Block.YELLOW_WOOL;
                        case 2 -> Block.BLUE_WOOL;
                        case 3 -> Block.GREEN_WOOL;
                        default -> Block.RED_WOOL;
                    });
                }
            }
        });

        libraryInstance.eventNode().addListener(PlayerBlockInteractEvent.class, e -> {
            e.setCancelled(true);
            Player player = e.getPlayer();
            if (player.isSneaking()) return;
            BlockVec pos = e.getBlockPosition();
            Block block = e.getBlock();
            if (!block.key().equals(Block.CHISELED_BOOKSHELF.key())) return;
            BlockFace shelfFace = BookManager.getBlockFace(block);
            if (e.getBlockFace() != shelfFace) return;

            int bookIndex = BookManager.getShelfBookIndex(e.getCursorPosition(), e.getBlockFace());
            BookLocation location = BookManager.getBookCoordinates(pos, bookIndex, playerStorage.getDimension(player));
            ItemStack book = bookManager.getBookFrom(location, block, e.getInstance());

            if (BookManager.isBookSlotOccupied(block, bookIndex)) {
                e.getInstance().setBlock(pos, BookManager.setShelfBook(block, location.book() % 6, false));
                player.getInventory().addItemStack(book);
            } else {
                ItemStack item = player.getInventory().getEquipment(EquipmentSlot.MAIN_HAND, player.getHeldSlot());
                WrittenBookContent bookContent = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
                if (item != null && item.material() == Material.WRITTEN_BOOK) {
                    if (book.equals(item)) {
                        player.getInventory().takeItemStack(item, TransactionOption.ALL);
                        e.getInstance().setBlock(pos, BookManager.setShelfBook(block, location.book() % 6, true));
                    } else {
                        player.sendMessage(Component.text("The book won't fit here...").color(RED));
                        sendLabel(player, bookContent);
                    }
                } else {
                    player.sendMessage(Component.text("There is no book on this shelf anymore...").color(RED));
                    sendLabel(player, bookContent);
                }
            }
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

    private static void sendLabel(Player player, WrittenBookContent bookContent) {
        player.sendMessage(Component.text("A label on this shelf reads ").color(RED)
                .append(Component.text(bookContent.title().text()).color(YELLOW).decorate(UNDERLINED))
                .append(Component.text(" by ").color(RED))
                .append(Component.text(bookContent.author()).color(YELLOW).decorate(UNDERLINED)));
    }
}
