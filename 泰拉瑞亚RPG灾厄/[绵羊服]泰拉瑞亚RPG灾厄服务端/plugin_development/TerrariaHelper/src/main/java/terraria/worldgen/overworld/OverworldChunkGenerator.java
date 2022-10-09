package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

// this version uses noise to generate terrain. DOES NOT WORK.
public class OverworldChunkGenerator extends ChunkGenerator {
    static long seed = 0;
    SimplexOctaveGenerator continentalness, erosion, corruption, peek_valley,
                            temperature, humidity,
                            terrainNoise;
    double CONTINENTALNESS_SCALE_FACTOR = 0.00075,
            EROSION_SCALE_FACTOR = 0.0005,
            CORRUPTION_SCALE_FACTOR = 0.002,
            PV_SCALE_FACTOR = 0.001,
            TEMPERATURE_SCALE_FACTOR = 0.0025,
            HUMIDITY_SCALE_FACTOR = 0.0015,
            TERRAIN_SCALE_FACTOR = 0.25;
    int OCTAVES = 4,
        SEA_LEVEL = 85;

    boolean tested;

    public OverworldChunkGenerator() {
        tested = false;
    }

    // test
    void test() {
        if (!tested) tested = true;
        else return;


        HashMap<Biome, Integer> biomeColors;
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


        int center = 0;
        int scale = 400;
        int jump = 25;
        double maxi = 0, mini = 0;
        BufferedImage contiImg = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB),
                erosImg = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB),
                corrImg = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB),
                peeksImg = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB),
                tempImg = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB),
                humiImg = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB),
                biomeImg = new BufferedImage(scale, scale, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < scale; i++)
            for (int j = 0; j < scale; j++) {
                int blockX = (i - (scale / 2)) * jump + center, blockZ = (j - (scale / 2)) * jump + center;
                double spawnProtection = getSpawnProtectionRatio(blockX, blockZ);
                double conti = getContinentalness(blockX, blockZ, spawnProtection),
                        eros = getErosion(blockX, blockZ, spawnProtection),
                        corr = getCorruption(blockX, blockZ, spawnProtection),
                        peeks = getPeekValley(blockX, blockZ, spawnProtection),
                        temp = getTemperature(blockX, blockZ, spawnProtection),
                        humi = getHumidity(blockX, blockZ, spawnProtection);
                maxi = Math.max(maxi, peeks);
                mini = Math.min(mini, peeks);
                contiImg.setRGB(i, j, new Color((int) (255 * (conti + 1) / 2)).getRGB());
                erosImg.setRGB(i, j, new Color((int) (255 * (eros + 1) / 2)).getRGB());
                corrImg.setRGB(i, j, new Color((int) (255 * (corr + 1) / 2)).getRGB());
                peeksImg.setRGB(i, j, new Color((int) (255 * (peeks + 1) / 2)).getRGB());
                tempImg.setRGB(i, j, new Color((int) (255 * (temp + 1) / 2)).getRGB());
                humiImg.setRGB(i, j, new Color((int) (255 * (humi + 1) / 2)).getRGB());

                Biome currBiome = getBiome(conti, eros, corr, temp, humi);
                biomeImg.setRGB(i, j, biomeColors.getOrDefault(currBiome, new Color(0, 255, 0).getRGB()));
            }
        Bukkit.getLogger().info(maxi + "~" + mini);
        File contiDir = new File("worldGenDebug/conti.png"),
                erosDir = new File("worldGenDebug/eros.png"),
                corrDir = new File("worldGenDebug/corr.png"),
                peeksDir = new File("worldGenDebug/peeks.png"),
                tempDir = new File("worldGenDebug/temp.png"),
                humiDir = new File("worldGenDebug/humi.png"),
                biomeDir = new File("worldGenDebug/biome.png");
        try {
            ImageIO.write(contiImg, "png", contiDir);
            ImageIO.write(erosImg, "png", erosDir);
            ImageIO.write(corrImg, "png", corrDir);
            ImageIO.write(peeksImg, "png", peeksDir);
            ImageIO.write(tempImg, "png", tempDir);
            ImageIO.write(humiImg, "png", humiDir);
            ImageIO.write(biomeImg, "png", biomeDir);
        } catch (IOException e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(e.getMessage());
        }

    }

    // noise functions
    double getSpawnProtectionRatio(double x, double z) {
        // spawn protection makes sure the large scale within the spawn location shall be uncorrupted land
        double distFromOrigin = Math.abs(x) + Math.abs(z);
        if (distFromOrigin < 750) return (750 - distFromOrigin) / 750;
        return 0;
    }
    double getContinentalness(double x, double z, double spawnProtection) {
        if (continentalness == null) {
            continentalness = new SimplexOctaveGenerator(seed, OCTAVES);
            continentalness.setScale(CONTINENTALNESS_SCALE_FACTOR);
        }
        double original = continentalness.noise(x, z, 2, 0.5, false) * 1;;
//        original = original * original * original;
        return (1 - spawnProtection) * original;
    }
    double getErosion(double x, double z, double spawnProtection) {
        if (erosion == null) {
            erosion = new SimplexOctaveGenerator(seed, OCTAVES);
            erosion.setScale(EROSION_SCALE_FACTOR);
        }
        double original = erosion.noise(x, z, 2, 0.5, false) * 1;;
//        original = original * original * original;
        return (1 * spawnProtection) + (original * (1 - spawnProtection));
    }
    double getCorruption(double x, double z, double spawnProtection) {
        if (corruption == null) {
            corruption = new SimplexOctaveGenerator(seed, OCTAVES);
            corruption.setScale(CORRUPTION_SCALE_FACTOR);
        }
        double original = corruption.noise(x, z, 2, 0.5, false) * 1;;
//        original = original * original * original;
        return (original * (1 - spawnProtection));
    }
    double getPeekValley(double x, double z, double spawnProtection) {
        if (peek_valley == null) {
            peek_valley = new SimplexOctaveGenerator(seed, OCTAVES);
            peek_valley.setScale(PV_SCALE_FACTOR);
        }
        double original = peek_valley.noise(x, z, 2, 0.5, false) * 1;;
//        original = original * original * original;
        return original * (1 - spawnProtection);
    }
    double getTemperature(double x, double z, double spawnProtection) {
        if (temperature == null) {
            temperature = new SimplexOctaveGenerator(seed, OCTAVES);
            temperature.setScale(TEMPERATURE_SCALE_FACTOR);
        }
        double original = temperature.noise(x, z, 2, 0.5, false) * 1;;
//        original = original * original * original;
        return original * (1 - spawnProtection);
    }
    double getHumidity(double x, double z, double spawnProtection) {
        if (humidity == null) {
            humidity = new SimplexOctaveGenerator(seed, OCTAVES);
            humidity.setScale(HUMIDITY_SCALE_FACTOR);
        }
        double original = humidity.noise(x, z, 2, 0.5, false) * 1;;
//        original = original * original * original;
        return original * (1 - spawnProtection);
    }

    // helper functions
    // biome
    public static Biome getBiome(double conti, double eros, double corr, double temp, double humi) {
        if (conti < -0.9)  return Biome.OCEAN;                          // ocean
        if (conti < -0.85) return Biome.BEACHES;                        // beach
        if (conti < -0.7)  return Biome.FOREST;                         // forest next to beaches
        if (conti < 0.7) {
            // inner part of land
            if (corr < -0.8) return Biome.ICE_FLATS;                    // hallow
            else if (corr < -0.15) {
                if (humi > 0.25 && temp > 0.25) return Biome.JUNGLE;    // jungle
                if (humi < -0.25 && temp > 0.25) return Biome.DESERT;   // desert
                if (temp < -0.25) return Biome.TAIGA_COLD;              // tundra
            } else if (corr > 0.15) {
                if (eros > 0.25) return Biome.MESA;                     // astral infection
                else if (eros < -0.25) return Biome.MUSHROOM_ISLAND;    // corruption
            }
            return Biome.FOREST;                                        // forest
        }
        if (conti < 0.85) return Biome.FOREST;                          // forest next to sulphurous beaches
        if (conti < 0.9)  return Biome.STONE_BEACH;                     // sulphurous beaches
        return Biome.DEEP_OCEAN;                                        // abyss/sulphurous ocean
    }
    // raw terrain
    public static double getTerrainHeight(double conti, double eros, double peeks) {
        double baseContiHeight;
        if (conti < -0.925) baseContiHeight = 70;
        else if (conti < -0.875) baseContiHeight = 70 + 30 * (conti + 0.925) / 0.05;
        else if (conti < 0.875) {
            double contiIdx = Math.abs(conti) / 0.875;
            if (contiIdx > 0.75) baseContiHeight = 100;
            else if (contiIdx > 0.6) baseContiHeight = 125 + 25 * (0.6 - contiIdx) / 0.15;
            else if (contiIdx > 0.5) baseContiHeight = 125;
            else if (contiIdx > 0.35) baseContiHeight = 100 + 25 * (contiIdx - 0.35) / 0.15;
            else baseContiHeight = 100;
        } else if (conti < 0.925) baseContiHeight = 100 * (0.925 - conti) / 0.05;
        else baseContiHeight = 0;
        if (baseContiHeight > 0) {
            // nothing should block the bottom of sulphurous ocean
            if (eros < 0 && Math.abs(conti) < 0.75) {
                baseContiHeight += eros * -15;
            }
            baseContiHeight += peeks * 20;
        }
        return baseContiHeight;
    }
    public static double getSquashFactor(double conti, double eros) {
        double contiABS = Math.abs(conti);
        double erosSquash = (eros + 1) / 2;
        if (contiABS > 0.9) return 1;
        if (contiABS > 0.75) {
            double originalErosMulti = (0.9 - contiABS) / 0.15;
            return (1 - originalErosMulti) + erosSquash * originalErosMulti;
        }
        return erosSquash;
    }
    private void initializeTerrain(ChunkData chunk, int i, int j, double blockX, double blockZ, double conti, double eros, double peeks, Biome biome) {
        if (terrainNoise == null) {
            terrainNoise = new SimplexOctaveGenerator(seed, OCTAVES);
            terrainNoise.setScale(TERRAIN_SCALE_FACTOR);
        }

        // this function creates the raw terrain of the chunk, consisting of only air, water or stone.
        double height = getTerrainHeight(conti, eros, peeks),
                squash = getSquashFactor(conti, eros);
        int topHeight = 0;
        for (int k = 0; k < 256; k ++) {
            // k stands for y-coordinate
            double terrainNoiseResult = terrainNoise.noise(blockX, k, blockZ, 2, 0.5, true);
            terrainNoiseResult = terrainNoiseResult * terrainNoiseResult;
            // square it so that the solid vs air is more distinguishable.

            double heightDiff = Math.abs(k - height);
//            double maxKeepDepth = 1.1 - Math.pow(heightDiff + 1, squash / -2);
            double maxKeepDepth = squash * heightDiff / 5;
            terrainNoiseResult += (k > height ? -1 : 1.25) * maxKeepDepth;

            if (k == 0) chunk.setBlock(i, k, j, Material.BEDROCK);
            else if (terrainNoiseResult > 0) {
                chunk.setBlock(i, k, j, Material.STONE);
                topHeight = k;
            } else chunk.setBlock(i, k, j, Material.AIR);
        }
        // setup top soil etc.
        generateTopSoil(chunk, i, topHeight, j, blockX, blockZ, biome);
    }
    // tweak terrain
    void generateTopSoil(ChunkData chunk, int i, int k, int j, double blockX, double blockZ, Biome biome) {
        double topSoilThicknessRandomizer = terrainNoise.noise(blockX * TERRAIN_SCALE_FACTOR * 2, blockZ * TERRAIN_SCALE_FACTOR * 2, 2, 0.5, true);
        double topSoilThickness;
        switch (biome) {
            case OCEAN: // ocean
            case BEACHES: // beach
            case DEEP_OCEAN: // sulphurous ocean
            case STONE_BEACH: // sulphurous beach
                topSoilThickness = 35 + topSoilThicknessRandomizer * 5;
                for (int y = k; y > 0; y --) {
                    if (chunk.getType(i, y, j) == Material.AIR) {
                        if (y <= SEA_LEVEL) chunk.setBlock(i, y, j, Material.WATER);
                    }
                    else if (--topSoilThickness > 0) chunk.setBlock(i, y, j, Material.SAND);
                }
                break;
            default: // forest style landscape, consisting of the majority of biomes.
                Material matTopSoil, matSoil, matStone;
                // setup biome block info
                switch (biome) {
                    case TAIGA_COLD: // tundra
                        matTopSoil = Material.SNOW_BLOCK;
                        matSoil = Material.SNOW_BLOCK;
                        matStone = Material.PACKED_ICE;
                        break;
                    case DESERT: // desert
                        matTopSoil = Material.SAND;
                        matSoil = Material.SAND;
                        matStone = Material.SANDSTONE;
                        break;
                    case MUSHROOM_ISLAND: // corruption
                        matTopSoil = Material.MYCEL;
                        matSoil = Material.DIRT;
                        matStone = Material.STAINED_CLAY;
                        break;
                    case ICE_FLATS: // hallow
                    case MESA: // astral infection
                        matTopSoil = Material.DIRT;
                        matSoil = Material.DIRT;
                        matStone = Material.STAINED_CLAY;
                        break;
                    default: // forest, jungle
                        matTopSoil = Material.GRASS;
                        matSoil = Material.DIRT;
                        matStone = Material.STONE;
                }
                double soilLayerHeight = 50 + topSoilThicknessRandomizer * 10;
                // setup soil/stone layers
                for (int y = k; y > 0; y --) {
                    if (chunk.getType(i, y, j) == Material.AIR) continue;
                    if (y > soilLayerHeight) {
                        if (chunk.getType(i, y + 1, j) == Material.AIR)
                            chunk.setBlock(i, y, j, matTopSoil);
                        else
                            chunk.setBlock(i, y, j, matSoil);
                    } else {
                        if (matStone == Material.STONE) break;
                        chunk.setBlock(i, y, j, matStone);
                    }
                }

        }
    }
    // generation
    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        if (seed == 0) seed = world.getSeed();

        test();

        ChunkData chunk = createChunkData(world);
        for (int i = 0; i < 16; i ++) {
            for (int j = 0; j < 16; j ++) {
                // utility values
                int blockX = x * 16 + i, blockZ = z * 16 + j;
                double spawnProtection = getSpawnProtectionRatio(blockX, blockZ);
                double conti = getContinentalness(blockX, blockZ, spawnProtection),
                        eros = getErosion(blockX, blockZ, spawnProtection),
                        corr = getCorruption(blockX, blockZ, spawnProtection),
                        peeks = getPeekValley(blockX, blockZ, spawnProtection),
                        temp = getTemperature(blockX, blockZ, spawnProtection),
                        humi = getHumidity(blockX, blockZ, spawnProtection);
                // setup biome
                Biome currBiome = getBiome(conti, eros, corr, temp, humi);
                biome.setBiome(i, j, currBiome);
                // init terrain
                initializeTerrain(chunk, i, j, blockX, blockZ, conti, eros, peeks, currBiome);
                // tweak terrain
            }
        }
        return chunk;
    }
}
