package dev.floffah.skillpaths;

import dev.floffah.skillpaths.commands.Skills;
import dev.floffah.skillpaths.gui.GUIEvents;
import dev.floffah.skillpaths.listeners.UserStuff;
import dev.floffah.skillpaths.skills.SkillType;
import dev.floffah.skillpaths.skills.types.Agility;
import dev.floffah.skillpaths.skills.types.Endurance;
import dev.floffah.skillpaths.user.UserStore;
import dev.floffah.skillpaths.util.Config;
import dev.floffah.skillpaths.util.Glow;
import dev.floffah.skillpaths.util.Messages;
import dev.floffah.util.items.NamespaceMap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SkillPaths extends JavaPlugin {
    private static Economy econ;
    public NamespaceMap keys;
    public UserStore users;

//    public File configfile;
//    public YamlConfiguration config;
//    public File messagesfile;
//    public YamlConfiguration messagesc;
//    public Messages messages;

    public Config config;
    public Messages messages;

    public Path userData;

    public SkillType[] skills;

    public static Economy getEcon() {
        return econ;
    }

    @Override
    public void onEnable() {
//        if (getServer().getPluginManager().getPlugin("FloffahUtil") == null) {
//            getLogger().warning("Couldnt not find plugin FloffahUtil.");
//            getServer().getPluginManager().disablePlugin(this);
//        }
        legacyFloffahUtilOnEnable();

        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
        } catch (Exception e) {
            System.err.println(e);
        }
        try {
            Glow glow = new Glow(keys.get("glow"));
            Enchantment.registerEnchantment(glow);
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        } catch (Exception e) {
            System.err.println(e);
        }

        skills = new SkillType[]{new Endurance(), new Agility()};

        for (SkillType skill : skills) {
            skill.init(this);
        }

        //vaultInit();
        postVault();
    }

    public void legacyFloffahUtilOnEnable() {
        if (!Files.exists(getDataFolder().toPath())) {
            getDataFolder().mkdir();
            getLogger().info("Created plugin directory");
        }
        userData = Paths.get(getDataFolder().toURI().toString(), "data");
        if (!Files.exists(userData)) {
            new File(userData.toString()).mkdir();
            getLogger().info("Created data directory");
        }
    }

    void vaultInit() {
        boolean did = true;
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            did = false;
            getLogger().severe("Vault is not present. Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            did = false;
            getLogger().severe("Vault economy was null. Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        econ = rsp.getProvider();
        postVault();
    }

    void postVault() {
        config = new Config(this);
        messages = new Messages(this);

        keys = new NamespaceMap(this);
        users = new UserStore(this);

        getServer().getPluginManager().registerEvents(new GUIEvents(this), this);
        getServer().getPluginManager().registerEvents(new UserStuff(this), this);
        getCommand("skillpaths").setExecutor(new Skills(this));

        if (!Files.exists(Paths.get("plugins/Skillpaths"))) {
            new File("plugins/SkillPaths").mkdirs();
        }
        if (!Files.exists(Paths.get(getDataFolder() + "/config.yml"))) {
            saveResource("config.yml", false);
        }
        if (!Files.exists(Paths.get(getDataFolder() + "/messages.yml"))) {
            saveResource("messages.yml", false);
        }
//        configfile = new File(getDataFolder() + "/config.yml");
//        messagesfile = new File(getDataFolder() + "/messages.yml");
//        config = YamlConfiguration.loadConfiguration(configfile);
//        messagesc = YamlConfiguration.loadConfiguration(messagesfile);
//        messages = new Messages(messagesc);


        getLogger().info("Enabled SkillPaths " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        users.users.keySet().forEach(uuid -> {
            boolean didrm = users.remove(uuid);
            Player plyr = getServer().getPlayer(uuid);
            if(!didrm && plyr != null) {
                getLogger().severe("Could not remove & save player " + plyr.getName() + " (" + plyr.getUniqueId() + ") although they are online");
            } else if(!didrm) {
                getLogger().warning("Could not remove & save player " + plyr.getName() + " (" + plyr.getUniqueId() + ") but they are offline so it shouldn't matter lol");
            }
        });
        getLogger().info("Disabled SkillPaths " + getDescription().getVersion());
    }
}
