package com.tracker.dto;

import jakarta.validation.constraints.NotBlank;

public class ConfirmacionRecepcionRequest {
    
    @NotBlank(message = "La firma digital es obligatoria")
    private String firmaDigital;
    
    // Constructores
    public ConfirmacionRecepcionRequest() {}
    
    // Getters y Setters
    public String getFirmaDigital() {
        return firmaDigital;
    }
    
    public void setFirmaDigital(String firmaDigital) {
        this.firmaDigital = firmaDigital;
    }
}

