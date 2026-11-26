package com.example.bombeiros.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * RNF10 - Encerrar sessões após determinado período de inatividade.
 *
 * O servidor (Tomcat) invalida a sessão sozinho depois do tempo configurado em
 * "server.servlet.session.timeout". Este interceptor detecta o caso em que o
 * navegador ainda envia o cookie de uma sessão que já expirou e avisa o usuário:
 *  - páginas: redireciona para /login?expirada=true
 *  - chamadas /api/...: responde 401 em JSON (o front redireciona para o login)
 */
public class SessaoExpiradaInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        boolean enviouCookieDeSessao = request.getRequestedSessionId() != null;
        boolean sessaoAindaValida = request.isRequestedSessionIdValid();

        if (!enviouCookieDeSessao || sessaoAindaValida) {
            return true;
        }

        String contexto = request.getContextPath();

        if (request.getRequestURI().startsWith(contexto + "/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"mensagem\":\"Sua sessão expirou por inatividade. Entre novamente.\"}");
            return false;
        }

        response.sendRedirect(contexto + "/login?expirada=true");
        return false;
    }
}
