package io.github.defective4.cubebabel.ui.inventory;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.command.FindBookCommand;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.type.AnvilInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class AnvilDialogInventory extends AnvilInventory {

    public enum RandomTitleMode {
        RANDOM("Random"), WORDS("Dictionary");

        private final String friendlyName;

        private RandomTitleMode(String friendlyName) {
            this.friendlyName = friendlyName;
        }
    }

    public enum RandomType {
        DIMENSION("Dimension ID"), PAGE("Page number"), TITLE("Text");

        private final String friendlyName;

        private RandomType(String friendlyName) {
            this.friendlyName = friendlyName;
        }
    }

    private static final int HEX_RANDOM_BITS = 85;

    private final ItemStack input;
    private final Random random = new Random();
    private RandomTitleMode randomTitleMode = RandomTitleMode.RANDOM;
    private int randomTitleModeIndex = 0;
    private final RandomType randType;
    private final LibraryServer server;

    public AnvilDialogInventory(String title, String initial, RandomType randomType, LibraryServer server) {
        super(title);
        randType = randomType;
        input = ItemStack.of(Material.NAME_TAG).withCustomName(Component.text(initial).decoration(ITALIC, false));
        this.server = server;
        setItemStack(0, input);
        ItemStack randomizer = createRandomizer();
        setItemStack(1, randomizer);
        setRepairCost((short) 0);
    }

    public void randomize() {
        String newValue = switch (randType) {
            case DIMENSION -> new BigInteger(HEX_RANDOM_BITS, random).toString(32);
            case TITLE -> switch (randomTitleMode) {
                case RANDOM -> {
                    char[] charset = server.getLibrary().getChars();
                    StringBuilder builder = new StringBuilder(15);
                    for (int i = 0; i < builder.capacity(); i++) {
                        builder.append(charset[random.nextInt(charset.length)]);
                    }
                    yield builder.toString().trim();
                }
                case WORDS -> {
                    StringBuilder builder = new StringBuilder(15);
                    FindBookCommand.fillBuilderWithRandomWords(random, server.getDictionary(), builder);
                    yield builder.toString().trim();
                }
                default -> null;
            };
            default -> newValue = Integer.toString(random.nextInt(100) + 1);
        };

        setItemStack(0, input.withCustomName(Component.text(newValue).decoration(ITALIC, false)));
    }

    public void switchRandomMode() {
        randomTitleModeIndex = (randomTitleModeIndex + 1) % RandomTitleMode.values().length;
        randomTitleMode = RandomTitleMode.values()[randomTitleModeIndex];
        setItemStack(1, createRandomizer());
    }

    private ItemStack createRandomizer() {
        ItemStack item = ItemStack.of(Material.EMERALD)
                .withCustomName(Component.text("Randomize").color(GREEN).decoration(ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Mode:").color(GRAY).decoration(ITALIC, false));
        if (randType == RandomType.TITLE) {
            for (RandomTitleMode mode : RandomTitleMode.values()) {
                boolean selected = mode == randomTitleMode;
                lore.add(Component.text("> ").color(DARK_GRAY).decoration(ITALIC, false).append(Component
                        .text(mode.friendlyName).color(selected ? GRAY : DARK_GRAY).decoration(UNDERLINED, selected)));
            }
            lore.add(Component.text(""));
            lore.add(Component.text("[RMB] to switch modes").decoration(ITALIC, false).color(YELLOW));
        } else {
            lore.add(Component.text("> ").color(DARK_GRAY).decoration(ITALIC, false)
                    .append(Component.text(randType.friendlyName).color(GRAY).decorate(UNDERLINED)));
        }
        return item.withLore(lore);
    }
}
