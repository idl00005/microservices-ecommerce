package com.Inicializacion;

import com.Entidades.Usuario;
import com.Repositorios.RepositorioUsuario;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            // Crear usuario 1
            Usuario admin = new Usuario("Ignacio","Delgado","idl00005@red.ujaen.es","123321123",passwordEncoder.encode("1234"),"admin");

            // Crear usuario 2
            Usuario user = new Usuario("Pedro","Rodríguez","pjrg00033@red.ujaen.es","123321123",passwordEncoder.encode("1234"),"user");

            // Usar el método `save` del repositorio para guardar los usuarios
            usuarioRepository.save(admin, user);

            logger.info("Usuarios iniciales creados correctamente.");
        } else {
            logger.info("Se encontraron usuarios existentes. No se crearán nuevos usuarios.");
        }
    }
}