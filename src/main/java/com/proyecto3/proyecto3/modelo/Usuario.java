package com.proyecto3.proyecto3.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Document(collection = "usuario")
public class Usuario {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String nombre;
    private String passwordHash;
    private String salt;

    private String rol;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        String rolNormalizado = (rol == null || rol.isBlank()) ? "CLIENTE" : rol.toUpperCase();
        return List.of(new SimpleGrantedAuthority("ROLE_" + rolNormalizado));
    }
}
