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

    /** Genera un salt aleatorio nuevo, codificado en Base64 para poder guardarlo como texto. */
    public static String generarSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Genera el hash de una contraseña en texto plano usando el salt indicado. */
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

    /** Compara una contraseña en texto plano contra el hash guardado, usando el mismo salt. */
    public static boolean verificarPassword(String passwordIngresada, String saltBase64, String hashGuardado) {
        String hashCalculado = hashPassword(passwordIngresada, saltBase64);
        return hashCalculado.equals(hashGuardado);
    }
}
