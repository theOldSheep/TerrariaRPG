package terraria.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import terraria.TerrariaHelper;
import terraria.worldgen.overworld.cavern.CavernChunkGenerator;


public class CavernRegisterListener implements Listener {
    @org.bukkit.event.EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCreateWorld(WorldLoadEvent evt) {
        if (evt.getWorld().getName().equals("world")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(TerrariaHelper.getInstance(), () -> {
                try {
                    Bukkit.getLogger().info("正在尝试初始化洞穴世界！");
                    if (Bukkit.getServer().getWorld("world_cavern") == null) {
                        new WorldCreator("world_cavern")
                                .generator(new CavernChunkGenerator())
                                .environment(World.Environment.NORMAL)
                                .type(WorldType.CUSTOMIZED)
                                .generateStructures(false)
                                .seed(TerrariaHelper.worldSeed)
                                .createWorld();
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().info("生成洞穴世界时发生错误！");
                    e.printStackTrace();
                    Bukkit.getLogger().info("正在关闭服务器……");
                    Bukkit.getServer().shutdown();
                } finally {
                    Bukkit.getLogger().info("洞穴世界初始化尝试完毕！");
                }
            }, 1);
        }
    }
}
