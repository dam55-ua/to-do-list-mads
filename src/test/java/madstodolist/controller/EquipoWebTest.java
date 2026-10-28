package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.TareaService;
import madstodolist.service.UsuarioService;
import madstodolist.service.EquipoService;
import madstodolist.dto.EquipoData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class EquipoWebTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EquipoService equipoService;

    @MockBean
    private ManagerUserSession managerUserSession;

    private Long addUsuarioBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario@ua.es");
        usuario.setPassword("123");
        return usuarioService.registrar(usuario).getId();
    }

    //Test de GET "/equipo"
    @Test
    public void listadoEquiposMuestraEquipos() throws Exception {
        // GIVEN
        Long usuarioId = addUsuarioBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Proyecto AAA"),
                        containsString("/equipos/" + equipo.getId())
                )));
    }

    @Test
    public void listadoEquiposVacioMuestraPagina() throws Exception {
        // GIVEN
        // Un usuario logueado y ningún equipo en la BD
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay equipos disponibles")));
    }

    @Test
    public void listadoEquiposDeniegaAccesoSinSesion() throws Exception {
        // GIVEN
        // managerUserSession.usuarioLogeado() devuelve null por defecto

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isUnauthorized());
    }

    //GET "/equipo/id"
    @Test
    public void detalleEquipoMuestraMiembros() throws Exception {
        // GIVEN
        Long usuarioId = addUsuarioBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuarioId);
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/" + equipo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Proyecto AAA"),
                        containsString("usuario@ua.es")
                )));
    }

    @Test
    public void detalleEquipoSinMiembrosMuestraMensaje() throws Exception {
        // GIVEN
        Long usuarioId = addUsuarioBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/" + equipo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Este equipo no tiene miembros")));
    }

    @Test
    public void detalleEquipoInexistenteDevuelve404() throws Exception {
        // GIVEN
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void detalleEquipoDeniegaAccesoSinSesion() throws Exception {
        // GIVEN
        // managerUserSession.usuarioLogeado() devuelve null por defecto

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/999"))
                .andExpect(status().isUnauthorized());
    }


}
