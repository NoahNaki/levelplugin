package me.nakilex.levelplugin.mob.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.nakilex.levelplugin.Main;

/**
 * Cancels damage-indicator particles before they reach clients.
 */
public final class DamageIndicatorPacketBlocker {

    public DamageIndicatorPacketBlocker(Main plugin) {
        ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.WORLD_PARTICLES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }

                EnumWrappers.Particle particle = event.getPacket().getParticles().read(0);
                if (particle == EnumWrappers.Particle.DAMAGE_INDICATOR) {
                    event.setCancelled(true);
                }
            }
        });
    }
}

