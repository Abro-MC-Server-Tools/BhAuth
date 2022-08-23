package ru.bronuh.bhauth;

public class AuthState {
	public String name;
	public long lastSeen;
	public String lastIp;
	public boolean isLoggedIn = false;
	public int authAttempts = 0;
}
