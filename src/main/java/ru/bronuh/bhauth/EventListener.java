package ru.bronuh.bhauth;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryView;

import java.util.logging.Logger;

/**
 * Слушает основные действия игрока и проверяет возможность их выполнения
 */
public class EventListener implements Listener {

	private final Config config;
	private final AuthManager authManager;
	private final Logger log;
	private BhAuth bhAuth;

	private TextColor warnColor = TextColor.color(255,100,100),
			okColor = TextColor.color(100,255,100);

	@Deprecated
	public EventListener(Config config, AuthManager authManager, Logger log){
		this.authManager = authManager;
		this.log = log;
		this.config = config;
	}

	public EventListener(PluginContext context) {
		config = context.getConfig();
		log = context.getLog();
		authManager = context.getAuthManager();
		bhAuth = context.getBhAuth();

		Bukkit.getScheduler().scheduleSyncRepeatingTask(bhAuth,()->{
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if(!authManager.isLoggedIn(onlinePlayer.getName())){
					onlinePlayer.setRemainingAir(onlinePlayer.getMaximumAir());
				}
			}
		},0,40);
	}




	// =============================================================================
	// ============================== EVENT LISTENERS ==============================
	// =============================================================================

	private boolean isAllowed(String commandText){
		String[] whitelist = {"/help","/reg","/log"};
		for(String prefix:whitelist){
			if(commandText.toLowerCase().startsWith(prefix)){
				return true;
			}
		}
		return false;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		String cmd = event.getMessage();
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name) && !isAllowed(cmd)){
			player.sendMessage(Component.text("Вы не можете использовать команды, пока не авторизуетесь", warnColor));
			event.setCancelled(true);
		}
	}


	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();
		String ip = player.getAddress().getAddress().toString();

		authManager.autoLoginUser(name,ip);
		if(!authManager.isLoggedIn(name)){
			if(authManager.isCached(name)){
				player.sendMessage(Component.text("Войдите в игру с помощью /log <пароль>", warnColor));
			}else{
				player.sendMessage(Component.text("Сначала необходимо зарегистрироваться! Используйте /reg <пароль> "+(config.fastRegistration ? "" : "<пароль> ")+"для регистрации", warnColor));
			}
		}else{
			player.sendMessage(Component.text("Вы автоматически авторизованы", okColor));
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent event) {
		authManager.playerOut(event.getPlayer().getName());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncChatEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();
		if(!authManager.isLoggedIn(name)&&!config.allowChat){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете писать в чат, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();


		Location from = event.getFrom();
		Location to = event.getTo();
		if (to == null) {
			return;
		}

		/*
		 * Честно спижжено с AuthMe Reloaded
		 * Limit player X and Z movements to 1 block
		 * Deny player Y+ movements (allows falling)
		 */

		if (from.getBlockX() == to.getBlockX()
				&& from.getBlockZ() == to.getBlockZ()
				&& from.getY() - to.getY() >= 0) {
			return;
		}

		if(!authManager.isLoggedIn(name)){
			event.setTo(event.getFrom());
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerHitPlayerEvent(EntityDamageByEntityEvent event) {
		if(event.getEntity() instanceof Player player){
			String name = player.getName();

			if(!authManager.isLoggedIn(name)){
				event.setCancelled(true);
				player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerDamageEvent(EntityDamageEvent event) {
		if(event.getEntity() instanceof Player player){
			String name = player.getName();

			if(!authManager.isLoggedIn(name)&&config.loggedOutGodmode){
				event.setCancelled(true);
				//player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
			}
		}
	}



	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerShear(PlayerShearEntityEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerFish(PlayerFishEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerEditBook(PlayerEditBookEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onSignChange(SignChangeEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerPickupItem(EntityPickupItemEvent event) {
		if(event.getEntity() instanceof Player player){
			String name = player.getName();

			if(!authManager.isLoggedIn(name)){
				event.setCancelled(true);
				player.sendMessage(Component.text("Вы не можете взаимодействовать с объектами, пока не авторизуетесь", warnColor));
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете выбрасывать предметы, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerHeldItem(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете использовать предметы, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете использовать предметы, пока не авторизуетесь", warnColor));
		}
	}

	private boolean isInventoryWhitelisted(InventoryView inventory) {
		if (inventory == null) {
			return false;
		}
		return false;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInventoryOpen(InventoryOpenEvent event) {
		final HumanEntity player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете использовать инвентарь, пока не авторизуетесь", warnColor));
			/*
			 * @note little hack cause InventoryOpenEvent cannot be cancelled for
			 * real, cause no packet is sent to server by client for the main inv
			 */
			Bukkit.getScheduler().scheduleSyncDelayedTask(bhAuth, player::closeInventory, 1);
		}


	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInventoryClick(InventoryClickEvent event) {
		HumanEntity player = event.getWhoClicked();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете брать предметы, пока не авторизуетесь", warnColor));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
		Player player = event.getPlayer();
		String name = player.getName();

		if(!authManager.isLoggedIn(name)){
			event.setCancelled(true);
			player.sendMessage(Component.text("Вы не можете использовать предметы, пока не авторизуетесь", warnColor));
		}
	}
}
