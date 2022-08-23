package ru.bronuh.bhauth;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Хранит информацию об игроках и их состоянии авторизации. Обеспечивает авторизацию и регистрацию пользователей.
 */
public class AuthManager {
	private final Map<String, AuthData> profilesCache = new ConcurrentHashMap<>();
	private final Map<String, AuthState> states = new ConcurrentHashMap<>();

	private final String profilesDir;
	private final Logger log;
	private boolean isReady = false;
	private Config config;



	public AuthManager(String pluginDir, Logger log, Config config){
		this.profilesDir = new File(pluginDir,"profiles").getPath();
		this.log = log;
		this.config = config;
	}

	/*
	Проклятое дерьмо, не трогай
	public AuthManager(PluginContext context){
		profilesDir = new File(context.getPluginDir(),"profiles").getPath();
		log = context.getLog();
		config = context.getConfig();
	}*/




	/**
	 * Подготавливает менеджер авторизации, проверяя директорию с профилями игроков и загружая их в кэш
	 */
	public void prepareAuthManager(){
		checkProfilesDir();
		loadAuthData();
	}




	/**
	 * Записывает время выхода и IP игрока для реализации автоматического входа
	 * @param name имя вышедшего игрока
	 */
	public void playerOut(String name){
		try{
			AuthState state = states.get(name.toLowerCase());
			state.lastSeen = Calendar.getInstance().getTimeInMillis();
			state.isLoggedIn = false;
		}catch(Exception e){
			log.warning("Не удалось обновить состояние игрока в AuthManager.playerOut(\""+name+"\"): \n"+e.getMessage());
		}
	}




	/**
	 * Проверяет наличие профиля игрока в кэше. В некоторых случаях наличие профиля в кэше не означает наличие файла (см. Config.allowFailedRegistration)
	 * @param name имя искомого игрока
	 * @return true, если в кэше есть такой ключ
	 */
	public boolean isCached(String name){
		return profilesCache.containsKey(name.toLowerCase());
	}



	/**
	 * Проверяет состояние авторизации игрока
	 * @param name имя пользователя
	 * @return true, если игрок авторизован
	 */
	public boolean isLoggedIn(String name){
		if(states.containsKey(name.toLowerCase())){
			return states.get(name.toLowerCase()).isLoggedIn;
		}
		return false;
	}


	/**
	 * Проверяет наличие попыток авторизации у пользователя
	 * @param name имя пользователя
	 * @return true, если совершенных попыток меньше, чем указано в конфиге
	 */
	public boolean hasAttempts(String name){
		if(states.containsKey(name.toLowerCase())){
			return states.get(name.toLowerCase()).authAttempts < config.attempts;
		}
		return true;
	}




	/**
	 * Сбрасывает попытки авторизации пользователя. Вызывается после исключения игрока с сервера
	 * @param name имя пользователя
	 */
	public void resetAttempts(String name){
		if(states.containsKey(name.toLowerCase())){
			states.get(name.toLowerCase()).authAttempts = 0;
		}
	}




	/**
	 * Проводит авторизацию пользователя
	 * @param name имя пользователя
	 * @param password предоставленный пароль
	 * @param ip IP с которого производится подключение
	 * @return Строка-ответ для возврата в чат
	 */
	public String loginUser(String name, String password, String ip){
		AuthState authState = states.get(name.toLowerCase());
		AuthData authData = profilesCache.get(name.toLowerCase());
		long now = Calendar.getInstance().getTimeInMillis();

		if(authState != null){
			if(authState.isLoggedIn){
					return "Вы уже авторизованы";
			}
		}

		if(authData != null){
			AuthState state = new AuthState();
			state.name = name;
			state.authAttempts = 1;
			state.lastSeen = now;
			state.lastIp = ip;
			state.isLoggedIn = false;
			states.put(name.toLowerCase(),state);

			if(authData.password.equals(password)){
				state.isLoggedIn = true;
				state.authAttempts = 0;
			}else{
				state.authAttempts++;
				return "Неверный пароль!";
			}
		}else{
			return "Сначала необходимо зарегистрироваться! Используйте /reg <пароль> "+(config.fastRegistration ? "" : "<пароль> ")+"для регистрации";
		}

		return "Вы вошли в игру";
	}




