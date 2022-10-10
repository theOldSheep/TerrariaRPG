package terraria;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import terraria.dragoncorehelper.RandomTitle;
import terraria.dragoncorehelper.playerKeyToggleListener;
import terraria.gameplay.CavernRegisterListener;
import terraria.util.YmlHelper;
import terraria.worldgen.overworld.OverworldChunkGenerator;


public class TerrariaHelper extends JavaPlugin {
    public static long worldSeed;
    public static TerrariaHelper instance;

    public TerrariaHelper() {
        super();
        instance = this;
        worldSeed = YmlHelper.getFile("plugins/Data/setting.yml").getLong("worldSeed", 114514);
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new OverworldChunkGenerator();
    }
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new playerKeyToggleListener(), this);
        Bukkit.getPluginManager().registerEvents(new RandomTitle(), this);
        Bukkit.getPluginManager().registerEvents(new CavernRegisterListener(), this);

        getLogger().info("泰拉瑞亚RPG插件部分已启动。");
        getLogger().info("世界种子: " + worldSeed);
    }
    @Override
    public void onDisable() {
        getLogger().info("泰拉瑞亚RPG插件部分已停用。");
    }

    public static TerrariaHelper getInstance() {
        return instance;
    }
}
