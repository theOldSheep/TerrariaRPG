package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Random;

public class OverworldCaveGenerator extends BlockPopulator {
    int yOffset;
    SimplexOctaveGenerator cheeseCaveGenerator, spaghettiGeneratorOne, spaghettiGeneratorTwo;


    static long test_cave = 0, test_cave_time = 0;
    static final boolean test_timing = false;
    public OverworldCaveGenerator(int yOffset, long seed, int OCTAVES) {
        this.yOffset = yOffset;

        cheeseCaveGenerator = new SimplexOctaveGenerator(seed, OCTAVES);
        cheeseCaveGenerator.setScale(0.0075);
        cheeseCaveGenerator.setYScale(cheeseCaveGenerator.getYScale() * 5);
        spaghettiGeneratorOne = new SimplexOctaveGenerator(seed, OCTAVES);
        spaghettiGeneratorOne.setScale(0.005);
        spaghettiGeneratorOne.setYScale(spaghettiGeneratorOne.getYScale() * 4);
        spaghettiGeneratorTwo = new SimplexOctaveGenerator(seed, OCTAVES);
        spaghettiGeneratorTwo.setScale(0.0055);
        spaghettiGeneratorTwo.setYScale(spaghettiGeneratorTwo.getYScale() * 4);

    }
    @Override
    public void populate(World wld, Random rdm, Chunk chunk) {

        long timing = System.nanoTime();

        for (int i = 0; i < 16; i++) {
            int currX = chunk.getX() * 16 + i;
            for (int j = 0; j < 16; j++) {
                int currZ = chunk.getZ() * 16 + j;
                // loop through y to set blocks
                for (int y_coord = 1; y_coord < 255; y_coord++) {
                    int effectualY = y_coord + yOffset;
                    double caveNoiseY = ((double) effectualY) / 3;
                    Block currBlock = chunk.getBlock(i, y_coord, j);
                    if (!currBlock.getType().isSolid()) break;
                    // setup two types of cave noise
                    double cheeseCaveNoise = -1, spaghettiCaveNoiseOne = -1, spaghettiCaveNoiseTwo = -1;
                    // cheese cave noise should be decreased above y=30, and completely gone above y=50
                    if (effectualY > 30) {
                        double caveNoiseOffset = ((double) (effectualY - 30)) / 20;
                        if (caveNoiseOffset < 2) {
                            cheeseCaveNoise = cheeseCaveGenerator.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false) - caveNoiseOffset;
                        }
                        switch (currBlock.getBiome()) {
                            case FOREST:
                            case JUNGLE:
                            case DESERT:
                            case TAIGA_COLD:
                                break;
                            default:
                                // only forest, jungle, desert and tundra get to have caves!
                                spaghettiCaveNoiseOne = spaghettiGeneratorOne.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
                                spaghettiCaveNoiseTwo = spaghettiGeneratorTwo.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
                        }
                    } else {
                        cheeseCaveNoise = cheeseCaveGenerator.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
                        spaghettiCaveNoiseOne = spaghettiGeneratorOne.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
                        spaghettiCaveNoiseTwo = spaghettiGeneratorTwo.noise(currX, caveNoiseY, currZ, 0.5, 0.5, false);
                    }
                    boolean isCave;
                    if (cheeseCaveNoise > 0.75)
                        isCave = true;
                    else isCave = Math.abs(spaghettiCaveNoiseOne) < 0.05 && Math.abs(spaghettiCaveNoiseTwo) < 0.05;
                    if (isCave)
                        currBlock.setType(Material.AIR, false);
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
