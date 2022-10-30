package terraria.util;

import org.bukkit.inventory.ItemStack;

public class ItemHelper {
    public static String[] splitItemName(String itemName) {
        if (itemName == null) return new String[]{"", ""};
        itemName = GenericHelper.trimText(itemName);
        if (itemName.contains("的 ")) {
            return itemName.split("的 ");
        }
        return new String[]{"", itemName};
    }
    public static String[] splitItemName(ItemStack item) {
        if (item == null) return new String[]{"", ""};
        return splitItemName(item.getItemMeta().getDisplayName());
    }
    public static int getWorth(String name) {
        if (name == null) name = "";
        String[] nameInfo = splitItemName(name);
        int worth = YmlHelper.getFile("plugins/Data/items.yml").getInt(nameInfo[1] + ".worth", 0);
        double worthMulti = YmlHelper.getFile("plugins/Data/prefix.yml").getDouble(
                "prefixInfo." + nameInfo[0] + ".priceMultiplier", 1);
        worth *= worthMulti;
        return (worth / 100) * 100;
    }
    public static int getWorth(ItemStack item) {
        if (item == null) return 0;
        return getWorth(item.getItemMeta().getDisplayName());
    }

}
