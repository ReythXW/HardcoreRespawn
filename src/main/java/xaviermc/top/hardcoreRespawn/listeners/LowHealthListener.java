package xaviermc.top.hardcoreRespawn.listeners;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;

public class LowHealthListener implements Listener {
    private final HardcoreRespawn plugin;
    private long lastHeartbeatTime = 0;

    public LowHealthListener(HardcoreRespawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 检查一滴血模式是否启用
        if (!plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
            return;
        }

        double threshold = plugin.getConfig().getDouble("settings.one_heart.low_health_threshold", 1.0);

        // 玩家受伤后检查是否处于低血量状态
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.getHealth() <= threshold) {
                applyLowHealthEffects(player);
            }
        }, 1L);
    }

    /**
     * 应用低血量效果（粒子+音效）
     */
    private void applyLowHealthEffects(Player player) {
        long currentTime = System.currentTimeMillis();

        // 限制音效播放频率（每2秒一次）
        if (currentTime - lastHeartbeatTime < 2000) {
            return;
        }
        lastHeartbeatTime = currentTime;

        // 播放心跳音效
        if (plugin.getConfig().getBoolean("settings.one_heart.sound_effect", true)) {
            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.0f);
        }

        // 显示红色粒子效果
        if (plugin.getConfig().getBoolean("settings.one_heart.particle_effect", true)) {
            Location loc = player.getLocation().add(0, 1, 0);
            player.spawnParticle(Particle.FIREWORK, loc, 10, 0.5, 0.5, 0.5, 0.1);
        }
    }
}