package com.example.backend.services;

import com.example.backend.dao.JdbcUserDAO;
import com.example.backend.dao.UserDAO;
import com.example.backend.models.AbstractUser;

import java.util.List;

public class UsuarioService {
    private final UserDAO dao = new JdbcUserDAO();

    public Integer create(AbstractUser user) throws Exception { return dao.create(user); }
    public AbstractUser getById(Integer id) throws Exception { return dao.findById(id); }
    public AbstractUser getByEmail(String email) throws Exception { return dao.findByEmail(email); }
    public List<AbstractUser> listAll() throws Exception { return dao.listAll(); }
    public boolean update(AbstractUser user) throws Exception { return dao.update(user); }
    public boolean delete(Integer id) throws Exception { return dao.delete(id); }
}
