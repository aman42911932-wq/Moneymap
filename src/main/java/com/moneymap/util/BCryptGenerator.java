package com.moneymap.util;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptGenerator {
    public static void main(String[] args) {
        String adminHash = BCrypt.hashpw("admin123", BCrypt.gensalt(10));
        String userHash = BCrypt.hashpw("user123", BCrypt.gensalt(10));
        System.out.println("BCRYPT_ADMIN_HASH=" + adminHash);
        System.out.println("BCRYPT_USER_HASH=" + userHash);
    }
}
