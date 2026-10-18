package dev.diamondsmp.core.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public abstract class Menu implements InventoryHolder {

    private final Inventory inventory;

    protected Menu(int rows, Component title) {
        this.inventory = Bukkit.createInventory(this, Math.max(1, Math.min(6, rows)) * 9, title);
    }

    @Override
    public final @NotNull Inventory getInventory() {
        return inventory;
    }

    public void open(Player viewer) {
        viewer.openInventory(inventory);
    }

    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    public void drag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    public void close(InventoryCloseEvent event) {
    }
}
