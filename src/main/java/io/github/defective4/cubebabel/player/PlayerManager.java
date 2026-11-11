package io.github.defective4.cubebabel.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerLoadedEvent;

public class PlayerManager {

    public static void initPlayer(PlayerLoadedEvent event) {
        Player p = event.getPlayer();
        p.setGameMode(GameMode.SURVIVAL);
        Pos vec = new Pos(6.5, 67, 3.5);
        p.teleport(vec.asPos());
    }

}
