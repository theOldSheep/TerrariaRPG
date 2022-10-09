package terraria.worldgen.overworld;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;

import java.util.Random;

public class OverworldBlockGenericPopulator extends BlockPopulator {
    // this block populator is the last step to set up the vast majority of the solid blocks within a chunk (i.e. stained terracotta)
    @Override
    public void populate(World wld, Random rdm, Chunk chunk) {
        int startX = chunk.getX() * 16, startZ = chunk.getZ() * 16;
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++) {
                int blockX = startX + i, blockZ = startZ + j;
                Biome biome = OverworldBiomeGenerator.getBiome(wld.getSeed(), blockX, blockZ);
                // additional setup for terracotta color etc.
                if (biome == Biome.MUSHROOM_ISLAND ||
                        biome == Biome.ICE_FLATS ||
                        biome == Biome.MESA ||
                        biome == Biome.COLD_BEACH ||
                        biome == Biome.FROZEN_OCEAN) {
                    for (int y = 1; y < 256; y ++) {
                        // TODO, setup terracotta/sand type
                    }
                }
            }
    }
}
