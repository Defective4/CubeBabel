package io.github.defective4.cubebabel.ui;

import static io.github.defective4.cubebabel.ui.inventory.OpenOptionsInventory.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.storage.PlayerStorage;
import io.github.defective4.cubebabel.ui.inventory.AnvilDialogInventory;
import io.github.defective4.cubebabel.ui.inventory.AnvilDialogInventory.RandomType;
import io.github.defective4.cubebabel.ui.inventory.OpenOptionsInventory;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerAnvilInputEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.inventory.type.AnvilInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;

public class UIManager {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    private final Map<Player, Consumer<String>> anvilCallbacks = new HashMap<>();
    private final Map<Player, Consumer<String>> bookCallbacks = new HashMap<>();
    private final Map<Player, Consumer<String>> chatCallbacks = new HashMap<>();
    private final LibraryServer server;

    public UIManager(LibraryServer server) {
        this.server = server;
        GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();
        handler.addListener(PlayerAnvilInputEvent.class, e -> {
            if (e.getInventory() instanceof AnvilInventory anvil) {
                ItemStack item = anvil.getItemStack(0);
                if (item != null && !item.isAir()) {
                    anvil.setItemStack(2,
                            e.getInput().isBlank() ? ItemStack.AIR
                                    : item.withCustomName(
                                            Component.text(e.getInput()).decoration(TextDecoration.ITALIC, false)));
                    anvil.setRepairCost((short) 0);
                }
            }
        });
        handler.addListener(InventoryPreClickEvent.class, e -> {
            Player player = e.getPlayer();
            AbstractInventory inventory = player.getOpenInventory();
            if (inventory instanceof AnvilDialogInventory anvil && anvilCallbacks.containsKey(player)) {
                e.setCancelled(true);
                if (e.getInventory() == anvil) {
                    switch (e.getSlot()) {
                        case 2 -> {
                            ItemStack item = anvil.getItemStack(2);
                            if (item != null && item.material() == Material.NAME_TAG) {
                                Component name = item.get(DataComponents.CUSTOM_NAME);
                                Consumer<String> callback = anvilCallbacks.get(player);
                                player.closeInventory();
                                callback.accept(name == null ? null : SERIALIZER.serialize(name));
                            }
                        }
                        case 1 -> {
                            if (e.getClick() instanceof Click.Left) {
                                anvil.randomize();
                            } else if (e.getClick() instanceof Click.Right) {
                                anvil.switchRandomMode();
                                player.playSound(Sound.sound(SoundEvent.BLOCK_LEVER_CLICK, Source.UI, 1f, 1f));
                            }
                        }
                        default -> {
                        }
                    }
                }
            } else if (inventory instanceof OpenOptionsInventory ops) {
                e.setCancelled(true);
                if (e.getInventory() == ops) {
                    switch (e.getSlot()) {
                        case SLOT_CANCEL -> player.closeInventory();
                        case SLOT_OPEN -> player.openBook(ops.getBook());
                        case SLOT_HIGHLIGHT -> {
                            player.closeInventory();
                            PlayerStorage storage = server.getPlayerStorage();
                            if (storage.checkForTask(player)) {
                                BossBar progress = BossBar.bossBar(
                                        Component.text("Highlighting words in the book...").color(NamedTextColor.GREEN),
                                        0f, Color.GREEN, Overlay.PROGRESS);
                                player.showBossBar(progress);
                                storage.putTask(player, server.getDictionary().highlightWords(ops.getBook(), book -> {
                                    player.hideBossBar(progress);
                                    storage.clearTask(player);
                                    player.openBook(book);
                                }, prog -> progress.progress(prog)));
                            }
                        }
                        case SLOT_OPEN_PAGE -> {
                            server.getUIManager().openAnvilForResult(player, "Page number", "1", numStr -> {
                                Book book = ops.getBook();
                                List<Component> pages = book.pages();
                                int num = 0;
                                try {
                                    num = Integer.parseInt(numStr);
                                    if (num < 1 || num > pages.size()) throw new NumberFormatException();
                                } catch (NumberFormatException e2) {
                                    player.sendMessage(Component.text("The page number you entered is invalid")
                                            .color(NamedTextColor.RED));
                                    return;
                                }
                                player.openBook(Book.book(book.title(), book.author(), List.of(pages.get(num - 1))));
                            }, RandomType.PAGE);
                        }
                        default -> {
                        }
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
            bookCallbacks.remove(player);
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

    public boolean executeBookCallback(Player player, String response) {
        if (bookCallbacks.containsKey(player)) {
            Consumer<String> callback = bookCallbacks.get(player);
            bookCallbacks.remove(player);
            callback.accept(response);
            return true;
        }
        return false;
    }

    public void openAnvilForResult(Player player, String prompt, String initial, Consumer<String> callback,
            RandomType randomType) {
        anvilCallbacks.put(player, callback);
        AnvilInventory anvil = new AnvilDialogInventory(prompt, initial, randomType, server);
        player.openInventory(anvil);
    }

    public void openBookDialog(Player player, Book book, Consumer<String> callback) {
        bookCallbacks.put(player, callback);
        player.openBook(book);
    }

}
