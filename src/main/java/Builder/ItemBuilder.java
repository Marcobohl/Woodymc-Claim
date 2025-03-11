package Builder;

import main.Main;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {
    private final ItemStack item;
    private ItemMeta meta;
    private static boolean isAuthlibAvailable = true;
    private int slot = -1;

    static {
        try {
            Class.forName("com.mojang.authlib.GameProfile");
        } catch (ClassNotFoundException e) {
            isAuthlibAvailable = false;
        }
    }

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }


    public ItemBuilder setPersistentData(String key, String value) {
        if (meta == null) {
            throw new IllegalStateException("ItemMeta ist null, PersistentData kann nicht gesetzt werden.");
        }

        NamespacedKey namespacedKey = new NamespacedKey(Main.getInstance(), key);
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);

        return this;
    }

    public static String getPersistentData(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) {
            return null; // Keine Meta-Daten vorhanden
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(Main.getInstance(), key);
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(namespacedKey, PersistentDataType.STRING)) {
            return container.get(namespacedKey, PersistentDataType.STRING);
        }

        return null;
    }

    // Methode zum Setzen eines spezifischen Slots
    public ItemBuilder setSlot(int slot) {
        this.slot = slot;
        return this;
    }

    // Methode zum Hinzufügen in das Inventar
    public void addToInventory(Inventory inventory) {
        item.setItemMeta(meta);
        if (slot >= 0 && slot < inventory.getSize()) { // Überprüfe, ob der Slot gültig ist
            inventory.setItem(slot, item);
        }
    }

    public ItemBuilder setDisplayName(String name) {
        meta.setDisplayName(name);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        meta.setLore(Arrays.asList(lore));
        return this;
    }

    public ItemBuilder addLore(String... additionalLore) {
        List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
        lore.addAll(Arrays.asList(additionalLore));
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level, boolean hideEnchant) {
        meta.addEnchant(enchantment, level, true);
        if (hideEnchant) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Verbirgt die Verzauberung im Tooltip
        }
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        if (unbreakable) {
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE); // Optional: versteckt "Unzerbrechlich" im Tooltip
        }
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder setDamage(int damage) {
        if (meta instanceof Damageable) {
            ((Damageable) meta).setDamage(damage);
        }
        return this;
    }

    public ItemBuilder setLeatherArmorColor(Color color) {
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(color);
        }
        return this;
    }

    public ItemBuilder setOwner(String ownerName) {
        if (meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwner(ownerName);
        }
        return this;
    }

    // Setzt eine benutzerdefinierte Skull-Textur mit der Base64-Textur-URL
    public ItemBuilder setCustomSkull(String textureValue) {
        if (!isAuthlibAvailable) {
            System.err.println("Authlib is not available. Custom skull textures cannot be set.");
            return this;
        }

        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            try {
                // GameProfile und Property Klassen sind nur verfügbar, wenn Authlib geladen ist
                Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

                Object profile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(UUID.randomUUID(), null);
                Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", textureValue);

                // Füge die Textur zum GameProfile hinzu
                gameProfileClass.getMethod("getProperties").invoke(profile)
                        .getClass().getMethod("put", String.class, propertyClass).invoke(
                                gameProfileClass.getMethod("getProperties").invoke(profile),
                                "textures",
                                property
                        );

                Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(skullMeta, profile);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.meta = skullMeta;
        }
        return this;
    }

    public ItemBuilder setPlayerSkull(String playerName) {
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            skullMeta.setOwner(playerName);
            this.meta = skullMeta;
        }
        return this;
    }

    public ItemStack build() {
        return item;
    }
}
