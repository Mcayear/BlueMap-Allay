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
package de.bluecolored.bluemap.allay;

import de.bluecolored.bluecommands.*;
import de.bluecolored.bluemap.common.commands.CommandExecutor;
import de.bluecolored.bluemap.common.commands.Commands;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import org.allaymc.api.command.Command;
import org.allaymc.api.command.CommandResult;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.command.SimpleCommand;
import org.allaymc.api.command.tree.CommandTree;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static de.bluecolored.bluemap.common.commands.TextFormat.NEGATIVE_COLOR;
import static net.kyori.adventure.text.Component.text;

/**
 * BlueMap Project 2024/7/6
 *
 * @author daoge_cmd
 */
public class AllayCommands {

    private final de.bluecolored.bluecommands.Command<CommandSource, Object> commands;
    private final CommandExecutor commandExecutor;

    public AllayCommands(final Plugin plugin) {
        this.commands = Commands.create(plugin);
        this.commandExecutor = new CommandExecutor(plugin);
    }

    public Collection<? extends Command> getRootCommands(){
        return List.of(new CommandProxy(((LiteralCommand<?, ?>) commands).getLiteral()));
    }

    private class CommandProxy extends SimpleCommand {// SimpleCommand 是 AllayMC 对象
        protected CommandProxy(String name) {
            super(name, name + " BlueMap Command");
        }

        @Override
        public void prepareCommandTree(CommandTree commandTree) {}

        @Override
        public CommandResult execute(CommandSender sender, String[] args) {
            // TODO: 完全实现它...
            AllayCommandSource context = new AllayCommandSource(sender);

            String input = this.name;
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

                return CommandResult.fail();
            }

            return executionResult.resultCode() > 0 ? CommandResult.success(null) : CommandResult.fail();
        }

        @Override
        public boolean isServerSideOnly() {
            return super.isServerSideOnly();
        }
    }
}