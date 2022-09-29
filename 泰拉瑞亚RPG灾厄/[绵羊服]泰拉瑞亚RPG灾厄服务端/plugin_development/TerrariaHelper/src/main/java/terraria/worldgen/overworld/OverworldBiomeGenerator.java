package terraria.worldgen.overworld;

import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.noise.PerlinNoiseGenerator;
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
    double lastRecCall = -1, total = 0, cached = 0;
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
                "setup_rough_biome",
                "smooth_biome",
                "zoom_in",
                "zoom_in",
                "add_islands",
                "zoom_in",
                "zoom_in",
                "zoom_in",
                "zoom_in",
                "zoom_in",
                "add_beach",
                "smooth_biome",
                "zoom_in",
                "zoom_in",
                "zoom_in",
                "smooth_biome"
        };
        biomeGenProcess = new String[] {
                "zoom_in",
                "add_islands",
                "zoom_in",
////                "add_islands",
////                "add_islands",
////                "add_islands",
                "fill_ocean",
                "add_islands",
////                "setup_rough_biome",
                "smooth_biome",
////                "zoom_in",
                "zoom_in",
                "add_islands",
////                "zoom_in",
////                "zoom_in",
////                "zoom_in",
////                "zoom_in",
                "zoom_in",
                "add_beach",
                "smooth_biome",
////                "zoom_in",
////                "zoom_in",
                "zoom_in",
                "smooth_biome"
        };
        biomeCache = new HashMap<>(CACHE_SIZE, 0.8f);
        biomeColors = new HashMap<>();
        biomeColors.put(Biome.FOREST,               new Color(0, 255, 0).getRGB()); //forest(normal)
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
    public void saveTestImage(int[][] biomeMap, String filename) {
        BufferedImage imageToSave = new BufferedImage(biomeMap.length, biomeMap[0].length, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < biomeMap.length; i ++)
            for (int j = 0; j < biomeMap[i].length; j ++) {
                Biome result;
                switch (biomeMap[i][j]) {
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
                imageToSave.setRGB(i, j, biomeColors.getOrDefault(result, new Color(0, 255, 0).getRGB()));
            }
        File dir_biome_map = new File("world/" + filename + ".png");
        try {
            ImageIO.write(imageToSave, "png", dir_biome_map);
        } catch (IOException e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(e.getMessage());
        }
    }
    public void generateTestImage(World world) {
        if (wld != null) return;
        //wld = world;
        // test: save images that test out enlarge and island addition
        final int size = 10;
        int[][] land_grid = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++)
                land_grid[i][j] = RandomGenerator.getRandom(world.getSeed(), i, j) < 0.1 ? 1 : 0;
        saveTestImage(land_grid, "0");
        // enlarge land/ocean and add islands. 2048 x 2048 for each grid.
        land_grid = zoom_in(land_grid, world, 1000, 1000, 2048);
        saveTestImage(land_grid, "1_1");
        land_grid = add_islands(land_grid, world, 1000, 1000, 2048);
        saveTestImage(land_grid, "1_2");
        // enlarge land/ocean, add islands thrice and fill some ocean. 1024 x 1024 for each grid.
        land_grid = zoom_in(land_grid, world, 1000, 1000,1024);
        saveTestImage(land_grid, "2_1");
        for (int i = 0; i < 3; i++)
            land_grid = add_islands(land_grid, world, 1000, 1000, 1024);
        saveTestImage(land_grid, "2_2");
        land_grid = fill_ocean(land_grid, world, 1000, 1000, 1024);
        saveTestImage(land_grid, "2_3");
        // TODO: add temperature

        // add islands
        land_grid = add_islands(land_grid, world, 1000, 1000, 1024);
        saveTestImage(land_grid, "3_1");
        // TODO: add ocean

        // TODO: smooth

        // enlarge * 2, add island then enlarge * 2, 64 x 64
        land_grid = zoom_in(land_grid, world, 1000, 1000, 512);
        land_grid = zoom_in(land_grid, world, 1000, 1000, 256);
        saveTestImage(land_grid, "4_1");
        land_grid = add_islands(land_grid, world, 1000, 1000, 256);
        saveTestImage(land_grid, "4_2");
        land_grid = zoom_in(land_grid, world, 1000, 1000, 128);
        land_grid = zoom_in(land_grid, world, 1000, 1000, 64);
        saveTestImage(land_grid, "final");
    }
    public void generateBiomeGridImage(World world) {
        final int size = 500;
        int[][] land_grid = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++)
                // land_grid[i][j] = getGeneralBiomeGrid(world, i * 64, j * 64, 1, 1);
        Bukkit.getLogger().info(land_grid + "");
        saveTestImage(land_grid, "biomeGrid");
    }
    public void generateBiomeImage(World world) {
        if (wld != null) return;
        wld = world;
        Bukkit.getLogger().info("START GENERATING BIOME MAP");
        // generateBiomeGridImage(wld);
        // test: save a map of biomes for testing purposes
        int scale = 1000;
        int jump = 1;
        double progress = 0, progressMax = scale * scale;
        long lastPrinted = Calendar.getInstance().getTimeInMillis();
        BufferedImage biomeMap = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < scale; i ++)
            for (int j = 0; j < scale; j ++) {
                int blockX = (i-(scale / 2)) * jump, blockZ = (j-(scale / 2)) * jump;
                Biome currBiome = getBiome(wld, blockX, blockZ);
                biomeMap.setRGB(i, j, biomeColors.getOrDefault(currBiome, new Color(0, 255, 0).getRGB()));

                progress ++;
                if (lastPrinted + 3000 < Calendar.getInstance().getTimeInMillis()) {
                    lastPrinted = Calendar.getInstance().getTimeInMillis();
                    Bukkit.getLogger().info("Generation progress: " + progress / progressMax);
                    Bukkit.getLogger().info("Cache size: " + biomeCache.size() + " / " + CACHE_SIZE);
                }
            }
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
    public int[][] zoom_in(int[][] original, World wld, int x, int z, int scale) {
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
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + i * 2 * scale + 1, z + j * 2 * scale);
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
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + i * 2 * scale, z + j * 2 * scale + 1);
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
                ArrayList<Integer> candidates = new ArrayList<>(4);
                HashMap<Integer, Integer> occurrence = new HashMap<>(6);
                for (int indX = i; indX <= i + 1; indX ++)
                    for (int indZ = j; indZ <= j + 1; indZ ++) {
                        int gridType = original[indX][indZ];
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
                int rdmResult = RandomGenerator.getRandomGenerator(wld.getSeed(), x + i * 2 * scale + 1, z + j * 2 * scale + 1).nextInt();
                result[i * 2 + 1][j * 2 + 1] = candidates.get(Math.abs(rdmResult) % candidates.size());
            }
        return result;
    }
    public int[][] add_islands(int[][] mapLand, World wld, int x, int z, int scale) {
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                boolean hasAdjacentLand = false;
                boolean hasAdjacentOcean = false;
                for (int checkI = i - 1; checkI < size; checkI += 2) {
                    if (checkI < 0) continue;
                    for (int checkJ = j - 1; checkJ < size; checkJ += 2) {
                        if (checkJ < 0) continue;
                        if (mapLand[i][j] == 1) hasAdjacentLand = true;
                        else hasAdjacentOcean = true;
                        if (hasAdjacentLand && hasAdjacentOcean) break;
                    }
                }
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + i * scale, z + j * scale);
                if (hasAdjacentLand && mapLand[i][j] == 0 && rdmResult < 0.33) result[i][j] = 1;
                else if (hasAdjacentOcean && mapLand[i][j] == 1 && rdmResult < 0.2) result[i][j] = 0;
                else result[i][j] = mapLand[i][j];
            }
        return result;
    }
    public int[][] fill_ocean(int[][] mapLand, World wld, int x, int z, int scale) {
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                result[i][j] = mapLand[i][j];
                // do nothing to lands
                if (mapLand[i][j] == 1) continue;
                boolean hasAdjacentLand = false;
                for (int checkI = i - 1; checkI < size; checkI += 2) {
                    if (checkI < 0) continue;
                    if (mapLand[checkI][j] == 1) {
                        hasAdjacentLand = true;
                        break;
                    }
                }
                if (!hasAdjacentLand)
                    for (int checkJ = j - 1; checkJ < size; checkJ += 2) {
                        if (checkJ < 0) continue;
                        if (mapLand[i][checkJ] == 1) {
                            hasAdjacentLand = true;
                            break;
                        }
                    }
                if (!hasAdjacentLand) {
                    // only try to make ocean a land when it has all water around
                    if (RandomGenerator.getRandom(wld.getSeed(), x + i * scale, z + j * scale) < 0.5) result[i][j] = 1;
                }
            }
        return result;
    }
    public int[][] smooth_biome(int[][] mapLand, World wld, int x, int z, int scale) {
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                result[i][j] = mapLand[i][j];
                boolean smoothed = false;
                if (i == 0 || j == 0 || i + 1 == size || j + 1 == size) continue;
                if (mapLand[i - 1][j] == mapLand[i + 1][j]) {
                    result[i][j] = mapLand[i - 1][j];
                    smoothed = true;
                }
                if (mapLand[i][j - 1] == mapLand[i][j + 1]) {
                    if (smoothed && RandomGenerator.getRandom(wld.getSeed(), x + i * scale, z + i * scale) < 0.5)
                        continue;
                    result[i][j] = mapLand[i][j - 1];
                }
            }
        return result;
    }
    public int[][] add_beach(int[][] mapLand, World wld, int x, int z, int scale) {
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                result[i][j] = mapLand[i][j];
                if (i == 0 || j == 0 || i + 1 == size || j + 1 == size) continue;
                int adjacentOcean = 0, adjacentSulphurous = 0;
                if (mapLand[i - 1][j] == -1) adjacentSulphurous ++;
                else if (mapLand[i - 1][j] == 0) adjacentOcean ++;
                if (mapLand[i + 1][j] == -1) adjacentSulphurous ++;
                else if (mapLand[i + 1][j] == 0) adjacentOcean ++;
                if (mapLand[i][j - 1] == -1) adjacentSulphurous ++;
                else if (mapLand[i][j - 1] == 0) adjacentOcean ++;
                if (mapLand[i][j + 1] == -1) adjacentSulphurous ++;
                else if (mapLand[i][j + 1] == 0) adjacentOcean ++;
                if (adjacentSulphurous >= adjacentOcean && adjacentSulphurous > 0) result[i][j] = 8;
                else if (adjacentOcean > adjacentSulphurous) result[i][j] = 9;
            }
        return result;
    }
    public int[][] setup_rough_biome(int[][] mapLand, World wld, int x, int z, int scale) {
        // -1: sulphurous ocean  0: ocean
        // 1: forest  2: jungle  3: tundra  4: desert  5: corruption  6: hallow  7: astral infection
        // 8: sulphurous beach  9: beach
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                int blockX = x + i * scale, blockZ = z + j * scale;
                boolean isNearSpawnloc = (Math.abs(blockX) < 1000 && Math.abs(blockZ) < 1000);
                double randomNum = RandomGenerator.getRandom(wld.getSeed(), blockX, blockZ);
                if (mapLand[i][j] == 0) {
                    // ocean
                    if (randomNum < 0.2) result[i][j] = -1;
                } else {
                    // land
                    if (randomNum < 0.075) result[i][j] = 2;
                    else if (randomNum < 0.15) result[i][j] = 3;
                    else if (randomNum < 0.225) result[i][j] = 4;
                    else if (randomNum < 0.26) result[i][j] = isNearSpawnloc ? 1 : 5;
                    else if (randomNum < 0.275) result[i][j] = isNearSpawnloc ? 1 : 6;
                    else if (randomNum < 0.3) result[i][j] = isNearSpawnloc ? 1 : 7;
                }
            }
        return result;
    }
    public int getGeneralBiomeGrid(World world, int x, int z, int gridSize, int recursion) {
        int gridX = x / gridSize, gridZ = z / gridSize;
        if (x < 0 && x % gridSize != 0) gridX --;
        if (z < 0 && z % gridSize != 0) gridZ --;
        String biomeLocKey = recursion + "|" + gridX + "|" + gridZ;
        total ++;
        if (biomeCache.containsKey(biomeLocKey)) {
            cached ++;
        } else {
            // setup original grid.
            final int radius = 1;
            String operation = biomeGenProcess[biomeGenProcess.length - recursion];
            int[][] land_grid = new int[radius * 2 + 1][radius * 2 + 1];
            int grid_x_begin, grid_z_begin;
            int gridSizeOffset = gridSize;
            if (operation.equals("zoom_in")) {
                gridSizeOffset = gridSize * 2;
                grid_x_begin = x / gridSizeOffset;
                grid_z_begin = z / gridSizeOffset;
                if (x < 0 && x % gridSizeOffset != 0) grid_x_begin--;
                if (z < 0 && z % gridSizeOffset != 0) grid_z_begin--;
            } else {
                grid_x_begin = gridX;
                grid_z_begin = gridZ;
            }
            // offset x and z by -radius. [radius][radius] contains the current grid to return.
            grid_x_begin -= radius;
            grid_z_begin -= radius;
            int offsetX = grid_x_begin * gridSizeOffset, offsetZ = grid_z_begin * gridSizeOffset;
            {
                int offsetI = radius * -1;
                for (int i = 0; i < radius * 2 + 1; i++) {
                    int offsetJ = radius * -1;
                    for (int j = 0; j < radius * 2 + 1; j++) {
                        if (recursion < biomeGenProcess.length) {
                            int blockX, blockZ;
                            blockX = x + offsetI * gridSizeOffset;
                            blockZ = z + offsetJ * gridSizeOffset;
                            land_grid[i][j] = getGeneralBiomeGrid(world, blockX, blockZ, gridSizeOffset, recursion + 1);
                        } else {
                            int grid_x_curr = gridX + i - radius, grid_z_curr = gridZ + j - radius;
                            if (grid_x_curr == 0 && grid_z_curr == 0) land_grid[i][j] = 1;
                            else if (RandomGenerator.getRandom(world.getSeed(), grid_x_curr, grid_z_curr) < 0.1)
                                land_grid[i][j] = 1;
                            else land_grid[i][j] = 0;
                        }
                        offsetJ++;
                    }
                    offsetI++;
                }
            }
            int marginDiscard = 1;
            if (operation.equals("setup_rough_biome")) {
                land_grid = setup_rough_biome(land_grid, wld, offsetX, offsetZ, gridSize);
                marginDiscard = 0;
            } else {
                switch (operation) {
                    case "zoom_in":
                        land_grid = zoom_in(land_grid, wld, offsetX, offsetZ, gridSize);
                        break;
                    case "add_islands":
                        land_grid = add_islands(land_grid, wld, offsetX, offsetZ, gridSize);
                        break;
                    case "fill_ocean":
                        land_grid = fill_ocean(land_grid, wld, offsetX, offsetZ, gridSize);
                        break;
                    case "smooth_biome":
                        land_grid = smooth_biome(land_grid, wld, offsetX, offsetZ, gridSize);
                        break;
                    case "add_beach":
                        land_grid = add_beach(land_grid, wld, offsetX, offsetZ, gridSize);
                        break;
                }
            }
            for (int i = marginDiscard; i + marginDiscard < land_grid.length; i++) {
                for (int j = marginDiscard; j + marginDiscard < land_grid[i].length; j++) {
                    int halfSize = land_grid.length / 2;
                    int grid_x_curr = gridX + i - halfSize, grid_z_curr = gridZ + j - halfSize;
                    String tempKey = recursion + "|" + grid_x_curr + "|" + grid_z_curr;
                    if (lastRecCall != recursion) {
                        lastRecCall = recursion;
////                    Bukkit.getLogger().info(cached / total + "/" + recursion + "|" + gridSizeOffset + "|" + gridSize + "........" + biomeLocKey + "   ||||   " + tempKey);
                    }
                    biomeCache.put(tempKey, land_grid[i][j]);
                }
            }
        }
        int result = biomeCache.get(biomeLocKey);
        if (biomeCache.size() > CACHE_DELETION_SIZE) {
            biomeCache.clear();
        }
        return result;
    }
        public Biome getBiome(World world, int x, int z) {
        // generateTestImage(world);
        generateBiomeImage(world);
        String biomeLocKey = 1+"|"+x+"|"+z;
        int rst;
        if (biomeCache.containsKey(biomeLocKey)) {
            rst = biomeCache.get(biomeLocKey);
            if (biomeCache.size() > CACHE_DELETION_SIZE) {
                biomeCache.clear();
            }
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