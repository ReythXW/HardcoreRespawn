package xaviermc.top.hardcoreRespawn.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;

public class JoinListener implements Listener {
    private final HardcoreRespawn plugin;

    public JoinListener(HardcoreRespawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 加载玩家数据
        plugin.getPlayerDataManager().loadPlayerData(player);

        // 应用一滴血模式（如果启用）
        if (plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
            plugin.getPlayerDataManager().applyOneHeartMode(player);
        }

        // 检查玩家是否在等待期
        if (plugin.getPlayerDataManager().isInWaitingPeriod(player)) {
            Location spawnLocation = player.getWorld().getSpawnLocation();
            player.teleport(spawnLocation);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(false);
            player.setFlying(false);

            plugin.getPlayerDataManager().resumeWaitingPeriod(player);
        } else {
            // 如果是新玩家，给予初始复活次数
            plugin.getPlayerDataManager().initializeNewPlayer(player);
        }
    }

    // 新增：监听世界切换事件，确保切换世界后仍保持一滴血模式
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
            // 延迟一小段时间确保玩家完全加载到新世界
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getPlayerDataManager().applyOneHeartMode(player);
            }, 20L);
        }
    }
}