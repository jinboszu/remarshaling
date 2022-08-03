package util;

import com.google.gson.Gson;

public class Util {
    public static final Gson gson = new Gson();

    public static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
