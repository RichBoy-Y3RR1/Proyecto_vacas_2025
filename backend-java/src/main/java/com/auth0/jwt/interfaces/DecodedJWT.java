package com.auth0.jwt.interfaces;

import java.util.Date;
import java.util.Map;

public class DecodedJWT {
    private final String jti;
    private final Date issuedAt;
    private final Date expiresAt;
    private final Map<String,Object> claims;

    public DecodedJWT(String jti, Date iat, Date exp, Map<String,Object> claims){ this.jti = jti; this.issuedAt = iat; this.expiresAt = exp; this.claims = claims; }
    public String getId(){ return jti; }
    public Date getIssuedAt(){ return issuedAt; }
    public Date getExpiresAt(){ return expiresAt; }
    public Claim getClaim(String name){ return new Claim(claims == null ? null : claims.get(name)); }
}
