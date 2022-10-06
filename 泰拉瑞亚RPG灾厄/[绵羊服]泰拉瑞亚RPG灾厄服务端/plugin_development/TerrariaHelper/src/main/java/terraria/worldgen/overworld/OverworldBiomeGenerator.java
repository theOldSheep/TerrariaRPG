package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import terraria.mathhelper.MathHelper;
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
    static final int CACHE_SIZE = 15000000, CACHE_DELETION_SIZE = CACHE_SIZE * 2 / 3, spawnLocProtectionRadius = 0;

    private void printGridInfo(int[][] grid) {
        Bukkit.getLogger().info("__________________");
        for (int[] row : grid) {
            String msg = "";
            for (int gridToPrint : row)
                msg += gridToPrint + " ";
            Bukkit.getLogger().info(msg);
        }
        Bukkit.getLogger().info("__________________");
    }
    public OverworldBiomeGenerator() {
        biomeGenProcess = new String[] {
                "zoom_in",
                "add_islands",
                "zoom_in",
                "add_islands",
                "fill_ocean",
                "add_islands",
                "fill_ocean",
                "add_islands",
                "zoom_in_smooth",
                "setup_rough_biome",
                "zoom_in_smooth",
                "zoom_in_smooth",
                "smooth_biome",
                "zoom_in_smooth",
                "zoom_in_smooth",
                "add_beach",
                "add_beach",
                "add_beach",
                "zoom_in_smooth",
                "zoom_in_smooth",
                "smooth_biome",
                "zoom_in_smooth",
                "zoom_in_smooth",
                "smooth_biome"
        };
//        biomeGenProcess = new String[]{
//                "setup_rough_biome",
//                "zoom_in_smooth",
//                "zoom_in_smooth",
//        };
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
        // generateBiomeGridImage(wld);
        // test: save a map of biomes for testing purposes
        int center = 0;
        int scale = 2500;
        int jump = 1;
        while (biomeGenProcess.length >= 1) {
            biomeCache.clear();
            Bukkit.getLogger().info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            Bukkit.getLogger().info("START GENERATING BIOME MAP " + biomeGenProcess.length);
            double progress = 0, progressMax = scale * scale;
            long lastPrinted = Calendar.getInstance().getTimeInMillis();
            BufferedImage biomeMap = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB);
            Bukkit.getLogger().info("Cache size: " + biomeCache.size() + " / " + CACHE_SIZE);
            int[][] biomeGridToPrint = new int[scale][scale];
            for (int i = 0; i < scale; i++)
                for (int j = 0; j < scale; j++) {
                    int blockX = (i - (scale / 2)) * jump + center, blockZ = (j - (scale / 2)) * jump + center;
                    Biome currBiome = getBiome(wld, blockX, blockZ);
                    biomeGridToPrint[i][j] = biomeCache.getOrDefault(1+"|"+blockX+"|"+blockZ, -2);
                    biomeMap.setRGB(i, j, biomeColors.getOrDefault(currBiome, new Color(0, 255, 0).getRGB()));
                    progress++;
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
            Bukkit.getLogger().info("Operation: " + biomeGenProcess[biomeGenProcess.length - 1]);
//            printGridInfo(biomeGridToPrint);
            File dir_biome_map = new File("worldGenDebug/biomesMap" + biomeGenProcess.length + "_" + biomeGenProcess[biomeGenProcess.length - 1] + ".png");
            try {
                ImageIO.write(biomeMap, "png", dir_biome_map);
            } catch (IOException e) {
                e.printStackTrace();
                Bukkit.getLogger().warning(e.getMessage());
            }
            Bukkit.getLogger().info("FINISHED GENERATING BIOME MAP " + biomeGenProcess.length);

//            if (biomeGenProcess[biomeGenProcess.length - 1].startsWith("zoom_in")) scale /= 2;
            String[] temp = new String[biomeGenProcess.length - 1];
            for (int idx = 0; idx < temp.length; idx ++) temp[idx] = biomeGenProcess[idx];
            biomeGenProcess = temp;
            break;
        }
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
                boolean isNearSpawnloc = (Math.abs(blockX) < spawnLocProtectionRadius * 2 && Math.abs(blockZ) < spawnLocProtectionRadius * 2);
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
                boolean isNearSpawnloc = (Math.abs(blockX) < spawnLocProtectionRadius * 2 && Math.abs(blockZ) < spawnLocProtectionRadius * 2);
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
                // smooth biome: put all biomes in adjacent grid in a hashmap
                ArrayList<Integer> candidates = new ArrayList<>(5);
                HashMap<Integer, Integer> occurrence = new HashMap<>(8);
                for (int xOffset = -1; xOffset <= 1; xOffset ++) {
                    for (int zOffset = -1; zOffset <= 1; zOffset++) {
                        if (xOffset == 0 && zOffset == 0) continue;
                        int gridType;
                        gridType = mapLand[i + zOffset][j + xOffset];
                        occurrence.put(gridType, occurrence.getOrDefault(gridType, 0) + 1);
                    }
                }
                // only the biome that have the highest rate of occurrence can be the result.
                int maxOccurrence = 0, numBiomeNeedMargin = 0;
                for (int createdGrid : occurrence.keySet()) {
                    // if two conflicting biomes around this grid needed a margin
                    if (biome_need_forest_margin(createdGrid) && ++numBiomeNeedMargin > 1)
                        break;
                    int currOccurrence = occurrence.get(createdGrid);
                    if (currOccurrence > maxOccurrence) {
                        maxOccurrence = currOccurrence;
                        candidates.clear();
                    }
                    if (currOccurrence == maxOccurrence)
                        candidates.add(createdGrid);
                }
                // smoothed biome
                if (numBiomeNeedMargin > 1)
                    result[i][j] = 1;
                else {
                    int rdmResult = RandomGenerator.getRandomGenerator(wld.getSeed(), x + j * scale, z + i * scale).nextInt();
                    result[i][j] = candidates.get(Math.abs(rdmResult) % candidates.size());
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
                result[i][j] = mapLand[i][j];
                int blockX = x + (j * scale), blockZ = z + (i * scale), rough_size = 3 * scale;
//                if (blockX % rough_size == 0 || blockZ % rough_size == 0) continue; // leave the margin intact.
//                if (blockX % rough_size != 0 && (blockX + 1) % rough_size != 0 &&
//                        blockZ % rough_size != 0 && (blockZ + 1) % rough_size != 0) continue; // leave the margin intact.
                blockX = MathHelper.betterFloorDivision(blockX, rough_size) * rough_size;
                blockZ = MathHelper.betterFloorDivision(blockZ, rough_size) * rough_size;
                boolean isNearSpawnloc = (Math.abs(blockX) < spawnLocProtectionRadius && Math.abs(blockZ) < spawnLocProtectionRadius);
                double randomNum = RandomGenerator.getRandom(wld.getSeed(), blockX, blockZ);
                if (mapLand[i][j] <= 0) {
                    // ocean
                    if (randomNum < 0.2) result[i][j] = -1;
                } else if (!isNearSpawnloc && mapLand[i][j] <= 7) {
                    // land (not beach)
                    if (randomNum < 1d/6) result[i][j] = 2; // jungle
                    else if (randomNum < 1d/3) result[i][j] = 3; // tundra
                    else if (randomNum < 1d/2) result[i][j] = 4; // desert
                    else if (randomNum < 2d/3) result[i][j] = 5; // corruption
                    else if (randomNum < 5d/6) result[i][j] = 6; // hallow
                    else result[i][j] = 7; // astral infection
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
        switch (operation) {
            case "setup_rough_biome":
                result = setup_rough_biome(land_grid, world, x_begin, z_begin, gridSize);
                break;
            case "zoom_in":
                result = zoom_in(land_grid, world, x_begin, z_begin, gridSize, false);
                break;
            case "zoom_in_smooth":
                result = zoom_in(land_grid, world, x_begin, z_begin, gridSize, true);
//                Bukkit.getLogger().info("___ZOOMIN___");
//                printGridInfo(land_grid);
//                printGridInfo(result);
//                Bukkit.getLogger().info("____________");
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
        return result;
    }
    private void saveBiomeGrid(int[][] land_grid, int marginDiscard, int x_begin, int z_begin, int gridSize, int recursion) {
        int grid_x_save_start = MathHelper.betterFloorDivision(x_begin, gridSize),
            grid_z_save_start = MathHelper.betterFloorDivision(z_begin, gridSize);
        for (int i = marginDiscard; i + marginDiscard < land_grid.length; i++) {
            for (int j = marginDiscard; j + marginDiscard < land_grid[i].length; j++) {
                int grid_x_save = grid_x_save_start + j, grid_z_save = grid_z_save_start + i;
                String tempKey = recursion + "|" + grid_x_save + "|" + grid_z_save;
                biomeCache.put(tempKey, land_grid[i][j]);
            }
        }
    }
    private int getGeneralBiomeGrid(World world, int x, int z, int gridSize, int recursion) {
        int gridX = MathHelper.betterFloorDivision(x, gridSize), gridZ = MathHelper.betterFloorDivision(z, gridSize);
        String biomeLocKey = recursion + "|" + gridX + "|" + gridZ;
        total ++;
        if (biomeCache.containsKey(biomeLocKey)) {
            cached ++;
        } else {
            // setup original position info.
            final int radius = 5;
            String operation = biomeGenProcess[biomeGenProcess.length - recursion];
            int[][] land_grid;
            int grid_x_begin, grid_z_begin, gridSizeOffset;
            if (operation.startsWith("zoom_in")) {
                gridSizeOffset = gridSize * 2;
            } else {
                gridSizeOffset = gridSize;
            }
            grid_x_begin = MathHelper.betterFloorDivision(x, gridSizeOffset);
            grid_z_begin = MathHelper.betterFloorDivision(z, gridSizeOffset);
            // offset x and z by -radius. [radius][radius] contains the grid at (x, z).
            grid_x_begin -= radius;
            grid_z_begin -= radius;
            int x_begin = grid_x_begin * gridSizeOffset, z_begin = grid_z_begin * gridSizeOffset;

            // load the grid 1 recursion level higher than current
            land_grid = getUpperLevelBiomeGrid(world, radius, x_begin, z_begin, gridSizeOffset, recursion);
            // manipulate the grid according to current operation
            int[][] manipulated_grid = manipulateBiomeGrid(land_grid, world, operation, x_begin, z_begin, gridSize);
            int marginDiscard = 1;
            if (operation.equals("setup_rough_biome")) marginDiscard = 0;
            else if (operation.startsWith("zoom_in")) marginDiscard = 0;
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