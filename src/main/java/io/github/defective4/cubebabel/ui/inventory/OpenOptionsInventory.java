package io.github.defective4.cubebabel.ui.inventory;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class OpenOptionsInventory extends Inventory {

    public static final int SLOT_CANCEL = 8;
    public static final int SLOT_HIGHLIGHT = 1;
    public static final int SLOT_OPEN = 0;

    private static final ItemStack CANCEL = ItemStack.builder(Material.BARRIER)
            .customName(Component.text("Cancel").decoration(ITALIC, false).color(RED)).build();
    private static final ItemStack HIGHLIGHT = ItemStack.builder(Material.WRITABLE_BOOK)
            .customName(Component.text("Open (highlight dictionary words)").decoration(ITALIC, false)).build();
    private static final ItemStack OPEN = ItemStack.builder(Material.BOOK)
            .customName(Component.text("Open").decoration(ITALIC, false)).build();

    private final Book book;

    public OpenOptionsInventory(Book book) {
        this.book = book;
        super(InventoryType.CHEST_1_ROW, "Open options");

        setItemStack(SLOT_OPEN, OPEN);
        setItemStack(SLOT_CANCEL, CANCEL);
        setItemStack(SLOT_HIGHLIGHT, HIGHLIGHT);
    }

    public Book getBook() {
        return book;
    }

}
