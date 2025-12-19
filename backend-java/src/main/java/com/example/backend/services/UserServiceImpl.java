package com.example.backend.services;

import com.example.backend.dao.JdbcUserDAO;
import com.example.backend.dao.UserDAO;
import com.example.backend.models.AbstractUser;

import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserDAO dao = new JdbcUserDAO();

    @Override
    public Integer register(AbstractUser u) throws Exception {
        // encapsulación: sólo el servicio maneja validaciones

        return dao.create(u);
    }

    @Override
    public AbstractUser find(Integer id) throws Exception { return dao.findById(id); }

    @Override
    public AbstractUser findByEmail(String email) throws Exception { return dao.findByEmail(email); }

    @Override
    public List<AbstractUser> listAll() throws Exception { return dao.listAll(); }
}
