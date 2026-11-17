package eu.puhoi.productie;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Credentials stay on device, encrypted with a non-exportable Android Keystore key. */
final class SecureCredentials {
    private static final String ALIAS = "puhoi_http_basic_v1";
    private final SharedPreferences prefs;
    SecureCredentials(Context context) {
        prefs = context.getSharedPreferences("private_auth", Context.MODE_PRIVATE);
    }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }
    void save(String username, String password) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        String raw = username + "\n" + password;
        byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));
        prefs.edit()
                .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString("data", Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply();
    }
    String[] read() {
        String iv = prefs.getString("iv", null), data = prefs.getString("data", null);
        if (iv == null || data == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(),
                    new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            String[] parts = new String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)),
                    StandardCharsets.UTF_8).split("\n", 2);
            return parts.length == 2 ? parts : null;
        } catch (Exception e) { clear(); return null; }
    }
    void clear() { prefs.edit().clear().apply(); }
}
