package ru.bronuh.bhauth;

/**
 * Хранит информацию об игроке
 */
public class AuthData {
    // Имя пользователя. Всегда должно быть в нижнем регистре
    public String playerName;

    // Пароль рользователя. Не шифруется (пока), так что админы смогут посмотреть пароль в файле
    public String password;
}
