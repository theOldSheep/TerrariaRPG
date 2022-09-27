package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.noise.PerlinNoiseGenerator;
import terraria.worldgen.RandomGenerator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class OverworldBiomeGenerator {
    World wld;
    HashMap<Biome, Integer> biomeColors;
    HashMap<String, Biome> biomeCache;
    HashMap<String, Integer> biomeGridGeneralCache;

    public OverworldBiomeGenerator() {
        biomeCache = new HashMap<>(750000);
        biomeGridGeneralCache = new HashMap<>(50000);
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
                Biome currBiome = biomeMap[i][j] == 0 ? Biome.OCEAN : Biome.FOREST;
                imageToSave.setRGB(i, j, biomeColors.getOrDefault(currBiome, new Color(0, 255, 0).getRGB()));
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
                land_grid[i][j] = getGeneralBiomeGrid(world, i * 64, j * 64);
        Bukkit.getLogger().info(land_grid + "");
        saveTestImage(land_grid, "biomeGrid");
    }
    public void generateBiomeImage(World world) {
        if (wld != null) return;
        wld = world;
        generateBiomeGridImage(wld);
        // test: save a map of biomes for testing purposes
        int scaleX = 1000, scaleZ = 1000;
        int jumpX = 10, jumpZ = 10;
        BufferedImage biomeMap = new BufferedImage(scaleX, scaleZ, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < scaleX; i ++)
            for (int j = 0; j < scaleZ; j ++) {
                int blockX = (i-(scaleX / 2)) * jumpX, blockZ = (j-(scaleZ / 2)) * jumpZ;
                Biome currBiome = getBiome(wld, blockX, blockZ);
                biomeMap.setRGB(i, j, biomeColors.getOrDefault(currBiome, new Color(0, 255, 0).getRGB()));
            }
        File dir_biome_map = new File("world/biomesMap.png");
        try {
            ImageIO.write(biomeMap, "png", dir_biome_map);
        } catch (IOException e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(e.getMessage());
        }
    }
    // biome enlarge helper functions
    public int[][] zoom_in(int[][] original, World wld, int x, int z, int scale) {
        // discard the margin! it does not have the information outside the grid, so it generates faulty!
        int sizeOriginal = original.length;
        int size = sizeOriginal * 2 - 3;
        int[][] result = new int[size][size];
        // o o o o o o o
        // o · · · · · o
        // o · * · * · o
        // o · · · · · o
        // o · * · * · o
        // o · · · · · o
        // o o o o o o o
        for (int i = 1; i + 1 < sizeOriginal; i ++)
            for (int j = 1; j + 1 < sizeOriginal; j ++) {
                result[i * 2][j * 2] = original[i][j];
            }
        for (int i = 0; i + 1 < sizeOriginal; i ++)
            for (int j = 1; j + 1 < sizeOriginal; j ++) {
                // o o o o o o o
                // o · * · * · o
                // o · · · · · o
                // o · * · * · o
                // o · · · · · o
                // o · * · * · o
                // o o o o o o o
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + i * 2 * scale, z + j * 2 * scale);
                if (rdmResult < 0.5) {
                    result[i * 2][j * 2 - 1] = original[i][j + 1];
                } else {
                    result[i * 2][j * 2 - 1] = original[i + 1][j + 1];
                }
            }
        for (int i = 1; i + 1 < sizeOriginal; i ++)
            for (int j = 0; j + 1 < sizeOriginal; j ++) {
                // o o o o o o o
                // o · · · · · o
                // o * · * · * o
                // o · · · · · o
                // o * · * · * o
                // o · · · · · o
                // o o o o o o o
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + i * 2 * scale, z + j * 2 * scale);
                if (rdmResult < 0.5) {
                    result[i * 2 - 1][j * 2] = original[i + 1][j];
                } else {
                    result[i * 2 - 1][j * 2] = original[i + 1][j + 1];
                }
            }
        for (int i = 0; i + 1 < sizeOriginal; i ++)
            for (int j = 0; j + 1 < sizeOriginal; j ++) {
                // o o o o o o o
                // o * · * · * o
                // o · · · · · o
                // o * · * · * o
                // o · · · · · o
                // o * · * · * o
                // o o o o o o o
                HashMap<Integer, Integer> diagonalBiome = new HashMap<>(8);
                diagonalBiome.put(original[i][j], diagonalBiome.getOrDefault(original[i][j], 0) + 1);
                diagonalBiome.put(original[i + 1][j], diagonalBiome.getOrDefault(original[i + 1][j], 0) + 1);
                diagonalBiome.put(original[i][j + 1], diagonalBiome.getOrDefault(original[i][j + 1], 0) + 1);
                diagonalBiome.put(original[i + 1][j + 1], diagonalBiome.getOrDefault(original[i + 1][j + 1], 0) + 1);
                int biomeMostCount = 0, biomeMost = 0;
                for (int biomeType : diagonalBiome.keySet())
                    if (diagonalBiome.get(biomeType) > biomeMostCount) {
                        biomeMostCount = diagonalBiome.get(biomeType);
                        biomeMost = biomeType;
                    }
                result[i * 2][j * 2] = biomeMost;
            }
        return result;
    }
    public int[][] add_islands(int[][] mapLand, World wld, int x, int z, int scale) {
        // discard the margin! it does not have the information outside the grid, so it generates faulty!
        int size = mapLand.length - 2;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                boolean hasAdjacentLand = false;
                boolean hasAdjacentOcean = false;
                for (int checkI = i - 1; checkI < size && checkI < i; checkI += 2) {
                    if (checkI < 0) continue;
                    for (int checkJ = j - 1; checkJ < size && checkJ < j; checkJ += 2) {
                        if (checkJ < 0) continue;
                        if (mapLand[i + 1][j + 1] >= 1) hasAdjacentLand = true;
                        else hasAdjacentOcean = true;
                        if (hasAdjacentLand && hasAdjacentOcean) break;
                    }
                }
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + i * scale, z + j * scale);
                if (hasAdjacentLand && mapLand[i + 1][j + 1] <= 0 && rdmResult < 0.33) result[i][j] = 1;
                else if (hasAdjacentOcean && mapLand[i + 1][j + 1] >= 1 && rdmResult < 0.2) result[i][j] = 0;
                else result[i][j] = mapLand[i + 1][j + 1];
            }
        return result;
    }
    public int[][] fill_ocean(int[][] mapLand, World wld, int x, int z, int scale) {
        // discard the margin! it does not have the information outside the grid, so it generates faulty!
        int size = mapLand.length - 2;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                result[i][j] = mapLand[i + 1][j + 1];
                // do nothing to lands
                if (mapLand[i + 1][j + 1] == 1) continue;
                boolean hasAdjacentLand = false;
                for (int checkI = i - 1; checkI < size && checkI < i; checkI += 2) {
                    if (checkI < 0) continue;
                    if (mapLand[checkI + 1][j + 1] >= 1) {
                        hasAdjacentLand = true;
                        break;
                    }
                }
                if (!hasAdjacentLand)
                    for (int checkJ = j - 1; checkJ < size && checkJ < j; checkJ += 2) {
                        if (checkJ < 0) continue;
                        if (mapLand[i + 1][checkJ + 1] >= 1) {
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
        // discard the margin! it does not have the information outside the grid, so it generates faulty!
        int size = mapLand.length - 2;
        int[][] result = new int[size][size];
        for (int i = 1; i <= size; i ++)
            for (int j = 1; j <= size; j ++) {
                result[i - 1][j - 1] = mapLand[i][j];
                boolean smoothed = false;
                if (mapLand[i - 1][j] == mapLand[i + 1][j]) {
                    result[i - 1][j - 1] = mapLand[i - 1][j];
                    smoothed = true;
                }
                if (mapLand[i][j - 1] == mapLand[i][j + 1]) {
                    if (smoothed && RandomGenerator.getRandom(wld.getSeed(), x + i * scale, z + i * scale) < 0.5)
                        continue;
                    result[i - 1][j - 1] = mapLand[i][j - 1];
                }
            }
        return result;
    }
    public int[][] add_beach(int[][] mapLand, World wld, int x, int z, int scale) {
        // discard the margin! it does not have the information outside the grid, so it generates faulty!
        int size = mapLand.length - 2;
        int[][] result = new int[size][size];
        for (int i = 1; i <= size; i ++)
            for (int j = 1; j <= size; j ++) {
                result[i - 1][j - 1] = mapLand[i][j];
                int adjacentOcean = 0, adjacentSulphurous = 0;
                if (mapLand[i - 1][j] == -1) adjacentSulphurous ++;
                else if (mapLand[i - 1][j] == 0) adjacentOcean ++;
                if (mapLand[i + 1][j] == -1) adjacentSulphurous ++;
                else if (mapLand[i + 1][j] == 0) adjacentOcean ++;
                if (mapLand[i][j - 1] == -1) adjacentSulphurous ++;
                else if (mapLand[i][j - 1] == 0) adjacentOcean ++;
                if (mapLand[i][j + 1] == -1) adjacentSulphurous ++;
                else if (mapLand[i][j + 1] == 0) adjacentOcean ++;
                if (adjacentSulphurous >= adjacentOcean && adjacentSulphurous > 0) result[i - 1][j - 1] = 8;
                else if (adjacentOcean > adjacentSulphurous) result[i - 1][j - 1] = 9;
            }
        return result;
    }
    public int[][] setup_rough_biome(int[][] mapLand, World wld, int x, int z, int scale) {
        // we DO NOT discard the margin here, as it does not require any information from adjacent position.
        // -1: sulphurous ocean  0: ocean
        // 1: forest  2: jungle  3: tundra  4: desert  5: corruption  6: hallow  7: astral infection
        // 8: sulphurous beach  9: beach
        int size = mapLand.length;
        int[][] result = new int[size][size];
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++) {
                int blockX = x + i * scale, blockZ = z + j * scale;
                if (Math.abs(blockX) < 1000 && Math.abs(blockZ) < 1000) continue;
                Random randomGen = RandomGenerator.getRandomGenerator(wld.getSeed(), blockX, blockZ);
                if (mapLand[i][j] == 0) {
                    // ocean
                    if (randomGen.nextDouble() < 0.35) result[i][j] = -1;
                } else {
                    // land
                    if (randomGen.nextDouble() < 0.075) result[i][j] = 2;
                    else if (randomGen.nextDouble() < 0.15) result[i][j] = 3;
                    else if (randomGen.nextDouble() < 0.225) result[i][j] = 4;
                    else if (randomGen.nextDouble() < 0.26) result[i][j] = 5;
                    else if (randomGen.nextDouble() < 0.275) result[i][j] = 6;
                    else if (randomGen.nextDouble() < 0.3) result[i][j] = 7;
                }
            }
        return result;
    }
    public int getGeneralBiomeGrid(World world, int x, int z) {
        // the general biome grid, more delicate calculations needed to become useful
        int gridSize = 64;
        int gridX = x / gridSize, gridZ = z / gridSize;
        String biomeLocKey = (gridX * gridSize) + "|" + (gridZ * gridSize);
        if (!biomeGridGeneralCache.containsKey(biomeLocKey)) {
            // setup original land/ocean. 4096 x 4096 for each grid.
            // 1: land  0: water
            final int radius = 10;
            int[][] land_grid = new int[radius * 2 + 1][radius * 2 + 1];
            int enlarge_total = 4096;
            int grid_x_begin = x / enlarge_total, grid_z_begin = z / enlarge_total;
            if (x < 0 && x % enlarge_total != 0) grid_x_begin --;
            if (z < 0 && z % enlarge_total != 0) grid_z_begin --;
            // offset x and z by -2. [2][2] contains the current position (x, z).
            grid_x_begin -= radius;
            grid_z_begin -= radius;
            int offsetX = grid_x_begin * enlarge_total, offsetZ = grid_z_begin * enlarge_total;
            {
                for (int i = 0; i < radius * 2 + 1; i++)
                    for (int j = 0; j < radius * 2 + 1; j++) {
                        int grid_x = grid_x_begin + i, grid_z = grid_z_begin + j;
                        if (grid_x == 0 && grid_z == 0) land_grid[i][j] = 1;
                        else if (RandomGenerator.getRandom(world.getSeed(), grid_x, grid_z) < 0.1) land_grid[i][j] = 1;
                        else land_grid[i][j] = 0;
                    }
            }
            String[] processToUse = {
                    "zoom_in",
                    "add_islands",
                    "zoom_in",
                    "add_islands",
                    "add_islands",
                    "add_islands",
                    "fill_ocean",
                    "setup_rough_biome",
                    "add_islands",
                    "smooth_biome",
                    "zoom_in",
                    "zoom_in",
                    "add_islands",
                    "zoom_in",
                    "zoom_in"
            };
            int scale = 4096;
            for (String operation : processToUse) {
                Bukkit.getLogger().info(land_grid.length + "");
                if (operation.equals("setup_rough_biome")) {
                    land_grid = setup_rough_biome(land_grid, wld, offsetX, offsetZ, scale);
                } else {
                    scale /= 2;
                    switch (operation) {
                        case "zoom_in":
                            land_grid = zoom_in(land_grid, wld, offsetX, offsetZ, scale);
                            break;
                        case "add_islands":
                            land_grid = add_islands(land_grid, wld, offsetX, offsetZ, scale);
                            break;
                        case "fill_ocean":
                            land_grid = fill_ocean(land_grid, wld, offsetX, offsetZ, scale);
                            break;
                        case "smooth_biome":
                            land_grid = smooth_biome(land_grid, wld, offsetX, offsetZ, scale);
                            break;
                    }
                    offsetX += scale;
                    offsetZ += scale;
                }
            }
            int marginDiscard = 0;
            for (int i = marginDiscard; i + marginDiscard < land_grid.length; i++)
                for (int j = marginDiscard; j + marginDiscard < land_grid[i].length; j++) {
                    String tempKey = (offsetX + i * gridSize) + "|" + (offsetZ + j * gridSize);
                    biomeGridGeneralCache.put(tempKey, land_grid[i][j]);
                }
        }
        return biomeGridGeneralCache.get(biomeLocKey);
    }
    public Biome getBiome(World world, int x, int z) {
        generateTestImage(world);
        generateBiomeImage(world);
        String biomeLocKey = x+"|"+z;
        if (biomeCache.containsKey(biomeLocKey)) {
            Biome rst = biomeCache.get(biomeLocKey);
            if (biomeCache.size() > 500000) {
                biomeCache.clear();
            }
            return rst;
        }
        // setup original land/ocean. 64 x 64 for each grid.
        // 1: land  0: water
        final int radius = 10;
        int[][] land_grid = new int[radius * 2 + 1][radius * 2 + 1];
        int enlarge_total = 64;
        int grid_x_begin = x / enlarge_total, grid_z_begin = z / enlarge_total;
        if (x < 0 && x % enlarge_total != 0) grid_x_begin --;
        if (z < 0 && z % enlarge_total != 0) grid_z_begin --;
        // offset x and z by -2. [2][2] contains the current position (x, z)
        grid_x_begin -= radius;
        grid_z_begin -= radius;
        int offsetX = grid_x_begin * enlarge_total, offsetZ = grid_z_begin * enlarge_total;
        for (int i = 0; i < radius * 2 + 1; i ++)
            for (int j = 0; j < radius * 2 + 1; j ++) {
                land_grid[i][j] = getGeneralBiomeGrid(world, (grid_x_begin + i) * enlarge_total, (grid_z_begin + j) * enlarge_total);
            }

        String[] processToUse = {
                "zoom_in",
                "zoom_in",
                "add_islands",
                "zoom_in",
                "add_beach",
                "zoom_in",
                "zoom_in",
                "zoom_in",
                "smooth_biome",
        };
        int scale = 64;
        for (String operation : processToUse) {
            if (operation.equals("setup_rough_biome")) {
                land_grid = setup_rough_biome(land_grid, wld, offsetX, offsetZ, scale);
            } else {
                scale /= 2;
                switch (operation) {
                    case "zoom_in":
                        land_grid = zoom_in(land_grid, wld, offsetX, offsetZ, scale);
                        break;
                    case "add_islands":
                        land_grid = add_islands(land_grid, wld, offsetX, offsetZ, scale);
                        break;
                    case "fill_ocean":
                        land_grid = fill_ocean(land_grid, wld, offsetX, offsetZ, scale);
                        break;
                    case "smooth_biome":
                        land_grid = smooth_biome(land_grid, wld, offsetX, offsetZ, scale);
                        break;
                    case "add_beach":
                        land_grid = add_beach(land_grid, wld, offsetX, offsetZ, scale);
                        break;
                }
                offsetX += scale;
                offsetZ += scale;
            }
        }

        Biome result;
        int marginDiscard = 0;
        for (int i = marginDiscard; i + marginDiscard < land_grid.length; i ++)
            for (int j = marginDiscard; j + marginDiscard < land_grid[i].length; j ++) {
                switch (land_grid[i][j]) {
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
                String tempLocKey = (offsetX + i)+"|"+(offsetZ + j);
                biomeCache.put(tempLocKey, result);
            }
        return biomeCache.get(biomeLocKey);
    }
    public enum OceanType {
        STANDARD, SULPHUROUS
    }
}