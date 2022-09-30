package terraria.worldgen;


import org.bukkit.util.noise.PerlinNoiseGenerator;

import java.util.Random;

public class RandomGenerator {
    public static double getRandom(long worldSeed, int x, int z) {
        return getRandomGenerator(worldSeed, x, z).nextDouble();
    }
    public static Random getRandomGenerator(long worldSeed, int x, int z) {
        return new Random(worldSeed + x * 11451L + z * 41919L + (long) x * z);
//        return new Random(worldSeed + new Random(new Random(x).nextLong() + z * 1145).nextLong());
    }
    public static double getRandomByPerlinNoise(long worldSeed, int x, int z) {
        // as perlin noise uses trig functions this is a lot slower than random generator.
        return PerlinNoiseGenerator.getNoise(worldSeed, x * 114514, z * 1919810);
    }
}
