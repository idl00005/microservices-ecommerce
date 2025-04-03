package com.Inicializacion;

import com.Entidades.Usuario;
import com.Repositorios.RepositorioUsuario;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

@Startup
@ApplicationScoped
public class ApplicationInitializer {

    @Inject
    RepositorioUsuario usuarioRepository;

    @Inject
    Logger logger;

    @PostConstruct
    @Transactional
    public void initialize() {
        // Verificar si ya existen usuarios
        if (usuarioRepository.count() == 0) {
            logger.info("No se encontraron usuarios. Creando usuarios iniciales...");

            // Crear usuario 1
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setPassword("admin123"); // En el futuro, encriptar la contraseña
            admin.setRole("admin");

            // Crear usuario 2
            Usuario user = new Usuario();
            user.setUsername("user");
            user.setPassword("user123");
            user.setRole("user");

            // Usar el método `save` del repositorio para guardar los usuarios
            usuarioRepository.save(admin, user);

            logger.info("Usuarios iniciales creados correctamente.");
        } else {
            logger.info("Se encontraron usuarios existentes. No se crearán nuevos usuarios.");
        }
    }
}