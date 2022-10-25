package terraria.worldgen.overworld;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

public class OrePopulator extends BlockPopulator {
    int yOffset;
    HashMap<String, Material> oreMaterials;
    static int SURFACE = 50,
            UNDERGROUND = 0,
            CAVERN = -100,
            DEEP_CAVERN = -150;
    public OrePopulator(int yOffset) {
        this.yOffset = yOffset;
        oreMaterials = new HashMap<>(50);
        oreMaterials.put("COPPER",              Material.COAL_ORE);
        oreMaterials.put("IRON",                Material.IRON_ORE);
        oreMaterials.put("SILVER",              Material.LAPIS_ORE);
        oreMaterials.put("GOLD",                Material.GOLD_ORE);
        oreMaterials.put("HELLSTONE",           Material.MAGMA);
        oreMaterials.put("COBALT",              Material.LAPIS_BLOCK);
        oreMaterials.put("LIFE_CRYSTAL",        Material.EMERALD_ORE);
        oreMaterials.put("MYTHRIL",             Material.EMERALD_BLOCK);
        oreMaterials.put("ADAMANTITE",          Material.REDSTONE_ORE);
        oreMaterials.put("CHARRED",             Material.QUARTZ_ORE);
        oreMaterials.put("CHLOROPHYTE",         Material.MOSSY_COBBLESTONE);
        oreMaterials.put("SEA_PRISM",           Material.PRISMARINE);
        oreMaterials.put("AERIALITE",           Material.DIAMOND_ORE);
        oreMaterials.put("CRYONIC",             Material.DIAMOND_BLOCK);
        oreMaterials.put("PERENNIAL",           Material.LIME_GLAZED_TERRACOTTA);
        oreMaterials.put("SCORIA",              Material.COAL_BLOCK);
        oreMaterials.put("ASTRAL",              Material.REDSTONE_BLOCK);
        oreMaterials.put("EXODIUM",             Material.BLACK_GLAZED_TERRACOTTA);
        oreMaterials.put("UELIBLOOM",           Material.BROWN_GLAZED_TERRACOTTA);
        oreMaterials.put("AURIC",               Material.YELLOW_GLAZED_TERRACOTTA);
    }
    // helper functions
    void generateSingleVein(World wld, Material oreType, int blockX, int blockY, int blockZ, int size) {
        double radius = (double) (size - 1) / 2,
                maxDistSqr = radius * radius * 1.25; // (radius * ) ^ 2
        for (int i = 0; i < size; i ++)
            for (int j = 0; j < size; j ++)
                for (int k = 0; k < size; k ++) {
                    if (blockY + j <= 0 || blockY + j >= 255) continue; // no overriding bedrock/outside of world
                    double xDistFromCenter = i - radius,
                            yDistFromCenter = j - radius,
                            zDistFromCenter = k - radius;
                    double distSqr = xDistFromCenter * xDistFromCenter + yDistFromCenter * yDistFromCenter + zDistFromCenter * zDistFromCenter;
                    if (distSqr > maxDistSqr) continue; // make the shape less sharp

                    Block blk = wld.getBlockAt(blockX + i, blockY + j, blockZ + k);
                    if (blk.getType().isSolid() && blk.getType() != Material.BEDROCK) blk.setType(oreType);
                }
    }
    void generateGenericOre(World wld, Random rdm, Chunk chunk, int yMax, int yMin, int stepSize, String oreName, int size) {
        Material oreType = oreMaterials.getOrDefault(oreName, Material.STONE);
        int blockXStart = chunk.getX() * 16, blockZStart = chunk.getZ() * 16;
        yMax = Math.min(256, yMax - yOffset);
        int margin = (size / 2), modulo = 16 - size;
        for (int y = yMax; y >= yMin; y -= stepSize) {
            generateSingleVein(wld,
                    oreType,
                    blockXStart + margin + rdm.nextInt(modulo),
                    y - rdm.nextInt(stepSize),
                    blockZStart + margin + rdm.nextInt(modulo),
                    size);
        }
    }

