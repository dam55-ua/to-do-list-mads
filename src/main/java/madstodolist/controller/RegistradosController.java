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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PostMapping("/registrados/{id}/bloquear")
    public String bloquearUsuario(@PathVariable("id") Long idUsuario, RedirectAttributes flash) {
        UsuarioData administrador = comprobarAdministrador();
        if (administrador.getId().equals(idUsuario)) {
            flash.addFlashAttribute("error", "No puedes bloquear tu propia cuenta de administrador");
            return "redirect:/registrados";
        }
        if (usuarioService.cambiarEstadoBloqueo(idUsuario, true) == null) {
            throw new UsuarioNotFoundException();
        }
        flash.addFlashAttribute("mensaje", "Usuario bloqueado correctamente");
        return "redirect:/registrados";
    }

    @PostMapping("/registrados/{id}/habilitar")
    public String habilitarUsuario(@PathVariable("id") Long idUsuario, RedirectAttributes flash) {
        comprobarAdministrador();
        if (usuarioService.cambiarEstadoBloqueo(idUsuario, false) == null) {
            throw new UsuarioNotFoundException();
        }
        flash.addFlashAttribute("mensaje", "Usuario habilitado correctamente");
        return "redirect:/registrados";
    }
}
