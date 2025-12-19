package com.example.backend.servlets;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class BaseServlet extends HttpServlet {
    protected final Gson gson = new Gson();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setCors(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    protected void setCors(HttpServletResponse resp){
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type,Authorization");
        resp.setContentType("application/json;charset=UTF-8");
    }

    protected void writeJson(HttpServletResponse resp, Object obj) throws IOException {
        setCors(resp);
        try (PrintWriter out = resp.getWriter()){
            out.print(gson.toJson(obj));
        }
    }

    protected Integer parseId(HttpServletRequest req){
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) return null;
        try {
            if (path.startsWith("/")) path = path.substring(1);
            return Integer.parseInt(path.split("/")[0]);
        } catch (Exception e){
            return null;
        }
    }
}
