package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import terraria.worldgen.RandomGenerator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class OverworldBiomeGenerator {
    World wld;
    HashMap<Biome, Integer> biomeColors;
    HashMap<String, Integer> biomeCache;
    String[] biomeGenProcess;

    double total = 0, cached = 0;
    static final int CACHE_SIZE = 15000000, CACHE_DELETION_SIZE = CACHE_SIZE * 2 / 3;

    public OverworldBiomeGenerator() {
        biomeGenProcess = new String[] {
                "zoom_in",
                "add_islands",
                "zoom_in",
                "add_islands",
                "add_islands",
                "add_islands",
                "fill_ocean",
                "add_islands",
                "zoom_in",
                "setup_rough_biome",
                "zoom_in",
                "zoom_in_smooth",
                "zoom_in_smooth",
                "smooth_biome",
                "zoom_in_smooth",
                "smooth_biome",
                "zoom_in_smooth",
                "add_beach",
                "add_beach",
                "add_beach",
                "zoom_in_smooth",
                "zoom_in_smooth",
                "zoom_in_smooth",
                "zoom_in_smooth",
                "smooth_biome"
        };
        biomeCache = new HashMap<>(CACHE_SIZE, 0.8f);
        biomeColors = new HashMap<>();
        biomeColors.put(Biome.FOREST,               new Color(0, 175, 0).getRGB()); //forest(normal)
        biomeColors.put(Biome.JUNGLE,               new Color(0, 100, 0).getRGB()); //jungle
        biomeColors.put(Biome.DESERT,               new Color(255, 255, 0).getRGB()); //desert
        biomeColors.put(Biome.MUTATED_DESERT,       new Color(0, 50, 80).getRGB()); //sunken sea
        biomeColors.put(Biome.BEACHES,              new Color(255, 255, 150).getRGB()); //beach
        biomeColors.put(Biome.OCEAN,                new Color(0, 0, 255).getRGB()); //ocean
        biomeColors.put(Biome.COLD_BEACH,           new Color(130, 110, 100).getRGB()); //sulphurous beach
        biomeColors.put(Biome.FROZEN_OCEAN,         new Color(120, 200, 150).getRGB()); //sulphurous ocean
        biomeColors.put(Biome.TAIGA_COLD,           new Color(150, 200, 255).getRGB()); //tundra
        biomeColors.put(Biome.MUSHROOM_ISLAND,      new Color(150, 0, 150).getRGB()); //corruption
        biomeColors.put(Biome.MESA,                 new Color(50, 25, 60).getRGB()); //astral infection
        biomeColors.put(Biome.ICE_FLATS,            new Color(255, 255, 255).getRGB()); //hallow
    }
    private void generateBiomeImage(World world) {
        if (wld != null) return;
        wld = world;
        Bukkit.getLogger().info("");
        Bukkit.getLogger().info("");
        Bukkit.getLogger().info("");
        Bukkit.getLogger().info("START GENERATING BIOME MAP");
        // generateBiomeGridImage(wld);
        // test: save a map of biomes for testing purposes
        int center = 0;
        int scale = 1000;
        int jump = 5;
        double progress = 0, progressMax = scale * scale;
        long lastPrinted = Calendar.getInstance().getTimeInMillis();
        BufferedImage biomeMap = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB);
        Bukkit.getLogger().info("Cache size: " + biomeCache.size() + " / " + CACHE_SIZE);
        for (int i = 0; i < scale; i ++)
            for (int j = 0; j < scale; j ++) {
                int blockX = (i-(scale / 2)) * jump + center, blockZ = (j-(scale / 2)) * jump + center;
                Biome currBiome = getBiome(wld, blockX, blockZ);
                biomeMap.setRGB(i, j, biomeColors.getOrDefault(currBiome, new Color(0, 255, 0).getRGB()));
                progress ++;
                if (lastPrinted + 1000 < Calendar.getInstance().getTimeInMillis()) {
                    lastPrinted = Calendar.getInstance().getTimeInMillis();
                    Bukkit.getLogger().info("Generation progress: " + progress / progressMax);
                    Bukkit.getLogger().info("Progress detail: " + progress + "/" + progressMax);
                    Bukkit.getLogger().info("Cache size: " + biomeCache.size() + " / " + CACHE_SIZE);
                }
            }
        Bukkit.getLogger().info("Generation progress: " + progress / progressMax);
        Bukkit.getLogger().info("Progress detail: " + progress + "/" + progressMax);
        Bukkit.getLogger().info("Cache size: " + biomeCache.size() + " / " + CACHE_SIZE);
        File dir_biome_map = new File("world/biomesMap.png");
        try {
            ImageIO.write(biomeMap, "png", dir_biome_map);
        } catch (IOException e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(e.getMessage());
        }
        Bukkit.getLogger().info("FINISHED GENERATING BIOME MAP");
    }
    // biome enlarge helper functions
    private int[][] zoom_in(int[][] original, World wld, int x, int z, int scale, boolean is_smooth) {
        int sizeOriginal = original.length;
        int size = original.length * 2 - 1;
        int[][] result = new int[size][size];
        // * · *
        // · · ·
        // * · *
        for (int i = 0; i < sizeOriginal; i ++)
            for (int j = 0; j < sizeOriginal; j ++) {
                result[i * 2][j * 2] = original[i][j];
            }
        for (int i = 0; i < sizeOriginal; i ++)
            for (int j = 0; j + 1 < sizeOriginal; j ++) {
                // · * ·
                // · · ·
                // · * ·
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + ((j * 2 + 1) * scale), z + (i * 2 * scale));
                if (rdmResult < 0.5) {
                    result[i * 2][j * 2 + 1] = original[i][j];
                } else {
                    result[i * 2][j * 2 + 1] = original[i][j + 1];
                }
            }
        for (int i = 0; i + 1 < sizeOriginal; i ++)
            for (int j = 0; j < sizeOriginal; j ++) {
                // · · ·
                // * · *
                // · · ·
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + (j * 2 * scale), z + ((i * 2 + 1) * scale));
                if (rdmResult < 0.5) {
                    result[i * 2 + 1][j * 2] = original[i][j];
                } else {
                    result[i * 2 + 1][j * 2] = original[i + 1][j];
                }
            }
        for (int i = 0; i + 1 < sizeOriginal; i ++)
            for (int j = 0; j + 1 < sizeOriginal; j ++) {
                // · · ·
                // · * ·
                // · · ·
                if (is_smooth) {
                    ArrayList<Integer> candidates = new ArrayList<>(5);
                    HashMap<Integer, Integer> occurrence = new HashMap<>(8);
                    for (int indX = j; indX <= j + 1; indX++)
                        for (int indZ = i; indZ <= i + 1; indZ++) {
                            int gridType = original[indZ][indX];
                            occurrence.put(gridType, occurrence.getOrDefault(gridType, 0) + 1);
                            candidates.add(gridType);
                        }
                    // only the biome that have the highest rate of occurrence can be the result.
                    int maxOccurrence = 0;
                    for (int createdGrid : occurrence.keySet()) {
                        int currOccurrence = occurrence.get(createdGrid);
                        if (currOccurrence > maxOccurrence) {
                            maxOccurrence = currOccurrence;
                            candidates.clear();
                        }
                        if (currOccurrence == maxOccurrence)
                            candidates.add(createdGrid);
                    }
                    int rdmResult = RandomGenerator.getRandomGenerator(wld.getSeed(), x + ((j * 2 + 1) * scale), z + ((i * 2 + 1) * scale)).nextInt();
                    result[i * 2 + 1][j * 2 + 1] = candidates.get(Math.abs(rdmResult) % candidates.size());
                } else {
                    double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + ((j * 2 + 1) * scale), z + ((i * 2 + 1) * scale));
                    if (rdmResult < 0.25)
                        result[i * 2 + 1][j * 2 + 1] = original[i][j];
                    else if (rdmResult < 0.5)
                        result[i * 2 + 1][j * 2 + 1] = original[i + 1][j];
                    else if (rdmResult < 0.75)
                        result[i * 2 + 1][j * 2 + 1] = original[i][j + 1];
                    else
                        result[i * 2 + 1][j * 2 + 1] = original[i + 1][j + 1];
                }
            }
        return result;
    }
    private int[][] add_islands(int[][] mapLand, World wld, int x, int z, int scale) {
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                int blockX = x + (j * scale), blockZ = z + (i * scale);
                boolean isNearSpawnloc = (Math.abs(blockX) < 1000 && Math.abs(blockZ) < 1000);
                result[i][j] = mapLand[i][j];
                if (i == 0 || j == 0 || i + 1 == size || j + 1 == size) continue; // skip margins
                boolean hasAdjacentLand = false;
                boolean hasAdjacentOcean = false;
                for (int checkI = i - 1; checkI <= i + 1; checkI += 2) {
                    for (int checkJ = j - 1; checkJ <= j + 1; checkJ += 2) {
                        if (mapLand[checkI][checkJ] >= 1) hasAdjacentLand = true;
                        else hasAdjacentOcean = true;
                        if (hasAdjacentLand && hasAdjacentOcean) break;
                    }
                    if (hasAdjacentLand && hasAdjacentOcean) break;
                }
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + (j * scale), z + (i * scale));
                if (hasAdjacentLand && mapLand[i][j] <= 0 && rdmResult < 0.33) result[i][j] = 1;
                else if (hasAdjacentOcean && mapLand[i][j] >= 1 && rdmResult < 0.2 && !isNearSpawnloc) result[i][j] = 0;
                else result[i][j] = mapLand[i][j];
            }
        return result;
    }
    private int[][] fill_ocean(int[][] mapLand, World wld, int x, int z, int scale) {
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                int blockX = x + (j * scale), blockZ = z + (i * scale);
                boolean isNearSpawnloc = (Math.abs(blockX) < 1000 && Math.abs(blockZ) < 1000);
                result[i][j] = mapLand[i][j];
                if (i == 0 || j == 0 || i + 1 == size || j + 1 == size) continue; // skip margins
                if (mapLand[i][j] >= 1) continue; // do nothing to lands
                boolean hasAdjacentLand = false;
                for (int checkI = i - 1; checkI < i + 1; checkI += 2) {
                    if (checkI < 0) continue;
                    if (mapLand[checkI][j] >= 1) {
                        hasAdjacentLand = true;
                        break;
                    }
                }
                if (!hasAdjacentLand)
                    for (int checkJ = j - 1; checkJ < j + 1; checkJ += 2) {
                        if (checkJ < 0) continue;
                        if (mapLand[i][checkJ] == 1) {
                            hasAdjacentLand = true;
                            break;
                        }
                    }
                if (!hasAdjacentLand) {
                    // only try to make ocean a land when it has all water around
                    if (isNearSpawnloc) result[i][j] = 1;
                    else if (RandomGenerator.getRandom(wld.getSeed(), x + (j * scale), z + (i * scale)) < 0.5) result[i][j] = 1;
                }
            }
        return result;
    }
    private boolean biome_need_forest_margin(int toCheck) {
        return toCheck >= 2 && toCheck <= 7;
    }
    private int[][] smooth_biome(int[][] mapLand, World wld, int x, int z, int scale) {
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                result[i][j] = mapLand[i][j];
                if (i == 0 || j == 0 || i + 1 == size || j + 1 == size) continue; // skip margins
                // put forest between different land biomes.
                if (biome_need_forest_margin(mapLand[i][j]) &&
                        (biome_need_forest_margin(mapLand[i - 1][j]) && mapLand[i][j] != mapLand[i - 1][j]) ||
                        (biome_need_forest_margin(mapLand[i + 1][j]) && mapLand[i][j] != mapLand[i + 1][j]) ||
                        (biome_need_forest_margin(mapLand[i][j - 1]) && mapLand[i][j] != mapLand[i][j - 1]) ||
                        (biome_need_forest_margin(mapLand[i][j + 1]) && mapLand[i][j] != mapLand[i][j + 1])){
                    result[i][j] = 1;
                }
                // · * ·
                // · o ·
                // · * ·
                else if (mapLand[i - 1][j] == mapLand[i + 1][j]) {
                    result[i][j] = mapLand[i - 1][j];
                }
                // · · ·
                // * o *
                // · · ·
                else if (mapLand[i][j - 1] == mapLand[i][j + 1]) {
                    if (RandomGenerator.getRandom(wld.getSeed(), x + (j * scale), z + (i * scale)) < 0.5)
                        continue;
                    result[i][j] = mapLand[i][j - 1];
                }
            }
        return result;
    }
    private int[][] add_beach(int[][] mapLand, World wld, int x, int z, int scale) {
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                result[i][j] = mapLand[i][j];
                if (i == 0 || j == 0 || i + 1 == size || j + 1 == size) continue; // skip margins
                if (mapLand[i][j] <= 0) continue; // no beach in oceans!
                int adjacentOcean = 0, adjacentSulphurous = 0;
                for (int idxOffset = -1; idxOffset <= 1; idxOffset += 2) {
                    int toCheck;
                    toCheck = mapLand[i + idxOffset][j];
                    if (toCheck == -1 || toCheck == 8) adjacentSulphurous ++;
                    else if (toCheck == 0 || toCheck == 9) adjacentOcean ++;
                    toCheck = mapLand[i][j + idxOffset];
                    if (toCheck == -1 || toCheck == 8) adjacentSulphurous ++;
                    else if (toCheck == 0 || toCheck == 9) adjacentOcean ++;
                }
                if (adjacentSulphurous >= adjacentOcean && adjacentSulphurous > 0) result[i][j] = 8;
                else if (adjacentOcean > adjacentSulphurous) result[i][j] = 9;
            }
        return result;
    }
    private int[][] setup_rough_biome(int[][] mapLand, World wld, int x, int z, int scale) {
        // -1: sulphurous ocean  0: ocean
        // 1: forest  2: jungle  3: tundra  4: desert  5: corruption  6: hallow  7: astral infection
        // 8: sulphurous beach  9: beach
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                int blockX = x + (j * scale), blockZ = z + (i * scale);
                boolean isNearSpawnloc = (Math.abs(blockX) < 750 && Math.abs(blockZ) < 750);
                double randomNum = RandomGenerator.getRandom(wld.getSeed(), blockX, blockZ);
                result[i][j] = mapLand[i][j];
                if (mapLand[i][j] <= 0) {
                    // ocean
                    if (randomNum < 0.2) result[i][j] = -1;
                } else if (!isNearSpawnloc) {
                    // land
                    if (randomNum < 0.15) result[i][j] = 2; // jungle
                    else if (randomNum < 0.3) result[i][j] = 3; // tundra
                    else if (randomNum < 0.45) result[i][j] = 4; // desert
                    else if (randomNum < 0.55) result[i][j] = 5; // corruption
                    else if (randomNum < 0.65) result[i][j] = 6; // hallow
                    else if (randomNum < 0.75) result[i][j] = 7; // astral infection
                }
            }
        return result;
    }

    private int[][] getUpperLevelBiomeGrid(World world, int radius, int x_begin, int z_begin, int gridSizeOffset, int recursion) {
        int land_grid[][] = new int[radius * 2 + 1][radius * 2 + 1];
        // load the grid 1 recursion level higher than current
        for (int i = 0; i < radius * 2 + 1; i++) {
            for (int j = 0; j < radius * 2 + 1; j++) {
                int blockX, blockZ;
                blockX = x_begin + (j * gridSizeOffset);
                blockZ = z_begin + (i * gridSizeOffset);
                if (recursion < biomeGenProcess.length) {
                    // up 1 recursion level
                    land_grid[i][j] = getGeneralBiomeGrid(world, blockX, blockZ, gridSizeOffset, recursion + 1);
                } else {
                    // initialize the highest level grid
                    if (Math.abs(blockX) <= 1024 && Math.abs(blockZ) <= 1024) land_grid[i][j] = 1;
                    else if (RandomGenerator.getRandom(world.getSeed(), blockX, blockZ) < 0.1)
                        land_grid[i][j] = 1;
                    else land_grid[i][j] = 0;
                }
            }
        }
        return land_grid;
    }
    private int[][] manipulateBiomeGrid(int[][] land_grid, World world, String operation, int x_begin, int z_begin, int gridSize) {
        // manipulate the grid according to current operation
        int[][] result;
        if (operation.equals("setup_rough_biome")) {
            result = setup_rough_biome(land_grid, world, x_begin, z_begin, gridSize);
        } else {
            switch (operation) {
                case "zoom_in":
                    result = zoom_in(land_grid, world, x_begin, z_begin, gridSize, false);
                    break;
                case "zoom_in_smooth":
                    result = zoom_in(land_grid, world, x_begin, z_begin, gridSize, true);
                    break;
                case "add_islands":
                    result = add_islands(land_grid, world, x_begin, z_begin, gridSize);
                    break;
                case "fill_ocean":
                    result = fill_ocean(land_grid, world, x_begin, z_begin, gridSize);
                    break;
                case "add_beach":
                    result = add_beach(land_grid, world, x_begin, z_begin, gridSize);
                    break;
                default:
                    result = smooth_biome(land_grid, world, x_begin, z_begin, gridSize);
            }
        }
        return result;
    }
    private void saveBiomeGrid(int[][] land_grid, int marginDiscard, int x_begin, int z_begin, int gridSize, int recursion) {
        for (int i = marginDiscard; i + marginDiscard < land_grid.length; i++) {
            for (int j = marginDiscard; j + marginDiscard < land_grid[i].length; j++) {
                int grid_x_save = (x_begin / gridSize) + j, grid_z_save = (z_begin / gridSize) + i;
                if (x_begin < 0 && x_begin % gridSize != 0) grid_x_save --;
                if (z_begin < 0 && z_begin % gridSize != 0) grid_z_save --;
                String tempKey = recursion + "|" + grid_x_save + "|" + grid_z_save;
                biomeCache.put(tempKey, land_grid[i][j]);
            }
        }
    }
    private int getGeneralBiomeGrid(World world, int x, int z, int gridSize, int recursion) {
        int gridX = x / gridSize, gridZ = z / gridSize;
        if (x < 0 && x % gridSize != 0) gridX --;
        if (z < 0 && z % gridSize != 0) gridZ --;
        String biomeLocKey = recursion + "|" + gridX + "|" + gridZ;
        total ++;
        if (biomeCache.containsKey(biomeLocKey)) {
            cached ++;
        } else {
            // setup original position info.
            final int radius = 2;
            String operation = biomeGenProcess[biomeGenProcess.length - recursion];
            int[][] land_grid;
            int grid_x_begin, grid_z_begin, gridSizeOffset;
            if (operation.startsWith("zoom_in")) {
                gridSizeOffset = gridSize * 2;
            } else {
                gridSizeOffset = gridSize;
            }
            grid_x_begin = x / gridSizeOffset;
            grid_z_begin = z / gridSizeOffset;
            if (x < 0 && x % gridSizeOffset != 0) grid_x_begin--;
            if (z < 0 && z % gridSizeOffset != 0) grid_z_begin--;
            // offset x and z by -radius. [radius][radius] contains the grid at (x, z).
            grid_x_begin -= radius;
            grid_z_begin -= radius;
            int x_begin = grid_x_begin * gridSizeOffset, z_begin = grid_z_begin * gridSizeOffset;

            // load the grid 1 recursion level higher than current
            land_grid = getUpperLevelBiomeGrid(world, radius, x_begin, z_begin, gridSizeOffset, recursion);
            // manipulate the grid according to current operation
            int[][] manipulated_grid = manipulateBiomeGrid(land_grid, world, operation, x_begin, z_begin, gridSize);
            int marginDiscard = operation.equals("setup_rough_biome") ? 0 : 1;
            //save the grid info
            saveBiomeGrid(manipulated_grid, marginDiscard, x_begin, z_begin, gridSize, recursion);
        }
        int result = biomeCache.get(biomeLocKey);
        if (biomeCache.size() > CACHE_DELETION_SIZE) {
            biomeCache.clear();
        }
        return result;
    }
    public Biome getBiome(World world, int x, int z) {
        generateBiomeImage(world);
        String biomeLocKey = 1+"|"+x+"|"+z;
        int rst;
        if (biomeCache.containsKey(biomeLocKey)) {
            rst = biomeCache.get(biomeLocKey);
        } else {
            rst = getGeneralBiomeGrid(world, x, z, 1, 1);
        }

        Biome result;
        switch (rst) {
            case -1:
                result = Biome.FROZEN_OCEAN; // sulphurous ocean
                break;
            case 0:
                result = Biome.OCEAN; // ocean
                break;
            case 2:
                result = Biome.JUNGLE; // jungle
                break;
            case 3:
                result = Biome.TAIGA_COLD; // tundra
                break;
            case 4:
                result = Biome.DESERT; // desert
                break;
            case 5:
                result = Biome.MUSHROOM_ISLAND; // corruption
                break;
            case 6:
                result = Biome.ICE_FLATS; // hallow
                break;
            case 7:
                result = Biome.MESA; // astral infection
                break;
            case 8:
                result = Biome.COLD_BEACH; // sulphurous beach
                break;
            case 9:
                result = Biome.BEACHES; // beach
                break;
            default:
                result = Biome.FOREST; //forest
        }
        Bukkit.getServer().shutdown();
        return result;
    }
}