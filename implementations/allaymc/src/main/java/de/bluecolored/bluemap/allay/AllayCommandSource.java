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

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.math.location.Location3dc;

import java.util.Optional;

/**
 * BlueMap Project 2024/7/6
 *
 * @author daoge_cmd
 */
public class AllayCommandSource implements CommandSource {

    private final CommandSender delegate;

    public AllayCommandSource(CommandSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public void sendMessage(Component text) {
        delegate.sendText(LegacyComponentSerializer.legacySection().serialize(text));
    }

    @Override
    public boolean hasPermission(String permission) {
        return delegate.hasPermission(permission);
    }

    @Override
    public Optional<Vector3d> getPosition() {
        var location = getLocation();

        if (location != null) {
            return Optional.of(new Vector3d(location.x(), location.y(), location.z()));
        }

        return Optional.empty();
    }

    @Override
    public Optional<ServerWorld> getWorld() {
        var location = getLocation();

        if (location != null) {
            var serverWorld = AllayPlugin.getInstance().getServerWorld(location.dimension());
            return Optional.ofNullable(serverWorld);
        }

        return Optional.empty();
    }

    private Location3dc getLocation() {
        Location3dc location = null;
        // Return non-null value only if the sender is an entity
        if (delegate.isEntity()) location = delegate.getCmdExecuteLocation();
        return location;
    }
}