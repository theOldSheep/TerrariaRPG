package terraria;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import terraria.dragoncorehelper.RandomTitle;
import terraria.dragoncorehelper.playerKeyToggleListener;
import terraria.gameplay.WorldRegisterListener;
import terraria.util.YmlHelper;
import terraria.worldgen.overworld.NoiseGeneratorTest;


public class TerrariaHelper extends JavaPlugin {
    public static long worldSeed;
    public static TerrariaHelper instance;

    public TerrariaHelper() {
        super();
        instance = this;
        worldSeed = YmlHelper.getFile("plugins/Data/setting.yml").getLong("worldSeed", 114514);
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new playerKeyToggleListener(), this);
        Bukkit.getPluginManager().registerEvents(new RandomTitle(), this);
        Bukkit.getPluginManager().registerEvents(new WorldRegisterListener(), this);

        this.getCommand("findNoise").setExecutor(new NoiseGeneratorTest());

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
