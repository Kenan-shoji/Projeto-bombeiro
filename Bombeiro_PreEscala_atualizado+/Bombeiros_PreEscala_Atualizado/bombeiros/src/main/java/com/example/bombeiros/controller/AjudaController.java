package com.example.bombeiros.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AjudaController {
    @GetMapping("/manual")
    public String manual(HttpSession session) {
        return session.getAttribute("usuarioLogadoId") == null ? "redirect:/login" : "Manual";
    }
}
