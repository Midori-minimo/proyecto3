package com.proyecto3.proyecto3.seguridad;

import com.proyecto3.proyecto3.contra.PasswordUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderPBKDF2 implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        String salt = PasswordUtil.generarSalt();
        String hash = PasswordUtil.hashPassword(rawPassword.toString(), salt);
        return hash + ":" + salt;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String storedPassword) {
        if (storedPassword == null || !storedPassword.contains(":")) {
            return false;
        }
        String[] partes = storedPassword.split(":", 2);
        if (partes.length != 2) return false;
        String hashGuardado = partes[0];
        String salt = partes[1];
        return PasswordUtil.verificarPassword(rawPassword.toString(), salt, hashGuardado);
    }
}
