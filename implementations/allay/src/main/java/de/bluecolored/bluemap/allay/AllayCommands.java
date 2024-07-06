package de.bluecolored.bluemap.allay;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.commands.Commands;
import org.allaymc.api.command.BaseCommand;
import org.allaymc.api.command.Command;
import org.allaymc.api.command.CommandResult;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.utils.TextFormat;

import java.util.ArrayList;
import java.util.Collection;

/**
 * BlueMap Project 2024/7/6
 *
 * @author daoge_cmd
 */
public class AllayCommands {
    private final CommandDispatcher<CommandSender> dispatcher;

    public AllayCommands(final Plugin plugin) {
        this.dispatcher = new CommandDispatcher<>();

        // register commands
        new Commands<>(plugin, dispatcher, bukkitSender -> new AllayCommandSource(plugin, bukkitSender));
    }

    public Collection<Command> getRootCommands(){
        Collection<Command> rootCommands = new ArrayList<>();

        for (CommandNode<CommandSender> node : this.dispatcher.getRoot().getChildren()) {
            rootCommands.add(new CommandProxy(node.getName()));
        }

        return rootCommands;
    }

    private class CommandProxy extends BaseCommand {
        protected CommandProxy(String name) {
            super(name, name + " command");
        }

        @Override
        public CommandResult execute(CommandSender sender, String[] args) {
            String command = name;
            if (args.length > 0) {
                command += " " + String.join(" ", args);
            }

            try {
                return dispatcher.execute(command, sender) > 0 ? CommandResult.success(null) : CommandResult.fail();
            } catch (Throwable t) {
                sender.sendText(TextFormat.RED + t.toString());
            }

            return CommandResult.fail();
        }
    }
}
