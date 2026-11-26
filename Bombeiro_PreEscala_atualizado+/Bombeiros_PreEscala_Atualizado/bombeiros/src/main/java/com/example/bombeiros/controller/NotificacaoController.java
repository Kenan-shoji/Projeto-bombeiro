
package com.example.bombeiros.controller;

import com.example.bombeiros.model.Notificacao;
import com.example.bombeiros.service.NotificacaoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.util.List;

@Controller
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;

    // Abrir a página de alertas
    @GetMapping("/alertas")
    public String abrirAlertas(
            HttpSession session,
            Model model) {

        Integer bombeiroId =
                (Integer) session.getAttribute("usuarioLogadoId");

        if (bombeiroId == null) {
            return "redirect:/login";
        }

        String perfil =
                (String) session.getAttribute("usuarioLogadoPerfil");

        boolean ehAdmin = perfil != null
                && perfil.equalsIgnoreCase("ADMINISTRADOR");

        List<Notificacao> notificacoes;
        long qtdNaoLidas;

        if (ehAdmin) {

            // Administrador visualiza todas as notificações
            notificacoes =
                    notificacaoService.listarTodasNotificacoes();

            qtdNaoLidas =
                    notificacaoService.contarTodasNaoLidas();

        } else {

            // Bombeiro visualiza somente as próprias notificações
            notificacoes =
                    notificacaoService.listarNotificacoes(bombeiroId);

            qtdNaoLidas =
                    notificacaoService.contarNaoLidas(bombeiroId);
        }

        model.addAttribute("notificacoes", notificacoes);

        model.addAttribute(
                "qtdNotificacoesNaoLidas",
                qtdNaoLidas
        );

        model.addAttribute(
                "usuarioNome",
                session.getAttribute("usuarioLogadoNome")
        );

        model.addAttribute("perfilNome", perfil);

        model.addAttribute("ehAdmin", ehAdmin);

        return "Alertas";
    }

    // Marcar uma notificação como lida
    @PostMapping("/alertas/marcar-lida/{id}")
    public String marcarComoLida(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Integer bombeiroId =
                (Integer) session.getAttribute("usuarioLogadoId");

        if (bombeiroId == null) {
            return "redirect:/login";
        }

        String perfil =
                (String) session.getAttribute("usuarioLogadoPerfil");

        boolean ehAdmin = perfil != null
                && perfil.equalsIgnoreCase("ADMINISTRADOR");

        try {

            if (ehAdmin) {

                // Administrador pode marcar qualquer notificação
                notificacaoService.marcarComoLidaPorAdmin(id);

            } else {

                // Bombeiro só pode marcar suas próprias notificações
                notificacaoService.marcarComoLida(id, bombeiroId);
            }

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Notificação marcada como lida."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "mensagemErro",
                    e.getMessage()
            );
        }

        return "redirect:/alertas";
    }

    // Marcar todas as notificações como lidas
    @PostMapping("/alertas/marcar-todas-lidas")
    public String marcarTodasComoLidas(
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Integer bombeiroId =
                (Integer) session.getAttribute("usuarioLogadoId");

        if (bombeiroId == null) {
            return "redirect:/login";
        }

        String perfil =
                (String) session.getAttribute("usuarioLogadoPerfil");

        boolean ehAdmin = perfil != null
                && perfil.equalsIgnoreCase("ADMINISTRADOR");

        try {

            if (ehAdmin) {

                // Marca todas as notificações do sistema
                notificacaoService.marcarTodasComoLidasPorAdmin();

            } else {

                // Marca somente as notificações do próprio bombeiro
                notificacaoService.marcarTodasComoLidas(bombeiroId);
            }

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Todas as notificações foram marcadas como lidas."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "mensagemErro",
                    e.getMessage()
            );
        }

        return "redirect:/alertas";
    }
}
