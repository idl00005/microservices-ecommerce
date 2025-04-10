package com.Otros;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ApplicationScoped
public class BCryptPasswordEncoderProducer {

    @Produces
    @ApplicationScoped
    public BCryptPasswordEncoder produceBCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}