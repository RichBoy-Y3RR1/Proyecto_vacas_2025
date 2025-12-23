package com.example.backend.services;

import com.example.backend.dao.EmpresaDAO;
import com.example.backend.dao.JdbcEmpresaDAO;
import com.example.backend.models.CompanyUser;

import java.util.List;

public class EmpresaService {
    private final EmpresaDAO dao = new JdbcEmpresaDAO();

    public Integer createCompany(String name, String email) throws Exception { return dao.createCompany(name,email); }
    public CompanyUser getById(Integer id) throws Exception { return dao.findById(id); }
    public List<CompanyUser> listAll() throws Exception { return dao.listAll(); }
    public boolean update(Integer id, CompanyUser data) throws Exception { return dao.update(id, data); }
    public boolean delete(Integer id) throws Exception { return dao.delete(id); }
}
