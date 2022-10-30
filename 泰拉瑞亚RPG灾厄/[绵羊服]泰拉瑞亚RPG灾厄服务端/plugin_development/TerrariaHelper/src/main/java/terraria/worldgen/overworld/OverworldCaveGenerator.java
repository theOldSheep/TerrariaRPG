package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Random;

public class OverworldCaveGenerator {
    int yOffset;
    SimplexOctaveGenerator cheeseCaveGenerator, spaghettiGeneratorOne, spaghettiGeneratorTwo;


    static long test_cave = 0, test_cave_time = 0,
                test_cave_setup = 0, test_cave_setup_time = 0,
                blockTotal = 0, regenerated = 0;
    static final boolean test_timing = false;
    public OverworldCaveGenerator(int yOffset, long seed, int OCTAVES) {
        this.yOffset = yOffset;

        Random rdm = new Random(seed);
        rdm.nextInt();
        cheeseCaveGenerator = new SimplexOctaveGenerator(rdm.nextLong(), OCTAVES);
        cheeseCaveGenerator.setScale(0.0075);
        cheeseCaveGenerator.setYScale(cheeseCaveGenerator.getYScale() * 5);
        spaghettiGeneratorOne = new SimplexOctaveGenerator(rdm.nextLong(), OCTAVES);
        spaghettiGeneratorOne.setScale(0.005);
        spaghettiGeneratorOne.setYScale(spaghettiGeneratorOne.getYScale() * 4);
        spaghettiGeneratorTwo = new SimplexOctaveGenerator(rdm.nextLong(), OCTAVES);
        spaghettiGeneratorTwo.setScale(0.0055);
        spaghettiGeneratorTwo.setYScale(spaghettiGeneratorTwo.getYScale() * 4);

    }
    private double[] getCavernNoise(Biome biome, int height, int currX, int effectualY, int currZ) {
        double[] result = new double[]{-1, -1, -1};
        switch (biome) {
            case DESERT:            // desert
            case COLD_BEACH:        // sulphurous beach
            case FROZEN_OCEAN:      // abyss/sulphurous ocean
                // caves for these biomes will be customized.
                return result;
        }
        double caveNoiseY = ((double) effectualY) / 3;
        if (effectualY > 30) {
            boolean hasRiver = height - yOffset - 2 < OverworldChunkGenerator.SEA_LEVEL;
            double caveNoiseOffset = ((double) (effectualY - 30)) / 20;
            if (caveNoiseOffset < 2) {
                result[0] = cheeseCaveGenerator.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false) - caveNoiseOffset;
            }
            switch (biome) {
                case FOREST:        // forest
                case JUNGLE:        // jungle
                case TAIGA_COLD:    // tundra
                case ICE_FLATS:     // hallow
                case MESA:          // astral infection
                    // only these biomes may have surface spaghetti caves!
                    result[1] = spaghettiGeneratorOne.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
                    result[2] = spaghettiGeneratorTwo.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
                    if (hasRiver) {
                        if (result[1] < 0)
                            result[1] -= caveNoiseOffset;
                        else
                            result[1] += caveNoiseOffset;
                        if (result[2] < 0)
                            result[2] -= caveNoiseOffset;
                        else
                            result[2] += caveNoiseOffset;
                    }
            }
        } else {
            result[0] = cheeseCaveGenerator.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
            result[1] = spaghettiGeneratorOne.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
            result[2] = spaghettiGeneratorTwo.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
        }
        return result;
    }
    private boolean validateCaveEstimate(double[] noise) {
        double cheeseThreshold = 0.7;
        double spaghettiThreshold = 0.2;
        return (noise[0] > cheeseThreshold) || (
                    (Math.abs(noise[1]) < spaghettiThreshold) &&
                    (Math.abs(noise[2]) < spaghettiThreshold));
    }
    private boolean validateCave(double[] noise) {
        double cheeseThreshold = 0.75;
        double spaghettiThreshold = 0.05;
        return (noise[0] > cheeseThreshold) || (
                    (Math.abs(noise[1]) < spaghettiThreshold) &&
                    (Math.abs(noise[2]) < spaghettiThreshold));
    }
    private boolean hasNearbyCaveEstimate(boolean[][][] caveEstimates, int estimateX, int estimateY, int estimateZ) {
        for (int i = estimateX - 1; i <= estimateX + 1; i ++)
            for (int j = estimateY - 1; j <= estimateY + 1; j ++)
                for (int k = estimateZ - 1; k <= estimateZ + 1; k ++)
                    if (caveEstimates[i][j][k]) return true;
        return false;
    }

    public void populate(World wld, ChunkGenerator.ChunkData chunk, ChunkGenerator.BiomeGrid biome, int[][] heightMap, int x, int z) {
        // setup cave estimates
        boolean[][][] caveEstimates = new boolean[6][66][6];
        for (int i = 0; i < 6; i ++) {
            int currX = (x << 4) + ((i - 1) << 2);
            for (int j = 0; j < 6; j ++) {
                int currZ = (z << 4) + ((j - 1) << 2);
                Biome columnBiome = wld.getBiome(currX, currZ);
                for (int y_coord = 0; y_coord < 66; y_coord ++) {
                    int effectualY = ((y_coord - 1) << 2) + yOffset;
                    caveEstimates[i][y_coord][j] = validateCaveEstimate(getCavernNoise(columnBiome, heightMap[i][j], currX, effectualY, currZ));
                }
            }
        }

        if (test_timing && ++test_cave_setup_time % 10 == 0)
            Bukkit.broadcastMessage("Time elapsed for setup cave estimates: " + test_cave_setup / test_cave_setup_time);
        if (test_timing && ++test_cave_time % 10 == 0)
            Bukkit.broadcastMessage("Time elapsed for generating cave: " + test_cave / test_cave_time);
        // setup actual blocks
        for (int i = 0; i < 16; i ++) {
            int currX = (x << 4) + i;
            int estimateX = 1 + (i >> 2);
            for (int j = 0; j < 16; j ++) {
                int currZ = (z << 4) + j;
                int estimateZ = 1 + (j >> 2);
                // loop through y to set blocks
                for (int y_coord = 1; y_coord < 255; y_coord ++) {
                    int effectualY = y_coord + yOffset;
                    int estimateY = 1 + (y_coord >> 2);
                    Material currBlock = chunk.getType(i, y_coord, j);
                    if (!currBlock.isSolid()) break;
                    // check if the nearby estimates contains cave

                    long timing = System.nanoTime();
                    boolean shouldCheckCave = hasNearbyCaveEstimate(caveEstimates, estimateX, estimateY, estimateZ);

                    if (test_timing){
                        test_cave_setup += (System.nanoTime() - timing);
                        timing = System.nanoTime();
                    }
                    if (shouldCheckCave) {
                        // setup two types of cave noise
                        double[] noise = getCavernNoise(biome.getBiome(i, j), heightMap[i][j], currX, effectualY, currZ);
                        // cheese cave noise should be decreased above y=30, and completely gone above y=50
                        boolean isCave = validateCave(noise);
                        if (test_timing) {
                            test_cave += (System.nanoTime() - timing);
                            blockTotal ++;
                            if (isCave) {
                                chunk.setBlock(i, y_coord, j, Material.AIR);
                                if (++regenerated % 100000 == 0) Bukkit.getLogger().info("Cave percentage: " + (double)regenerated / blockTotal);
                            }
                        }
                    }
                }
            }
        }
    }
    public void populate_no_estimate(ChunkGenerator.ChunkData chunk, ChunkGenerator.BiomeGrid biome, int[][] heightMap, int x, int z) {
        // setup actual blocks
        for (int i = 0; i < 16; i ++) {
            int currX = (x << 4) + i;
            for (int j = 0; j < 16; j ++) {
                int currZ = (z << 4) + j;
                // loop through y to set blocks
                for (int y_coord = 1; y_coord < 255; y_coord ++) {
                    int effectualY = y_coord + yOffset;
                    Material currBlock = chunk.getType(i, y_coord, j);
                    if (!currBlock.isSolid()) break;

                    long timing = System.nanoTime();
                    // setup two types of cave noise
                    double[] noise = getCavernNoise(biome.getBiome(i, j), heightMap[i][j], currX, effectualY, currZ);
                    // cheese cave noise should be decreased above y=30, and completely gone above y=50
                    boolean isCave = validateCave(noise);
                    if (test_timing){
                        test_cave += (System.nanoTime() - timing);
                    }
                    if (isCave) {
                        chunk.setBlock(i, y_coord, j, Material.AIR);
                    }
                }
            }
        }
    }

}
