package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.HashMap;
import java.util.Random;

public class OverworldChunkGenerator extends ChunkGenerator {
    HashMap<Biome, Integer> biomeHeight, biomeHeightVariance;
    int BIOME_HEIGHT_DEFAULT, BIOME_HEIGHT_VARIANCE_DEFAULT, OCTAVES;
    int NEARBY_BIOME_SAMPLE_RADIUS, NEARBY_BIOME_SAMPLE_STEPSIZE;
    int OCEAN_HEIGHT;
    double FREQUENCY;
    OverworldBiomeGenerator biomeGen;
    public OverworldChunkGenerator() {
        biomeGen = new OverworldBiomeGenerator();
        // variables for surface height
        biomeHeight = new HashMap<>();
        biomeHeightVariance = new HashMap<>();
        BIOME_HEIGHT_DEFAULT = 100;
        BIOME_HEIGHT_VARIANCE_DEFAULT = 10;
        OCTAVES = 8;
        FREQUENCY = 0.05221649073;
        NEARBY_BIOME_SAMPLE_RADIUS = 35;
        NEARBY_BIOME_SAMPLE_STEPSIZE = 1;
        OCEAN_HEIGHT = 85;
        // setup height/variance for biomes
        // ocean
        biomeHeight.put(        Biome.OCEAN,75);
        biomeHeightVariance.put(Biome.OCEAN,3);
        // sulphurous ocean
        biomeHeight.put(        Biome.FROZEN_OCEAN,0);
        biomeHeightVariance.put(Biome.FROZEN_OCEAN,0);
        // astral infection
        biomeHeightVariance.put(Biome.MESA,20);
        // corruption
        biomeHeightVariance.put(Biome.MUSHROOM_ISLAND,20);
    }
    public void tweakBiome(World world, int x, int z, BiomeGrid biome) {
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++) {
                int blockX = x * 16 + i, blockZ = z * 16 + j;
                biome.setBiome(i, j, biomeGen.getBiome(world, blockX, blockZ));
            }
    }
    // helper functions
    private ChunkData initializeTerrain(World world, SimplexOctaveGenerator octaveGen, int blockXStart, int blockZStart) {
        // this function creates the raw terrain of the chunk, consisting of only air, water or stone.
        ChunkData chunk = createChunkData(world);
        double heightOffset, squashFactor, height;
        double biomesSampled = (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2) * (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2);
        int currX, currZ;


        // loop through all blocks.
        for (int i = 0; i < 16; i ++) {
            currZ = blockZStart + i;
            for (int j = 0; j < 16; j++) {
                currX = blockXStart + j;
                // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                heightOffset = 0;
                squashFactor = 0;
                for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
                    int currSampleX = currX + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ ++) {
                        int currSampleZ = currZ + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                        Biome currBiome = biomeGen.getBiome(world, currSampleX, currSampleZ);
                        heightOffset += biomeHeight.getOrDefault(           currBiome, BIOME_HEIGHT_DEFAULT);
                        squashFactor += biomeHeightVariance.getOrDefault(   currBiome, BIOME_HEIGHT_VARIANCE_DEFAULT);
                    }
                }
                heightOffset /= biomesSampled;
                squashFactor /= biomesSampled;

                height = heightOffset + (squashFactor * octaveGen.noise(currX, currZ, FREQUENCY, squashFactor, true));

                // loop through y to set blocks
                for (int y_coord = 0; y_coord < 256; y_coord ++) {
                    if (y_coord == 0)                   chunk.setBlock(j, y_coord, i, Material.BEDROCK);
                    else if (y_coord < height)          chunk.setBlock(j, y_coord, i, Material.STONE);
                    else if (y_coord < OCEAN_HEIGHT)    chunk.setBlock(j, y_coord, i, Material.WATER);
                    else                                chunk.setBlock(j, y_coord, i, Material.AIR);
                }
            }
        }
        return chunk;
    }








    private ChunkData initializeTerrain(World world, SimplexOctaveGenerator octaveGen, int blockXStart, int blockZStart, boolean dummy) {
        // this function creates the raw terrain of the chunk, consisting of only air, water or stone.
        ChunkData chunk = createChunkData(world);
        double heightOffset, squashFactor, height;
        double biomesSampled = (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2) * (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2);
        int currX = 0, currZ;

        // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
        heightOffset = 0;
        squashFactor = 0;
        for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
            int currSampleX = blockXStart + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;
            for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ ++) {
                int currSampleZ = blockZStart + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                Biome currBiome = biomeGen.getBiome(world, currSampleX, currSampleZ);
                heightOffset += biomeHeight.getOrDefault(           currBiome, BIOME_HEIGHT_DEFAULT);
                squashFactor += biomeHeightVariance.getOrDefault(   currBiome, BIOME_HEIGHT_VARIANCE_DEFAULT);
            }
        }
        heightOffset /= biomesSampled;
        squashFactor /= biomesSampled;

        // loop through all blocks.
        double heightOffset_tmp, squashFactor_tmp;
        for (int i = 0; i < 16; i ++) {
            currZ = blockZStart + i;
            // these will be modified as we loop through the x coordinates.
            heightOffset_tmp = heightOffset;
            squashFactor_tmp = squashFactor;
            for (int j = 0; j < 16; j++) {
                currX = blockXStart + j;
                height = heightOffset_tmp + (squashFactor_tmp * octaveGen.noise(currX, currZ, FREQUENCY, squashFactor_tmp, true));


                double test_heightOffset = 0;
                for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
                    int currSampleX = currX + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ ++) {
                        int currSampleZ = currZ + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                        Biome currBiome = biomeGen.getBiome(world, currSampleX, currSampleZ);
                        test_heightOffset += biomeHeight.getOrDefault(           currBiome, BIOME_HEIGHT_DEFAULT);
                    }
                }
                test_heightOffset /= biomesSampled;
                if (Math.abs(test_heightOffset - heightOffset_tmp) > 0.00001) {
                    Bukkit.getLogger().info("INCOSISTANCY: " + i + ", " + j + "(got " + heightOffset_tmp + " instead of " + test_heightOffset);
                }

                // loop through y to set blocks
                for (int y_coord = 0; y_coord < 256; y_coord ++) {
//                    if (y_coord == 0)                   chunk.setBlock(j, y_coord, i, Material.BEDROCK);
//                    else if (y_coord < height)          chunk.setBlock(j, y_coord, i, Material.STONE);
//                    else if (y_coord < OCEAN_HEIGHT)    chunk.setBlock(j, y_coord, i, Material.WATER);
//                    else                                chunk.setBlock(j, y_coord, i, Material.AIR);
                    if (y_coord == 0)                                           chunk.setBlock(j, y_coord, i, Material.BEDROCK);
                    else if (y_coord < heightOffset_tmp)                        chunk.setBlock(j, y_coord, i, Material.STONE);
                    else if (y_coord < heightOffset_tmp + squashFactor_tmp)     chunk.setBlock(j, y_coord, i, Material.GLASS);
                    else                                                        chunk.setBlock(j, y_coord, i, Material.AIR);
                }

                // then, we tweak the offset info as x increases.
                if (j + 1 < 16) {
                    double heightOffsetTweak = 0, squashFactorTweak = 0;
                    int currSample_dropX = currX - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    int currSample_addX  = currX + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                    for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ++) {
                        int currSampleZ = currZ + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                        Bukkit.getLogger().info("X operation: " + currX + "," + currZ + "::: - " + currSample_dropX + ", " + currSampleZ + " + " + currSample_addX + ", " + currSampleZ);
                        Biome currBiome_drop = biomeGen.getBiome(world, currSample_dropX, currSampleZ);
                        heightOffsetTweak -= biomeHeight.getOrDefault(           currBiome_drop, BIOME_HEIGHT_DEFAULT);
                        squashFactorTweak -= biomeHeightVariance.getOrDefault(   currBiome_drop, BIOME_HEIGHT_VARIANCE_DEFAULT);
                        Biome currBiome_add = biomeGen.getBiome(world, currSample_addX, currSampleZ);
                        heightOffsetTweak += biomeHeight.getOrDefault(           currBiome_add, BIOME_HEIGHT_DEFAULT);
                        squashFactorTweak += biomeHeightVariance.getOrDefault(   currBiome_add, BIOME_HEIGHT_VARIANCE_DEFAULT);
                    }
                    heightOffsetTweak /= biomesSampled;
                    squashFactorTweak /= biomesSampled;
                    heightOffset_tmp += heightOffsetTweak;
                    squashFactor_tmp += squashFactorTweak;
                }
            }

            // then, we tweak the offset info as z increases.
            if (i + 1 < 16) {
                double heightOffsetTweak = 0, squashFactorTweak = 0;
                int currSample_dropZ = currZ - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                int currSample_addZ  = currZ + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
                    int currSampleX = blockXStart + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;

                    Bukkit.getLogger().info("Z operation: " + currX + "," + currZ + "::: - " + currSampleX + ", " + currSample_dropZ + " + " + currSampleX + ", " + currSample_addZ);

                    Biome currBiome_drop = biomeGen.getBiome(world, currSampleX, currSample_dropZ);
                    squashFactorTweak -= biomeHeight.getOrDefault(           currBiome_drop, BIOME_HEIGHT_DEFAULT);
                    squashFactorTweak -= biomeHeightVariance.getOrDefault(   currBiome_drop, BIOME_HEIGHT_VARIANCE_DEFAULT);
                    Biome currBiome_add = biomeGen.getBiome(world, currSampleX, currSample_addZ);
                    squashFactorTweak += biomeHeight.getOrDefault(           currBiome_add, BIOME_HEIGHT_DEFAULT);
                    squashFactorTweak += biomeHeightVariance.getOrDefault(   currBiome_add, BIOME_HEIGHT_VARIANCE_DEFAULT);
                }
                heightOffsetTweak /= biomesSampled;
                squashFactorTweak /= biomesSampled;
                heightOffset += heightOffsetTweak;
                squashFactor += squashFactorTweak;
            }
        }
        return chunk;
    }












    private ChunkData initializeTerrain(World world, SimplexOctaveGenerator octaveGen, int blockXStart, int blockZStart, boolean dummy, boolean backup) {
        // this function creates the raw terrain of the chunk, consisting of only air, water or stone.
        ChunkData chunk = createChunkData(world);
        double heightOffset, squashFactor, height;
        double biomesSampled = (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2) * (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2);
        int currX = 0, currZ = 0;

        // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
        heightOffset = 0;
        squashFactor = 0;
        for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
            int currSampleX = blockXStart + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;
            for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ ++) {
                int currSampleZ = blockZStart + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                Biome currBiome = biomeGen.getBiome(world, currSampleX, currSampleZ);
                heightOffset += biomeHeight.getOrDefault(           currBiome, BIOME_HEIGHT_DEFAULT);
                squashFactor += biomeHeightVariance.getOrDefault(   currBiome, BIOME_HEIGHT_VARIANCE_DEFAULT);
            }
        }
        heightOffset /= biomesSampled;
        squashFactor /= biomesSampled;

        // loop through all blocks.
        double heightOffset_tmp, squashFactor_tmp;
        for (int i = 0; i < 16; i ++) {
            currZ = blockZStart + i;
            // these will be modified as we loop through the x coordinates.
            heightOffset_tmp = heightOffset;
            squashFactor_tmp = squashFactor;
            for (int j = 0; j < 16; j++) {
                currX = blockXStart + j;
                height = heightOffset_tmp + (squashFactor_tmp * octaveGen.noise(currX, currZ, FREQUENCY, squashFactor_tmp, true));

                // loop through y to set blocks
                for (int y_coord = 0; y_coord < 256; y_coord ++) {
                    if (y_coord == 0)                   chunk.setBlock(j, y_coord, i, Material.BEDROCK);
                    else if (y_coord < height)          chunk.setBlock(j, y_coord, i, Material.STONE);
                    else if (y_coord < OCEAN_HEIGHT)    chunk.setBlock(j, y_coord, i, Material.WATER);
                    else                                chunk.setBlock(j, y_coord, i, Material.AIR);
                }

                // then, we tweak the offset info as x increases.
                if (j + 1 < 16) {
                    double heightOffsetTweak = 0, squashFactorTweak = 0;
                    int currSample_dropX = currX - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    int currSample_addX  = currX + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                    for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ++) {
                        int currSampleZ = currZ + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                        Biome currBiome_drop = biomeGen.getBiome(world, currSample_dropX, currSampleZ);
                        heightOffsetTweak -= biomeHeight.getOrDefault(           currBiome_drop, BIOME_HEIGHT_DEFAULT);
                        squashFactorTweak -= biomeHeightVariance.getOrDefault(   currBiome_drop, BIOME_HEIGHT_VARIANCE_DEFAULT);
                        Biome currBiome_add = biomeGen.getBiome(world, currSample_addX, currSampleZ);
                        heightOffsetTweak += biomeHeight.getOrDefault(           currBiome_add, BIOME_HEIGHT_DEFAULT);
                        squashFactorTweak += biomeHeightVariance.getOrDefault(   currBiome_add, BIOME_HEIGHT_VARIANCE_DEFAULT);
                    }
                    heightOffsetTweak /= biomesSampled;
                    squashFactorTweak /= biomesSampled;
                    heightOffset_tmp += heightOffsetTweak;
                    squashFactor_tmp += squashFactorTweak;
                }
            }

            // then, we tweak the offset info as z increases.
            if (i + 1 < 16) {
                double heightOffsetTweak = 0, squashFactorTweak = 0;
                int currSample_dropZ = currZ - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                int currSample_addZ  = currZ + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
                    int currSampleX = currX + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    Biome currBiome_drop = biomeGen.getBiome(world, currSampleX, currSample_dropZ);
                    squashFactorTweak -= biomeHeight.getOrDefault(           currBiome_drop, BIOME_HEIGHT_DEFAULT);
                    squashFactorTweak -= biomeHeightVariance.getOrDefault(   currBiome_drop, BIOME_HEIGHT_VARIANCE_DEFAULT);
                    Biome currBiome_add = biomeGen.getBiome(world, currSampleX, currSample_addZ);
                    squashFactorTweak += biomeHeight.getOrDefault(           currBiome_add, BIOME_HEIGHT_DEFAULT);
                    squashFactorTweak += biomeHeightVariance.getOrDefault(   currBiome_add, BIOME_HEIGHT_VARIANCE_DEFAULT);
                }
                heightOffsetTweak /= biomesSampled;
                squashFactorTweak /= biomesSampled;
                heightOffset += heightOffsetTweak;
                squashFactor += squashFactorTweak;
            }
        }
        return chunk;
    }
    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        // setup biome
        tweakBiome(world, x, z, biome);
        // init terrain
        SimplexOctaveGenerator octaveGen = new SimplexOctaveGenerator(world, OCTAVES);
//        ChunkData chunk = initializeTerrain(world, octaveGen, x * 16, z * 16);
        ChunkData chunk = initializeTerrain(world, octaveGen, x * 16, z * 16, false);
        // tweak terrain
        return chunk;
    }
}
