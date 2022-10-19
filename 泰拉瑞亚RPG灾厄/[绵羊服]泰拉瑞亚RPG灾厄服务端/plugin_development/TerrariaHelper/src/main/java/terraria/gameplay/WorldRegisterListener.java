package terraria.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import terraria.TerrariaHelper;
import terraria.worldgen.overworld.OverworldChunkGenerator;
import terraria.worldgen.overworld.cavern.CavernChunkGenerator;


public class WorldRegisterListener implements Listener {
    @org.bukkit.event.EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCreateWorld(WorldLoadEvent evt) {
        if (evt.getWorld().getName().equals("world")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(TerrariaHelper.getInstance(), () -> {
                try {
                    if (Bukkit.getServer().getWorld("world_surface") == null) {
                        Bukkit.getLogger().info("正在尝试初始化地面世界！");
                        new WorldCreator("world_surface")
                                .generator(OverworldChunkGenerator.getInstance())
                                .environment(World.Environment.NORMAL)
                                .type(WorldType.CUSTOMIZED)
                                .generateStructures(false)
                                .seed(TerrariaHelper.worldSeed)
                                .createWorld();
                    }
                    if (Bukkit.getServer().getWorld("world_cavern") == null) {
                        Bukkit.getLogger().info("正在尝试初始化洞穴世界！");
                        new WorldCreator("world_cavern")
                                .generator(CavernChunkGenerator.getInstance())
                                .environment(World.Environment.NORMAL)
                                .type(WorldType.CUSTOMIZED)
                                .generateStructures(false)
                                .seed(TerrariaHelper.worldSeed)
                                .createWorld();
                    }
                    // TODO: change world generator for hell world to a new one
                    if (Bukkit.getServer().getWorld("world_hell") == null) {
                        Bukkit.getLogger().info("正在尝试初始化地狱世界！");
                        new WorldCreator("world_hell")
                                .generator(CavernChunkGenerator.getInstance())
                                .environment(World.Environment.NETHER)
                                .type(WorldType.CUSTOMIZED)
                                .generateStructures(false)
                                .seed(TerrariaHelper.worldSeed)
                                .createWorld();
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().info("初始化世界时发生错误！");
                    e.printStackTrace();
                    Bukkit.getLogger().info("正在关闭服务器……");
                    Bukkit.getServer().shutdown();
                } finally {
                    Bukkit.getLogger().info("世界初始化尝试完毕！");
                }
            }, 1);
        }
    }
}
