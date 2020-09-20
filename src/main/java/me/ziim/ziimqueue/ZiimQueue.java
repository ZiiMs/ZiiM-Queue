package me.ziim.ziimqueue;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class ZiimQueue extends Plugin {

    ScheduledTask task;

    public static Configuration getConfig() {
        Configuration configuration = null;
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(ProxyServer.getInstance().getPluginsFolder() + "/ZiimQueue/" + "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configuration;
    }

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, new QueueEvents());
        setupConfig();
        QueueEvents.loadConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
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
