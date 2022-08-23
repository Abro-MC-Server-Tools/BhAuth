package ru.bronuh.bhauth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Logger;


/**
 * Обработчик команд плагина
 */
public class AuthCmd implements CommandExecutor {
    Config config;
    AuthManager authManager;
    Logger log;
    private TextColor warnColor = TextColor.color(255,100,100),
            okColor = TextColor.color(100,255,100);


    @Deprecated
    public AuthCmd(Config config, AuthManager authManager, Logger log) {
        this.authManager = authManager;
        this.log = log;
        this.config = config;
    }

    public AuthCmd(PluginContext context) {
        this.authManager = context.getAuthManager();
        this.log = context.getLog();
        this.config = context.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String str, String[] args) {
        if (sender instanceof Player player) {
            String name = player.getName();
            String ip = player.getAddress().getAddress().toString();
            int argsCount = args.length;
            String password = argsCount > 0 ? args[0] : "";
            String passwordRepeat = argsCount > 1 ? args[1] : "";

            if(cmd.getName().equals("register")){
                if(!config.fastRegistration){
                    if(argsCount<2 || !password.equals(passwordRepeat)){
                        player.sendMessage(Component.text("Регистрация не удалась. Пароль должен быть указан дважды: /reg <pass> <pass>",warnColor));
                        return false;
                    }
                }
                player.sendMessage(authManager.registerUser(name,password,ip));
                return true;
            }



            if(cmd.getName().equals("login")){
                if(!authManager.isCached(name)) {
                    player.sendMessage(Component.text("Вы не зарегистрированы!",warnColor));
                }
                player.sendMessage(authManager.loginUser(name,password,ip));
                if(!authManager.hasAttempts(name)){
                    player.kick(Component.text("Превышено количество попыток авторизации",warnColor));
                    authManager.resetAttempts(name);
                }
                return true;
            }
        }
        return false; //вернем "ложь" если команда выполнена неправильно (настраивается в plugin.yml)
    }



}
