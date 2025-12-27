package com.example.backend.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import com.google.gson.Gson;

// Simple JWT utility that doesn't depend on external auth0 library at runtime.
// Uses HMAC-SHA256 signing with a JSON payload. Provides createToken and verify
// returning a compatible com.auth0.jwt.interfaces.DecodedJWT implemented in project.
public class JwtUtil {
    private static final String SECRET = System.getenv().getOrDefault("JWT_SECRET","change_this_secret");
    private static final long EXP_MS = 1000L * 60 * 60 * 24; // 24h
    private static final Gson gson = new Gson();

    public static String createToken(Map<String,Object> claims){
        long now = System.currentTimeMillis();
        String jti = java.util.UUID.randomUUID().toString();
        java.util.Map<String,Object> payload = new java.util.HashMap<>();
        payload.put("jti", jti);
        payload.put("iat", now);
        payload.put("exp", now + EXP_MS);
        if (claims != null) payload.putAll(claims);
        String header = base64Url(gson.toJson(java.util.Map.of("alg","HS256","typ","JWT")));
        String body = base64Url(gson.toJson(payload));
        String sig = hmacSha256(header + "." + body, SECRET);
        return header + "." + body + "." + sig;
    }

    public static DecodedJWT verify(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("invalid_token_format");
        String header = parts[0]; String body = parts[1]; String sig = parts[2];
        String expected = hmacSha256(header + "." + body, SECRET);
        if (!constantTimeEquals(expected, sig)) throw new IllegalArgumentException("invalid_signature");
        String json = new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8);
        java.util.Map map = gson.fromJson(json, java.util.Map.class);
        Double expD = map.containsKey("exp")? ((Number)map.get("exp")).doubleValue():null;
        if (expD != null){ long exp = expD.longValue(); if (System.currentTimeMillis() > exp) throw new IllegalArgumentException("token_expired"); }
        String jti = map.containsKey("jti") ? String.valueOf(map.get("jti")) : null;
        Date iat = map.containsKey("iat") ? new Date(((Number)map.get("iat")).longValue()) : null;
        Date exp = map.containsKey("exp") ? new Date(((Number)map.get("exp")).longValue()) : null;
        return new DecodedJWT(jti, iat, exp, map);
    }

    private static String base64Url(String s){ return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }

    private static String hmacSha256(String data, String key){
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (Exception e){ throw new RuntimeException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b){ if (a == null || b == null) return false; if (a.length() != b.length()) return false; int res = 0; for (int i=0;i<a.length();i++) res |= a.charAt(i) ^ b.charAt(i); return res == 0; }
}
