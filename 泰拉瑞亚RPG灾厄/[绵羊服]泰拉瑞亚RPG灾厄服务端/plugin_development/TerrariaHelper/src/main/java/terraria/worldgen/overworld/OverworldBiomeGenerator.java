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
        land_grid = zoomIn(land_grid, world, 1000, 1000, 2048);
        saveTestImage(land_grid, "1_1");
        land_grid = add_islands(land_grid, world, 1000, 1000, 2048);
        saveTestImage(land_grid, "1_2");
        // enlarge land/ocean, add islands thrice and fill some ocean. 1024 x 1024 for each grid.
        land_grid = zoomIn(land_grid, world, 1000, 1000,1024);
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
        land_grid = zoomIn(land_grid, world, 1000, 1000, 512);
        land_grid = zoomIn(land_grid, world, 1000, 1000, 256);
        saveTestImage(land_grid, "4_1");
        land_grid = add_islands(land_grid, world, 1000, 1000, 256);
        saveTestImage(land_grid, "4_2");
        land_grid = zoomIn(land_grid, world, 1000, 1000, 128);
        land_grid = zoomIn(land_grid, world, 1000, 1000, 64);
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
        int jumpX = 1, jumpZ = 1;
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
    public static OceanType getOceanType(World world, int x, int z) {
        PerlinNoiseGenerator noiseGen = new PerlinNoiseGenerator(world.getSeed() + 114);
        if (noiseGen.noise((double)x / 1000, (double)z / 1000) < 0)
            return OceanType.SULPHUROUS;
        return OceanType.STANDARD;
    }
    // biome enlarge helper functions
    public int[][] zoomIn(int[][] original, World wld, int x, int z, int scale) {
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
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + i * 2 * scale, z + j * 2 * scale);
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
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + i * 2 * scale, z + j * 2 * scale);
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
                double rdmResult = RandomGenerator.getRandom(wld.getSeed(), x + (i * 2 + 1) * scale, z + (j * 2 + 1) * scale);
                if (rdmResult < 0.25) {
                    result[i * 2 + 1][j * 2 + 1] = original[i][j];
                } else if (rdmResult < 0.5) {
                    result[i * 2 + 1][j * 2 + 1] = original[i + 1][j];
                } else if (rdmResult < 0.75) {
                    result[i * 2 + 1][j * 2 + 1] = original[i][j + 1];
                } else {
                    result[i * 2 + 1][j * 2 + 1] = original[i + 1][j + 1];
                }
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
    public int[][] setup_temperature(int[][] mapLand, World wld, int x, int z, int scale) {
        // TODO
        int size = mapLand.length;
        int[][] result = new int[size][size];

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
            int[][] land_grid = new int[3][3];
            int enlarge_total = 4096;
            int grid_x_begin = x / enlarge_total, grid_z_begin = z / enlarge_total;
            if (x < 0 && x % enlarge_total != 0) grid_x_begin --;
            if (z < 0 && z % enlarge_total != 0) grid_z_begin --;
            // offset x and z by -1. [1][1] contains the current position (x, z).
            grid_x_begin --;
            grid_z_begin --;
            int offsetX = grid_x_begin * enlarge_total, offsetZ = grid_z_begin * enlarge_total;
            {
                for (int i = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++) {
                        int grid_x = grid_x_begin + i, grid_z = grid_z_begin + j;
                        if (grid_x == 0 && grid_z == 0) land_grid[i][j] = 1;
                        else if (RandomGenerator.getRandom(world.getSeed(), grid_x, grid_z) < 0.1) land_grid[i][j] = 1;
                        else land_grid[i][j] = 0;
                    }
            }
            // enlarge land/ocean and add islands. 2048 x 2048 for each grid.
            land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 2048);
            land_grid = add_islands(land_grid, wld, offsetX, offsetZ, 2048);
            // enlarge land/ocean, add islands thrice and fill some ocean. 1024 x 1024 for each grid.
            land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 1024);
            for (int i = 0; i < 3; i++)
                land_grid = add_islands(land_grid, wld, offsetX, offsetZ, 1024);
            land_grid = fill_ocean(land_grid, wld, offsetX, offsetZ, 1024);
            // TODO: add temperature

            // add islands
            land_grid = add_islands(land_grid, wld, offsetX, offsetZ, 1024);
            // TODO: add ocean

            // TODO: smooth

            // enlarge * 2, add island then enlarge * 2, 64 x 64
            land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 512);
            land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 256);
            land_grid = add_islands(land_grid, wld, offsetX, offsetZ, 256);
            land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 128);
            land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 64);
            for (int i = 0; i < land_grid.length; i++)
                for (int j = 0; j < land_grid[i].length; j++) {
                    String tempKey = (offsetX + i * gridSize) + "|" + (offsetZ + j * gridSize);
                    biomeGridGeneralCache.put(tempKey, land_grid[i][j]);
                }
        }
        // Bukkit.getLogger().info("biomeLocKey: " + biomeLocKey);
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
        int[][] land_grid = new int[3][3];
        int enlarge_total = 64;
        int grid_x_begin = x / enlarge_total, grid_z_begin = z / enlarge_total;
        if (x < 0 && x % enlarge_total != 0) grid_x_begin --;
        if (z < 0 && z % enlarge_total != 0) grid_z_begin --;
        // offset x and z by -1. [1][1] contains the current position (x, z)
        grid_x_begin --;
        grid_z_begin --;
        int offsetX = grid_x_begin * enlarge_total, offsetZ = grid_z_begin * enlarge_total;
        for (int i = 0; i < 3; i ++)
            for (int j = 0; j < 3; j ++) {
                land_grid[i][j] = getGeneralBiomeGrid(world, (grid_x_begin + i) * enlarge_total, (grid_z_begin + j) * enlarge_total);
////                Bukkit.getLogger().info("Biome gen @ " + x + ", " + z + ", grid @ " + (grid_x_begin + i) * enlarge_total + ", " + (grid_z_begin + j) * enlarge_total);
            }
        // TODO: tweak ocean and other special biomes

        // enlarge, 32 * 32
        land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 32);
        // TODO: smooth

        // enlarge and add island, 16 * 16
        land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 16);
        land_grid = add_islands(land_grid, wld, offsetX, offsetZ, 16);
        // enlarge and add beach etc. 8 * 8
        land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 8);
        // TODO: add beach

        // enlarge * 3, 1 * 1
        land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 4);
        land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 2);
        land_grid = zoomIn(land_grid, wld, offsetX, offsetZ, 1);
        // TODO: smooth

        Biome result;
        for (int i = 0; i < land_grid.length; i ++)
            for (int j = 0; j < land_grid[i].length; j ++) {
                if (land_grid[i][j] == 0) result = Biome.OCEAN;
                else result = Biome.FOREST;
                String tempLocKey = (offsetX + i)+"|"+(offsetZ + j);
                biomeCache.put(tempLocKey, result);
            }
        return biomeCache.get(biomeLocKey);
    }
    public enum LandType {
        OCEAN, MUTATED, COLD, NORMAL, WARM
    }
    public enum OceanType {
        STANDARD, SULPHUROUS
    }
}
