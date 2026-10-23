package madstodolist.service;

import madstodolist.dto.EquipoData;
import madstodolist.dto.TareaData;
import madstodolist.model.Equipo;
import madstodolist.model.Usuario;
import madstodolist.repository.EquipoRepository;
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
            return null;
        }
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional(readOnly = true)
    public List<EquipoData> findAllOrdenadoPorNombre() {
        List<Equipo> equipos = equipoRepository.findAll();
        if (equipos.isEmpty()) {
            return null;
        }
        // Ordenamos la lista por nombre de Equipo
        List<EquipoData> equiposData = equipos.stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
        Collections.sort(equiposData, (a, b) -> a.getNombre().compareTo(b.getNombre()));
        return equiposData;
    }
}