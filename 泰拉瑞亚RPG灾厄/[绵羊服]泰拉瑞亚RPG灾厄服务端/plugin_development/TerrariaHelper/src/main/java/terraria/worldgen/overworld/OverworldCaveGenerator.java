package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Random;

public class OverworldCaveGenerator extends BlockPopulator {
    int yOffset;
    SimplexOctaveGenerator cheeseCaveGenerator, spaghettiGeneratorOne, spaghettiGeneratorTwo;


    static long test_cave = 0, test_cave_time = 0,
                test_cave_setup = 0, test_cave_setup_time = 0,
                blockTotal = 0, regenerated = 0;
    static final boolean test_timing = true;
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
    double[] getCavernNoise(Biome biome, int currX, int effectualY, int currZ) {
        double[] result = new double[]{-1, -1, -1};
        switch (biome) {
            case DESERT:            // desert
            case FROZEN_OCEAN:      // abyss/sulphurous ocean
                // caves for these biomes will be customized.
                return result;
        }
        double caveNoiseY = ((double) effectualY) / 3;
        if (effectualY > 30) {
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
            }
        } else {
            result[0] = cheeseCaveGenerator.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
            result[1] = spaghettiGeneratorOne.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
            result[2] = spaghettiGeneratorTwo.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
        }
        return result;
    }
    boolean validateCave(double[] noise, boolean isRoughEstimate) {
        double cheeseThreshold = isRoughEstimate ? 0.7 : 0.75;
        double spaghettiThreshold = isRoughEstimate ? 0.15 : 0.05;
        boolean isCave;
        if (noise[0] > cheeseThreshold)
            isCave = true;
        else isCave = Math.abs(noise[1]) < spaghettiThreshold && Math.abs(noise[2]) < spaghettiThreshold;
        return isCave;
    }
    boolean hasNearbyCaveEstimate(boolean[][][] caveEstimates, int estimateX, int estimateY, int estimateZ) {
        for (int i = estimateX - 1; i <= estimateX + 1; i ++)
            for (int j = estimateY - 1; j <= estimateY + 1; j ++)
                for (int k = estimateZ - 1; k <= estimateZ + 1; k ++)
                    if (caveEstimates[i][j][k]) return true;
        return false;
    }
    @Override
    public void populate(World wld, Random rdm, Chunk chunk) {

        long timing = System.nanoTime();

        // setup cave estimates
        boolean[][][] caveEstimates = new boolean[6][66][6];
        for (int i = 0; i < 6; i ++) {
            int currX = chunk.getX() * 16 + i * 4 - 4;
            for (int j = 0; j < 6; j ++) {
                int currZ = chunk.getZ() * 16 + j * 4 - 4;
                Biome columnBiome = wld.getBiome(currX, currZ);
                for (int y_coord = 0; y_coord < 66; y_coord ++) {
                    int effectualY = y_coord * 4 + yOffset - 4;
                    caveEstimates[i][y_coord][j] = validateCave(getCavernNoise(columnBiome, currX, effectualY, currZ), true);
                }
            }
        }
        // setup actual blocks
        for (int i = 0; i < 16; i ++) {
            int currX = chunk.getX() * 16 + i;
            int estimateX = 1 + i / 4;
            for (int j = 0; j < 16; j ++) {
                int currZ = chunk.getZ() * 16 + j;
                int estimateZ = 1 + j / 4;
                // loop through y to set blocks
                for (int y_coord = 1; y_coord < 255; y_coord ++) {
                    int effectualY = y_coord + yOffset;
                    int estimateY = 1 + y_coord / 4;
                    Block currBlock = chunk.getBlock(i, y_coord, j);
                    if (!currBlock.getType().isSolid()) break;
                    // check if the nearby estimates contains cave
                    if (hasNearbyCaveEstimate(caveEstimates, estimateX, estimateY, estimateZ)) {
                        // setup two types of cave noise
                        double[] noise = getCavernNoise(currBlock.getBiome(), currX, effectualY, currZ);
                        // cheese cave noise should be decreased above y=30, and completely gone above y=50
                        boolean isCave = validateCave(noise, false);
                        blockTotal ++;
                        if (isCave) {
                            currBlock.setType(Material.AIR, false);
                            if (++regenerated % 100000 == 0) Bukkit.getLogger().info("Cave percentage: " + (double)regenerated / blockTotal);
                        }
                    }
                }
            }
        }

        if (test_timing){
            test_cave += (System.nanoTime() - timing);
            test_cave_time ++;
            if (test_cave_time % 10 == 0)
                Bukkit.broadcastMessage("Time elapsed for generating cave: " + test_cave / test_cave_time);
        }
    }
}
