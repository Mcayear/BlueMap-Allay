/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.bukkit;

import de.bluecolored.bluecommands.*;
import de.bluecolored.bluecommands.Command;
import de.bluecolored.bluecommands.LiteralCommand;
import de.bluecolored.bluecommands.ParseResult;
import de.bluecolored.bluemap.common.commands.CommandExecutor;
import de.bluecolored.bluemap.common.commands.Commands;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.bluecolored.bluemap.common.commands.TextFormat.NEGATIVE_COLOR;
import static net.kyori.adventure.text.Component.text;

public class BukkitCommands {

    private final Command<CommandSource, Object> commands;
    private final CommandExecutor commandExecutor;

    public BukkitCommands(final Plugin plugin) {
        this.commands = Commands.create(plugin);
        this.commandExecutor = new CommandExecutor(plugin);
    }

    public Collection<? extends BukkitCommand> getRootCommands(){
        return List.of(new CommandProxy(((LiteralCommand<?, ?>) commands).getLiteral()));
    }

    private class CommandProxy extends BukkitCommand {

        protected CommandProxy(String name) {
            super(name);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
            BukkitCommandSource context = new BukkitCommandSource(sender);

            String input = commandLabel;
            if (args.length > 0) {
                input += " " + String.join(" ", args);
            }
            ParseResult<CommandSource, Object> result = commands.parse(context, input);
            CommandExecutor.ExecutionResult executionResult = commandExecutor.execute(result);

            if (executionResult.parseFailure()) {
                Optional<ParseFailure<CommandSource, Object>> failure = result.getFailures().stream()
                        .max(Comparator.comparing(ParseFailure::getPosition));

                if (failure.isPresent()) {
                    context.sendMessage(text(failure.get().getReason()).color(NEGATIVE_COLOR));
                } else {
                    context.sendMessage(text("Unknown command!").color(NEGATIVE_COLOR));
                }

                return false;
            }

            return executionResult.resultCode() > 0;
        }

    }

}
