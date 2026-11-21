package io.github.defective4.cubebabel;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

import io.github.defective4.cubebabel.book.BookManager;
import io.github.defective4.cubebabel.book.Dictionary;
import io.github.defective4.cubebabel.command.BookDialogCommand;
import io.github.defective4.cubebabel.command.CenterCommand;
import io.github.defective4.cubebabel.command.FindBookCommand;
import io.github.defective4.cubebabel.command.GamemodeCommand;
import io.github.defective4.cubebabel.command.RandomBookCommand;
import io.github.defective4.cubebabel.command.SpawnCommand;
import io.github.defective4.cubebabel.generator.HubGenerator;
import io.github.defective4.cubebabel.generator.LibraryGenerator;
import io.github.defective4.cubebabel.schematic.SchematicManager;
import io.github.defective4.cubebabel.storage.PlayerStorage;
import io.github.defective4.cubebabel.storage.PlayerStorage.HighlightedBook;
import io.github.defective4.cubebabel.ui.HubCatalogue;
import io.github.defective4.cubebabel.ui.UIManager;
import io.github.defective4.jbabel.BabelCatalogue;
import io.github.defective4.jbabel.BabelLibrary;
import io.github.defective4.jbabel.BookLocation;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.WrittenBookContent;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.packet.server.play.UnloadChunkPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.SchedulerManager;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.DimensionType.Skybox;

public class LibraryServer {
    public static final BlockVec CENTER_ROOM;
    public static final BlockVec CENTER_ROOM_LOCATION;
    public static final BlockVec MAX_ROOM = new BlockVec(2727270, 2142855);
    public static final Pos SPAWN_POINT = new Pos(6.5, 67, 3.5);
    private static LibraryServer INSTANCE;
    private final BookManager bookManager;
    private final BabelCatalogue catalogue;
    private final Dictionary dictionary;

    private final HubCatalogue hubCatalogue;

    private final InstanceContainer hubInstance;
    private final BabelLibrary library;
    private final InstanceContainer libraryInstance;
    private final PlayerStorage playerStorage;
    private final SchematicManager schematicManager = new SchematicManager();
    private final MinecraftServer server;
    private final UIManager uiManager;

    static {
        CENTER_ROOM = new BlockVec(MAX_ROOM.blockX() / 2, MAX_ROOM.blockZ() / 2);
        CENTER_ROOM_LOCATION = LibraryGenerator.getRoomLocation(CENTER_ROOM.blockX(), CENTER_ROOM.blockZ());
    }

    public LibraryServer(BabelLibrary library, Dictionary dictionary) {
        if (INSTANCE != null) throw new IllegalStateException("Only one library server instance is allowed at a time");
        INSTANCE = this;
        this.library = Objects.requireNonNull(library);
        this.dictionary = Objects.requireNonNull(dictionary);
        server = MinecraftServer.init();
        libraryInstance = initInstance("babel:library", new LibraryGenerator());
        hubInstance = initInstance("babel:hub", new HubGenerator(schematicManager.getHub()));
        playerStorage = new PlayerStorage();
        catalogue = new BabelCatalogue.Builder(library).withRandomBookBits(1024 * 16).create();
        bookManager = new BookManager();
        hubCatalogue = new HubCatalogue();
        uiManager = new UIManager();
        initEvents();
        initCommands();
        initSchedulers();

        MinecraftServer.getConnectionManager().setPlayerProvider(
                (con, profile) -> new Player(con, new GameProfile(UUID.randomUUID(), profile.name())));
    }

