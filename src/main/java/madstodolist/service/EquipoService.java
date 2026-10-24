package madstodolist.service;

import madstodolist.dto.EquipoData;
import madstodolist.dto.TareaData;
import madstodolist.dto.UsuarioData;
import madstodolist.model.Equipo;
import madstodolist.model.Usuario;
import madstodolist.repository.EquipoRepository;
import madstodolist.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoService {

    @Autowired
    EquipoRepository equipoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public EquipoData crearEquipo(String nombre) {
        Equipo e = new Equipo(nombre);
        equipoRepository.save(e);
        return modelMapper.map(e, EquipoData.class);
    }

    @Transactional(readOnly = true)
    public EquipoData recuperarEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);

        if (equipo == null){
            throw new EquipoServiceException("ERROR");
        }
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional(readOnly = true)
    public List<EquipoData> findAllOrdenadoPorNombre() {
        List<Equipo> equipos = equipoRepository.findAll();
        if (equipos.isEmpty()) {
            throw new EquipoServiceException("ERROR");
        }
        // Ordenamos la lista por nombre de Equipo
        List<EquipoData> equiposData = equipos.stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
        Collections.sort(equiposData, (a, b) -> a.getNombre().compareTo(b.getNombre()));
        return equiposData;
    }

    @Transactional
    public void añadirUsuarioAEquipo(Long id, Long id1) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null){
            throw new EquipoServiceException("ERROR");
        }
        Usuario usuario = usuarioRepository.findById(id1).orElse(null);
        if (usuario == null){
            throw new EquipoServiceException("ERROR");
        }
        equipo.addUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioData> usuariosEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if(equipo == null){
            throw new EquipoServiceException("ERROR");
        }
        // Hacemos uso de Java Stream API para mapear la lista de entidades a DTOs.
        return equipo.getUsuarios().stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioData.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EquipoData> equiposUsuario (Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            throw new EquipoServiceException("ERROR");
        }
        // Hacemos uso de Java Stream API para mapear la lista de entidades a DTOs.
        List<EquipoData> equipos = usuario.getEquipos().stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
        // Ordenamos la lista por id de tarea
        return equipos;
    }

    @Transactional
    public void eliminarUsuarioDeEquipo(Long id, Long id1) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null){
            throw new EquipoServiceException("ERROR");
        }
        Usuario usuario = usuarioRepository.findById(id1).orElse(null);
        if (usuario == null){
            throw new EquipoServiceException("ERROR");
        }
        equipo.removeUsuario(usuario);
    }

    @Transactional
    public EquipoData modificarNombreEquipo(Long equipoId, String nuevoNombre){
        Equipo equipo = equipoRepository.findById(equipoId).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("ERROR");
        }
        equipo.setNombre(nuevoNombre);
        equipo = equipoRepository.save(equipo);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional
    public void eliminarEquipo(Long id){
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("ERROR");
        }
        for (Usuario usuario : equipo.getUsuarios()){
            equipo.removeUsuario(usuario);
        }
        equipoRepository.delete(equipo);
    }
}