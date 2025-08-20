package me.nakilex.levelplugin.utils.item;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Small fluent helper for constructing {@link ItemStack} instances from Nexo
 * item templates while allowing basic customization such as display name,
 * lore and amount.  If the provided item id does not exist the builder falls
 * back to a barrier item to avoid null references.
 */
public final class ItemStackBuilder {

    private final ItemStack stack;

    private ItemStackBuilder(String id) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        this.stack = builder != null ? builder.build() : new ItemStack(Material.BARRIER);
    }

    /** Begin building from a Nexo item identifier. */
    public static ItemStackBuilder fromId(String id) {
        return new ItemStackBuilder(id);
    }

    /** Set the display name of the item. */
    public ItemStackBuilder name(String name) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return this;
    }

    /** Replace the lore of the item. */
    public ItemStackBuilder lore(List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return this;
    }

    /** Set the stack amount. */
    public ItemStackBuilder amount(int amount) {
        stack.setAmount(amount);
        return this;
    }

    /** Finish building and return the resulting stack. */
    public ItemStack build() {
        return stack;
    }
}
