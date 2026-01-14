package me.nakilex.levelplugin.npc.nms.network;

import net.minecraft.network.Connection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketFlow;
import net.minecraft.network.PacketSendListener;

public class EmptyConnection extends Connection {
    public EmptyConnection(PacketFlow flow) {
        super(flow);
    }

    @Override
    public void send(Packet<?> packet) {
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener listener) {
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
