package terraria.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import terraria.TerrariaHelper;

public class WorldHelper {
    public static int getMoonPhase() {
        World overworld = Bukkit.getWorld(TerrariaHelper.Constants.WORLD_NAME_SURFACE);
        if (overworld != null) {
            return (int) ((overworld.getFullTime() / 24000) % 8);
        }
        return 0;
    }
    public static String getHeightLayer(Location loc) {
        switch (loc.getWorld().getName()) {
            case TerrariaHelper.Constants.WORLD_NAME_SURFACE: {
                if (loc.getY() > 200) return "space";
                if (loc.getY() > 50) return "surface";
                return "underground";
            }
            case TerrariaHelper.Constants.WORLD_NAME_CAVERN: {
                return "cavern";
            }
            case TerrariaHelper.Constants.WORLD_NAME_UNDERWORLD: {
                return "underworld";
            }
            default:
                return "surface";
        }
    }
    public static String getBiome(Location loc) {
        String height = getHeightLayer(loc);
        switch (height) {
            case "underworld":
            case "space":
                return height;
            default: {
                Biome biome = loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockZ());
                switch (biome) {
                    case MUSHROOM_ISLAND:
                    case MUSHROOM_ISLAND_SHORE:
                        return "corruption";
                    case ICE_FLATS:
                    case MUTATED_ICE_FLATS:
                        return "hallow";
                    case MESA:
                    case MUTATED_MESA:
                        return "astral_infection";
                    case DESERT:
                        return "desert";
                    case MUTATED_DESERT:
                        return "sunken_sea";
                    case BEACHES:
                    case OCEAN:
                        return "ocean";
                    case FROZEN_OCEAN:
                    case COLD_BEACH:
                        return "sulphurous_ocean";
                    case TAIGA_COLD:
                    case MUTATED_TAIGA_COLD:
                        return "tundra";
                    case JUNGLE:
                    case MUTATED_JUNGLE:
                        return "jungle";
                    default:
                        return "normal";
                }
            }
        }
    }

}
