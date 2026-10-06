package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Controller de la página de usuarios registrados (/registrados)
@Controller
public class RegistradosController {

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    ManagerUserSession managerUserSession;

    @GetMapping("/registrados")
    public String listadoRegistrados(Model model) {

        // Listado de usuarios registrados (identificador y correo electrónico)
        model.addAttribute("usuarios", usuarioService.findAll());

        // Usuario logeado, para la barra de menú
        Long idUsuario = managerUserSession.usuarioLogeado();
        model.addAttribute("usuarioLogeado",
                idUsuario == null ? null : usuarioService.findById(idUsuario));

        return "listaRegistrados";
    }
}
