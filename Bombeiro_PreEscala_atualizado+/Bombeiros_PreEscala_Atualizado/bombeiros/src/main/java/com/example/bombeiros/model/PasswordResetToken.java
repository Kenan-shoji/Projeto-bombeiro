package com.example.bombeiros.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true, length = 100)
    private String token;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bombeiro_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Bombeiro bombeiro;
    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;
    public Integer getId(){return id;}
    public String getToken(){return token;} public void setToken(String v){token=v;}
    public Bombeiro getBombeiro(){return bombeiro;} public void setBombeiro(Bombeiro v){bombeiro=v;}
    public LocalDateTime getExpiraEm(){return expiraEm;} public void setExpiraEm(LocalDateTime v){expiraEm=v;}
}
