package com.example.backend.services;

import com.example.backend.models.AbstractUser;

import java.util.List;

public interface UserService {
    Integer register(AbstractUser u) throws Exception;
    AbstractUser find(Integer id) throws Exception;
    AbstractUser findByEmail(String email) throws Exception;
    List<AbstractUser> listAll() throws Exception;
}
