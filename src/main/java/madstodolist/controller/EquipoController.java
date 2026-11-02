package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.EquipoNotFoundException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.EquipoServiceException;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EquipoController {
    @Autowired
    EquipoService equipoService;

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    ManagerUserSession managerUserSession;

    private UsuarioData comprobarUsuarioLogeado() {
        Long idUsuario = managerUserSession.usuarioLogeado();
        if (idUsuario == null) {
            throw new UsuarioNoLogeadoException();
        }

        UsuarioData usuario = usuarioService.findById(idUsuario);
        if (usuario == null) {
            throw new UsuarioNoLogeadoException();
        }
        return usuario;
    }


    private void addUsuarioLogeado(Model model, UsuarioData usuarioLogeado) {
        model.addAttribute("usuarioLogeado", usuarioLogeado);
    }


    //GET "/equipos"
    @GetMapping("/equipos")
    public String listadoEquipos(Model model) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();

        List<EquipoData> equipos;
        try {
            equipos = equipoService.findAllOrdenadoPorNombre();
        } catch (EquipoServiceException e) {
            // findAllOrdenadoPorNombre() lanza excepción si no hay equipos.
            // Un listado vacío no es un error: mostramos la página vacía.
            equipos = Collections.emptyList();
        }

        model.addAttribute("equipos", equipos);
        addUsuarioLogeado(model, usuarioLogeado);

        return "listaEquipos";
    }

    //GET "equipos/id"
    @GetMapping("/equipos/{id}")
    public String detalleEquipo(@PathVariable("id") Long idEquipo, Model model) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();

        EquipoData equipo;
        try {
            equipo = equipoService.recuperarEquipo(idEquipo);
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }

        List<UsuarioData> usuarios = equipoService.usuariosEquipo(idEquipo);

        boolean pertenece = usuarios.stream()
                .anyMatch(u -> u.getId().equals(usuarioLogeado.getId()));

        model.addAttribute("equipo", equipo);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("pertenece", pertenece);
        addUsuarioLogeado(model, usuarioLogeado);

        return "detalleEquipo";
    }

    // POST "/equipos"
    @PostMapping("/equipos")
    public String crearEquipo(@RequestParam("nombre") String nombre,
                              RedirectAttributes flash) {
        comprobarUsuarioLogeado();

        if (nombre == null || nombre.trim().isEmpty()) {
            flash.addFlashAttribute("error", "El nombre del equipo no puede estar vacío");
            return "redirect:/equipos";
        }

        equipoService.crearEquipo(nombre.trim());
        flash.addFlashAttribute("mensaje", "Equipo creado correctamente");
        return "redirect:/equipos";
    }

    // POST "/equipos/{id}/unirse"
    @PostMapping("/equipos/{id}/unirse")
    public String unirseEquipo(@PathVariable("id") Long idEquipo,
                               RedirectAttributes flash) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();

        try {
            equipoService.añadirUsuarioAEquipo(idEquipo, usuarioLogeado.getId());
            flash.addFlashAttribute("mensaje", "Te has unido al equipo");
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }

        return "redirect:/equipos/" + idEquipo;
    }

    // POST "/equipos/{id}/salir"
    @PostMapping("/equipos/{id}/salir")
    public String salirEquipo(@PathVariable("id") Long idEquipo,
                              RedirectAttributes flash) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();

        try {
            equipoService.eliminarUsuarioDeEquipo(idEquipo, usuarioLogeado.getId());
            flash.addFlashAttribute("mensaje", "Has salido del equipo");
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }

        return "redirect:/equipos/" + idEquipo;
    }

}

