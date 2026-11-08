package io.github.defective4.cubebabel;

import io.github.defective4.cubebabel.generator.LibraryGenerator;
import io.github.defective4.cubebabel.room.HexagonManager;
import io.github.defective4.cubebabel.schematic.SchematicManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

public class LibraryServer {
    public static final BlockVec CENTER_ROOM;
    public static final BlockVec CENTER_ROOM_LOCATION;
    public static final BlockVec MAX_ROOM = new BlockVec(2727270, 2142855);
    private final RegistryKey<DimensionType> libraryDimension;
    private final InstanceContainer libraryInstance;
    private final SchematicManager schematicManager = new SchematicManager();
    private final MinecraftServer server;

    static {
        CENTER_ROOM = new BlockVec(MAX_ROOM.blockX() / 2, MAX_ROOM.blockZ() / 2);
        CENTER_ROOM_LOCATION = HexagonManager.getRoomLocation(CENTER_ROOM.blockX(), CENTER_ROOM.blockZ());
    }

    public LibraryServer() {
        server = MinecraftServer.init();
        libraryDimension = MinecraftServer.getDimensionTypeRegistry().register("minecraft:library",
                DimensionType.builder().ambientLight(1.0f).build());

        libraryInstance = MinecraftServer.getInstanceManager().createInstanceContainer(libraryDimension);
        libraryInstance.setChunkSupplier(LightingChunk::new);
        libraryInstance.setGenerator(new LibraryGenerator(this));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class,
                e -> e.setSpawningInstance(libraryInstance));

        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoadedEvent.class, event -> {
            Player p = event.getPlayer();
            p.setGameMode(GameMode.CREATIVE);
            BlockVec vec = new BlockVec(MAX_ROOM.blockX() / 2, MAX_ROOM.blockZ() / 2);
            p.teleport(HexagonManager.getRoomLocation(vec.blockX(), vec.blockZ()).withY(64).asPos());
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent.class, e -> {
            e.setCancelled(true);
            BlockVec roomIndex = HexagonManager.getRoomIndex(e.getEntity().getPosition().asBlockVec());
            int x = roomIndex.blockX();
            int z = roomIndex.blockZ();
            e.getPlayer().sendMessage(HexagonManager.getRoomLocation(x, z).toString());
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
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public void start(String host, int port) {
        server.start(host, port);
    }
}
