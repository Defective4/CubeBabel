package io.github.defective4.cubebabel.storage;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerLoadedEvent;

public class PlayerStorage {
    private final Map<UUID, BigInteger> dimensions = new HashMap<>();

    public BigInteger getDimension(Player player) {
        return dimensions.getOrDefault(player.getUuid(), BigInteger.ZERO);
    }

    public void setDimension(Player player, BigInteger dimension) {
        dimensions.put(player.getUuid(), Objects.requireNonNull(dimension));
    }

    public static void initPlayer(PlayerLoadedEvent event) {
        Player p = event.getPlayer();
        p.setGameMode(GameMode.SURVIVAL);
    }
}
