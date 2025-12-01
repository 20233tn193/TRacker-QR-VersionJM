package com.tracker.service;

import com.tracker.model.Movimiento;
import com.tracker.model.Paquete;
import com.tracker.repository.MovimientoRepository;
import com.tracker.repository.PaqueteRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteService {
    
    @Autowired
    private MovimientoRepository movimientoRepository;
    
    @Autowired
    private PaqueteRepository paqueteRepository;
    
    @Value("${app.report.output-dir:./reports}")
    private String outputDir;
    
    public byte[] generarReporteTrazabilidad(LocalDateTime inicio, LocalDateTime fin, String empleadoId) {
        List<Movimiento> movimientos;
        
        if (empleadoId != null && !empleadoId.isEmpty()) {
            movimientos = movimientoRepository.findByEmpleadoIdAndFechaHoraBetween(empleadoId, inicio, fin);
        } else {
            movimientos = movimientoRepository.findByFechaHoraBetween(inicio, fin);
        }
        
        return generarPDF(movimientos, inicio, fin);
    }
    
    private byte[] generarPDF(List<Movimiento> movimientos, LocalDateTime inicio, LocalDateTime fin) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("Reporte de Trazabilidad", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Información del reporte
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
            Paragraph info = new Paragraph();
            info.add(new Chunk("Período: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            info.add(new Chunk(inicio.format(formatter) +
                             " - " + fin.format(formatter), infoFont));
            info.add(Chunk.NEWLINE);
            info.add(new Chunk("Total de movimientos: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            info.add(new Chunk(String.valueOf(movimientos.size()), infoFont));
            info.setSpacingAfter(15);
            document.add(info);
            
            // Tabla de movimientos
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 2, 2, 2, 2, 3});
            
            // Encabezados
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            addTableHeader(table, "ID Paquete", headerFont);
            addTableHeader(table, "Estado", headerFont);
            addTableHeader(table, "Ubicación", headerFont);
            addTableHeader(table, "Empleado", headerFont);
            addTableHeader(table, "Fecha/Hora", headerFont);
            addTableHeader(table, "Observaciones", headerFont);
            
            // Datos
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
            for (Movimiento movimiento : movimientos) {
                table.addCell(createCell(movimiento.getPaqueteId(), cellFont));
                table.addCell(createCell(movimiento.getEstado().name(), cellFont));
                table.addCell(createCell(movimiento.getUbicacion(), cellFont));
                table.addCell(createCell(movimiento.getEmpleadoNombre(), cellFont));
                LocalDateTime fechaMovimiento = movimiento.getFechaHora().toDate()
                        .toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
                table.addCell(createCell(fechaMovimiento.format(formatter), cellFont));
                table.addCell(createCell(movimiento.getObservaciones() != null ? 
                        movimiento.getObservaciones() : "", cellFont));
            }
            
            document.add(table);
            
            // Estadísticas
            document.add(Chunk.NEWLINE);
            Paragraph stats = new Paragraph("Estadísticas", 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            stats.setSpacingAfter(10);
            document.add(stats);
            
            Map<String, Long> estadisticasPorEstado = movimientos.stream()
                    .collect(Collectors.groupingBy(m -> m.getEstado().name(), Collectors.counting()));
            
            for (Map.Entry<String, Long> entry : estadisticasPorEstado.entrySet()) {
                Paragraph stat = new Paragraph(entry.getKey() + ": " + entry.getValue(), infoFont);
                document.add(stat);
            }
            
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte PDF", e);
        }
    }
    
    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(BaseColor.DARK_GRAY);
        header.setBorderWidth(2);
        header.setPhrase(new Phrase(text, font));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setPadding(5);
        table.addCell(header);
    }
    
    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
    
    public Map<String, Long> obtenerEstadisticasEntregas(LocalDateTime inicio, LocalDateTime fin) {
        List<Paquete> paquetes = paqueteRepository.findByFechaCreacionBetween(inicio, fin);
        
        return paquetes.stream()
                .filter(p -> p.getEstado() == com.tracker.model.EstadoPaquete.ENTREGADO)
                .collect(Collectors.groupingBy(
                        p -> p.getFechaCreacion().toDate().toInstant()
                                .atZone(ZoneOffset.UTC).toLocalDateTime().getMonth().name(),
                        Collectors.counting()
                ));
    }
}

