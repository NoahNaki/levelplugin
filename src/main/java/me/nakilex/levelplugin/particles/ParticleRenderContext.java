package me.nakilex.levelplugin.particles;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public record ParticleRenderContext(Player player, Location center, Location orientation, int points, int tick,
                                    int durationTicks) {}
