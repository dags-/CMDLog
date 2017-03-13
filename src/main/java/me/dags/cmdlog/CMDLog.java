package me.dags.cmdlog;

import com.flowpowered.math.vector.Vector3i;
import com.google.inject.Inject;
import me.dags.textmu.MarkupSpec;
import me.dags.textmu.MarkupTemplate;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author dags <dags@dags.me>
 */
@Plugin(id = "cmdlog", name = "CMDLog", version = "0.1", description = "Log commands to console")
public class CMDLog {

    private final MarkupSpec main = MarkupSpec.create();
    private final ConfigurationLoader<CommentedConfigurationNode> config;

    private MarkupTemplate template = main.template("");
    private SimpleDateFormat dateFormat = new SimpleDateFormat();
    private SimpleDateFormat timeFormat = new SimpleDateFormat();

    @Inject
    public CMDLog(@DefaultConfig(sharedRoot = false) ConfigurationLoader<CommentedConfigurationNode> loader) {
        this.config = loader;
        reload(null);
    }

    @Listener
    public void reload(GameReloadEvent event) {
        String logTemplate = "\\[{date}-{time}] {name} (w:{world}, l:{position}): '/{input}'";
        String dateFormat = "dd/MM/yy";
        String timeFormat = "hh:mm:ss";

        CommentedConfigurationNode node = load(config);
        logTemplate = node.getNode("template").getString(logTemplate);
        dateFormat = node.getNode("date_format").getString(dateFormat);
        timeFormat = node.getNode("time_format").getString(timeFormat);

        node.getNode("template").setValue(logTemplate);
        node.getNode("date_format").setValue(dateFormat);
        node.getNode("time_format").setValue(timeFormat);

        this.template = main.template(logTemplate);
        this.dateFormat = new SimpleDateFormat(dateFormat);
        this.timeFormat = new SimpleDateFormat(timeFormat);

        save(config, node);
    }

    @Listener(order = Order.POST)
    public void onCommand(SendCommandEvent event, @Root CommandSource source) {
        Date date = Calendar.getInstance().getTime();

        Text log = template
                .with("date", dateFormat.format(date))
                .with("time", timeFormat.format(date))
                .with("name", source.getName())
                .with("world", getWorld(source))
                .with("position", getPosition(source))
                .with("input", getInput(event.getCommand(), event.getArguments()))
                .render();

        Sponge.getServer().getConsole().sendMessage(log);
    }

    private static String getWorld(CommandSource source) {
        if (source instanceof Entity) {
            Entity located = (Entity) source;
            return located.getWorld().getName();
        }
        return "n/a";
    }

    private static String getPosition(CommandSource source) {
        if (source instanceof Entity) {
            Entity located = (Entity) source;
            Vector3i pos = located.getLocation().getBlockPosition();
            return String.format("%s,%s,%s", pos.getX(), pos.getY(), pos.getZ());
        }
        return "n/a";
    }

    private static String getInput(String command, String args) {
        return args.isEmpty() ? command : command + " " + args;
    }

    private static CommentedConfigurationNode load(ConfigurationLoader<CommentedConfigurationNode> loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            return loader.createEmptyNode();
        }
    }

    private static void save(ConfigurationLoader<CommentedConfigurationNode> loader, CommentedConfigurationNode node) {
        try {
            loader.save(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}