package ar.edu.utn.frba.dds.servicios.fachadas;

import ar.edu.utn.frba.dds.interfaces.input.ErrorDTO;
import ar.edu.utn.frba.dds.repositories.RepoUsuarios;
import ar.edu.utn.frba.dds.repositories.utils.FactoryRepositorio;
import ar.edu.utn.frba.dds.server.login.User;
import ar.edu.utn.frba.dds.servicios.fachadas.exceptions.MiHuellaApiException;
import spark.Request;

import java.util.Optional;

public class FachadaUsuarios {
    private final RepoUsuarios repoUsuarios;

    public FachadaUsuarios() {
        this.repoUsuarios = FactoryRepositorio.getByOutputType(RepoUsuarios.class);
    }

    public boolean isAuthenticated(String username, String password) {
        return username.isEmpty() || password.isEmpty() || !buscarUsuarioEnRepo(username, password).isPresent();
    }

    public Boolean existeUsuario(String username) {
        return this.repoUsuarios.findByUsername(username).isPresent();
    }

    public Optional<User> buscarUsuarioEnRepo(String username, String password) {
        Optional<User> mayBeUser = this.repoUsuarios.findByUsername(username);

        if(mayBeUser.isPresent() && mayBeUser.get().getPassword().equals(password))
            return mayBeUser;

        return Optional.empty();
    }

    public User agregar(User user){
        return this.repoUsuarios.agregar(user);
    }

    public Optional<User> findById(Integer idUsuario) {
        return repoUsuarios.buscar(idUsuario);
    }

    public Optional<User> findByRolId(String rol, Integer idUsuario) {
        return repoUsuarios.findByRolId(rol, idUsuario);
    }

    public boolean estaLogueado(Request request) {
        return request.session().attribute("idUsuario") != null;
    }

    public void validarRequest(User usuario) {
        if(usuario == null || usuario.getUsername() == null || usuario.getPassword() == null)
            throw new MiHuellaApiException(new ErrorDTO("ERROR_DE_REQUEST",
                    "Debe proporcionar un usuario con su username y contraseña"));

        if (existeUsuario(usuario.getUsername()))
            throw new MiHuellaApiException(new ErrorDTO("ERROR_DE_DOMINIO",
                    "Ya existe un usuario con ese nombre"));
    }
}