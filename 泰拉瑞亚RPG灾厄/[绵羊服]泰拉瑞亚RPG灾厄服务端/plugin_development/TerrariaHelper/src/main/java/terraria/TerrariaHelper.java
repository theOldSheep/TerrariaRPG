package terraria;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import terraria.dragoncorehelper.RandomTitle;
import terraria.dragoncorehelper.playerKeyToggleListener;
import terraria.util.PlayerHelper;
import terraria.worldgen.WorldRegisterListener;
import terraria.util.YmlHelper;
import terraria.worldgen.overworld.NoiseGeneratorTest;


public class TerrariaHelper extends JavaPlugin {
    public static class Constants {
        public static final String WORLD_NAME_SURFACE = "world_surface", WORLD_NAME_CAVERN = "world_cavern", WORLD_NAME_UNDERWORLD = "world_underworld";
    }
    public static long worldSeed;
    public static TerrariaHelper instance;

    public TerrariaHelper() {
        super();
        instance = this;
        worldSeed = YmlHelper.getFile("plugins/Data/setting.yml").getLong("worldSeed", 114514);
    }

    public void initThreads() {
        YmlHelper.threadSaveYml();
        PlayerHelper.threadGrapplingHook();
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new playerKeyToggleListener(), this);
        Bukkit.getPluginManager().registerEvents(new RandomTitle(), this);
        Bukkit.getPluginManager().registerEvents(new WorldRegisterListener(), this);

        initThreads();

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
