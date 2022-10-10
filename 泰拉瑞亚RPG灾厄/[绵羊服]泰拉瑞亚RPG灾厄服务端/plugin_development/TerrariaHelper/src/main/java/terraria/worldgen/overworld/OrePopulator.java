package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

import java.util.Random;

public class OrePopulator extends BlockPopulator {
    int yOffset;
    public OrePopulator(int yOffset) {
        this.yOffset = yOffset;
    }
    // helper functions
    void generateSingleVein(World wld, Material oreType, int blockX, int blockY, int blockZ, int radius) {
        for (int i = radius * -1; i <= radius; i ++)
            for (int j = Math.max(1, (radius * -1 + i)); j < Math.min(254, (radius - i)); j ++)
                for (int k = radius * -1 + i + j; k < radius - i - j; k ++) {
                    Block blk = wld.getBlockAt(blockX + i, blockY + j, blockZ + k);
                    if (blk.getType() == Material.STONE) blk.setType(oreType);
                }
    }
    // TODO: 矿脉生成导致附近区块加载死循环
    void generateGenericOre(World wld, Random rdm, Chunk chunk, int yMax, int yMin, int stepSize, Material oreType, int radius) {
        int blockXStart = chunk.getX() * 16, blockZStart = chunk.getZ() * 16;
        Bukkit.getLogger().info(blockXStart+"|"+blockZStart);
        yMax = Math.min(256, yMax - yOffset);
        for (int y = yMax; y >= yMin; y -= stepSize) {
            generateSingleVein(wld,
                    oreType,
                    blockXStart + (rdm.nextInt() % 16),
                    y - (rdm.nextInt() % stepSize),
                    blockZStart + (rdm.nextInt() % 16),
                    radius);
        }
    }

    void generateCopper(World wld, Random rdm, Chunk chunk) {
        // one vein per 16 * 16 * 12
        generateGenericOre(wld, rdm, chunk, 50, 0, 12, Material.COAL_ORE, 5);
    }
    void generateIron(World wld, Random rdm, Chunk chunk) {
        // one vein per 16 * 16 * 16
        generateGenericOre(wld, rdm, chunk, 30, 0, 16, Material.IRON_ORE, 5);
    }
    void generateSilver(World wld, Random rdm, Chunk chunk) {
        // one vein per 16 * 16 * 24
        generateGenericOre(wld, rdm, chunk, -30, 0, 24, Material.LAPIS_ORE, 4);
    }
    void generateGold(World wld, Random rdm, Chunk chunk) {
        // one vein per 16 * 16 * 32
        generateGenericOre(wld, rdm, chunk, -100, 0, 32, Material.GOLD_ORE, 4);
    }
    @Override
    public void populate(World wld, Random rdm, Chunk chunk) {
        generateCopper(wld, rdm, chunk);
        generateIron(wld, rdm, chunk);
        generateSilver(wld, rdm, chunk);
        generateGold(wld, rdm, chunk);
    }
}
