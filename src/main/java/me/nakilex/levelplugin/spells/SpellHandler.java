package me.nakilex.levelplugin.spells;

@FunctionalInterface
public interface SpellHandler {
    void cast(SpellContext context);
}
