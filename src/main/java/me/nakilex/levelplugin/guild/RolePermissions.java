package me.nakilex.levelplugin.guild;

public class RolePermissions {
    private boolean vaultAccess;
    private boolean kick;
    private boolean acceptApplicants;
    private boolean changeMotd;
    private boolean manageRelations;
    private boolean manageRoles;

    public RolePermissions(boolean vaultAccess, boolean kick, boolean acceptApplicants, boolean changeMotd, boolean manageRelations, boolean manageRoles) {
        this.vaultAccess = vaultAccess;
        this.kick = kick;
        this.acceptApplicants = acceptApplicants;
        this.changeMotd = changeMotd;
        this.manageRelations = manageRelations;
        this.manageRoles = manageRoles;
    }

    public boolean has(GuildPermission perm) {
        return switch (perm) {
            case VAULT_ACCESS -> vaultAccess;
            case KICK -> kick;
            case ACCEPT_APPLICANTS -> acceptApplicants;
            case CHANGE_MOTD -> changeMotd;
            case MANAGE_RELATIONS -> manageRelations;
            case MANAGE_ROLES -> manageRoles;
        };
    }

    public void set(GuildPermission perm, boolean value) {
        switch (perm) {
            case VAULT_ACCESS -> vaultAccess = value;
            case KICK -> kick = value;
            case ACCEPT_APPLICANTS -> acceptApplicants = value;
            case CHANGE_MOTD -> changeMotd = value;
            case MANAGE_RELATIONS -> manageRelations = value;
            case MANAGE_ROLES -> manageRoles = value;
        }
    }

    public void toggle(GuildPermission perm) {
        set(perm, !has(perm));
    }
}
