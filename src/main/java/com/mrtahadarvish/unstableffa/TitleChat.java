package com.mrtahadarvish.unstableffa;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows a player's equipped shop title in front of their name in chat:
 * "Lost Cause Steve » hello". The coloured title is cached per player because
 * chat runs on another thread.
 */
public final class TitleChat implements Listener {

    private final UnstableFFA plugin;
    private final Map<UUID, Component> cache = new ConcurrentHashMap<>();

    public TitleChat(UnstableFFA plugin) {
        this.plugin = plugin;
    }

    /** Re-reads the player's equipped title. Call after buying, equipping or removing one. */
    public void refresh(Player p) {
        UUID id = p.getUniqueId();
        String titleId = plugin.data().getTitle(id);
        ShopManager.Title title = titleId == null ? null : plugin.shop().getTitle(titleId);
        if (title == null || (title.price > 0 && !plugin.data().ownsTitle(id, title.id))) {
            cache.remove(id);
        } else {
            cache.put(id, plugin.parse(title.mini()));
        }
    }

    public void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            refresh(p);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        refresh(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cache.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!plugin.getConfig().getBoolean("titles.show-in-chat", true)) {
            return;
        }
        Component prefix = cache.get(e.getPlayer().getUniqueId());
        if (prefix == null) {
            return;
        }
        e.renderer((source, name, message, viewer) -> Component.text()
                .append(prefix)
                .append(Component.space())
                .append(name)
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(message)
                .build());
    }
}
