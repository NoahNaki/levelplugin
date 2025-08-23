package me.nakilex.levelplugin.utils;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for looking up a player's country by IP address.
 * Results are cached for the lifetime of the JVM.
 */
public class GeoIpUtil {
    private static final String API = "http://ip-api.com/json/%s?fields=countryCode";
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private GeoIpUtil() {
        // Utility class
    }

    /**
     * Resolve the 2-letter country code for the given IP.
     *
     * @param ip IPv4/IPv6 address
     * @return country code (e.g. "US"), or null if unavailable
     */
    public static String getCountry(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        String cached = CACHE.get(ip);
        if (cached != null) {
            return cached;
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(String.format(API, ip)).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                String country = parseCountryCode(sb.toString());
                if (country != null) {
                    CACHE.put(ip, country);
                }
                return country;
            }
        } catch (IOException ex) {
            Bukkit.getLogger().warning("GeoIp lookup failed for " + ip + ": " + ex.getMessage());
            return null;
        }
    }

    private static String parseCountryCode(String json) {
        int idx = json.indexOf("\"countryCode\"");
        if (idx == -1) return null;
        int start = json.indexOf(':', idx);
        if (start == -1) return null;
        int q1 = json.indexOf('"', start + 1);
        if (q1 == -1) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 == -1) return null;
        return json.substring(q1 + 1, q2);
    }
}
