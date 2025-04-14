package DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoDTO (String id, String nombre, BigDecimal precio, Integer stock) {}
