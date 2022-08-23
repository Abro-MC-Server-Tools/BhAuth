package ru.bronuh.bhauth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Основной класс плагина
 */
public final class BhAuth extends JavaPlugin {

	String pluginDir = getDataFolder().getPath();
	Logger log = getLogger();
	Config config = new Config();
	AuthManager authManager = new AuthManager(pluginDir, log, config);
	PluginContext pluginContext = new PluginContext(config, authManager, log, pluginDir, this);

	@Override
	public void onEnable() {
		log.info("Запущен плагин авторизации");
		loadConfig();
		authManager.prepareAuthManager();

		AuthCmd authCmd = new AuthCmd(pluginContext);
		getServer().getPluginManager().registerEvents(new EventListener(pluginContext),this);
		getServer().getPluginCommand("register").setExecutor(authCmd);
		getServer().getPluginCommand("login").setExecutor(authCmd);
	}


	@Override
	public void onDisable() {
		// Магия ничегонеделания
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.sendMessage(Component.text("Плагин перезагружен. Необходимо снова авторизоваться", TextColor.color(255,100,100)));
		}
		// TODO: Сделать че-нить
	}


	private void loadConfig(){
		// TODO: Покурить рефлексию и автоматизировать работу с конфигом

		// Пытается получить конфиг из файла. Если файла нет - конфиг пуст
		FileConfiguration fileConfig = getConfig();

		// Назначение стандартных значений в конфиг
		fileConfig.addDefault("allowFailedRegistration",config.allowFailedRegistration);
		fileConfig.addDefault("attempts",config.attempts);
		fileConfig.addDefault("autoAuthTime",config.autoAuthTime);
		fileConfig.addDefault("fastRegistration",config.fastRegistration);
		fileConfig.addDefault("loggedOutGodmode",config.loggedOutGodmode);
		fileConfig.addDefault("allowChat",config.allowChat);

		// В случае появления недостающих параметров в конфиге - они будут дописаны
		fileConfig.options().copyDefaults(true);
		saveConfig();

		// Перенос значений из загружнного файла в экземпляр конфига
		config.allowFailedRegistration = fileConfig.getBoolean("allowFailedRegistration");
		config.attempts = fileConfig.getInt("attempts");
		config.fastRegistration = fileConfig.getBoolean("fastRegistration");
		config.autoAuthTime = fileConfig.getInt("autoAuthTime");
		config.loggedOutGodmode = fileConfig.getBoolean("loggedOutGodmode");
		config.allowChat = fileConfig.getBoolean("allowChat");
	}
}
