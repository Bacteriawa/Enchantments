package dev.bacteriawa;

import org.bukkit.plugin.java.JavaPlugin;

public final class Enchantments extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new EnchantmentsExtractor(this), this);
        getLogger().info("Enchantments 加载完毕");
    }

    @Override
    public void onDisable() {
        getLogger().info("Enchantments 感谢使用");
    }
}
