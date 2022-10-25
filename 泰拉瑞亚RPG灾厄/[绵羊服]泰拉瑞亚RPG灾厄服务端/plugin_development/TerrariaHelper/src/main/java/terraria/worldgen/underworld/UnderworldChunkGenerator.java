package terraria.worldgen.underworld;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.PerlinOctaveGenerator;
import terraria.TerrariaHelper;
import terraria.worldgen.Interpolate;
import terraria.worldgen.overworld.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UnderworldChunkGenerator extends ChunkGenerator {
    static long seed = TerrariaHelper.worldSeed;
    public static int OCTAVES = 4,
            LAVA_LEVEL, FLOOR_LEVEL, CEIL_LEVEL, PLATEAU_HEIGHT;

    public static PerlinOctaveGenerator biomeGenerator, floorGenerator, floorDetailGenerator,
                                        ceilGenerator, ceilDetailGenerator;
    public static Interpolate floorHeightProvider, ceilingHeightProvider;
    static UnderworldChunkGenerator instance = new UnderworldChunkGenerator();
    static List<BlockPopulator> populators;
    private UnderworldChunkGenerator() {
        super();
        // terrain noise functions
        Random rdm = new Random(seed);
        floorGenerator = new PerlinOctaveGenerator(rdm.nextLong(), OCTAVES);
        floorGenerator.setScale(0.01);
        floorDetailGenerator = new PerlinOctaveGenerator(rdm.nextLong(), OCTAVES);
        floorDetailGenerator.setScale(0.02);
        ceilGenerator = new PerlinOctaveGenerator(rdm.nextLong(), OCTAVES);
        ceilGenerator.setScale(0.01);
        ceilDetailGenerator = new PerlinOctaveGenerator(rdm.nextLong(), OCTAVES);
        ceilDetailGenerator.setScale(0.05);
        biomeGenerator = new PerlinOctaveGenerator(rdm.nextLong(), OCTAVES);
        biomeGenerator.setScale(0.001);
        // constants
        seed = TerrariaHelper.worldSeed;
        LAVA_LEVEL = 50;
        FLOOR_LEVEL = 60;
        PLATEAU_HEIGHT = 15;
        CEIL_LEVEL = 100;
        // interpolates
        floorHeightProvider = new Interpolate(new Interpolate.InterpolatePoint[]{
                Interpolate.InterpolatePoint.create(-0.8 , 10),
                Interpolate.InterpolatePoint.create(-0.7 , LAVA_LEVEL - 25),
                Interpolate.InterpolatePoint.create(-0.4 , LAVA_LEVEL - 10),
                Interpolate.InterpolatePoint.create( 0   , FLOOR_LEVEL + PLATEAU_HEIGHT),
                Interpolate.InterpolatePoint.create( 0.5 , LAVA_LEVEL - 10),
                Interpolate.InterpolatePoint.create( 0.6 , LAVA_LEVEL - 20),
        }, "underworld_floor_heightmap");
        ceilingHeightProvider = new Interpolate(new Interpolate.InterpolatePoint[]{
                Interpolate.InterpolatePoint.create(-0.7 , 0),
                Interpolate.InterpolatePoint.create(-0.6 , CEIL_LEVEL - 10),
                Interpolate.InterpolatePoint.create(-0.2 , CEIL_LEVEL),
                Interpolate.InterpolatePoint.create( 0.2 , CEIL_LEVEL + 20),
                Interpolate.InterpolatePoint.create( 0.6 , CEIL_LEVEL),
                Interpolate.InterpolatePoint.create( 0.7 , 0),
        }, "underworld_ceil_heightmap");
        // block populators
        populators = new ArrayList<>();
        populators.add(new OrePopulator(0));
    }
    static double getFloorHeight(double noise, double noiseMulti) {
        double result = floorHeightProvider.getY(noise);
        double variance = result - LAVA_LEVEL;
        variance *= Math.abs(noiseMulti);
        return LAVA_LEVEL + variance;
    }
    static double getCeilHeight(double noise, double noiseMulti) {
        double result = ceilingHeightProvider.getY(noise);
        double variance = result - CEIL_LEVEL;
        if (variance > -10) variance *= Math.abs(noiseMulti);
        return CEIL_LEVEL + variance;
    }
    public static void tweakBiome(int x, int z, BiomeGrid biome) {
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++) {
                int blockX = x * 16 + i, blockZ = z * 16 + j;
                if (biomeGenerator.noise(blockX, blockZ, 0.5, 0.5, false) < -0.5)
                    biome.setBiome(i, j, Biome.SAVANNA);
                else
                    biome.setBiome(i, j, Biome.HELL);
            }
    }
    public static UnderworldChunkGenerator getInstance() {
        return instance;
    }
    // helper functions
    // init terrain (rough + detail)
    public static void initializeTerrain(ChunkData chunk, int blockXStart, int blockZStart, BiomeGrid biome) {
        // this function creates the raw terrain of the chunk, consisting of only air, lava or netherrack.
        int currX, currZ;

        // loop through all blocks.
        for (int i = 0; i < 16; i ++) {
            currX = blockXStart + i;
            for (int j = 0; j < 16; j++) {
                currZ = blockZStart + j;

                // setup height info according to nearby biomes.

                double floorNoise = floorGenerator.noise(currX, currZ, 0.5, 0.5, false);
                double floorDetail = floorDetailGenerator.noise(currX, currZ, 0.5, 0.5, false);
                double ceilNoise = ceilGenerator.noise(currX, currZ, 0.5, 0.5, false);
                double ceilDetail = ceilDetailGenerator.noise(currX, currZ, 0.5, 0.5, false);
                double floorHeight = getFloorHeight(floorNoise, floorDetail);
                double ceilHeight = getCeilHeight(ceilNoise, ceilDetail) + Math.random() * 2;

                // loop through y to set blocks
                for (int y_coord = 0; y_coord <= 128; y_coord++) {
                    if (y_coord == 0 || y_coord == 128)
                        chunk.setBlock(i, y_coord, j, Material.BEDROCK);
                    else if (y_coord < floorHeight || y_coord > ceilHeight)
                        if (biome.getBiome(i, j) == Biome.HELL)
                            chunk.setBlock(i, y_coord, j, Material.NETHERRACK);
                        else
                            chunk.setBlock(i, y_coord, j, Material.RED_NETHER_BRICK);
                    else if (y_coord <= LAVA_LEVEL)
                        chunk.setBlock(i, y_coord, j, Material.LAVA);
                    else
                        chunk.setBlock(i, y_coord, j, Material.AIR);
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
        initializeTerrain(chunk, x << 4, z << 4, biome);
        return chunk;
    }
    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return populators;
    }
}
