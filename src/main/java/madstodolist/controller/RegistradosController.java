package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.UsuarioNoAutorizadoException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.controller.exception.UsuarioNotFoundException;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Controller de la página de usuarios registrados (/registrados)
@Controller
public class RegistradosController {

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    ManagerUserSession managerUserSession;

    private UsuarioData comprobarAdministrador() {
        Long idUsuario = managerUserSession.usuarioLogeado();
        if (idUsuario == null) {
            throw new UsuarioNoLogeadoException();
        }

        UsuarioData usuarioLogeado = usuarioService.findById(idUsuario);
        if (usuarioLogeado == null || !usuarioLogeado.isAdministrador()) {
            throw new UsuarioNoAutorizadoException();
        }
        return usuarioLogeado;
    }

    @GetMapping("/registrados")
    public String listadoRegistrados(Model model) {
        UsuarioData usuarioLogeado = comprobarAdministrador();

        // Listado de usuarios registrados (identificador y correo electrónico)
        model.addAttribute("usuarios", usuarioService.findAll());

        model.addAttribute("usuarioLogeado", usuarioLogeado);

        return "listaRegistrados";
    }

    @GetMapping("/registrados/{id}")
    public String descripcionRegistrado(@PathVariable("id") Long idUsuario, Model model) {
        UsuarioData usuarioLogeado = comprobarAdministrador();

        UsuarioData usuario = usuarioService.findById(idUsuario);
        if (usuario == null) {
            throw new UsuarioNotFoundException();
        }

        model.addAttribute("usuario", usuario);

        model.addAttribute("usuarioLogeado", usuarioLogeado);

        return "descripcionUsuario";
    }
}
