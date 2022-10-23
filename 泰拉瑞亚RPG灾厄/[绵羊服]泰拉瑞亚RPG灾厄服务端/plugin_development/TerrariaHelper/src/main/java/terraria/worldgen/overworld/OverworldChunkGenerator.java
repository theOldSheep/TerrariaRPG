package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.PerlinOctaveGenerator;
import terraria.TerrariaHelper;
import terraria.util.MathHelper;
import terraria.worldgen.Interpolate;
import terraria.worldgen.Interpolate.InterpolatePoint;

import java.util.*;

public class OverworldChunkGenerator extends ChunkGenerator {
    static long seed = TerrariaHelper.worldSeed;
    public static int OCTAVES = 4,
            NEARBY_BIOME_SAMPLE_RADIUS, NEARBY_BIOME_SAMPLE_STEPSIZE,
            LAND_HEIGHT, RIVER_DEPTH, PLATEAU_HEIGHT, SEA_LEVEL, LAVA_LEVEL;
    static int yOffset_overworld = 0;
    static double FREQUENCY;
    public static PerlinOctaveGenerator terrainGenerator, terrainGeneratorTwo, terrainDetailGenerator,
                                        stoneVeinGenerator;
    public static Interpolate astralInfectionHeightProvider, oceanHeightProvider, genericHeightProvider;
    static long test_rough = 0, test_rough_time = 0,
            test_soil = 0, test_soil_time = 0,
            test_sample = 0, test_sample_time = 0,
            test_biome = 0, test_biome_time = 0;
    static OverworldChunkGenerator instance = new OverworldChunkGenerator();
    static List<BlockPopulator> populators;
    private OverworldChunkGenerator() {
        super();
        // send noise info
        Bukkit.getScheduler().scheduleSyncRepeatingTask(TerrariaHelper.getInstance(), () -> {
            Player laomianyang = Bukkit.getPlayer("laomianyang");
            if (laomianyang != null) {
                double noise = terrainGenerator.noise(laomianyang.getLocation().getX(), laomianyang.getLocation().getZ(), 0.5, 0.5, false);
                double noiseTwo = terrainGeneratorTwo.noise(laomianyang.getLocation().getX(), laomianyang.getLocation().getZ(), 0.5, 0.5, true);
                noiseTwo = noiseTwo * 0.75 + 0.5;
                laomianyang.sendMessage("noise: " + noise + ", " + noiseTwo);
            }
        }, 1, 5);
        // terrain noise functions
        Random rdm = new Random(seed);
        terrainGenerator = new PerlinOctaveGenerator(rdm.nextLong(), OCTAVES);
        terrainGenerator.setScale(0.0005);
        terrainGeneratorTwo = new PerlinOctaveGenerator(rdm.nextLong(), OCTAVES);
        terrainGeneratorTwo.setScale(0.0075);
        terrainDetailGenerator = new PerlinOctaveGenerator(rdm.nextLong(), OCTAVES);
        terrainDetailGenerator.setScale(0.025);
        stoneVeinGenerator = new PerlinOctaveGenerator(rdm.nextLong(), 1);
        stoneVeinGenerator.setScale(0.05);
        // constants
        seed = TerrariaHelper.worldSeed;
        FREQUENCY = 0.05221649073;
        NEARBY_BIOME_SAMPLE_RADIUS = 25;
        NEARBY_BIOME_SAMPLE_STEPSIZE = 1;
        SEA_LEVEL = 90;
        RIVER_DEPTH = 15;
        PLATEAU_HEIGHT = 50;
        LAND_HEIGHT = 100;
        LAVA_LEVEL = -150;
        // interpolates
        astralInfectionHeightProvider = new Interpolate(new InterpolatePoint[]{
                InterpolatePoint.create(-0.5 , LAND_HEIGHT + PLATEAU_HEIGHT),
                InterpolatePoint.create(-0.45, LAND_HEIGHT),
                InterpolatePoint.create( 0.45, LAND_HEIGHT),
                InterpolatePoint.create( 0.5 , LAND_HEIGHT + PLATEAU_HEIGHT)
        }, "astral_infection_heightmap");
        oceanHeightProvider = new Interpolate(new InterpolatePoint[]{
                InterpolatePoint.create(-0.1, SEA_LEVEL - 50),
                InterpolatePoint.create( 0  , SEA_LEVEL - 25),
                InterpolatePoint.create( 0.1, SEA_LEVEL - 30)
        }, "ocean_heightmap");
        genericHeightProvider = new Interpolate(new InterpolatePoint[]{
                InterpolatePoint.create(-0.85   , LAND_HEIGHT + PLATEAU_HEIGHT),
                InterpolatePoint.create(-0.8  , LAND_HEIGHT),
                InterpolatePoint.create(-0.7   , LAND_HEIGHT),
                InterpolatePoint.create(-0.6   , LAND_HEIGHT + 50),
                InterpolatePoint.create(-0.5   , SEA_LEVEL - 10),
                InterpolatePoint.create(-0.4   , LAND_HEIGHT + 100),
                InterpolatePoint.create(-0.3   , LAND_HEIGHT),
                InterpolatePoint.create(-0.05  , SEA_LEVEL),
                InterpolatePoint.create(0      , SEA_LEVEL - RIVER_DEPTH * 2), // the depth is multiplied by noise2. it is expected to be around 0.5.
                InterpolatePoint.create(0.05   , SEA_LEVEL),
                InterpolatePoint.create(0.3    , LAND_HEIGHT),
                InterpolatePoint.create(0.4    , LAND_HEIGHT + 50),
                InterpolatePoint.create(0.5    , LAND_HEIGHT + 30),
                InterpolatePoint.create(0.6    , LAND_HEIGHT + 40),
                InterpolatePoint.create(0.7    , LAND_HEIGHT),
                InterpolatePoint.create(0.8   , LAND_HEIGHT),
                InterpolatePoint.create(0.85    , LAND_HEIGHT + PLATEAU_HEIGHT)
        }, "generic_heightmap");
        // block populators
        populators = new ArrayList<>();
        populators.add(new OverworldCaveGenerator(yOffset_overworld, seed, OCTAVES));
        populators.add(new OverworldBlockGenericPopulator());
        populators.add(new OrePopulator(yOffset_overworld));
        populators.add(new TreePopulator());
    }
    static double getTerrainHeight(Biome currBiome, double noise, double noiseTwo) {
        double result;
        switch (currBiome) {
            case FROZEN_OCEAN:              // sulphurous ocean
                return 0;
            case BEACHES:                   // beach
            case COLD_BEACH:                // sulphurous beach
                return SEA_LEVEL;
            case OCEAN:                     // ocean
                result = oceanHeightProvider.getY(noise);
                return LAND_HEIGHT + (result - LAND_HEIGHT) * noiseTwo;
            case MESA:                      // astral infection
                result = astralInfectionHeightProvider.getY(noise);
                return LAND_HEIGHT + (result - LAND_HEIGHT) * noiseTwo;
            case JUNGLE:                    // jungle
                return SEA_LEVEL;
            default:
                result = genericHeightProvider.getY(noise);
                return LAND_HEIGHT + (result - LAND_HEIGHT) * noiseTwo;
        }
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
    static void generateTopSoil(ChunkData chunk, int i, int height, int j, int blockX, int blockZ, Biome biome, int yOffset) {
        // although it is named as such, this actually generates stone layers too.
        double topSoilThicknessRandomizer = stoneVeinGenerator.noise(blockX, blockZ, 0.5, 0.5, false);
        double topSoilThickness;
        switch (biome) {
            case OCEAN: // ocean
            case BEACHES: // beach
            case FROZEN_OCEAN: // sulphurous ocean
            case COLD_BEACH: // sulphurous beach
                topSoilThickness = 35 + topSoilThicknessRandomizer * 5 + yOffset;
                for (int y = Math.min(height - yOffset, 254); y > 0; y--) {
                    int effectualY = y + yOffset;
                    if (chunk.getType(i, y, j) == Material.AIR) {
                        if (effectualY <= SEA_LEVEL) chunk.setBlock(i, y, j, Material.WATER);
                    } else if (--topSoilThickness > 0) chunk.setBlock(i, y, j, Material.SAND);
                    else break;
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
                for (int y = Math.min(height - yOffset, 254); y > 0; y--) {
                    if (chunk.getType(i, y, j) == Material.AIR) continue;
                    Material matToSet;
                    int effectualY = y + yOffset;
                    boolean isStoneVein = stoneVeinGenerator.noise(blockX, effectualY, blockZ, 0.5, 0.5, false) > 0.5;
                    if (effectualY > soilLayerHeight) {
                        if (isStoneVein)
                            matToSet = matStone;
                        else
                            matToSet = matSoil;
                    } else {
                        if (isStoneVein)
                            matToSet = matSoil;
                        else
                            matToSet = matStone;
                    }
                    if (chunk.getType(i, y + 1, j) == Material.AIR && matToSet == matSoil)
                        matToSet = matTopSoil;
                    chunk.setBlock(i, y, j, matToSet);
                }
        }

        if (yOffset >= 0) {
            // for surface only
            for (int y = SEA_LEVEL; y > 0; y--) {
                if (chunk.getType(i, y, j) == Material.AIR) {
                    chunk.setBlock(i, y, j, Material.WATER);
                } else break;
            }
        }
    }
    // init terrain (rough + detail)
    public static void initializeTerrain(ChunkData chunk, int blockXStart, int blockZStart, BiomeGrid biome, int yOffset) {
        // this function creates the raw terrain of the chunk, consisting of only air, water or stone.
        double height;
        double biomesSampled = (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2) * (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2);
        int currX, currZ;

        HashMap<Biome, Integer> nearbyBiomeMap = new HashMap<>();
        for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
            int currSampleX = blockXStart + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;
            for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ++) {
                int currSampleZ = blockZStart + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                Biome currBiome = OverworldBiomeGenerator.getBiome(seed, currSampleX, currSampleZ);
                nearbyBiomeMap.put(currBiome, nearbyBiomeMap.getOrDefault(currBiome, 0) + 1);
            }
        }
        HashMap<Biome, Integer> nearbyBiomeMapBackup = (HashMap<Biome, Integer>) nearbyBiomeMap.clone();

        // loop through all blocks.
        for (int i = 0; i < 16; i ++) {
            currX = blockXStart + i;
            nearbyBiomeMap = (HashMap<Biome, Integer>) nearbyBiomeMapBackup.clone();
            for (int j = 0; j < 16; j++) {
                currZ = blockZStart + j;

                // setup height info according to nearby biomes.
                height = 0;
                double terrainNoise = terrainGenerator.noise(currX, currZ, 0.5, 0.5, false);
                double terrainNoiseTwo = terrainGeneratorTwo.noise(currX, currZ, 0.5, 0.5, true);
                terrainNoiseTwo = terrainNoiseTwo * 0.75 + 0.5;
                int totalBiomes = 0;
                for (Biome bom : nearbyBiomeMap.keySet()) {
                    totalBiomes += nearbyBiomeMap.get(bom);
                    height += getTerrainHeight(bom, terrainNoise, terrainNoiseTwo) * nearbyBiomeMap.get(bom);
                }
                if (totalBiomes != biomesSampled) Bukkit.getLogger().info("NOT MATCHING?? " + i + ", " + j + ", " + totalBiomes);
                height /= biomesSampled;
                double spawnMulti = (double)(Math.abs(currX) + Math.abs(currZ)) / 1000; // make sure no absurd landscape occurs around the spawn point
                if (spawnMulti < 1)
                    height = LAND_HEIGHT * (1 - spawnMulti) + height * spawnMulti;
                if (height > 50)
                    height += 10 * terrainDetailGenerator.noise(currX, currZ, 0.5, 0.5, true);

                // loop through y to set blocks
                for (int y_coord = 0; y_coord < 256; y_coord ++) {
                    int effectualY = y_coord + yOffset;
                    if (y_coord == 0)
                        chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (y_coord == 255 && yOffset < 0)
                        chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (effectualY < height)
                        chunk.setBlock(i, y_coord, j, Material.STONE);
                    else
                        chunk.setBlock(i, y_coord, j, Material.AIR);
                }

                generateTopSoil(chunk, i, (int) Math.ceil(height), j, currX, currZ, biome.getBiome(i, j), yOffset);

                // sliding window technique
                if (j + 1 < 16) {
                    int currSample_dropZ = currZ - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    int currSample_addZ = currZ + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    for (int sampleOffset = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffset <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffset++) {
                        int currSampleX = currX + sampleOffset * NEARBY_BIOME_SAMPLE_STEPSIZE;
                        Biome currBiome_drop = OverworldBiomeGenerator.getBiome(seed, currSampleX, currSample_dropZ);
                        nearbyBiomeMap.put(currBiome_drop, nearbyBiomeMap.getOrDefault(currBiome_drop, 0) - 1);
                        Biome currBiome_add =  OverworldBiomeGenerator.getBiome(seed, currSampleX, currSample_addZ);
                        nearbyBiomeMap.put(currBiome_add,  nearbyBiomeMap.getOrDefault(currBiome_add,  0) + 1);
                    }
                }
            }
            // sliding window technique.
            if (i + 1 < 16) {
                int currSample_dropX = currX - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                int currSample_addX  = currX + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                for (int sampleOffset = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffset <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffset++) {
                    int currSampleZ = blockZStart + sampleOffset * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    Biome currBiome_drop = OverworldBiomeGenerator.getBiome(seed, currSample_dropX, currSampleZ);
                    nearbyBiomeMapBackup.put(currBiome_drop, nearbyBiomeMapBackup.getOrDefault(currBiome_drop, 0) - 1);
                    Biome currBiome_add = OverworldBiomeGenerator.getBiome(seed, currSample_addX, currSampleZ);
                    nearbyBiomeMapBackup.put(currBiome_add,  nearbyBiomeMapBackup.getOrDefault(currBiome_add,  0) + 1);
                }
            }
        }
    }
    public static void initializeTerrain_timingTest(ChunkData chunk, int blockXStart, int blockZStart, BiomeGrid biome, int yOffset) {

        long timing;

        // this function creates the raw terrain of the chunk, consisting of only air, water or stone.
        double height;
        double biomesSampled = (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2) * (1 + NEARBY_BIOME_SAMPLE_RADIUS * 2);
        int currX, currZ;

        HashMap<Biome, Integer> nearbyBiomeMap = new HashMap<>();
        for (int sampleOffsetX = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetX <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetX++) {
            int currSampleX = blockXStart + sampleOffsetX * NEARBY_BIOME_SAMPLE_STEPSIZE;
            for (int sampleOffsetZ = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffsetZ <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffsetZ++) {
                int currSampleZ = blockZStart + sampleOffsetZ * NEARBY_BIOME_SAMPLE_STEPSIZE;
                Biome currBiome = OverworldBiomeGenerator.getBiome(seed, currSampleX, currSampleZ);
                nearbyBiomeMap.put(currBiome, nearbyBiomeMap.getOrDefault(currBiome, 0) + 1);
            }
        }
        HashMap<Biome, Integer> nearbyBiomeMapBackup = (HashMap<Biome, Integer>) nearbyBiomeMap.clone();

        // loop through all blocks.
        for (int i = 0; i < 16; i ++) {
            currX = blockXStart + i;
            nearbyBiomeMap = (HashMap<Biome, Integer>) nearbyBiomeMapBackup.clone();
            for (int j = 0; j < 16; j++) {
                currZ = blockZStart + j;

                // setup height info according to nearby biomes.
                height = 0;
                double terrainNoise = terrainGenerator.noise(currX, currZ, 0.5, 0.5, false);
                double terrainNoiseTwo = terrainGeneratorTwo.noise(currX, currZ, 0.5, 0.5, true);
                terrainNoiseTwo = terrainNoiseTwo * 0.75 + 0.5;
                int totalBiomes = 0;
                for (Biome bom : nearbyBiomeMap.keySet()) {
                    totalBiomes += nearbyBiomeMap.get(bom);
                    height += getTerrainHeight(bom, terrainNoise, terrainNoiseTwo) * nearbyBiomeMap.get(bom);
                }
                if (totalBiomes != biomesSampled) Bukkit.getLogger().info("NOT MATCHING?? " + i + ", " + j + ", " + totalBiomes);
                height /= biomesSampled;
                double spawnMulti = (double)(Math.abs(currX) + Math.abs(currZ)) / 500; // make sure no absurd landscape occurs around the spawn point
                if (spawnMulti < 1)
                    height = LAND_HEIGHT * (1 - spawnMulti) + height * spawnMulti;
//                if (height > 50)
//                    height += 10 * terrainDetailGenerator.noise(currX, currZ, 0.5, 0.5, true);

                timing = System.nanoTime();

                // loop through y to set blocks
                for (int y_coord = 0; y_coord < 256; y_coord ++) {
                    int effectualY = y_coord + yOffset;
                    if (y_coord == 0)
                        chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (y_coord == 255 && yOffset < 0)
                        chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (effectualY < height)
                        chunk.setBlock(i, y_coord, j, Material.STONE);
                    else
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

                // sliding window technique
                if (j + 1 < 16) {

                    timing = System.nanoTime();

                    int currSample_dropZ = currZ - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    int currSample_addZ = currZ + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    for (int sampleOffset = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffset <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffset++) {
                        int currSampleX = currX + sampleOffset * NEARBY_BIOME_SAMPLE_STEPSIZE;
                        Biome currBiome_drop = OverworldBiomeGenerator.getBiome(seed, currSampleX, currSample_dropZ);
                        nearbyBiomeMap.put(currBiome_drop, nearbyBiomeMap.getOrDefault(currBiome_drop, 0) - 1);
                        Biome currBiome_add =  OverworldBiomeGenerator.getBiome(seed, currSampleX, currSample_addZ);
                        nearbyBiomeMap.put(currBiome_add,  nearbyBiomeMap.getOrDefault(currBiome_add,  0) + 1);
                    }

                    test_sample += (System.nanoTime() - timing);
                    test_sample_time ++;
                    if (test_sample_time % 2560 == 0)
                        // this was 12097780
                        // now 1671719, (~1.7 ms, good enough)
                        // 16600000
                        Bukkit.broadcastMessage("Time elapsed for sampling nearby biome: " + test_sample * 256 / test_sample_time);
                }
            }
            // sliding window technique.
            if (i + 1 < 16) {
                int currSample_dropX = currX - NEARBY_BIOME_SAMPLE_RADIUS * NEARBY_BIOME_SAMPLE_STEPSIZE;
                int currSample_addX  = currX + (NEARBY_BIOME_SAMPLE_RADIUS + 1) * NEARBY_BIOME_SAMPLE_STEPSIZE;
                // setup height info according to nearby biomes at both offset 0. Then use sliding window technique to derive the height everywhere.
                for (int sampleOffset = NEARBY_BIOME_SAMPLE_RADIUS * -1; sampleOffset <= NEARBY_BIOME_SAMPLE_RADIUS; sampleOffset++) {
                    int currSampleZ = blockZStart + sampleOffset * NEARBY_BIOME_SAMPLE_STEPSIZE;
                    Biome currBiome_drop = OverworldBiomeGenerator.getBiome(seed, currSample_dropX, currSampleZ);
                    nearbyBiomeMapBackup.put(currBiome_drop, nearbyBiomeMapBackup.getOrDefault(currBiome_drop, 0) - 1);
                    Biome currBiome_add = OverworldBiomeGenerator.getBiome(seed, currSample_addX, currSampleZ);
                    nearbyBiomeMapBackup.put(currBiome_add,  nearbyBiomeMapBackup.getOrDefault(currBiome_add,  0) + 1);
                }
            }
        }
    }
    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        // setup biome
        tweakBiome(x, z, biome);
        // init terrain
        ChunkData chunk = createChunkData(world);
//        initializeTerrain_timingTest(chunk, x * 16, z * 16, biome, yOffset_overworld);
        initializeTerrain(chunk, x * 16, z * 16, biome, yOffset_overworld);
        // tweak terrain
        return chunk;
    }
    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return populators;
    }
}