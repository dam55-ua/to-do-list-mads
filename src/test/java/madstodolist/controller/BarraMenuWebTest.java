package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.TareaData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.TareaService;
import madstodolist.service.UsuarioService;
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

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class BarraMenuWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TareaService tareaService;

    // Moqueamos el managerUserSession para poder moquear el usuario logeado
    @MockBean
    private ManagerUserSession managerUserSession;

    // Registra un usuario de prueba con nombre y devuelve su id
    private Long addUsuarioBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("barra@ua");
        usuario.setPassword("123");
        usuario.setNombre("Daniel");
        usuario = usuarioService.registrar(usuario);
        return usuario.getId();
    }

    @Test
    public void aboutSinLogearMuestraEnlacesLoginYRegistro() throws Exception {
        // GIVEN
        // un usuario no logeado (usuarioLogeado() devuelve null por defecto)

        // WHEN, THEN
        // la página "acerca de" muestra los enlaces a login y registro,
        // y NO aparece la opción de cerrar sesión.
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(allOf(
                        containsString("href=\"/login\""),
                        containsString("href=\"/registro\""),
                        not(containsString("href=\"/logout\"")),
                        not(containsString("dropdown-menu"))
                )));
    }

    @Test
    public void aboutLogeadoMuestraBarraCompleta() throws Exception {
        // GIVEN
        // un usuario logeado
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // la página "acerca de" muestra la barra común:
        // enlace a tareas, el nombre del usuario y cerrar sesión.
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(allOf(
                        containsString("/usuarios/" + usuarioId + "/tareas"),
                        containsString("Tareas"),
                        containsString("Daniel"),
                        containsString("Cerrar sesión Daniel")
                )));
    }

    @Test
    public void listaTareasMuestraBarraDeMenu() throws Exception {
        // GIVEN
        // un usuario con una tarea en la BD y logeado
        Long usuarioId = addUsuarioBD();
        tareaService.nuevaTareaUsuario(usuarioId, "Lavar coche");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // el listado de tareas muestra la barra de menú con el enlace
        // a la página "acerca de" y la opción de cerrar sesión.
        String url = "/usuarios/" + usuarioId.toString() + "/tareas";

        this.mockMvc.perform(get(url))
                .andExpect(content().string(allOf(
                        containsString("navbar"),
                        containsString("href=\"/about\""),
                        containsString("Cerrar sesión Daniel")
                )));
    }

    @Test
    public void loginNoMuestraBarraDeMenu() throws Exception {
        // WHEN, THEN
        // la página de login NO contiene la barra de menú
        this.mockMvc.perform(get("/login"))
                .andExpect(content().string(allOf(
                        not(containsString("Cerrar sesión")),
                        not(containsString("navbar-brand"))
                )));
    }

    @Test
    public void registroNoMuestraBarraDeMenu() throws Exception {
        // WHEN, THEN
        // la página de registro NO contiene la barra de menú
        this.mockMvc.perform(get("/registro"))
                .andExpect(content().string(allOf(
                        not(containsString("Cerrar sesión")),
                        not(containsString("navbar-brand"))
                )));
    }
}
