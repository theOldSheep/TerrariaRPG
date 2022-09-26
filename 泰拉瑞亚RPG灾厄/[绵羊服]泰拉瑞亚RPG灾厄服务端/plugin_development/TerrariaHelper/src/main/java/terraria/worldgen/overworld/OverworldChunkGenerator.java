package terraria.worldgen.overworld;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Random;

public class OverworldChunkGenerator extends ChunkGenerator {
    OverworldBiomeGenerator biomeGen;
    public OverworldChunkGenerator() {
        biomeGen = new OverworldBiomeGenerator();
    }
    public void tweakBiome(World world, int x, int z, BiomeGrid biome) {
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++) {
                int blockX = x * 16 + i, blockZ = z * 16 + j;
                biome.setBiome(i, j, biomeGen.getBiome(world, blockX, blockZ));
            }
    }
    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        // setup biome
        tweakBiome(world, x, z, biome);
        int currentHeight = 50;
        // basic terrain
        ChunkData chunk = createChunkData(world);
        SimplexOctaveGenerator generator = new SimplexOctaveGenerator(new Random(world.getSeed()), 8);
        generator.setScale(0.005D);
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++) {
                currentHeight = (int) ((generator.noise(x * 16 + i, z * 16 + j, 0.5D, 0.5D, true) +1 ) * 15D + 50D);
                switch (biome.getBiome(i, j)) {
                    case OCEAN:
                    case FROZEN_OCEAN:
                        chunk.setBlock(i, currentHeight, j, Material.STATIONARY_WATER);
                        break;
                    case BEACHES:
                    case COLD_BEACH:
                        chunk.setBlock(i, currentHeight, j, Material.SAND);
                        break;
                    case MUSHROOM_ISLAND:
                        chunk.setBlock(i, currentHeight, j, Material.MYCEL);
                        break;
                    default:
                        chunk.setBlock(i, currentHeight, j, Material.GRASS);
                }
                chunk.setBlock(i, currentHeight-1, j, Material.DIRT);
                for (int k = currentHeight-2; k > 0; k--)
                    chunk.setBlock(i, k, j, Material.STONE);
                chunk.setBlock(i, 0, j, Material.BEDROCK);
            }
        return chunk;
    }
}
