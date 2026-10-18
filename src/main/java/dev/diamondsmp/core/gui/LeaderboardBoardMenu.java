package dev.diamondsmp.core.gui;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.message.Messages;
import dev.diamondsmp.core.storage.LeaderboardPage;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import dev.diamondsmp.core.util.Format;
import dev.diamondsmp.core.util.Items;
import dev.diamondsmp.core.util.Lookup;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class LeaderboardBoardMenu extends Menu {

    private static final int PAGE_SIZE = 45;
    private static final int PREVIOUS_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final DiamondSMP plugin;
    private final Player viewer;
    private final LeaderboardSection section;
    private final int page;
    private final LeaderboardPage data;

    private LeaderboardBoardMenu(DiamondSMP plugin, Player viewer, LeaderboardSection section, int page,
                                 LeaderboardPage data) {
        super(6, plugin.messages().display("menus.leaderboard.board-title",
                "section", plugin.messages().get("menus.leaderboard.sections." + section.id() + ".title"),
                "page", page + 1, "pages", pages(data.total())));
        this.plugin = plugin;
        this.viewer = viewer;
        this.section = section;
        this.page = page;
        this.data = data;
        render();
    }

    public static void open(DiamondSMP plugin, Player viewer, LeaderboardSection section, int page) {
        int safePage = Math.max(0, page);
        plugin.profiles().page(section.type(), safePage * PAGE_SIZE, PAGE_SIZE).thenAccept(data ->
                plugin.scheduler().entity(viewer, () -> {
                    if (!viewer.isOnline()) {
                        return;
                    }
                    LeaderboardBoardMenu menu = new LeaderboardBoardMenu(plugin, viewer, section, safePage, data);
                    menu.open(viewer);
                    plugin.heads().resolve(data.entries(), () -> plugin.scheduler().entity(viewer, () -> {
                        if (viewer.getOpenInventory().getTopInventory().getHolder() == menu) {
                            menu.render();
                        }
                    }));
                }));
    }

    private static int pages(int total) {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void render() {
        Inventory inventory = getInventory();
        inventory.clear();
        Messages messages = plugin.messages();
        Component sectionName = messages.get("menus.leaderboard.sections." + section.id() + ".title");
        List<ProfileSnapshot> entries = data.entries();
        for (int index = 0; index < entries.size() && index < PAGE_SIZE; index++) {
            ProfileSnapshot entry = entries.get(index);
            Object[] placeholders = placeholders(entry, page * PAGE_SIZE + index + 1, sectionName);
            inventory.setItem(index, plugin.heads().head(entry.uuid(), entry.name(),
                    messages.display("menus.leaderboard.entry.name", placeholders),
                    messages.lore("menus.leaderboard.entry.lore", placeholders)));
        }
        if (entries.isEmpty()) {
            inventory.setItem(22, Items.create(Material.BARRIER, messages.display("menus.leaderboard.empty"),
                    null, false));
        }
        int pages = pages(data.total());
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, Items.create(Material.ARROW,
                    messages.display("menus.leaderboard.previous", "page", page, "pages", pages), null, false));
        }
        inventory.setItem(BACK_SLOT, Items.create(Material.OAK_DOOR,
                messages.display("menus.leaderboard.back"), null, false));
        if (page + 1 < pages) {
            inventory.setItem(NEXT_SLOT, Items.create(Material.ARROW,
                    messages.display("menus.leaderboard.next", "page", page + 2, "pages", pages), null, false));
        }
        ItemStack filler = Items.create(Lookup.material(plugin.settings().getString("leaderboard.filler"),
                Material.GRAY_STAINED_GLASS_PANE), Component.text(" "), null, false);
        for (int slot = PAGE_SIZE; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler.clone());
            }
        }
    }

    private Object[] placeholders(ProfileSnapshot entry, int rank, Component sectionName) {
        Player online = Bukkit.getPlayer(entry.uuid());
        boolean visible = online != null && plugin.visibility().canSee(viewer, online);
        return new Object[]{
                "rank", rank,
                "player", entry.name(),
                "section", sectionName == null ? Component.empty() : sectionName,
                "value", section.value(entry),
                "kills", entry.kills(),
                "deaths", entry.deaths(),
                "kdr", Format.decimal(entry.ratio()),
                "killstreak", entry.bestStreak(),
                "streak", entry.streak(),
                "playtime", Format.duration(entry.playtime() / 1000L),
                "status", plugin.messages().get(visible ? "menus.leaderboard.online" : "menus.leaderboard.offline")
        };
    }

    @Override
    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked.getHolder() != this || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getSlot();
        if (slot == PREVIOUS_SLOT && page > 0) {
            open(plugin, player, section, page - 1);
        } else if (slot == NEXT_SLOT && page + 1 < pages(data.total())) {
            open(plugin, player, section, page + 1);
        } else if (slot == BACK_SLOT) {
            new LeaderboardMenu(plugin).open(player);
        }
    }
}
