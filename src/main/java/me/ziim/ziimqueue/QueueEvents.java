package me.ziim.ziimqueue;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class QueueEvents implements Listener {

    public static String mainServer;
    public static String queueServer;
    public static int Capacity;
    public List<ProxiedPlayer> queue = new ArrayList<>();
    public LinkedHashMap<ProxiedPlayer, ScheduledTask> playerTasks = new LinkedHashMap<>();
    public ScheduledTask task;

    Plugin plugin = ProxyServer.getInstance().getPluginManager().getPlugin("ZiimQueue");

    public static void loadConfig() {
        mainServer = ZiimQueue.getConfig().getString("MainServer");
        queueServer = ZiimQueue.getConfig().getString("QueueServer");
        Capacity = ZiimQueue.getConfig().getInt("QueueCapacity");
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (Capacity != -1) {
            if (Capacity <= queue.size()) {
                event.setCancelReason(new TextComponent("Queue is full"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {

    }

    public void reconnect() {
        task = ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            ServerInfo main = ProxyServer.getInstance().getServerInfo(mainServer);
            try {
                main.ping((result, error) -> {
                    if (error == null) {
                        if (!queue.isEmpty()) {
                            int maxSlots = result.getPlayers().getMax();
                            cancel();
                            for (int i = 0; i < maxSlots; i++) {
                                ProxiedPlayer newPlayer = queue.remove(0);
                                cancelTask(newPlayer);
                                newPlayer.sendMessage(new TextComponent("Server rebooting, attempting to join the server."));
                                newPlayer.connect(main);
                            }
                        }
                    }
                });
            } catch (Error e) {
                e.printStackTrace();
            }
        }, 5L, 10L, TimeUnit.SECONDS);
    }


    public void cancel() {
        ProxyServer.getInstance().getScheduler().cancel(plugin);
    }

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent e) {
        ServerInfo queServer = ProxyServer.getInstance().getServerInfo(queueServer);
        ProxyServer.getInstance().getLogger().info(ChatColor.YELLOW + e.getReason().toString());
        if (e.getTarget() == queServer) {
            ServerInfo main = ProxyServer.getInstance().getServerInfo(mainServer);
            ProxiedPlayer player = e.getPlayer();
            main.ping((result, error) -> {
                if (error != null) {
                    queue.add(player);
                    showQueue(player);
                    reconnect();
                } else {
                    int maxSlots = result.getPlayers().getMax();
                    if (main.getPlayers().size() < maxSlots) {
                        player.connect(main);
                    } else {
                        queue.add(player);
                        showQueue(player);
                    }
                }
            });
        }
    }

    @EventHandler
    public void onServerKick(ServerKickEvent e) {
        ProxiedPlayer player = e.getPlayer();
        ProxyServer.getInstance().getLogger().info(ChatColor.YELLOW + BaseComponent.toPlainText(e.getKickReasonComponent()));
        if (BaseComponent.toPlainText(e.getKickReasonComponent()).contains("full")) {
            queue.add(player);
            showQueue(player);
        }
    }

    public void showQueue(ProxiedPlayer player) {
        ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Position in queue: " + (queue.indexOf(player) + 1) + "/" + queue.size()));
        }, 0L, 1500L, TimeUnit.MILLISECONDS);
        playerTasks.put(player, task);
    }

    public void cancelTask(ProxiedPlayer player) {
        for (ProxiedPlayer p : playerTasks.keySet()) {
            if (p == player) {
                ProxyServer.getInstance().getScheduler().cancel(playerTasks.get(p));
            }
        }
    }


    @EventHandler
    public void onDisconnect(ServerDisconnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        ServerInfo main = ProxyServer.getInstance().getServerInfo(mainServer);
        ServerInfo server = e.getTarget();
        if (!queue.isEmpty()) {
            if (server.getName().equals("main")) {
                try {
                    main.ping((result, error) -> {
                        if (error == null) {
                            if (!queue.isEmpty()) {
                                int maxSlots = result.getPlayers().getMax();
                                for (int i = 0; i < maxSlots; i++) {
                                    ProxiedPlayer newPlayer = queue.remove(0);
                                    newPlayer.sendMessage(new TextComponent("Player leaving, attempting to join the server."));
                                    cancelTask(newPlayer);
                                    newPlayer.connect(main);
                                }
                            }
                        }
                    });
                } catch (Error err) {
                    err.printStackTrace();
                }

            } else if (server.getName().equals(queueServer)) {
                queue.removeIf(q -> q.equals(player));
            }
        }
    }
}
