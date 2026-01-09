package me.nakilex.levelplugin.hud.conditions;

import org.bukkit.entity.Player;

public enum HudConditionType {
    ALWAYS {
        @Override
        public boolean matches(Player player) {
            return true;
        }
    },
    UNDERWATER {
        @Override
        public boolean matches(Player player) {
            if (player == null) {
                return false;
            }
            return player.isInWater();
        }
    },
    DEAD {
        @Override
        public boolean matches(Player player) {
            if (player == null) {
                return false;
            }
            return player.isDead() || player.getHealth() <= 0.0;
        }
    },
    NOT_DEAD {
        @Override
        public boolean matches(Player player) {
            if (player == null) {
                return false;
            }
            return !player.isDead() && player.getHealth() > 0.0;
        }
    };

    public abstract boolean matches(Player player);
}
