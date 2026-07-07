package eci.dosw.alpha.EventService.dto;

import lombok.Data;

/**
 * Datos de entrada para crear un evento. Se usa en lugar de la entidad
 * persistente {@code Event} para evitar exponer el modelo de dominio en la
 * capa web (mass assignment / SonarCloud java:S4684).
 */
@Data
public class CreateEventRequest {
    private String name;
    private String description;
    private String category;
    private String date;
    private int capacity;
}
