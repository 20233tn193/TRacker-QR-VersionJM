package com.tracker.dto;

public class SatisfaccionResponse {
    
    private long totalPaquetes;
    private long paquetesEntregados;
    private double indiceCumplimiento; 
    
    // Constructores
    public SatisfaccionResponse() {}
    
    public SatisfaccionResponse(long totalPaquetes, long paquetesEntregados, double indiceCumplimiento) {
        this.totalPaquetes = totalPaquetes;
        this.paquetesEntregados = paquetesEntregados;
        this.indiceCumplimiento = indiceCumplimiento;
    }
    
    // Getters y Setters
    public long getTotalPaquetes() {
        return totalPaquetes;
    }
    
    public void setTotalPaquetes(long totalPaquetes) {
        this.totalPaquetes = totalPaquetes;
    }
    
    public long getPaquetesEntregados() {
        return paquetesEntregados;
    }
    
    public void setPaquetesEntregados(long paquetesEntregados) {
        this.paquetesEntregados = paquetesEntregados;
    }
    
    public double getIndiceCumplimiento() {
        return indiceCumplimiento;
    }
    
    public void setIndiceCumplimiento(double indiceCumplimiento) {
        this.indiceCumplimiento = indiceCumplimiento;
    }
}

