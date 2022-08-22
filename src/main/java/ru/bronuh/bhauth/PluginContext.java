package ru.bronuh.bhauth;

import java.util.logging.Logger;

// Хранит в себе ссылки на основные зависимости.
public class PluginContext {
	private AuthManager authManager;
	private Logger log;
	private Config config;
	private String pluginDir;
	private BhAuth bhAuth;

	public PluginContext(Config config, AuthManager authManager, Logger log, String pluginDir, BhAuth bhAuth) {
		this.authManager = authManager;
		this.log = log;
		this.config = config;
		this.pluginDir = pluginDir;
		this.bhAuth = bhAuth;
	}

	public String getPluginDir() {
		return pluginDir;
	}

	public Config getConfig() {
		return config;
	}

	public Logger getLog() {
		return log;
	}

	public AuthManager getAuthManager() {
		return authManager;
	}

	public BhAuth getBhAuth() {
		return bhAuth;
	}
}
