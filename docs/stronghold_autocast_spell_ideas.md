# Stronghold Auto-Cast Spell Ideas (Classless)

This list is intentionally focused on **auto-cast reliability**: low-aim requirements, readable effects in dense waves, and roles that stay useful while the player is moving.

## Design goals
- No class dependency.
- Minimal precision targeting (nearest enemy / densest cluster / self-centered effects).
- High readability under heavy mob density.
- Distinct roles: wave clear, priority damage, control, sustain, emergency safety.
- Upgrade-friendly spell shapes that can scale without unique one-off logic.

## New spell candidates (name + short description)

1. **Cinder Halo**  
   Emits a close-range fire ring around the player, ideal for consistent anti-swarm clearing.

2. **Siegebreak Bolt**  
   Fires a fast piercing bolt at the nearest elite/high-health enemy to stabilize dangerous pulls.

3. **Gravitic Latch**  
   Deploys a pull field at the densest enemy cluster, grouping mobs for follow-up auto-casts.

4. **Aegis Orbit**  
   Summons rotating shards that chip nearby enemies and absorb minor incoming projectile pressure.

5. **Bloodpact Sigil**  
   Places a timed sigil beneath the player that grants a shield and brief heal-on-hit sustain.

6. **Thunder Mesh**  
   Chains lightning through nearby enemies, excelling when packs are naturally clumped.

7. **Shrapnel Bloom**  
   Launches a seed projectile that bursts into radial fragments on contact for hybrid single/AoE pressure.

8. **Frostwire Mine**  
   Auto-drops a proximity mine near the player’s forward lane; detonates with damage + slow on trigger.

9. **Phoenix Trace**  
   Leaves a short-lived burning trail behind the player, rewarding movement and choke-point kiting.

10. **Rift Mortar**  
    Lobs delayed arc blasts at predicted clusters for high-value area denial in dense waves.

11. **Void Leech**  
    Applies stacking marks to nearby enemies; max stacks detonate and return mana or barrier value.

12. **Wardline Pulse**  
    Sends a frontal pulse that briefly weakens enemy damage output during high-pressure moments.

## Optional reusable trigger profiles

To keep the implementation generic and reusable, map each spell to a shared trigger profile + target resolver:

- `OFFENSE_PERIODIC`: cast when cooldown is ready and an enemy is in range.
- `OFFENSE_CLUSTER`: cast only when cluster size threshold is met.
- `OFFENSE_PRIORITY`: cast at nearest elite/high-health target.
- `DEFENSE_SURROUNDED`: cast when nearby enemy count spikes.
- `DEFENSE_HEALTH_THRESHOLD`: cast when player HP falls below configured percent.
- `UTILITY_PATHING`: cast around player or slightly forward along movement direction.

This avoids per-spell special-case logic and keeps future Stronghold spells compatible with the same auto-cast runtime.

## Suggested first implementation batch

- **Cinder Halo** (evergreen clear)
- **Siegebreak Bolt** (priority target pressure)
- **Gravitic Latch** (control / setup)
- **Bloodpact Sigil** (sustain)

This 4-spell batch gives a complete baseline loop before adding higher-variance options like mines, trails, and delayed mortars.
