package com.example.bombeiros.controller;

import com.example.bombeiros.model.Bombeiro;
import com.example.bombeiros.model.PasswordResetToken;
import com.example.bombeiros.repository.BombeiroRepository;
import com.example.bombeiros.repository.PasswordResetTokenRepository;
import com.example.bombeiros.service.PasswordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class RecuperacaoSenhaController {
    private final BombeiroRepository bombeiros;
    private final PasswordResetTokenRepository tokens;
    private final PasswordService passwords;
    private final JavaMailSender mailSender;
    @Value("${app.url:http://localhost:8080}") private String appUrl;
    @Value("${spring.mail.username:}") private String remetente;
    public RecuperacaoSenhaController(BombeiroRepository b, PasswordResetTokenRepository t, PasswordService p, JavaMailSender m) { bombeiros=b; tokens=t; passwords=p; mailSender=m; }

    @GetMapping("/esqueci-senha") public String paginaEsqueci(){ return "EsqueciSenha"; }
    @PostMapping("/esqueci-senha") @Transactional
    public String solicitar(@RequestParam("email") String email, Model model) {
        Bombeiro b=bombeiros.findByEmail(email.trim()).orElse(null);
        if (b != null) {
            try {
                tokens.deleteByBombeiroId(b.getId());
                PasswordResetToken t=new PasswordResetToken(); t.setBombeiro(b); t.setToken(UUID.randomUUID().toString()); t.setExpiraEm(LocalDateTime.now().plusMinutes(30)); tokens.save(t);
                SimpleMailMessage msg=new SimpleMailMessage(); msg.setTo(b.getEmail()); if(remetente!=null&&!remetente.isBlank()) msg.setFrom(remetente); msg.setSubject("Redefinição de senha - Pré-Escala"); msg.setText("Use este link em até 30 minutos para redefinir sua senha: " + appUrl + "/redefinir-senha?token=" + t.getToken()); mailSender.send(msg);
            } catch (Exception ex) { model.addAttribute("erro", "Não foi possível enviar o e-mail. Verifique a configuração SMTP do sistema e tente novamente."); return "EsqueciSenha"; }
        }
        model.addAttribute("mensagem", "Se o e-mail estiver cadastrado, você receberá um link de recuperação válido por 30 minutos."); return "EsqueciSenha";
    }
    @GetMapping("/redefinir-senha") public String paginaRedefinir(@RequestParam("token") String token, Model model) {
        PasswordResetToken t=tokens.findByToken(token).orElse(null);
        if(t==null||t.getExpiraEm().isBefore(LocalDateTime.now())) { model.addAttribute("erro", "Link inválido ou expirado. Solicite uma nova recuperação."); return "EsqueciSenha"; }
        model.addAttribute("token",token); return "RedefinirSenha";
    }
    @PostMapping("/redefinir-senha") @Transactional
    public String redefinir(@RequestParam("token") String token,@RequestParam("senha") String senha,@RequestParam("confirmarSenha") String confirmarSenha,Model model) {
        PasswordResetToken t=tokens.findByToken(token).orElse(null);
        if(t==null||t.getExpiraEm().isBefore(LocalDateTime.now())) { model.addAttribute("erro","Link inválido ou expirado."); return "EsqueciSenha"; }
        if(!senha.equals(confirmarSenha)||!passwords.senhaForte(senha)) { model.addAttribute("erro","As senhas devem ser iguais e atender aos critérios: 8 caracteres, maiúscula, minúscula, número e caractere especial."); model.addAttribute("token",token); return "RedefinirSenha"; }
        Bombeiro b=t.getBombeiro(); b.setSenhaHash(passwords.encode(senha)); bombeiros.save(b); tokens.delete(t); return "redirect:/login?senhaRedefinida";
    }
}
