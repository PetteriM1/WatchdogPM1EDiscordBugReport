package me.petterim1.watchdogextension;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.bugreport.BugReportGenerator;
import cn.nukkit.utils.bugreport.BugReportPlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Plugin extends PluginBase implements BugReportPlugin {

    public void onEnable() {
        saveDefaultConfig();
        BugReportGenerator.plugin = this;
        getLogger().info("BugReportPlugin set to " + Plugin.class.getName());
    }

    public void bugReport(Throwable error, String log) {
        if (error == null && log != null) {
            try {
                Class.forName("me.petterim1.discordchat.Loader");
                Class<?> c_Loader = Class.forName("me.petterim1.discordchat.Loader");
                Field f_jda = c_Loader.getDeclaredField("jda");
                f_jda.setAccessible(true);
                JDA jda = (JDA) f_jda.get(null);
                if (jda != null) {
                    TextChannel channel = jda.getTextChannelById(getConfig().getString("discord-bugs-channel-id"));
                    if (channel != null) {
                        Field f_config = c_Loader.getDeclaredField("config");
                        f_config.setAccessible(true);
                        Config config = (Config) f_config.get(null);
                        getLogger().info("Sending a crash report to Discord ...");
                        channel.sendMessage(getConfig().getString("ping-ppl") + " **" + config.getString("serverIp", "<unknown server>") + ':' + config.getString("serverPort", "<unknown port>") + " sent a crash report**")
                                .addFile(Files.write(Paths.get(getServer().getDataPath() + "/stack.txt"), log.getBytes()).toFile()).queue();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
