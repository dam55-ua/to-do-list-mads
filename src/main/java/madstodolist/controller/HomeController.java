package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    ManagerUserSession managerUserSession;

    @Autowired
    UsuarioService usuarioService;

    @GetMapping("/about")
    public String about(Model model) {

        // Añadimos al modelo el usuario logeado (o null si no hay sesión)
        // para que la barra de menú muestre la versión correspondiente
        Long idUsuario = managerUserSession.usuarioLogeado();
        model.addAttribute("usuarioLogeado",
                idUsuario == null ? null : usuarioService.findById(idUsuario));

        return "about";
    }

}
