package com.auth0.jwt.interfaces;

public class Claim {
    private final Object value;
    public Claim(Object value){ this.value = value; }
    public boolean isNull(){ return value == null; }
    public String asString(){ return value == null ? null : String.valueOf(value); }
    public Integer asInt(){ if (value instanceof Number) return ((Number)value).intValue(); try { return value==null?null:Integer.valueOf(String.valueOf(value)); } catch(Exception e){ return null; } }
}
