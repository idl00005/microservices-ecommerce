package com.Entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;

@Entity
public class Usuario {
    @NotBlank(message = "El correo no puede estar vacío.")
    @Email(message = "El correo debe tener un formato válido.") // Validación del formato de correo electrónico
    @Id
    private String correo;

    @NotBlank(message = "El nombre no puede estar vacío.") // No permite nulo ni cadenas vacías
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.") // Longitud máxima de 50 caracteres
    private String nombre;

    @NotBlank(message = "El apellido no puede estar vacío.")
    @Size(max = 50, message = "El apellido no puede superar los 50 caracteres.")
    private String apellidos;

    @Pattern(regexp = "\\d{9,15}", message = "El teléfono debe tener entre 10 y 15 dígitos.")
    private String telefono;

    @NotBlank(message = "La contraseña no puede estar vacía.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.") // Longitud mínima de 8 caracteres
    private String password;

    @NotBlank(message = "El rol no puede estar vacío.") // Obligatorio
    private String rol;


    public Usuario() {
    }

    public Usuario(String nombre, String apellidos, String correo, String telefono, String password, String rol) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.correo = correo;
        this.telefono = telefono;
        this.password = password;
        this.rol = rol;
    }

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "nombre='" + nombre + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", correo='" + correo + '\'' +
                ", telefono='" + telefono + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}
