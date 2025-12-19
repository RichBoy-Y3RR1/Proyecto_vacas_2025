package com.example.backend.dao;

import com.example.backend.models.AbstractUser;
import com.example.backend.models.CompanyUser;

import java.util.List;

public interface EmpresaDAO {
    Integer createCompany(String name, String email) throws Exception;
    CompanyUser findById(Integer id) throws Exception;
    List<CompanyUser> listAll() throws Exception;
}
