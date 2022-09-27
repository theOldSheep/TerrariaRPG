package terraria.worldgen;


import org.bukkit.util.noise.PerlinNoiseGenerator;

import java.util.Random;

public class RandomGenerator {
    public static double getRandom(long worldSeed, int x, int z) {
        return new Random(worldSeed + new Random(new Random(x).nextLong() + z * 1145).nextLong()).nextDouble();
    }
    public static Random getRandomGenerator(long worldSeed, int x, int z) {
        return new Random(worldSeed + new Random(new Random(x).nextLong() + z * 1145).nextLong());
    }
    public static double getRandomByPerlinNoise(long worldSeed, int x, int z) {
        return PerlinNoiseGenerator.getNoise(worldSeed, x * 114514, z * 1919810);
    }
}
