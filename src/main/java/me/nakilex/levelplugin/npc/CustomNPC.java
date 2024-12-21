package me.nakilex.levelplugin.npc;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class CustomNPC {
    private EntityType mobType;
    private String name; // Nullable
    private Direction directionFacing;
    private boolean alwaysFacePlayer;
    private String skin; // URL or texture reference
    private String onRightClickCommand;
    private Location position;
    private String interactMessage;
    private boolean isMovable;
    private int cooldown; // in seconds
    private String role;
    private String npcID;
    private boolean isVisible;
    private boolean isInvulnerable;
    private NPCEquipment equipment;
    private String particleEffects;

    public CustomNPC(EntityType mobType, String name, Direction directionFacing, boolean alwaysFacePlayer,
                     String skin, String onRightClickCommand, Location position, String interactMessage,
                     boolean isMovable, int cooldown, String role, String npcID, boolean isVisible,
                     boolean isInvulnerable, NPCEquipment equipment, String particleEffects) {
        this.mobType = mobType;
        this.name = name;
        this.directionFacing = directionFacing;
        this.alwaysFacePlayer = alwaysFacePlayer;
        this.skin = skin;
        this.onRightClickCommand = onRightClickCommand;
        this.position = position;
        this.interactMessage = interactMessage;
        this.isMovable = isMovable;
        this.cooldown = cooldown;
        this.role = role;
        this.npcID = npcID;
        this.isVisible = isVisible;
        this.isInvulnerable = isInvulnerable;
        this.equipment = equipment;
        this.particleEffects = particleEffects;
    }

    // Getters and Setters
    public EntityType getMobType() {
        return mobType;
    }

    public void setMobType(EntityType mobType) {
        this.mobType = mobType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Direction getDirectionFacing() {
        return directionFacing;
    }

    public void setDirectionFacing(Direction directionFacing) {
        this.directionFacing = directionFacing;
    }

    public boolean isAlwaysFacePlayer() {
        return alwaysFacePlayer;
    }

    public void setAlwaysFacePlayer(boolean alwaysFacePlayer) {
        this.alwaysFacePlayer = alwaysFacePlayer;
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public String getOnRightClickCommand() {
        return onRightClickCommand;
    }

    public void setOnRightClickCommand(String onRightClickCommand) {
        this.onRightClickCommand = onRightClickCommand;
    }

    public Location getPosition() {
        return position;
    }

    public void setPosition(Location position) {
        this.position = position;
    }

    public String getInteractMessage() {
        return interactMessage;
    }

    public void setInteractMessage(String interactMessage) {
        this.interactMessage = interactMessage;
    }

    public boolean isMovable() {
        return isMovable;
    }

    public void setMovable(boolean movable) {
        isMovable = movable;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNpcID() {
        return npcID;
    }

    public void setNpcID(String npcID) {
        this.npcID = npcID;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        isInvulnerable = invulnerable;
    }

    public NPCEquipment getEquipment() {
        return equipment;
    }

    public void setEquipment(NPCEquipment equipment) {
        this.equipment = equipment;
    }

    public String getParticleEffects() {
        return particleEffects;
    }

    public void setParticleEffects(String particleEffects) {
        this.particleEffects = particleEffects;
    }

    // Convenience Methods
    public String getFormattedLocation() {
        return position.getWorld().getName() + "," + position.getX() + "," + position.getY() + "," + position.getZ();
    }

    public void updatePosition(Location newPosition) {
        this.position = newPosition;
    }

    public boolean shouldFacePlayer() {
        return alwaysFacePlayer;
    }

    public boolean hasInteractMessage() {
        return interactMessage != null && !interactMessage.isEmpty();
    }

    public boolean hasCommand() {
        return onRightClickCommand != null && !onRightClickCommand.isEmpty();
    }

    // Direction Enum
    public enum Direction {
        NORTH, EAST, SOUTH, WEST, UP, DOWN;
    }

    // Nested NPCEquipment Class
    public static class NPCEquipment {
        private String helmet;
        private String chestplate;
        private String leggings;
        private String boots;
        private String weapon;

        public NPCEquipment(String helmet, String chestplate, String leggings, String boots, String weapon) {
            this.helmet = helmet;
            this.chestplate = chestplate;
            this.leggings = leggings;
            this.boots = boots;
            this.weapon = weapon;
        }

        // Getters and Setters
        public String getHelmet() {
            return helmet;
        }

        public void setHelmet(String helmet) {
            this.helmet = helmet;
        }

        public String getChestplate() {
            return chestplate;
        }

        public void setChestplate(String chestplate) {
            this.chestplate = chestplate;
        }

        public String getLeggings() {
            return leggings;
        }

        public void setLeggings(String leggings) {
            this.leggings = leggings;
        }

        public String getBoots() {
            return boots;
        }

        public void setBoots(String boots) {
            this.boots = boots;
        }

        public String getWeapon() {
            return weapon;
        }

        public void setWeapon(String weapon) {
            this.weapon = weapon;
        }
    }
}
