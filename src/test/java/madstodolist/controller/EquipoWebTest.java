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

import madstodolist.repository.EquipoRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

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

    @Autowired
    private EquipoRepository equipoRepository;

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

    //Test de GET "/equipo/id"
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

    //Test de POST "/equipos"

    @Test
    public void crearEquipoRedirigeYGuarda() throws Exception {
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        this.mockMvc.perform(post("/equipos").param("nombre", "Proyecto CCC"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"));

        assertThat(equipoService.findAllOrdenadoPorNombre())
                .extracting(EquipoData::getNombre)
                .contains("Proyecto CCC");
    }

    @Test
    public void crearEquipoConNombreVacioNoCreaEquipo() throws Exception {
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        this.mockMvc.perform(post("/equipos").param("nombre", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"));

        assertThat(equipoRepository.count()).isZero();
    }

    @Test
    public void crearEquipoSinSesionDevuelve401() throws Exception {
        this.mockMvc.perform(post("/equipos").param("nombre", "Proyecto CCC"))
                .andExpect(status().isUnauthorized());
    }

    //Test de POST "equipo/id/unirse"
    @Test
    public void unirseEquipoRedirigeYAnadeUsuario() throws Exception {
        Long usuarioId = addUsuarioBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/unirse"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos/" + equipo.getId()));

        assertThat(equipoService.usuariosEquipo(equipo.getId()))
                .extracting(UsuarioData::getId)
                .contains(usuarioId);
    }

    @Test
    public void unirseEquipoInexistenteDevuelve404() throws Exception {
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        this.mockMvc.perform(post("/equipos/999/unirse"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void unirseEquipoSinSesionDevuelve401() throws Exception {
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/unirse"))
                .andExpect(status().isUnauthorized());
    }

    //Test de POST "equipo/id/salir"

    @Test
    public void salirEquipoRedirigeYEliminaUsuario() throws Exception {
        Long usuarioId = addUsuarioBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuarioId);
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/salir"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos/" + equipo.getId()));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
    }

    @Test
    public void salirEquipoSinSesionDevuelve401() throws Exception {
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/salir"))
                .andExpect(status().isUnauthorized());
    }

    //Comporbación Unir/Salir de equipo

    @Test
    public void flujoUnirseCambiaBotonEnDetalle() throws Exception {
        Long usuarioId = addUsuarioBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // Antes de unirse: se muestra "Unirme"
        this.mockMvc.perform(get("/equipos/" + equipo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Unirme al equipo")));

        // Se une
        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/unirse"))
                .andExpect(status().is3xxRedirection());

        // Después de unirse: se muestra "Salir"
        this.mockMvc.perform(get("/equipos/" + equipo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Salir del equipo")));
    }


}
