// src/main/java/xaviermc/top/hardcoreRespawn/HardcoreRespawn.java
package xaviermc.top.hardcoreRespawn;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xaviermc.top.hardcoreRespawn.commands.RespawnCommand;
import xaviermc.top.hardcoreRespawn.listeners.*;
import xaviermc.top.hardcoreRespawn.managers.PlayerDataManager;
import xaviermc.top.hardcoreRespawn.database.DatabaseManager;
import xaviermc.top.hardcoreRespawn.utils.MessageUtils;

public class HardcoreRespawn extends JavaPlugin {
    private static HardcoreRespawn instance;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置文件
        checkConfigVersion();
        MessageUtils.loadMessages();

        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.initializeDatabase();

        // 初始化数据管理器
        playerDataManager = new PlayerDataManager(this);
        
        // 启动在线时间检查任务
        playerDataManager.startOnlineTimeCheckTask();

        // 注册命令和 TabCompleter
        RespawnCommand respawnCommand = new RespawnCommand(this);
        getCommand("respawn").setExecutor(respawnCommand);
        getCommand("respawn").setTabCompleter(respawnCommand);

        // 注册监听器
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityKillListener(this), this);
        getServer().getPluginManager().registerEvents(new LowHealthListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this); // ← 新增

        getLogger().info("HardcoreRespawn 插件已启用！");
        getLogger().info("一滴血模式：" + getConfig().getBoolean("settings.one_heart.enabled", true));
        getLogger().info("等待期模式：旁观者 + 指令白名单");
        getLogger().info("Autheme兼容模式：" + getConfig().getBoolean("settings.authme_supported", false));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("HardcoreRespawn 插件已禁用！");
    }

    public static HardcoreRespawn getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    /**
     * 检测玩家是否处于未登录状态
     * @param player 玩家对象
     * @return 如果玩家未登录返回true，否则返回false
     */
    public static boolean isPlayerLoggedOut(Player player) {
        // 检查配置是否启用了AuthMe支持
        if (!getInstance().getConfig().getBoolean("settings.authme_supported", false)) {
            return false;
        }

        // 软依赖检测：检查服务端是否加载了AuthMe插件
        try {
            // 尝试获取AuthMe插件实例
            org.bukkit.plugin.Plugin authMePlugin = getInstance().getServer().getPluginManager().getPlugin("AuthMe");
            if (authMePlugin != null && authMePlugin.isEnabled()) {
                // 使用反射调用AuthMe API，避免硬依赖
                Class<?> authMeApiClass = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
                Object apiInstance = authMeApiClass.getMethod("getInstance").invoke(null);
                boolean isAuthenticated = (boolean) authMeApiClass.getMethod("isAuthenticated", Player.class).invoke(apiInstance, player);
                return !isAuthenticated;
            }
        } catch (Exception e) {
            // 如果AuthMe插件不存在或API调用失败，默认返回false
            getInstance().getLogger().fine("AuthMe插件未找到或API调用失败，默认视为玩家已登录");
        }

        return false;
    }

    /**
     * 检查配置文件版本，如果版本不匹配则替换为默认配置
     */
    private void checkConfigVersion() {
        // 保存默认配置文件到插件目录（如果不存在）
        saveDefaultConfig();
        
        // 获取当前配置文件的版本
        String currentVersion = getConfig().getString("version", "0.0");
        
        // 获取默认配置文件的版本
        String defaultVersion = "0.0";
        try {
            // 获取默认配置文件资源
            java.io.InputStream resourceStream = getResource("config.yml");
            if (resourceStream != null) {
                org.bukkit.configuration.file.FileConfiguration defaultConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(resourceStream)
                );
                defaultVersion = defaultConfig.getString("version", "0.0");
                resourceStream.close();
            } else {
                getLogger().severe("无法获取默认配置文件资源！");
                return;
            }
        } catch (Exception e) {
            getLogger().severe("加载默认配置文件时出错: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // 比较版本
        if (!currentVersion.equals(defaultVersion)) {
            getLogger().warning("检测到配置文件版本不匹配！当前版本: " + currentVersion + "，默认版本: " + defaultVersion);
            getLogger().warning("正在替换为默认配置文件...");
            
            // 重命名旧配置文件为config-old.yml
            java.io.File oldConfigFile = new java.io.File(getDataFolder(), "config.yml");
            java.io.File backupConfigFile = new java.io.File(getDataFolder(), "config-old.yml");
            
            // 如果备份文件已存在，先删除
            if (backupConfigFile.exists()) {
                backupConfigFile.delete();
            }
            
            if (oldConfigFile.exists()) {
                if (!oldConfigFile.renameTo(backupConfigFile)) {
                    getLogger().severe("无法重命名旧配置文件！");
                    return;
                }
                getLogger().info("旧配置文件已备份为: config-old.yml");
            }
            
            // 保存新的默认配置文件
            saveDefaultConfig();
            reloadConfig();
            
            getLogger().info("配置文件已更新至版本: " + defaultVersion);
        }
    }
}