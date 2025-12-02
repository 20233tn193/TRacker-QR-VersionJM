package com.tracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.password-reset.base-url:http://localhost:8080/api/usuarios}")
    private String baseUrl;
    
    public void enviarEmailRecuperacionPassword(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Recuperación de Contraseña - Sistema de Rastreo");
        message.setText(crearMensajeRecuperacion(token));
        
        mailSender.send(message);
    }
    
    private String crearMensajeRecuperacion(String token) {
        String resetUrl = baseUrl + "/resetPassword?token=" + token;
        
        return "Hola,\n\n" +
               "Has solicitado recuperar tu contraseña. " +
               "Para restablecer tu contraseña, haz clic en el siguiente enlace:\n\n" +
               resetUrl + "\n\n" +
               "Este enlace expirará en 1 hora.\n\n" +
               "Si no solicitaste este cambio, ignora este correo.\n\n" +
               "Saludos,\n" +
               "Equipo de Sistema de Rastreo";
    }
}

