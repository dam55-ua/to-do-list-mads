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
import madstodolist.service.EquipoServiceException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private Long addAdministradorBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("admin@ua.es");
        usuario.setPassword("123");
        usuario.setAdministrador(true);
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

    //Test de POST "/equipos/id/renombrar"
    @Test
    public void administradorRenombraEquipo() throws Exception {
        Long adminId = addAdministradorBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(adminId);

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/renombrar")
                        .param("nombre", "Proyecto BBB"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos/" + equipo.getId()));

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto BBB");
    }

    @Test
    public void usuarioNoAdministradorNoPuedeRenombrar() throws Exception {
        Long usuarioId = addUsuarioBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/renombrar")
                        .param("nombre", "Proyecto BBB"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("No tiene permisos suficientes"));

        // El nombre no ha cambiado
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto AAA");
    }

    @Test
    public void renombrarEquipoSinSesionDevuelve401() throws Exception {
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/renombrar")
                        .param("nombre", "Proyecto BBB"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void renombrarEquipoInexistenteDevuelve404() throws Exception {
        Long adminId = addAdministradorBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(adminId);

        this.mockMvc.perform(post("/equipos/999/renombrar").param("nombre", "Proyecto BBB"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void administradorEliminaEquipo() throws Exception {
        Long adminId = addAdministradorBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(adminId);

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"));

        assertThatThrownBy(() -> equipoService.recuperarEquipo(equipo.getId()))
                .isInstanceOf(EquipoServiceException.class);
    }

    @Test
    public void usuarioNoAdministradorNoPuedeEliminar() throws Exception {
        Long usuarioId = addUsuarioBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/eliminar"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("No tiene permisos suficientes"));

        // El equipo sigue existiendo
        assertThat(equipoService.recuperarEquipo(equipo.getId())).isNotNull();
    }

    @Test
    public void eliminarEquipoSinSesionDevuelve401() throws Exception {
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");

        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/eliminar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void eliminarEquipoInexistenteDevuelve404() throws Exception {
        Long adminId = addAdministradorBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(adminId);

        this.mockMvc.perform(post("/equipos/999/eliminar"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void detalleEquipoMuestraPanelAdminSiEsAdministrador() throws Exception {
        Long adminId = addAdministradorBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(adminId);

        this.mockMvc.perform(get("/equipos/" + equipo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administración del equipo")));
    }

    @Test
    public void detalleEquipoNoMuestraPanelAdminSiNoEsAdministrador() throws Exception {
        Long usuarioId = addUsuarioBD();
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        this.mockMvc.perform(get("/equipos/" + equipo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Administración del equipo"))));
    }


}
