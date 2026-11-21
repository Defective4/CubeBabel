package io.github.defective4.cubebabel.ui;

import static net.kyori.adventure.text.format.TextDecoration.*;

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
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.Material;

public class HubCatalogue {

    private static final String CATALOGUE_KEY = "catalogue";
    private static final String CONTENT_SEARCH_MODE_KEY = "contentSearchMode";
    private final Book catalogue, contentSearchMode;

    public HubCatalogue() {
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/ui/books.json"))) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            catalogue = readBookFromArray(root.getAsJsonObject(CATALOGUE_KEY));
            contentSearchMode = readBookFromArray(root.getAsJsonObject(CONTENT_SEARCH_MODE_KEY));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    public Book getCatalogue() {
        return catalogue;
    }

    public Book getContentSearchModeBook() {
        return contentSearchMode;
    }

    public void initEvents() {
        InstanceContainer hubInstance = LibraryServer.getInstance().getHubInstance();
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

    private static Book readBookFromArray(JsonObject obj) {
        List<Component> pages = new ArrayList<>();
        JsonArray array = obj.getAsJsonArray("pages");

        for (JsonElement pageElement : array)
            if (pageElement instanceof JsonArray pageArray) {
                Component page = null;
                for (JsonElement lineElement : pageArray) {
                    Component part;
                    if (lineElement instanceof JsonObject lineObj) {
                        part = Component.text(lineObj.get("text").getAsString()).decoration(BOLD, false);
                        if (lineObj.has("bold")) {
                            part = part.decorate(BOLD);
                        }
                        if (lineObj.has("hover")) {
                            part = part.hoverEvent(
                                    HoverEvent.showText(Component.text(lineObj.get("hover").getAsString())));
                        }
                        if (lineObj.has("page")) {
                            part = part.decorate(UNDERLINED)
                                    .clickEvent(ClickEvent.changePage(lineObj.get("page").getAsInt()));
                        } else if (lineObj.has("command")) {
                            part = part.decorate(UNDERLINED)
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

        return Book.book(Component.text(obj.get("title").getAsString()),
                Component.text(obj.get("author").getAsString()), pages);
    }
}
