package pims;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** SHA-256 hashing so plain-text passwords are never stored in the database. */
public final class PasswordUtil {
    private PasswordUtil() { }

    public static String sha256(String text) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
