package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.PerlinOctaveGenerator;
import terraria.TerrariaHelper;

import java.util.*;

public class OverworldChunkGenerator extends ChunkGenerator {
    static long seed = TerrariaHelper.worldSeed;
    static HashMap<Biome, Integer> biomeHeight, biomeHeightVariance;
    static int BIOME_HEIGHT_DEFAULT, BIOME_HEIGHT_VARIANCE_DEFAULT, OCTAVES;
    static int NEARBY_BIOME_SAMPLE_RADIUS, NEARBY_BIOME_SAMPLE_STEPSIZE;
    static int SEA_LEVEL, LAVA_LEVEL, yOffset = 0;
    static double FREQUENCY;
    static PerlinOctaveGenerator terrainGenerator, cheeseCaveGenerator, spaghettiGeneratorOne, spaghettiGeneratorTwo;
    static long test_rough = 0, test_rough_time = 0,
            test_soil = 0, test_soil_time = 0,
            test_sample = 0, test_sample_time = 0,
            test_biome = 0, test_biome_time = 0;
    static OverworldChunkGenerator instance = new OverworldChunkGenerator();
    private OverworldChunkGenerator() {
        // terrain noise functions
        terrainGenerator = new PerlinOctaveGenerator(seed, OCTAVES);
        terrainGenerator.setScale(0.005);
        cheeseCaveGenerator = new PerlinOctaveGenerator(seed, OCTAVES);
        cheeseCaveGenerator.setScale(0.00175);
        spaghettiGeneratorOne = new PerlinOctaveGenerator(seed, OCTAVES);
        spaghettiGeneratorOne.setScale(0.0025);
        spaghettiGeneratorTwo = new PerlinOctaveGenerator(seed, OCTAVES);
        spaghettiGeneratorTwo.setScale(0.003);

        seed = TerrariaHelper.worldSeed;
        // variables for surface height
        biomeHeight = new HashMap<>();
        biomeHeightVariance = new HashMap<>();
        BIOME_HEIGHT_DEFAULT = 100;
        BIOME_HEIGHT_VARIANCE_DEFAULT = 10;
        OCTAVES = 4;
        FREQUENCY = 0.05221649073;
        NEARBY_BIOME_SAMPLE_RADIUS = 6;
        // if this is NOT 1, THE SLIDING WINDOW TECHNIQUE WILL NOT WORK!
        // NEW: we abandoned the sliding window, feel free to change this
        NEARBY_BIOME_SAMPLE_STEPSIZE = 5;
        SEA_LEVEL = 80;
        LAVA_LEVEL = -150;
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
    public static void tweakBiome(int x, int z, BiomeGrid biome) {
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++) {
                int blockX = x * 16 + i, blockZ = z * 16 + j;
                biome.setBiome(i, j, OverworldBiomeGenerator.getBiome(seed, blockX, blockZ));
            }
    }
    public static OverworldChunkGenerator getInstance() {
        return instance;
    }
    // helper functions
    // chunk block material details
    static void generateTopSoil(ChunkData chunk, int i, int k, int j, int blockX, int blockZ, Biome biome, int yOffset) {
        // although it is named as such, this actually generates stone layers too.
        double topSoilThicknessRandomizer = terrainGenerator.noise(blockX, 0, blockZ, 0.5, 0.5, false);
        double topSoilThickness;
        switch (biome) {
            case OCEAN: // ocean
            case BEACHES: // beach
            case FROZEN_OCEAN: // sulphurous ocean
            case COLD_BEACH: // sulphurous beach
                topSoilThickness = 35 + topSoilThicknessRandomizer * 5;
                for (int y = Math.min(k - yOffset, 254); y > 0; y--) {
                    int effectualY = y + yOffset;
                    if (chunk.getType(i, y, j) == Material.AIR) {
                        if (effectualY <= SEA_LEVEL) chunk.setBlock(i, y, j, Material.WATER);
                    } else if (--topSoilThickness > 0) chunk.setBlock(i, y, j, Material.SAND);
                }
                for (int y = Math.min(SEA_LEVEL - yOffset, 254); y > 0; y--) {
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
                for (int y = Math.min(k - yOffset, 254); y > 0; y--) {
                    if (chunk.getType(i, y, j) == Material.AIR) continue;
                    int effectualY = y + yOffset;
                    if (effectualY > soilLayerHeight) {
                        if (chunk.getType(i, y + 1, j) == Material.AIR)
                            chunk.setBlock(i, y, j, matTopSoil);
                        else if (chunk.getType(i, y - 1, j) == Material.AIR && matSoil.hasGravity())
                            chunk.setBlock(i, y, j, matStone); // prevents the cave from collapsing
                        else
                            chunk.setBlock(i, y, j, matSoil);
                    } else {
                        if (matStone == Material.STONE) break;
                        if (matStone == matSoil) {
                            // in case biome such as jungle underground still needed topsoil
                            if (chunk.getType(i, y + 1, j) == Material.AIR)
                                chunk.setBlock(i, y, j, matTopSoil);
                            else
                                chunk.setBlock(i, y, j, matSoil);
                        } else chunk.setBlock(i, y, j, matStone);
                    }
                }
        }
    }
    // init terrain (rough + detail)
    public static void initializeTerrain(ChunkData chunk, int blockXStart, int blockZStart, BiomeGrid biome, int yOffset) {
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
                Biome currBiome = OverworldBiomeGenerator.getBiome(seed, currSampleX, currSampleZ);
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
                height = heightOffset_tmp + (squashFactor_tmp * terrainGenerator.noise(currX, 0, currZ, 0.5, 0.5, false));

                // loop through y to set blocks
                for (int y_coord = 0; y_coord < 256; y_coord ++) {
                    int effectualY = y_coord + yOffset;
                    if (y_coord == 0)
                        chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (y_coord == 255 && yOffset < 0)
                        chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (effectualY < height) {
                        // setup two types of cave noise
                        double cheeseCaveNoise = -1, spaghettiCaveNoiseOne = -1, spaghettiCaveNoiseTwo = -1;
                        // cheese cave noise should be decreased above y=30, and completely gone above y=50
                        if (effectualY > 30) {
                            double caveNoiseOffset = ((double) (effectualY - 30)) / 20;
                            if (caveNoiseOffset < 2) {
                                cheeseCaveNoise = cheeseCaveGenerator.noise(currX, effectualY, currZ, 0.5, 0.5, false) - caveNoiseOffset;
                                switch (biome.getBiome(i, j)) {
                                    case FOREST:
                                    case JUNGLE:
                                    case DESERT:
                                    case TAIGA_COLD:
                                        break;
                                    default:
                                        // only forest, jungle, desert and tundra get to have caves!
                                        spaghettiCaveNoiseOne = spaghettiGeneratorOne.noise(currX, effectualY, currZ, 0.5, 0.5, false) - caveNoiseOffset;
                                        spaghettiCaveNoiseTwo = spaghettiGeneratorTwo.noise(currX, effectualY, currZ, 0.5, 0.5, false) - caveNoiseOffset;
                                }
                            }
                        } else {
                            cheeseCaveNoise = cheeseCaveGenerator.noise(currX, effectualY, currZ, 0.5, 0.5, false);
                            spaghettiCaveNoiseOne = spaghettiGeneratorOne.noise(currX, effectualY, currZ, 0.5, 0.5, false);
                            spaghettiCaveNoiseTwo = spaghettiGeneratorTwo.noise(currX, effectualY, currZ, 0.5, 0.5, false);
                        }
                        boolean isCave;
                        if (cheeseCaveNoise > 0.95)
                            isCave = true;
                        else isCave = Math.abs(spaghettiCaveNoiseOne) < 0.075 && Math.abs(spaghettiCaveNoiseTwo) < 0.075;
                        if (isCave)
                            chunk.setBlock(i, y_coord, j, Material.AIR);
                        else
                            chunk.setBlock(i, y_coord, j, Material.STONE);
                    } else
                        chunk.setBlock(i, y_coord, j, Material.AIR);
                }

                generateTopSoil(chunk, i, (int) Math.ceil(height), j, currX, currZ, biome.getBiome(i, j), yOffset);

                // then, we tweak the offset info as z increases.
                if (j + 1 < 16) {
                    double heightOffsetTweak = 0, squashFactorTweak = 0;
                    int currSample_dropZ = currZ - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    int currSample_addZ  = currZ + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                    for (int sampleOffset = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffset <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffset++) {
                        int currSampleX = currX + sampleOffset * NEARBY_BIOME_SAMPLE_STEPSIZE;
                        Biome currBiome_drop = OverworldBiomeGenerator.getBiome(seed, currSampleX, currSample_dropZ);
                        heightOffsetTweak -= biomeHeight.getOrDefault(           currBiome_drop, BIOME_HEIGHT_DEFAULT);
                        squashFactorTweak -= biomeHeightVariance.getOrDefault(   currBiome_drop, BIOME_HEIGHT_VARIANCE_DEFAULT);
                        Biome currBiome_add = OverworldBiomeGenerator.getBiome(seed, currSampleX, currSample_addZ);
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
                    Biome currBiome_drop = OverworldBiomeGenerator.getBiome(seed, currSample_dropX, currSampleZ);
                    heightOffsetTweak -= biomeHeight.getOrDefault(           currBiome_drop, BIOME_HEIGHT_DEFAULT);
                    squashFactorTweak -= biomeHeightVariance.getOrDefault(   currBiome_drop, BIOME_HEIGHT_VARIANCE_DEFAULT);
                    Biome currBiome_add = OverworldBiomeGenerator.getBiome(seed, currSample_addX, currSampleZ);
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
    public static void initializeTerrain_timingTest(ChunkData chunk, int blockXStart, int blockZStart, BiomeGrid biome, int yOffset) {

        long timing;

        // this function creates the raw terrain of the chunk, consisting of only air, water or stone.
        double heightOffset, squashFactor, height;
        double biomesSampled = (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2) * (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2);
        int currX, currZ;



        // loop through all blocks.
        for (int i = 0; i < 16; i ++) {
            currX = blockXStart + i;
            for (int j = 0; j < 16; j++) {
                currZ = blockZStart + j;

                timing = System.nanoTime();

                // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                heightOffset = 0;
                squashFactor = 0;
                for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
                    int currSampleX = currX + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ++) {
                        int currSampleZ = currZ + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                        Biome currBiome = OverworldBiomeGenerator.getBiome(seed, currSampleX, currSampleZ);
                        heightOffset += biomeHeight.getOrDefault(currBiome, BIOME_HEIGHT_DEFAULT);
                        squashFactor += biomeHeightVariance.getOrDefault(currBiome, BIOME_HEIGHT_VARIANCE_DEFAULT);
                    }
                }
                heightOffset /= biomesSampled;
                squashFactor /= biomesSampled;

                test_sample += (System.nanoTime() - timing);
                test_sample_time ++;
                if (test_sample_time % 2560 == 0)
                    // this was 12097780
                    // now 1671719, (~1.7 ms, good enough)
                    Bukkit.broadcastMessage("Time elapsed for sampling nearby biome: " + test_sample * 256 / test_sample_time);
                timing = System.nanoTime();

                height = heightOffset + (squashFactor * terrainGenerator.noise(currX, 0, currZ, 0.5, 0.5, false));

                // loop through y to set blocks
                for (int y_coord = 0; y_coord < 256; y_coord ++) {
                    int effectualY = y_coord + yOffset;
                    if (y_coord == 0)
                        chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (y_coord == 255 && yOffset < 0)
                        chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (effectualY < height) {
                        // setup two types of cave noise
                        double cheeseCaveNoise = -1, spaghettiCaveNoiseOne = -1, spaghettiCaveNoiseTwo = -1;
                        // cheese cave noise should be decreased above y=30, and completely gone above y=50
                        if (effectualY > 30) {
                            double caveNoiseOffset = ((double) (effectualY - 30)) / 20;
                            if (caveNoiseOffset < 2) {
                                cheeseCaveNoise = cheeseCaveGenerator.noise(currX, effectualY, currZ, 0.5, 0.5, false) - caveNoiseOffset;
                                switch (biome.getBiome(i, j)) {
                                    case FOREST:
                                    case JUNGLE:
                                    case DESERT:
                                    case TAIGA_COLD:
                                        break;
                                    default:
                                        // only forest, jungle, desert and tundra get to have caves!
                                        spaghettiCaveNoiseOne = spaghettiGeneratorOne.noise(currX, effectualY, currZ, 0.5, 0.5, false) - caveNoiseOffset;
                                        spaghettiCaveNoiseTwo = spaghettiGeneratorTwo.noise(currX, effectualY, currZ, 0.5, 0.5, false) - caveNoiseOffset;
                                }
                            }
                        } else {
                            cheeseCaveNoise = cheeseCaveGenerator.noise(currX, effectualY, currZ, 0.5, 0.5, false);
                            spaghettiCaveNoiseOne = spaghettiGeneratorOne.noise(currX, effectualY, currZ, 0.5, 0.5, false);
                            spaghettiCaveNoiseTwo = spaghettiGeneratorTwo.noise(currX, effectualY, currZ, 0.5, 0.5, false);
                        }
                        boolean isCave;
                        if (cheeseCaveNoise > 0.95)
                            isCave = true;
                        else isCave = Math.abs(spaghettiCaveNoiseOne) < 0.075 && Math.abs(spaghettiCaveNoiseTwo) < 0.075;
                        if (isCave)
                            chunk.setBlock(i, y_coord, j, Material.AIR);
                        else
                            chunk.setBlock(i, y_coord, j, Material.STONE);
                    } else
                        chunk.setBlock(i, y_coord, j, Material.AIR);
                }

                test_rough += (System.nanoTime() - timing);
                test_rough_time ++;
                if (test_rough_time % 2560 == 0)
                    // this was around 32310417
                    // now 23811911, ~23.8 ms after changing to perlin octave
                    Bukkit.broadcastMessage("Time elapsed for setup rough blocks: " + test_rough * 256 / test_rough_time);
                timing = System.nanoTime();

                generateTopSoil(chunk, i, (int) Math.ceil(height), j, currX, currZ, biome.getBiome(i, j), yOffset);

                test_soil += (System.nanoTime() - timing);
                test_soil_time ++;
                if (test_soil_time % 2560 == 0)
                    Bukkit.broadcastMessage("Time elapsed for setup top soil: " + test_soil * 256 / test_soil_time);
                timing = System.nanoTime();
            }
        }
    }
    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        // setup biome
//        long timing = System.nanoTime();

        tweakBiome(x, z, biome);

//        test_biome += (System.nanoTime() - timing);
//        test_biome_time ++;
//        if (test_biome_time % 10 == 0)
//            Bukkit.broadcastMessage("Time elapsed for setup biome: " + test_biome / test_biome_time);
        // init terrain
        ChunkData chunk = createChunkData(world);
//        initializeTerrain_timingTest(chunk, x * 16, z * 16, biome, yOffset);
        initializeTerrain(chunk, x * 16, z * 16, biome, yOffset);
        // tweak terrain
        return chunk;
    }
    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        ArrayList<BlockPopulator> result = new ArrayList<>();
        result.add(new OverworldBlockGenericPopulator());
        result.add(new OrePopulator(yOffset));
        return result;
    }
}