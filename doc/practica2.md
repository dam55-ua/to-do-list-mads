# Documentación técnica de la versión 1.1.0

## Evolución de la aplicación

La versión 1.1.0 amplía la aplicación de tareas con navegación común, consulta de usuarios registrados y capacidades básicas de administración. La implementación mantiene la arquitectura existente de Spring Boot: los controladores atienden las rutas y preparan el modelo, los servicios concentran las reglas de negocio, los repositorios acceden a JPA y las plantillas Thymeleaf generan las páginas.

## Navegación y páginas de usuarios

`src/main/resources/templates/fragments.html` contiene el fragmento Navbar de Bootstrap. Incluye el acceso a «Acerca de» y cambia las opciones según exista un usuario autenticado: cuando hay sesión ofrece el acceso a tareas y un menú de usuario; desde «Acerca de», si no hay sesión, muestra enlaces a login y registro. El nombre del usuario se obtiene mediante el atributo `usuarioLogeado` que preparan los controladores que renderizan páginas con la barra. Las plantillas de login y registro no insertan el fragmento.

La clase `RegistradosController` atiende `/registrados` y `/registrados/{id}`. El listado obtiene los usuarios mediante `UsuarioService.findAll()`, y la página de descripción usa `findById(Long)` para mostrar el identificador, correo, nombre y fecha de nacimiento. `listaRegistrados.html` añade un enlace a la descripción de cada fila; `descripcionUsuario.html` presenta los datos sin incluir la contraseña.

## Administración y protección

El rol se almacena en el campo booleano `administrador` de la entidad `Usuario` y se transporta entre capas en `UsuarioData` y `RegistroData`. `UsuarioRepository.existsByAdministrador(...)` consulta si ya hay un administrador. El formulario `formRegistro.html` representa la elección mediante una casilla visible solo cuando todavía no existe uno. La regla también se aplica en `UsuarioService.registrar(...)` para no depender únicamente de la interfaz. En el login, `LoginController` redirige al administrador a `/registrados` y al resto de usuarios a su lista de tareas.

El método privado `comprobarAdministrador()` centraliza la autorización del listado, la descripción y las acciones administrativas. Si falta sesión lanza `UsuarioNoLogeadoException`; si el usuario no existe o no tiene rol de administrador lanza `UsuarioNoAutorizadoException`, que responde con HTTP 401. Los identificadores inexistentes producen HTTP 404 mediante `UsuarioNotFoundException`.

El bloqueo añade el booleano persistente `bloqueado` a `Usuario` y `UsuarioData`. El servicio `cambiarEstadoBloqueo(Long, boolean)` persiste el cambio; los endpoints POST de bloqueo y habilitación verifican primero el rol. El listado expone las acciones y el estado actual. El administrador no puede bloquearse a sí mismo: el controller rechaza la operación aunque se invoque directamente y la plantilla no ofrece ese botón para la cuenta administradora.

Durante el login, `UsuarioService.login(...)` devuelve `USER_BLOCKED` para una cuenta bloqueada y `LoginController` presenta un mensaje de error en el formulario, sin crear una sesión.

```java
if (administrador.getId().equals(idUsuario)) {
    flash.addFlashAttribute("error",
            "No puedes bloquear tu propia cuenta de administrador");
    return "redirect:/registrados";
}
```

Esta comprobación en el servidor es importante aunque el botón no aparezca: las acciones POST pueden solicitarse sin usar la interfaz.

## Pruebas

`UsuarioServiceTest` verifica el registro único de administrador y que bloquear impide iniciar sesión mientras habilitarlo restaura el acceso. `UsuarioWebTest` cubre la redirección del administrador, la visibilidad de la casilla y el mensaje del login cuando una cuenta está bloqueada. `RegistradosWebTest` comprueba el contenido del listado y las descripciones, las respuestas de autorización, y el flujo de bloqueo y habilitación, incluida la prohibición de bloquear la cuenta administradora. Las pruebas web usan MockMvc; las de servicio prueban las reglas contra la base de datos de prueba.

Las plantillas nuevas de usuarios son `listaRegistrados.html` y `descripcionUsuario.html`; también se ampliaron `formRegistro.html`, `listaRegistrados.html` y `descripcionUsuario.html` para las funciones administrativas. Las modificaciones mantienen la contraseña fuera de las vistas de consulta pública de los datos del usuario y comprueban las acciones sensibles en el servidor.
