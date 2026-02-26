package org.jeecg.common.license.core;

public class LicenseKeyValidator {

    private static final String BASE32_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static boolean validate(String key) {
        if (key == null || !key.matches("^LIC-[A-Z0-9]{4}-[A-Z0-9]{16}-[A-Z0-9]{2}$")) {
            return false;
        }
        String[] parts = key.split("-");
        String code = parts[1];
        String random = parts[2];
        String check = parts[3];
        return check.equals(luhnChecksum(code + random));
    }

    private static String luhnChecksum(String input) {
        int sum = 0;
        for (int i = 0; i < input.length(); i++) {
            int val = BASE32_CHARS.indexOf(input.charAt(i));
            if (val < 0) val = input.charAt(i) - 'A';
            if (i % 2 == 0) {
                val *= 2;
                if (val > 31) val -= 31;
            }
            sum += val;
        }
        int check = (32 - (sum % 32)) % 32;
        int c1 = check / BASE32_CHARS.length();
        int c2 = check % BASE32_CHARS.length();
        return "" + BASE32_CHARS.charAt(Math.abs(c1) % BASE32_CHARS.length())
                + BASE32_CHARS.charAt(Math.abs(c2) % BASE32_CHARS.length());
    }
}
