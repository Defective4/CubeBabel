package io.github.defective4.cubebabel.book;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.defective4.cubebabel.LibraryServer;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.Material;

public class HubCatalogue {

    private final Book catalogue;
    private final LibraryServer server;

    public HubCatalogue(LibraryServer server) {
        this.server = server;
        JsonArray root;
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/ui/catalogue.json"))) {
            root = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("catalogue");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        List<Component> pages = new ArrayList<>();

        for (JsonElement pageElement : root)
            if (pageElement instanceof JsonArray pageArray) {
                Component page = null;
                for (JsonElement lineElement : pageArray) {
                    Component part;
                    if (lineElement instanceof JsonObject lineObj) {
                        part = Component.text(lineObj.get("text").getAsString());
                        if (lineObj.has("page")) {
                            part = part.decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.changePage(lineObj.get("page").getAsInt()));
                        } else if (lineObj.has("command")) {
                            part = part.decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.runCommand(lineObj.get("command").getAsString()));
                        }
                    } else {
                        part = Component.text(lineElement.getAsString());
                    }
                    if (page == null) {
                        page = part;
                    } else {
                        page = page.append(part);
                    }
                }
                pages.add(page);
            }

        catalogue = Book.book(Component.text("Catalogue of Babel"), Component.text("???"), pages);
    }

    public Book getCatalogue() {
        return catalogue;
    }

    public void initEvents() {
        InstanceContainer hubInstance = server.getHubInstance();
        hubInstance.eventNode().addListener(PlayerBlockInteractEvent.class, e -> {
            e.setCancelled(true);
            if (e.getBlock().material() == Material.LECTERN) {
                e.getPlayer().openBook(catalogue);
            }
        });
        hubInstance.eventNode().addListener(PlayerEntityInteractEvent.class, e -> {
            if (e.getTarget().getEntityType() == EntityType.VILLAGER) {
                e.getPlayer().openBook(catalogue);
            }
        });
    }

}
