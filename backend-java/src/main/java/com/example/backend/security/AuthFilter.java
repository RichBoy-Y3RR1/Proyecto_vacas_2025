package com.example.backend.security;

import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.io.IOException;

@WebFilter(urlPatterns = {"/api/*"})
public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    private boolean isPublicPath(HttpServletRequest req){
        String path = req.getRequestURI();
        String method = req.getMethod();
        // allow auth endpoints and public GETs
        if (path.startsWith(req.getContextPath() + "/api/auth")) return true;
        if ("GET".equalsIgnoreCase(method) && (path.startsWith(req.getContextPath() + "/api/videojuegos") || path.startsWith(req.getContextPath() + "/api/categorias") || path.startsWith(req.getContextPath() + "/api/banner"))) return true;
        return false;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (isPublicPath(req)) { chain.doFilter(request, response); return; }

        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")){ resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); resp.setContentType("application/json;charset=UTF-8"); resp.getWriter().print("{\"error\":\"missing_token\"}"); return; }
        String token = auth.substring("Bearer ".length());
        try {
            DecodedJWT dj = JwtUtil.verify(token);
            // check revocation
            String jti = dj.getId();
            com.example.backend.dao.RevokedTokenDAO rdao = new com.example.backend.dao.RevokedTokenDAO();
            if (jti != null && rdao.isRevoked(jti)){
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print("{\"error\":\"token_revoked\"}");
                return;
            }
            // attach claims
            String role = dj.getClaim("role").asString();
            Integer userId = dj.getClaim("userId").asInt();
            if (role != null) req.setAttribute("role", role);
            if (userId != null) req.setAttribute("userId", userId);
            if (dj.getClaim("companyId") != null && !dj.getClaim("companyId").isNull()) req.setAttribute("companyId", dj.getClaim("companyId").asInt());
            chain.doFilter(request, response);
        } catch (Exception e){
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().print("{\"error\":\"invalid_token\"}");
        }
    }

    @Override
    public void destroy() { }
}
