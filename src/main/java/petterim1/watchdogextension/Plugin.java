package petterim1.watchdogextension;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.bugreport.BugReportGenerator;
import cn.nukkit.utils.bugreport.BugReportPlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Plugin extends PluginBase implements BugReportPlugin {

    private int tick;
    private int seen;
    private Field f_jda;
    private TaskHandler task;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            Class<?> c_Loader = Class.forName("me.petterim1.discordchat.Loader");
            f_jda = c_Loader.getDeclaredField("jda");
            f_jda.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        BugReportGenerator.plugin = this;
        getLogger().info("BugReportPlugin set to " + Plugin.class.getName());

        int after = getConfig().getInt("players-online-watchdog-active-after");
        int threshold = getConfig().getInt("players-online-watchdog-threshold");

        if (after > 1 && threshold > 0) {
            getLogger().info("Watchdog active");

            task = getServer().getScheduler().scheduleDelayedRepeatingTask(this, () -> {
                tick += 100;

                if (tick > after) {
                    task.remove();
                    task = null;
                    getLogger().info("Watchdog inactive");

                    if (seen < threshold) {
                        try {
                            JDA jda = (JDA) f_jda.get(null);
                            if (jda != null) {
                                TextChannel channel = jda.getTextChannelById(getConfig().getString("discord-bugs-channel-id", "0"));
                                if (channel != null) {
                                    getLogger().info("Sending a Watchdog report to Discord");
                                    channel.sendMessage(getConfig().getString("ping-ppl") + " **" + getServer().getMotd() + " sent a Watchdog report**\nSeen max " + seen + " players in " + (tick / 20) + " seconds").queue();
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    return;
                }

                int players = getServer().getOnlinePlayersCount();
                if (players > seen) {
                    seen = players;
                }
            }, 100, 100);
        }
    }

    @Override
    public void bugReport(Throwable error, String log) {
        if (error != null || log == null) {
            return;
        }

        try {
            JDA jda = (JDA) f_jda.get(null);
            if (jda != null) {
                TextChannel channel = jda.getTextChannelById(getConfig().getString("discord-bugs-channel-id", "0"));
                if (channel != null) {
                    getLogger().info("Sending a crash report to Discord");
                    channel.sendMessage(getConfig().getString("ping-ppl") + " **" + getServer().getMotd() + " sent a crash report**")
                            .addFile(Files.write(Paths.get(getServer().getDataPath() + "/stack.txt"), log.getBytes()).toFile()).queue();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
