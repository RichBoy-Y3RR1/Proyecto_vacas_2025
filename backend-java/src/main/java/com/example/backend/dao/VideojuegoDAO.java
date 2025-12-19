package com.example.backend.dao;

import com.example.backend.models.Videojuego;
import java.util.List;

public interface VideojuegoDAO {
    List<Videojuego> listAll() throws Exception;
    Integer create(Videojuego data) throws Exception;
    boolean update(Integer id, Videojuego data) throws Exception;
    boolean setForSale(Integer id, boolean forSale) throws Exception;
}
