package terraria.worldgen.overworld;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.HashMap;
import java.util.Random;

public class OverworldChunkGenerator extends ChunkGenerator {
    static long seed = 0;
    HashMap<Biome, Integer> biomeHeight, biomeHeightVariance;
    int BIOME_HEIGHT_DEFAULT, BIOME_HEIGHT_VARIANCE_DEFAULT, OCTAVES;
    int NEARBY_BIOME_SAMPLE_RADIUS, NEARBY_BIOME_SAMPLE_STEPSIZE;
    int SEA_LEVEL;
    double FREQUENCY;
    OverworldBiomeGenerator biomeGen;
    SimplexOctaveGenerator terrainGenerator;
    public OverworldChunkGenerator() {
        biomeGen = new OverworldBiomeGenerator();
        // variables for surface height
        biomeHeight = new HashMap<>();
        biomeHeightVariance = new HashMap<>();
        BIOME_HEIGHT_DEFAULT = 100;
        BIOME_HEIGHT_VARIANCE_DEFAULT = 10;
        OCTAVES = 4;
        FREQUENCY = 0.05221649073;
        NEARBY_BIOME_SAMPLE_RADIUS = 25;
        NEARBY_BIOME_SAMPLE_STEPSIZE = 1; // if this is NOT 1, THE SLIDING WINDOW TECHNIQUE WILL NOT WORK!
        SEA_LEVEL = 80;
        // setup height/variance for biomes
        // ocean
        biomeHeight.put(        Biome.OCEAN,60);
        biomeHeightVariance.put(Biome.OCEAN,3);
        // sulphurous ocean
        biomeHeight.put(        Biome.FROZEN_OCEAN,0);
        biomeHeightVariance.put(Biome.FROZEN_OCEAN,0);
        // astral infection
        biomeHeightVariance.put(Biome.MESA,20);
        // corruption
        biomeHeightVariance.put(Biome.MUSHROOM_ISLAND,20);
    }
    public void tweakBiome(int x, int z, BiomeGrid biome) {
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++) {
                int blockX = x * 16 + i, blockZ = z * 16 + j;
                biome.setBiome(i, j, biomeGen.getBiome(seed, blockX, blockZ));
            }
    }
    // helper functions
    // chunk block material details
    void generateTopSoil(ChunkData chunk, int i, int k, int j, double blockX, double blockZ, Biome biome) {
        double topSoilThicknessRandomizer = terrainGenerator.noise(blockX, blockZ, 2, 0.5, false);
        double topSoilThickness;
        switch (biome) {
            case OCEAN: // ocean
            case BEACHES: // beach
            case FROZEN_OCEAN: // sulphurous ocean
            case COLD_BEACH: // sulphurous beach
                topSoilThickness = 35 + topSoilThicknessRandomizer * 5;
                for (int y = k; y > 0; y--) {
                    if (chunk.getType(i, y, j) == Material.AIR) {
                        if (y <= SEA_LEVEL) chunk.setBlock(i, y, j, Material.WATER);
                    } else if (--topSoilThickness > 0) chunk.setBlock(i, y, j, Material.SAND);
                }
                for (int y = SEA_LEVEL; y > 0; y--) {
                    if (chunk.getType(i, y, j) == Material.AIR) {
                        chunk.setBlock(i, y, j, Material.WATER);
                    } else break;
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
                    case JUNGLE: // astral infection
                        matTopSoil = Material.GRASS;
                        matSoil = Material.DIRT;
                        matStone = Material.DIRT;
                        break;
                    default: // forest
                        matTopSoil = Material.GRASS;
                        matSoil = Material.DIRT;
                        matStone = Material.STONE;
                }
                double soilLayerHeight = 50 + topSoilThicknessRandomizer * 10;
                // setup soil/stone layers
                for (int y = k; y > 0; y--) {
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
    // init terrain (rough + detail)
    private void initializeTerrain(ChunkData chunk, SimplexOctaveGenerator octaveGen, int blockXStart, int blockZStart, BiomeGrid biome) {
        // this function creates the raw terrain of the chunk, consisting of only air, water or stone.
        double heightOffset, squashFactor, height;
        double biomesSampled = (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2) * (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2);
        int currX, currZ;

        // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
        heightOffset = 0;
        squashFactor = 0;
        for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
            int currSampleX = blockXStart + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;
            for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ ++) {
                int currSampleZ = blockZStart + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                Biome currBiome = biomeGen.getBiome(seed, currSampleX, currSampleZ);
                heightOffset += biomeHeight.getOrDefault(           currBiome, BIOME_HEIGHT_DEFAULT);
                squashFactor += biomeHeightVariance.getOrDefault(   currBiome, BIOME_HEIGHT_VARIANCE_DEFAULT);
            }
        }
        heightOffset /= biomesSampled;
        squashFactor /= biomesSampled;

        // loop through all blocks.
        double heightOffset_tmp, squashFactor_tmp;
        for (int i = 0; i < 16; i ++) {
            currX = blockXStart + i;
            // these will be modified as we loop through the x coordinates.
            heightOffset_tmp = heightOffset;
            squashFactor_tmp = squashFactor;
            for (int j = 0; j < 16; j++) {
                currZ = blockZStart + j;
                height = heightOffset_tmp + (squashFactor_tmp * octaveGen.noise(currX, currZ, 2, 0.5, false));

                // loop through y to set blocks
                for (int y_coord = 0; y_coord < 256; y_coord ++) {
                    // TODO: tweak the cave noise so that it looks more natural and does not interfere with the ground
                    // setup two types of cave noise
                    double cheeseCaveNoise = terrainGenerator.noise(currX * 1.2, y_coord * 1.2, currZ * 1.2, 2, 0.5, false),
                            spaghettiCaveNoise = terrainGenerator.noise(currX * 1.1, y_coord * 1.1, currZ * 1.1, 2, 0.5, false);
                    // cheese cave noise should be decreased above y=30, and completely gone above y=50
                    if (y_coord > 30) cheeseCaveNoise -= ((double) (y_coord - 30)) / 20;
                    if (y_coord == 0) chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (y_coord < height) {
                        if (cheeseCaveNoise > 0.3 || Math.abs(spaghettiCaveNoise) < 0.1)
                            chunk.setBlock(i, y_coord, j, Material.AIR);
                        else
                            chunk.setBlock(i, y_coord, j, Material.STONE);
                    } else {
                        chunk.setBlock(i, y_coord, j, Material.AIR);
                    }
                }
                generateTopSoil(chunk, i, (int) Math.ceil(height), j, currX, currZ, biome.getBiome(i, j));

                // then, we tweak the offset info as z increases.
                if (j + 1 < 16) {
                    double heightOffsetTweak = 0, squashFactorTweak = 0;
                    int currSample_dropZ = currZ - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    int currSample_addZ  = currZ + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                    for (int sampleOffset = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffset <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffset++) {
                        int currSampleX = currX + sampleOffset * NEARBY_BIOME_SAMPLE_STEPSIZE;
                        Biome currBiome_drop = biomeGen.getBiome(seed, currSampleX, currSample_dropZ);
                        heightOffsetTweak -= biomeHeight.getOrDefault(           currBiome_drop, BIOME_HEIGHT_DEFAULT);
                        squashFactorTweak -= biomeHeightVariance.getOrDefault(   currBiome_drop, BIOME_HEIGHT_VARIANCE_DEFAULT);
                        Biome currBiome_add = biomeGen.getBiome(seed, currSampleX, currSample_addZ);
                        heightOffsetTweak += biomeHeight.getOrDefault(           currBiome_add, BIOME_HEIGHT_DEFAULT);
                        squashFactorTweak += biomeHeightVariance.getOrDefault(   currBiome_add, BIOME_HEIGHT_VARIANCE_DEFAULT);
                    }
                    heightOffsetTweak /= biomesSampled;
                    squashFactorTweak /= biomesSampled;
                    heightOffset_tmp += heightOffsetTweak;
                    squashFactor_tmp += squashFactorTweak;
                }
            }

            // then, we tweak the offset info as x increases.
            if (i + 1 < 16) {
                double heightOffsetTweak = 0, squashFactorTweak = 0;
                int currSample_dropX = currX - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                int currSample_addX  = currX + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                for (int sampleOffset = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffset <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffset++) {
                    int currSampleZ = blockZStart + sampleOffset * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    Biome currBiome_drop = biomeGen.getBiome(seed, currSample_dropX, currSampleZ);
                    heightOffsetTweak -= biomeHeight.getOrDefault(           currBiome_drop, BIOME_HEIGHT_DEFAULT);
                    squashFactorTweak -= biomeHeightVariance.getOrDefault(   currBiome_drop, BIOME_HEIGHT_VARIANCE_DEFAULT);
                    Biome currBiome_add = biomeGen.getBiome(seed, currSample_addX, currSampleZ);
                    heightOffsetTweak += biomeHeight.getOrDefault(           currBiome_add, BIOME_HEIGHT_DEFAULT);
                    squashFactorTweak += biomeHeightVariance.getOrDefault(   currBiome_add, BIOME_HEIGHT_VARIANCE_DEFAULT);
                }
                heightOffsetTweak /= biomesSampled;
                squashFactorTweak /= biomesSampled;
                heightOffset += heightOffsetTweak;
                squashFactor += squashFactorTweak;
            }
        }
    }
    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        if (seed == 0) seed = world.getSeed();
        // setup biome
        tweakBiome(x, z, biome);
        // init terrain
        if (terrainGenerator == null) {
            terrainGenerator = new SimplexOctaveGenerator(seed, OCTAVES);
            terrainGenerator.setScale(0.005);
        }
        ChunkData chunk = createChunkData(world);
        initializeTerrain(chunk, terrainGenerator, x * 16, z * 16, biome);
        // tweak terrain
        return chunk;
    }
}