    public BabelCatalogue getCatalogue() {
        return catalogue;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    public HubCatalogue getHubCatalogue() {
        return hubCatalogue;
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

    public PlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public UIManager getUIManager() {
        return uiManager;
    }

    public void start(String host, int port) {
        server.start(host, port);
    }

    private void handleInteraction(Player player, BlockVec pos, Block block, Point cursorPosition, BlockFace blockFace,
            Instance instance) {
        int bookIndex = BookManager.getShelfBookIndex(cursorPosition, blockFace);
        BookLocation location = BookManager.getBookCoordinates(pos, bookIndex, playerStorage.getDimension(player));
        HighlightedBook hl = playerStorage.getResolvedHighlightedBooks().get(player);
        if (hl != null && hl.location().equals(location)) {
            playerStorage.getResolvedHighlightedBooks().remove(player);
            if (hl.page() >= 0) {
                player.sendMessage(Component.text("The content you are looking for is on page %s".formatted(hl.page()))
                        .color(LIGHT_PURPLE));
            }
        }
        ItemStack book = bookManager.getBookFrom(location, block, instance);

        if (BookManager.isBookSlotOccupied(block, bookIndex)) {
            playerStorage.putBookshelf(player, pos, BookManager.setShelfBook(block, location.book() % 6, false));
            player.getInventory().addItemStack(book);
            player.playSound(Sound.sound(SoundEvent.BLOCK_CHISELED_BOOKSHELF_PICKUP, Source.BLOCK, 1f, 1f));
        } else {
            ItemStack item = player.getInventory().getEquipment(EquipmentSlot.MAIN_HAND, player.getHeldSlot());
            WrittenBookContent bookContent = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (item != null && item.material() == Material.WRITTEN_BOOK) {
                if (book.equals(item)) {
                    player.getInventory().takeItemStack(item, TransactionOption.ALL);
                    playerStorage.putBookshelf(player, pos, BookManager.setShelfBook(block, location.book() % 6, true));
                    player.playSound(Sound.sound(SoundEvent.BLOCK_CHISELED_BOOKSHELF_INSERT, Source.BLOCK, 1f, 1f));
                } else {
                    player.sendMessage(Component.text("The book won't fit here...").color(RED));
                    sendLabel(player, bookContent);
                }
            } else {
                player.sendMessage(Component.text("There is no book on this shelf anymore...").color(RED));
                sendLabel(player, bookContent);
            }
        }
    }

    private void initEvents() {
        GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();
        handler.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            Pos vec = SPAWN_POINT;
            e.getPlayer().setRespawnPoint(vec);
            e.setSpawningInstance(hubInstance);
        });

        handler.addListener(PlayerLoadedEvent.class, event -> {
            PlayerStorage.initPlayer(event);
        });

        handler.addListener(PlayerUseItemEvent.class, e -> {
            ItemStack item = e.getItemStack();
            if (item != null && item.material() == Material.WRITTEN_BOOK && playerStorage.checkForTask(e.getPlayer())) {
                BookManager.openBook(e.getPlayer(), item);
            }
        });

        handler.addListener(PlayerBlockBreakEvent.class, e -> e.setCancelled(true));
        handler.addListener(PlayerBlockPlaceEvent.class, e -> e.setCancelled(true));
        handler.addListener(PlayerMoveEvent.class, e -> {
            Pos pos = e.getNewPosition();
            if (pos.y() < 0) {
                pos = pos.withY(39);
                Player player = e.getPlayer();
                Instance instance = player.getInstance();
                for (int y = pos.blockY(); y < 100; y++) {
                    pos = pos.add(0, 1, 0);
                    if (instance.getBlock(pos).solid()) {
                        pos = pos.add(0, 1, 0);
                        break;
                    }
                }
                player.teleport(pos);
            }
        });
        handler.addListener(PlayerPacketOutEvent.class, e -> {
            int cx, cz;
            if (e.getPacket() instanceof ChunkDataPacket p) {
                cx = p.chunkX();
                cz = p.chunkZ();
            } else if (e.getPacket() instanceof UnloadChunkPacket p) {
                cx = p.chunkX();
                cz = p.chunkZ();
            } else
                return;
            playerStorage.removeBookshelves(e.getPlayer(), cx, cz);
        });
        handler.addListener(AddEntityToInstanceEvent.class, e -> {
            if (e.getEntity() instanceof Player player) {
                playerStorage.removeBookshelves(player);
            }
        });

        libraryInstance.eventNode().addListener(PlayerBlockInteractEvent.class, e -> {
            e.setCancelled(true);
            if (e.getInstance() != libraryInstance) return;
            Player player = e.getPlayer();
            if (player.isSneaking()) return;
            BlockVec pos = e.getBlockPosition();
            Block block = playerStorage.getBookshelf(player, pos);
            if (!block.key().equals(Block.CHISELED_BOOKSHELF.key())) return;
            BlockFace shelfFace = BookManager.getBlockFace(block);
            if (e.getBlockFace() != shelfFace) return;

            handleInteraction(player, pos, block, e.getCursorPosition(), shelfFace, e.getInstance());
        });

        hubCatalogue.initEvents();
    }

