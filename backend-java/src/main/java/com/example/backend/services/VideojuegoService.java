package com.example.backend.services;

import com.example.backend.dao.JdbcVideojuegoDAO;
import com.example.backend.dao.VideojuegoDAO;
import com.example.backend.models.Videojuego;

import java.util.List;

public class VideojuegoService {
    private final VideojuegoDAO dao = new JdbcVideojuegoDAO();

    public List<Videojuego> listAll() throws Exception { return dao.listAll(); }
    public Videojuego getById(Integer id) throws Exception { return dao.findById(id); }
    public Integer create(Videojuego data) throws Exception { return dao.create(data); }
    public boolean update(Integer id, Videojuego data) throws Exception { return dao.update(id, data); }
    public boolean delete(Integer id) throws Exception { return dao.delete(id); }
    public boolean setForSale(Integer id, boolean forSale) throws Exception { return dao.setForSale(id, forSale); }
}
