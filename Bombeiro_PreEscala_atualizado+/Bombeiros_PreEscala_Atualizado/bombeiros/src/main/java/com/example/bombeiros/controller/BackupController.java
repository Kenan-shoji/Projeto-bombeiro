package com.example.bombeiros.controller;

import com.example.bombeiros.service.BackupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RNF11 - Permite ao administrador gerar um backup na hora e consultar os existentes.
 * Os backups periódicos acontecem sozinhos (ver BackupService).
 */
@RestController
@RequestMapping("/api/admin/backup")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    private boolean ehAdmin(HttpSession session) {
        Object perfil = session.getAttribute("usuarioLogadoPerfil");
        return perfil != null && "ADMINISTRADOR".equalsIgnoreCase(perfil.toString());
    }

    private ResponseEntity<?> acessoNegado() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("mensagem", "Acesso permitido somente ao administrador."));
    }

    @PostMapping
    public ResponseEntity<?> gerarAgora(HttpSession session) {
        if (!ehAdmin(session)) {
            return acessoNegado();
        }
        try {
            Path arquivo = backupService.gerarBackup();
            return ResponseEntity.ok(descrever(arquivo, "Backup gerado com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensagem", "Não foi possível gerar o backup: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listar(HttpSession session) {
        if (!ehAdmin(session)) {
            return acessoNegado();
        }
        try {
            List<Map<String, Object>> lista = backupService.listarBackups().stream()
                    .map(p -> descrever(p, null))
                    .toList();
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensagem", "Não foi possível listar os backups."));
        }
    }

    private Map<String, Object> descrever(Path arquivo, String mensagem) {
        Map<String, Object> info = new LinkedHashMap<>();
        if (mensagem != null) {
            info.put("mensagem", mensagem);
        }
        info.put("arquivo", arquivo.getFileName().toString());
        try {
            info.put("tamanhoBytes", Files.size(arquivo));
            info.put("criadoEm", Files.getLastModifiedTime(arquivo).toInstant().toString());
        } catch (Exception ignorada) {
            // informações opcionais
        }
        return info;
    }
}
