package io.github.defective4.cubebabel;

import io.github.defective4.cubebabel.generator.LibraryGenerator;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

public class LibraryServer {
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
            p.teleport(new Pos(8, 42, 8));
        });
    }

    public void start(String host, int port) {
        server.start(host, port);
    }
}
