package com.example.backend.integration;

import com.example.backend.models.Gamer;
import com.example.backend.models.AbstractUser;
import com.example.backend.models.Videojuego;
import com.example.backend.services.EmpresaService;
import com.example.backend.services.UsuarioService;
import com.example.backend.services.VideojuegoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class ServiceIntegrationTest {

    @Test
    public void fullCrudFlows() throws Exception {
        EmpresaService es = new EmpresaService();
        VideojuegoService vs = new VideojuegoService();
        UsuarioService us = new UsuarioService();

        Integer companyId = es.createCompany("TestCo","testco@example.com");
        Assertions.assertNotNull(companyId);

        Videojuego v = new Videojuego();
        v.setNombre("Test Game");
        v.setDescripcion("Desc");
        v.setPrecio(new BigDecimal("9.99"));
        v.setEdad_clasificacion("+18");

        Integer vid = vs.create(v);
        Assertions.assertNotNull(vid);

        Videojuego found = vs.getById(vid);
        Assertions.assertNotNull(found);
        Assertions.assertEquals("Test Game", found.getNombre());

        v.setNombre("Updated Game");
        boolean updated = vs.update(vid, v);
        Assertions.assertTrue(updated);

        boolean forSale = vs.setForSale(vid, true);
        Assertions.assertTrue(forSale);

        boolean deleted = vs.delete(vid);
        Assertions.assertTrue(deleted);

        Gamer g = new Gamer();
        g.setEmail("gamer@example.com");
        g.setPasswordHash("pwd");
        g.setNickname("player1");
        Integer uid = us.create(g);
        Assertions.assertNotNull(uid);

        AbstractUser fetched = us.getById(uid);
        Assertions.assertNotNull(fetched);
        Assertions.assertTrue(fetched instanceof Gamer);

        boolean udel = us.delete(uid);
        Assertions.assertTrue(udel);
    }
}