    // first, all ores from vanilla Terraria
    void generateCopper(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, SURFACE, 0, 32, "COPPER", 4);
    }
    void generateIron(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, UNDERGROUND, 0, 48, "IRON", 4);
    }
    void generateSilver(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, CAVERN, 0, 64, "SILVER", 4);
    }
    void generateGold(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, DEEP_CAVERN, 0, 96, "GOLD", 4);
    }
    void generateCobalt(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, UNDERGROUND, 0, 64, "COBALT", 4);
    }
    void generateMythril(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, CAVERN, 0, 96, "MYTHRIL", 5);
    }
    void generateAdamantite(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, DEEP_CAVERN, 0, 150, "ADAMANTITE", 5);
    }
    void generateChlorophyte(World wld, Random rdm, Chunk chunk) {
        if (wld.getBiome(chunk.getX() * 16, chunk.getZ() * 16) == Biome.JUNGLE)
            generateGenericOre(wld, rdm, chunk, CAVERN, 0, 150, "CHLOROPHYTE", 5);
    }
    // Calamity
    // pre-hardmode
    void generateSeaPrism(World wld, Random rdm, Chunk chunk) {
        if (wld.getBiome(chunk.getX() * 16, chunk.getZ() * 16) == Biome.DESERT)
            generateGenericOre(wld, rdm, chunk, UNDERGROUND, 0, 75, "SEA_PRISM", 5);
    }
    void generateAerialite(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, CAVERN, 0, 150, "AERIALITE", 6);
    }
    // hardmode
    // charred ore only generates in the hell level.
    void generateCryonic(World wld, Random rdm, Chunk chunk) {
        if (wld.getBiome(chunk.getX() * 16, chunk.getZ() * 16) == Biome.TAIGA_COLD)
            generateGenericOre(wld, rdm, chunk, UNDERGROUND, 0, 150, "CRYONIC", 6);
    }
    void generatePerennial(World wld, Random rdm, Chunk chunk) {
        if (wld.getBiome(chunk.getX() * 16, chunk.getZ() * 16) == Biome.JUNGLE)
            generateGenericOre(wld, rdm, chunk, UNDERGROUND, 0, 150, "PERENNIAL", 6);
    }
    void generateScoria(World wld, Random rdm, Chunk chunk) {
        if (wld.getBiome(chunk.getX() * 16, chunk.getZ() * 16) == Biome.FROZEN_OCEAN)
            generateGenericOre(wld, rdm, chunk, CAVERN, 0, 150, "SCORIA", 5);
    }
    void generateAstral(World wld, Random rdm, Chunk chunk) {
        if (yOffset < 0) return; // only surface world get this ore
        if (wld.getBiome(chunk.getX() * 16, chunk.getZ() * 16) != Biome.MESA) return;
        if (rdm.nextDouble() < 0.01) {
            int xCenter = chunk.getX() * 16 + (int) (Math.random() * 16),
                    zCenter = chunk.getZ() * 16 + (int) (Math.random() * 16);
            int height = wld.getHighestBlockYAt(xCenter, zCenter);
            Material oreMat = oreMaterials.getOrDefault("ASTRAL", Material.STONE);
            // set spherical cluster of ore
            for (int xOffset = -8; xOffset <= 8; xOffset ++) {
                for (int zOffset = -8; zOffset <= 8; zOffset ++) {
                    int horDistSqr = (xOffset * xOffset) + (zOffset * zOffset);
                    if (horDistSqr > 64) continue;
                    int xSet = xCenter + xOffset, zSet = zCenter + zOffset;
                    for (int ySet = wld.getHighestBlockYAt(xSet, zSet); (ySet - height) * (ySet - height) + horDistSqr < 64; ySet --) {
                        wld.getBlockAt(xSet, ySet, zSet).setType(oreMat);
                    }
                }
            }
            // the altar on the top
            wld.getBlockAt(xCenter, height + 1, zCenter).setType(Material.ENDER_PORTAL_FRAME);
        }
    }
    // post-moon lord

    void generateExodium(World wld, Random rdm, Chunk chunk) {
        if (yOffset < 0) return; // only surface world get this ore
        if (rdm.nextDouble() < 0.001) {
            int xCenter = chunk.getX() * 16 + (int) (Math.random() * 16),
                    zCenter = chunk.getZ() * 16 + (int) (Math.random() * 16),
                    height = 215 + rdm.nextInt(20);
            Material oreMat = oreMaterials.getOrDefault("EXODIUM", Material.STONE);
            // set spherical cluster of ore
            for (int xOffset = -10; xOffset <= 10; xOffset ++) {
                for (int zOffset = -10; zOffset <= 10; zOffset ++) {
                    int horDistSqr = (xOffset * xOffset) + (zOffset * zOffset);
                    if (horDistSqr > 100) continue;
                    int xSet = xCenter + xOffset, zSet = zCenter + zOffset;
                    for (int ySet = height - 10; ySet <= height + 10; ySet ++) {
                        if ((ySet - height) * (ySet - height) + horDistSqr > 100) continue;
                        wld.getBlockAt(xSet, ySet, zSet).setType(oreMat);
                    }
                }
            }
        }
    }
    void generateUelibloom(World wld, Random rdm, Chunk chunk) {
        if (wld.getBiome(chunk.getX() * 16, chunk.getZ() * 16) == Biome.JUNGLE)
            generateGenericOre(wld, rdm, chunk, CAVERN, 0, 125, "UELIBLOOM", 6);
    }
    void generateAuric(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, DEEP_CAVERN, 0, 500, "AURIC", 8);
    }
    void generateHellstone(World wld, Random rdm, Chunk chunk) {
        generateGenericOre(wld, rdm, chunk, 75, 0, 24, "HELLSTONE", 4);
    }
    void generateCharred(World wld, Random rdm, Chunk chunk) {
        if (wld.getBiome(chunk.getX() * 16, chunk.getZ() * 16) == Biome.SAVANNA)
            generateGenericOre(wld, rdm, chunk, 60, 0, 32, "CHARRED", 5);
    }
    void generateUndergroundLake(World world, Random random, Chunk chunk) {
        // source code from https://bukkit.fandom.com/wiki/Developing_a_World_Generator_Plugin
        // which is converted from vanilla minecraft's lake generator
        if (random.nextInt(100) < 10) {
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();

            int randomX = chunkX * 16 + random.nextInt(16);
            int randomZ = chunkZ * 16 + random.nextInt(16);
            int y;

            for (y = 1; world.getBlockAt(randomX, y, randomZ).getType() != Material.AIR; y++) {
                if (y >= 225) return;
            }
            y -= 7;

            Block block = world.getBlockAt(randomX + 8, y, randomZ + 8);

            if (world.getEnvironment() == World.Environment.NORMAL && y + yOffset > -150) {
                block.setType(Material.WATER);
            } else {
                block.setType(Material.LAVA);
            }

            boolean[] booleans = new boolean[2048];

            int i = random.nextInt(4) + 4;

            int j, j1, k1;

            for (j = 0; j < i; ++j) {
                double d0 = random.nextDouble() * 6.0D + 3.0D;
                double d1 = random.nextDouble() * 4.0D + 2.0D;
                double d2 = random.nextDouble() * 6.0D + 3.0D;
                double d3 = random.nextDouble() * (16.0D - d0 - 2.0D) + 1.0D + d0 / 2.0D;
                double d4 = random.nextDouble() * (8.0D - d1 - 4.0D) + 2.0D + d1 / 2.0D;
                double d5 = random.nextDouble() * (16.0D - d2 - 2.0D) + 1.0D + d2 / 2.0D;

                for (int k = 1; k < 15; ++k) {
                    for (int l = 1; l < 15; ++l) {
                        for (int i1 = 0; i1 < 7; ++i1) {
                            double d6 = (k - d3) / (d0 / 2.0D);
                            double d7 = (i1 - d4) / (d1 / 2.0D);
                            double d8 = (l - d5) / (d2 / 2.0D);
                            double d9 = d6 * d6 + d7 * d7 + d8 * d8;

                            if (d9 < 1.0D) {
                                booleans[(k * 16 + l) * 8 + i1] = true;
                            }
                        }
                    }
                }
            }

            for (j = 0; j < 16; ++j) {
                for (k1 = 0; k1 < 16; ++k1) {
                    for (j1 = 0; j1 < 8; ++j1) {
                        if (booleans[(j * 16 + k1) * 8 + j1]) {
                            world.getBlockAt(randomX + j, y + j1, randomZ + k1).setType(j1 > 4 ? Material.AIR : block.getType());
                        }
                    }
                }
            }

            for (j = 0; j < 16; ++j) {
                for (k1 = 0; k1 < 16; ++k1) {
                    for (j1 = 4; j1 < 8; ++j1) {
                        if (booleans[(j * 16 + k1) * 8 + j1]) {
                            int X1 = randomX + j;
                            int Y1 = y + j1 - 1;
                            int Z1 = randomZ + k1;

                            if (world.getBlockAt(X1, Y1, Z1).getType() == Material.DIRT) {
                                world.getBlockAt(X1, Y1, Z1).setType(Material.GRASS);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void populate(World wld, Random rdm, Chunk chunk) {
//        generateUndergroundLake(wld, rdm, chunk);
        // overworld
        if (wld.getEnvironment() == World.Environment.NORMAL) {
            // vanilla Terraria
            generateCopper(wld, rdm, chunk);
            generateIron(wld, rdm, chunk);
            generateSilver(wld, rdm, chunk);
            generateGold(wld, rdm, chunk);
            generateCobalt(wld, rdm, chunk);
            generateMythril(wld, rdm, chunk);
            generateAdamantite(wld, rdm, chunk);
            generateChlorophyte(wld, rdm, chunk);
            // Calamity, pre-hardmode
            generateSeaPrism(wld, rdm, chunk);
            generateAerialite(wld, rdm, chunk);
            // Calamity, hardmode
            generateCryonic(wld, rdm, chunk);
            generatePerennial(wld, rdm, chunk);
            generateScoria(wld, rdm, chunk);
            generateAstral(wld, rdm, chunk);
            // Calamity, post-moon lord
            generateExodium(wld, rdm, chunk);
            generateUelibloom(wld, rdm, chunk);
            generateAuric(wld, rdm, chunk);
        } else {
            // nether
            generateHellstone(wld, rdm, chunk);
            generateCharred(wld, rdm, chunk);
        }
    }
}
