package terraria.worldgen.overworld.cavern;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import terraria.TerrariaHelper;
import terraria.worldgen.overworld.OrePopulator;
import terraria.worldgen.overworld.OverworldBlockGenericPopulator;
import terraria.worldgen.overworld.OverworldCaveGenerator;
import terraria.worldgen.overworld.OverworldChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CavernChunkGenerator extends ChunkGenerator {
    static final int yOffset = -253;
    static CavernChunkGenerator instance = new CavernChunkGenerator();
    ArrayList<BlockPopulator> populators;
    public static CavernChunkGenerator getInstance() {
        return instance;
    }

    public CavernChunkGenerator() {
        super();
        // init populator
        populators = new ArrayList<>();
        populators.add(new OverworldCaveGenerator(yOffset, TerrariaHelper.worldSeed, OverworldChunkGenerator.OCTAVES));
        populators.add(new OverworldBlockGenericPopulator());
        populators.add(new OrePopulator(yOffset));
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        // setup biome
        OverworldChunkGenerator.tweakBiome(x, z, biome);
        // init terrain
        ChunkData chunk = createChunkData(world);
        OverworldChunkGenerator.initializeTerrain(chunk, x * 16, z * 16, biome, yOffset);
        // tweak terrain
        return chunk;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return populators;
    }
}