package terraria.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class YmlHelper {
    static HashMap<String, ConfigurationSection> ymlCache = new HashMap<>();

    public static ConfigurationSection getFile(String filePath) {
        if (ymlCache.containsKey(filePath))
            return ymlCache.get(filePath);

        ConfigurationSection fileContent = YamlConfiguration.loadConfiguration(new File(filePath));
        ymlCache.put(filePath, fileContent);
        return fileContent;
    }
    // getter
    public static ConfigurationSection getSection(String filePath, String nodeName) {
        return getFile(filePath).getConfigurationSection(nodeName);
    }
}
