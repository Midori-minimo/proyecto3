package com.proyecto3.proyecto3.contra;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
public class PasswordUtil {

    private static final int ITERACIONES = 65536;
    private static final int LONGITUD_LLAVE = 256;

    public static String generarSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(String password, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERACIONES, LONGITUD_LLAVE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error al generar el hash de la contraseña", e);
        }
    }

    public static boolean verificarPassword(String passwordIngresada, String saltBase64, String hashGuardado) {
        String hashCalculado = hashPassword(passwordIngresada, saltBase64);
        return hashCalculado.equals(hashGuardado);
    }
}
