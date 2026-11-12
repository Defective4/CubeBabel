package io.github.defective4.cubebabel.player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerAnvilInputEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.inventory.type.AnvilInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class PlayerManager {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    private final Map<Player, Consumer<String>> anvilCallbacks = new HashMap<>();
    private final Map<Player, Consumer<String>> chatCallbacks = new HashMap<>();

    public PlayerManager() {
        GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();
        handler.addListener(PlayerAnvilInputEvent.class, e -> {
            if (e.getInventory() instanceof AnvilInventory anvil) {
                ItemStack item = anvil.getItemStack(0);
                if (item != null && !item.isAir()) {
                    anvil.setItemStack(2,
                            e.getInput().isBlank() ? ItemStack.AIR : item.withCustomName(Component.text(e.getInput())));
                    anvil.setRepairCost((short) 0);
                }
            }
        });
        handler.addListener(InventoryPreClickEvent.class, e -> {
            Player player = e.getPlayer();
            if (player.getOpenInventory() instanceof AnvilInventory anvil && anvilCallbacks.containsKey(player)) {
                e.setCancelled(true);
                if (e.getInventory() == anvil && e.getSlot() == 2) {
                    ItemStack item = anvil.getItemStack(2);
                    if (item != null && item.material() == Material.NAME_TAG) {
                        Component name = item.get(DataComponents.CUSTOM_NAME);
                        Consumer<String> callback = anvilCallbacks.get(player);
                        player.closeInventory();
                        callback.accept(name == null ? null : SERIALIZER.serialize(name));
                    }
                }
            }
        });
        handler.addListener(InventoryCloseEvent.class, e -> {
            if (e.getInventory() instanceof AnvilInventory) {
                anvilCallbacks.remove(e.getPlayer());
            }
        });
        handler.addListener(PlayerDisconnectEvent.class, e -> {
            Player player = e.getPlayer();
            anvilCallbacks.remove(player);
            chatCallbacks.remove(player);
        });
        handler.addListener(PlayerChatEvent.class, e -> {
            Player player = e.getPlayer();
            if (chatCallbacks.containsKey(player)) {
                e.setCancelled(true);
                chatCallbacks.get(player).accept(e.getRawMessage());
                chatCallbacks.remove(player);
            }
        });
    }

    public void chatPrompt(Player player, Component prompt, Consumer<String> callback) {
        chatCallbacks.put(player, callback);
        player.sendMessage(prompt);
    }

    public void openAnvilForResult(Player player, String prompt, String initial, Consumer<String> callback) {
        anvilCallbacks.put(player, callback);
        AnvilInventory anvil = new AnvilInventory(prompt);
        anvil.setItemStack(0, ItemStack.of(Material.NAME_TAG).withCustomName(Component.text(initial)));
        anvil.setRepairCost((short) 0);
        player.openInventory(anvil);
    }

    public static void initPlayer(PlayerLoadedEvent event) {
        Player p = event.getPlayer();
        p.setGameMode(GameMode.SURVIVAL);
    }

}