	/**
	 * Пробует провести атоматическую авторизацию
	 * @param name имя пользоателя
	 * @param ip IP адрес пользователя
	 * @return true, если автоматическая авторизация произведена успешно.
	 */
	public boolean autoLoginUser(String name, String ip){
		AuthState authState = states.get(name.toLowerCase());
		long now = Calendar.getInstance().getTimeInMillis();
		if(authState != null){
			if(authState.lastIp.equals(ip) && (now-authState.lastSeen)/1000 < config.autoAuthTime){
				authState.isLoggedIn = true;
				return true;
			}
		}
		return false;
	}




	/**
	 * Регистрирует нового пользователя, сохраняя его данные в файл
	 * @param name имя пользователя. Приводится к нижнему регистру
	 * @param password пароль
	 * @param ip IP адрес пользователя
	 * @return строка со статусом регистрации
	 */
	public String registerUser(String name, String password, String ip){
		if(profilesCache.containsKey(name.toLowerCase())){
			return "Игрок с таким именем уже зарегистрирован";
		}

		// Подготовка профиля пользователя
		AuthData authData = new AuthData();
		authData.playerName = name.toLowerCase();
		authData.password = password;

		Path path = new File(profilesDir,name+".json").toPath();
		String content = new Gson().toJson(authData);

		// Создание профиля и добавление его в кэш. В случае успеха пользователь может продолжить играть если это разрешено в настройках плагина
		try{
			profilesCache.put(authData.playerName,authData);
		}catch (Exception e){
			log.warning("Ошибка при попытке регистрации пользователя "+name+":\n"+e.getMessage());
			return "Ошибка при попытке регистрации. Обратитесь к администрации за помощью (Ошибка при кэшировании).";
		}

		// Подготовка состояния пользователя, если профиль был успешно создан
		AuthState authState = new AuthState();
		authState.name = name.toLowerCase();
		authState.authAttempts = 0;

		// Запись профиля в файл
		try {
			Files.writeString(path, content, StandardOpenOption.CREATE);
		} catch (IOException e) {
			log.warning("Ошибка при попытке регистрации пользователя "+name+":\n"+e.getMessage()+"\n");
			e.printStackTrace();
			String whenAllowed = "";
			// В случае, если играть без сохранения профиля запрещено, профиль удаляется. Игрок не может продолжать играть.
			if(!config.allowFailedRegistration){
				profilesCache.remove(authData.playerName);
				return "Ошибка при попытке регистрации. Обратитесь к администрации за помощью (Ошибка при записи в файл).\n"+whenAllowed;
			}else{
				whenAllowed = "Тем не менее, временный профиль был создан. Ваш персонаж будет защищен паролем до следующего перезапуска.";
			}
			return "Ошибка при попытке регистрации. Обратитесь к администрации за помощью (Ошибка при записи в файл).\n"+whenAllowed;
		}
		authState.isLoggedIn = true;
		authState.lastSeen = Calendar.getInstance().getTimeInMillis();
		authState.lastIp = ip;
		states.put(authState.name,authState);
		return "Регистрация завершена";
	}




	/**
	 * Загружает профили игроков и кэширует их
	 */
	private void loadAuthData(){
		if(isReady)
			return;

		File dir = new File(profilesDir);
		File[] files = dir.listFiles(file -> (file.isFile()&&file.getName().toLowerCase().endsWith(".json")));
		Gson gson = new Gson();

		for (File file:files) {
			try {
				AuthData data = gson.fromJson(Files.readString(file.toPath()),AuthData.class);
				profilesCache.put(data.playerName.toLowerCase(),data);
			} catch (IOException e) {
				log.warning("Не удалось прочитать файл "+file.getName()+": "+e.getMessage());
			}
		}
		isReady = true;
	}




	/**
	 * Обеспечивает наличие директории для хранения профилей пользователей
	 */
	private void checkProfilesDir(){
		File dir = new File(profilesDir);
		try{
			dir.mkdir();
		}catch (Exception e){
			log.warning("Не удалось создать директорию "+profilesDir+": "+e.getMessage());
		}
	}
}
