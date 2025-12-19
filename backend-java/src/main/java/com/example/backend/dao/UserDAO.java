package com.example.backend.dao;

import com.example.backend.models.AbstractUser;

import java.util.List;

public interface UserDAO {
    Integer create(AbstractUser user) throws Exception;
    AbstractUser findById(Integer id) throws Exception;
    AbstractUser findByEmail(String email) throws Exception;
    List<AbstractUser> listAll() throws Exception;
    boolean update(AbstractUser user) throws Exception;
    boolean delete(Integer id) throws Exception;
}
