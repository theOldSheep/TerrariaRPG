package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.noise.PerlinNoiseGenerator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class OverworldBiomeGenerator {
    static BufferedImage biomeMap;
    static HashMap<Biome, Integer> biomeColors;
    // features for biome determination
    static HashMap<Biome, BiomesFeature> biomeFeatures;
    public static int featureNumber = 4;
    // 海拔， 降水， 腐蚀， 温度
    public static PerlinNoiseGenerator[] noiseGenerators;
    public static class BiomesFeature {
        double[] features;
        public BiomesFeature(double... featuresInit) {
            features = new double[featureNumber];
            for (int i = 0; i < featureNumber; i ++) {
                if (i >= featuresInit.length) features[i] = 0;
                else features[i] = featuresInit[i];
            }
        }
        public double compare(double... featuresCompare) {
            double diff = 0;
            for (int i = 0; i < featureNumber; i ++) {
                if (i >= featuresCompare.length) diff += features[i] * features[i];
                else diff += (features[i] - featuresCompare[i]) * (features[i] - featuresCompare[i]);
            }
            return diff;
        }
    }
    public OverworldBiomeGenerator() {
        // biome colors for map test
        biomeColors = new HashMap<>();
        biomeColors.put(Biome.FOREST,               new Color(0, 255, 0).getRGB()); //forest
        biomeColors.put(Biome.JUNGLE,               new Color(0, 100, 0).getRGB()); //jungle
        biomeColors.put(Biome.DESERT,               new Color(255, 255, 0).getRGB()); //desert
        biomeColors.put(Biome.BEACHES,              new Color(255, 255, 150).getRGB()); //beach
        biomeColors.put(Biome.OCEAN,                new Color(0, 0, 255).getRGB()); //ocean
        biomeColors.put(Biome.COLD_BEACH,           new Color(130, 110, 100).getRGB()); //sulphurous beach
        biomeColors.put(Biome.FROZEN_OCEAN,         new Color(120, 200, 150).getRGB()); //sulphurous ocean
        biomeColors.put(Biome.TAIGA_COLD,           new Color(150, 200, 255).getRGB()); //tundra
        biomeColors.put(Biome.MUSHROOM_ISLAND,      new Color(150, 0, 150).getRGB()); //corruption
        biomeColors.put(Biome.MESA,                 new Color(50, 25, 60).getRGB()); //astral infection
        biomeColors.put(Biome.ICE_FLATS,            new Color(255, 255, 255).getRGB()); //hallow
        // 海拔， 降水， 腐蚀， 温度
        // biome features
        biomeFeatures = new HashMap<>();
        biomeFeatures.put(Biome.FOREST,             new BiomesFeature(0, 0, 0, 0)); //forest
        biomeFeatures.put(Biome.JUNGLE,             new BiomesFeature(0, 1, -0.5, 1)); //jungle
        biomeFeatures.put(Biome.DESERT,             new BiomesFeature(0, -1, -0.5, 1)); //desert
        biomeFeatures.put(Biome.OCEAN,              new BiomesFeature(-0.5, 0, -0.5, -0.5)); //ocean
        biomeFeatures.put(Biome.FROZEN_OCEAN,       new BiomesFeature(-0.5, 0, 1, 0.5)); //sulphurous ocean
        biomeFeatures.put(Biome.TAIGA_COLD,         new BiomesFeature(0, -0.5, -0.5, -1)); //tundra
        biomeFeatures.put(Biome.MUSHROOM_ISLAND,    new BiomesFeature(0, 0, 1, 0.5)); //corruption
        biomeFeatures.put(Biome.MESA,               new BiomesFeature(1, 0, 1, -0.5)); //astral infection
        biomeFeatures.put(Biome.ICE_FLATS,          new BiomesFeature(0, 0, -1, 0)); //hallow
    }

    public static void initGenerator(World wld) {
        if (noiseGenerators != null) return;
        // initialize perlin noise generators
        noiseGenerators = new PerlinNoiseGenerator[]{
                new PerlinNoiseGenerator(wld.getSeed()),
                new PerlinNoiseGenerator(wld.getSeed() * 114),
                new PerlinNoiseGenerator(wld.getSeed() * 514),
                new PerlinNoiseGenerator(wld.getSeed() + 1919810)
        };
        featureNumber = noiseGenerators.length;
        // test: save a map of biomes for testing purposes
        biomeMap = new BufferedImage(5000, 5000, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < biomeMap.getWidth(); i ++)
            for (int j = 0; j < biomeMap.getHeight(); j ++) {
                Biome currBiome = getBiome(wld, i-(biomeMap.getWidth() / 2), j-(biomeMap.getHeight() / 2));
                biomeMap.setRGB(i, j, biomeColors.getOrDefault(currBiome, new Color(0, 255, 0).getRGB()));
            }
        File output_dir = new File("biomesMap.png");
        try {
            ImageIO.write(biomeMap, "png", output_dir);
        } catch (IOException e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(e.getMessage());
        }
    }
    public static Biome getBiome(World world, int x, int z) {
        initGenerator(world);
        double[] biomesFeature = new double[featureNumber];
        for (int i = 0; i < featureNumber; i ++) {
            biomesFeature[i] = noiseGenerators[i].noise((double)x / 200, (double)z / 200);
        }
        Biome result = Biome.FOREST;
        double bestMatch = 1919810;
        for (Biome b : biomeFeatures.keySet()) {
            double diff = biomeFeatures.get(b).compare(biomesFeature);
            if (diff < bestMatch) {
                bestMatch = diff;
                result = b;
            }
        }
        return result;
    }
}