    private void initSchedulers() {
        Map<Player, HighlightedBook> hlBlocks = playerStorage.getHighlightedBooks();
        Map<Player, HighlightedBook> rlBlocks = playerStorage.getResolvedHighlightedBooks();
        List<Player> resolved = new ArrayList<>();

        SchedulerManager schedulerManager = MinecraftServer.getSchedulerManager();
        schedulerManager.scheduleTask(() -> {
            for (Entry<Player, HighlightedBook> entry : hlBlocks.entrySet()) {
                Player player = entry.getKey();
                if (!player.isOnline()) {
                    resolved.add(player);
                    continue;
                }
                HighlightedBook hlBook = entry.getValue();
                int bookIndex = hlBook.location().book() % 6;
                BlockVec blockVec = hlBook.block();
                if (libraryInstance.isChunkLoaded(blockVec)) {
                    Block block = libraryInstance.getBlock(blockVec);
                    if (block.material() == Material.CHISELED_BOOKSHELF) {
                        BlockFace face = BookManager.getBlockFace(block);
                        Pos particleVec = blockVec.asPos().add(0.5, bookIndex >= 3 ? 0.1 : 0.6, 0.5);

                        double mod = 1 / 3d / 2d + bookIndex % 3 / 3d - 0.5;

                        particleVec = switch (face) {
                            default -> particleVec.sub(0.6, 0, -mod);
                            case SOUTH -> particleVec.add(mod, 0, 0.6);
                            case EAST -> particleVec.add(0.6, 0, -mod);
                            case NORTH -> particleVec.sub(mod, 0, 0.6);
                        };

                        if (player.isOnline()) {
                            playerStorage.resolveHighlight(player,
                                    new HighlightedBook(blockVec, hlBook.location(), particleVec, hlBook.page()));
                        }
                        resolved.add(player);
                    }
                }
            }
            if (!resolved.isEmpty()) {
                for (Player u : resolved) {
                    hlBlocks.remove(u);
                }
                resolved.clear();
            }
            for (Entry<Player, HighlightedBook> entry : rlBlocks.entrySet()) {
                Point value = entry.getValue().particlesLocation();
                Player player = entry.getKey();
                if (player.getInstance() != libraryInstance) {
                    continue;
                }
                if (!player.isOnline()) {
                    resolved.add(player);
                    continue;
                }
                if (libraryInstance.isChunkLoaded(value) && value.distance(player.getPosition()) < 13) {
                    Particle particle = Particle.DUST.withColor(NamedTextColor.GREEN);
                    player.sendPacket(new ParticlePacket(particle, value, new Pos(0, 0, 0), 0, 1));
                }
            }
            if (!resolved.isEmpty()) {
                for (Player u : resolved) {
                    rlBlocks.remove(u);
                }
                resolved.clear();
            }
        }, TaskSchedule.immediate(), TaskSchedule.tick(5));
    }

    public static LibraryServer getInstance() {
        return INSTANCE;
    }

    public static void teleportPlayer(Player player, InstanceContainer instance, Pos target) {
        if (player.getInstance() == instance) {
            player.teleport(target);
        } else {
            player.setInstance(instance, target);
        }
    }

    private static void initCommands() {
        CommandManager mgr = MinecraftServer.getCommandManager();

        mgr.register(new RandomBookCommand());
        mgr.register(new SpawnCommand());
        mgr.register(new FindBookCommand());
        mgr.register(new CenterCommand());
//        mgr.register(new DebugCommand(this));
        mgr.register(new GamemodeCommand());
        mgr.register(new BookDialogCommand());
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
