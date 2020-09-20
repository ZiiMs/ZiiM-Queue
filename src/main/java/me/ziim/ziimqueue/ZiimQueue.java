package me.ziim.ziimqueue;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class ZiimQueue extends Plugin {

    ScheduledTask task;

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, new QueueEvents());
        setupConfig();
        QueueEvents.loadConfig();
    }

    public void reconnect() {
        task = getProxy().getScheduler().schedule(this, () -> {
            getLogger().info(ChatColor.YELLOW + "Running scheduler");
            ServerInfo main1 = ProxyServer.getInstance().getServerInfo("main");
            main1.ping((result1, error1) -> {
                if (error1 != null) {
                    getLogger().info(ChatColor.RED + "Server is down!!");
                } else {
                    getLogger().info(ChatColor.GREEN + "Server is UP!!");

                }
            });
        }, 5L, 10L, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Configuration getConfig() {
        Configuration configuration = null;
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(ProxyServer.getInstance().getPluginsFolder() + "/ZiimQueue/" + "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configuration;
    }

    public void setupConfig() {

        if (!getDataFolder().exists()) getDataFolder().mkdir();
        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try {
                getLogger().info("Creating new file!");
                file.createNewFile();
                Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                configuration.set("MainServer", "main");
                configuration.set("QueueServer", "queue");
                configuration.set("QueueCapacity", 1);
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
            } catch (IOException err) {
                err.printStackTrace();
            }
        }
    }
}
