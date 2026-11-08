package io.github.defective4.cubebabel;

import java.util.Arrays;

import io.github.defective4.cubebabel.generator.LibraryGenerator;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

public class LibraryServer {
    private static final BlockVec MAX_ROOM = new BlockVec(2727270, 2142855);
    private final RegistryKey<DimensionType> libraryDimension;
    private final InstanceContainer libraryInstance;
    private final MinecraftServer server;

    public LibraryServer() {
        server = MinecraftServer.init();
        libraryDimension = MinecraftServer.getDimensionTypeRegistry().register("minecraft:library",
                DimensionType.builder().ambientLight(1.0f).build());

        libraryInstance = MinecraftServer.getInstanceManager().createInstanceContainer(libraryDimension);
        libraryInstance.setChunkSupplier(LightingChunk::new);
        libraryInstance.setGenerator(new LibraryGenerator());

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class,
                e -> e.setSpawningInstance(libraryInstance));

        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoadedEvent.class, event -> {
            Player p = event.getPlayer();
            p.setGameMode(GameMode.CREATIVE);
            BlockVec vec = new BlockVec(MAX_ROOM.blockX() / 2, MAX_ROOM.blockZ() / 2);
            p.teleport(getRoomLocation(vec.blockX(), vec.blockZ()).withY(41).asPos());
        });
    }

    public void start(String host, int port) {
        server.start(host, port);
    }

    public static BlockVec getRoomIndex(BlockVec pos) {
        int z = pos.blockZ() + 7;
        boolean b = false;
        if ((pos.blockX() + 7) / 11 % 2 == 1) {
            z -= 7;
            b = true;
        }

        int cx = (pos.blockX() + 7) / 11 * 11;
        int cz = z / 14 * 14;
        if (b) {
            cz += 7;
        }

        int y = pos.blockY();

        BlockVec[] vectors = new BlockVec[] { new BlockVec(cx, y, cz), new BlockVec(cx + 11, y, cz + 7),
                new BlockVec(cx + 11, y, cz - 7), new BlockVec(cx - 11, y, cz + 7), new BlockVec(cx - 11, y, cz - 7),
                new BlockVec(cx, y, cz + 14), new BlockVec(cx, y, cz - 14), };

        BlockVec closest = Arrays.stream(vectors).sorted((v1, v2) -> {
            double diff = v1.distance(pos) - v2.distance(pos);
            return diff > 0 ? 1 : diff < 0 ? -1 : 0;
        }).findFirst().get();

        int roomX = closest.blockX() / 11;
        int roomZ = closest.blockZ() / 14;

        return new BlockVec(roomX, roomZ);
    }

    public static BlockVec getRoomLocation(int x, int z) {
        int roomX = x * 11;
        int roomZ = z * 14 + roomX % 2 * 7;

        return new BlockVec(roomX, roomZ);
    }
}
