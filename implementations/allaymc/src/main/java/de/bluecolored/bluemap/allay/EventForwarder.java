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

import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.player.PlayerJoinEvent;
import org.allaymc.api.eventbus.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collection;

public class EventForwarder {

    private final Collection<ServerEventListener> listeners;

    public EventForwarder() {
        listeners = new ArrayList<>();
    }

    public synchronized void addListener(ServerEventListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeAllListeners() {
        listeners.clear();
    }

    @EventHandler
    public synchronized void onPlayerJoin(PlayerJoinEvent evt) {
        for (ServerEventListener listener : listeners) listener.onPlayerJoin(evt.getPlayer().getUUID());
    }

    @EventHandler
    public synchronized void onPlayerLeave(PlayerQuitEvent evt) {
        for (ServerEventListener listener : listeners) listener.onPlayerJoin(evt.getPlayer().getUUID());
    }
}