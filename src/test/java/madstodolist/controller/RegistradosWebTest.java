package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class RegistradosWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    // Moqueamos el managerUserSession para poder moquear el usuario logeado
    @MockBean
    private ManagerUserSession managerUserSession;

    // Registra dos usuarios de prueba en la BD y devuelve el id del primero
    private Long addUsuariosBD() {
        UsuarioData ana = new UsuarioData();
        ana.setEmail("ana.garcia@ua.es");
        ana.setPassword("clave-secreta-ana");
        ana = usuarioService.registrar(ana);

        UsuarioData luis = new UsuarioData();
        luis.setEmail("luis.perez@ua.es");
        luis.setPassword("clave-secreta-luis");
        usuarioService.registrar(luis);

        return ana.getId();
    }

    @Test
    public void listadoRegistradosMuestraIdYCorreo() throws Exception {
        // GIVEN
        // Dos usuarios registrados en la BD y un usuario logeado
        Long usuarioId = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // la petición GET a /registrados devuelve una página que lista
        // el identificador y el correo de cada usuario registrado,
        // pero NUNCA muestra las contraseñas.
        this.mockMvc.perform(get("/registrados"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("ana.garcia@ua.es"),
                        containsString("luis.perez@ua.es"),
                        containsString("/registrados/" + usuarioId),
                        not(containsString("clave-secreta-ana")),
                        not(containsString("clave-secreta-luis"))
                )));
    }

    @Test
    public void listadoRegistradosMuestraBarraDeMenu() throws Exception {
        // GIVEN
        // Un usuario logeado
        Long usuarioId = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // la página del listado incluye la barra de menú común,
        // con el enlace a la página de tareas del usuario logeado.
        this.mockMvc.perform(get("/registrados"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("navbar"),
                        containsString("href=\"/usuarios/" + usuarioId + "/tareas\""),
                        containsString("Cerrar sesión")
                )));
    }

    @Test
    public void descripcionRegistradoMuestraDatosSinPassword() throws Exception {
        Long usuarioId = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        this.mockMvc.perform(get("/registrados/" + usuarioId))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString(usuarioId.toString()),
                        containsString("ana.garcia@ua.es"),
                        containsString("No indicada"),
                        not(containsString("clave-secreta-ana"))
                )));
    }

    @Test
    public void descripcionUsuarioInexistenteDevuelve404() throws Exception {
        this.mockMvc.perform(get("/registrados/999"))
                .andExpect(status().isNotFound());
    }
}
