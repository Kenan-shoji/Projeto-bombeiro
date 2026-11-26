package com.example.bombeiros.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * RNF11 - Realizar cópias de segurança periódicas do banco de dados.
 *
 * Gera arquivos "backup_AAAAMMDD_HHMMSS.sql" na pasta configurada e mantém
 * somente os mais recentes. Configuração em application.properties (backup.*).
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter FORMATO_NOME =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String PREFIXO = "backup_";
    private static final String EXTENSAO = ".sql";

    private final DataSource dataSource;
    private final Path diretorio;
    private final int manter;
    private final boolean ativo;

    public BackupService(
            DataSource dataSource,
            @Value("${backup.diretorio:backups}") String diretorio,
            @Value("${backup.manter:7}") int manter,
            @Value("${backup.ativo:true}") boolean ativo) {
        this.dataSource = dataSource;
        this.diretorio = Path.of(diretorio).toAbsolutePath();
        this.manter = manter;
        this.ativo = ativo;
    }

    /** Execução periódica (padrão: todos os dias às 02:00). */
    @Scheduled(cron = "${backup.cron:0 0 2 * * *}")
    public void backupAgendado() {
        if (!ativo) {
            return;
        }
        try {
            Path arquivo = gerarBackup();
            log.info("Backup periódico concluído: {}", arquivo);
        } catch (Exception e) {
            log.error("Falha no backup periódico do banco de dados", e);
        }
    }

    /** Gera um backup agora e devolve o caminho do arquivo criado. */
    public synchronized Path gerarBackup() throws IOException, SQLException {
        Files.createDirectories(diretorio);

        String nome = PREFIXO + LocalDateTime.now().format(FORMATO_NOME) + EXTENSAO;
        Path destino = diretorio.resolve(nome);
        Path temporario = diretorio.resolve(nome + ".tmp");

        try (Connection conexao = dataSource.getConnection();
             Writer escritor = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {

            restringirPermissoes(temporario);
            new DumpSqlWriter().escrever(conexao, escritor);

        } catch (IOException | SQLException | RuntimeException e) {
            Files.deleteIfExists(temporario);
            throw e;
        }

        // Só aparece como backup válido depois de concluído por inteiro.
        Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);

        removerAntigos();
        return destino;
    }

    /** Lista os backups existentes, do mais recente para o mais antigo. */
    public List<Path> listarBackups() throws IOException {
        if (!Files.isDirectory(diretorio)) {
            return new ArrayList<>();
        }
        try (Stream<Path> arquivos = Files.list(diretorio)) {
            return arquivos
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith(PREFIXO) && n.endsWith(EXTENSAO);
                    })
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList();
        }
    }

    private void removerAntigos() throws IOException {
        if (manter <= 0) {
            return; // 0 ou negativo = nunca apagar
        }
        List<Path> backups = listarBackups();
        for (int i = manter; i < backups.size(); i++) {
            Files.deleteIfExists(backups.get(i));
            log.info("Backup antigo removido: {}", backups.get(i).getFileName());
        }
    }

    /** O backup contém dados pessoais e hashes de senha: só o dono do arquivo lê (Linux/macOS). */
    private void restringirPermissoes(Path arquivo) {
        try {
            Files.setPosixFilePermissions(arquivo, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignorada) {
            // Windows: sem permissões POSIX, segue normalmente
        }
    }
}
