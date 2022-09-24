package terraria;

import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import terraria.dragoncorehelper.RandomTitle;
import terraria.dragoncorehelper.playerKeyToggleListener;
import terraria.worldgen.overworld.OverworldChunkGenerator;


public class TerrariaHelper extends JavaPlugin {
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new OverworldChunkGenerator();
    }
    public TerrariaHelper getInstance() {
        return this;
    }
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new playerKeyToggleListener(), this);
        Bukkit.getPluginManager().registerEvents(new RandomTitle(), this);
        getLogger().info("泰拉瑞亚RPG插件部分已启动。");
    }
    @Override
    public void onDisable() {
        getLogger().info("泰拉瑞亚RPG插件部分已停用。");
    }
}
