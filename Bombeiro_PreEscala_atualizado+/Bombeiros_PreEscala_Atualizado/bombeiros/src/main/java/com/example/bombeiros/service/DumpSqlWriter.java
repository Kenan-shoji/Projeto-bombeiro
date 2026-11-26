package com.example.bombeiros.service;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Gera um script SQL (estrutura + dados) de todas as tabelas do banco
 * da conexão informada, usando somente JDBC. Não depende do "mysqldump".
 *
 * O script gerado pode ser restaurado com:
 *   mysql -u root -p escala_bombeiros < backup_AAAAMMDD_HHMMSS.sql
 */
public class DumpSqlWriter {

    private static final int LINHAS_POR_INSERT = 100;

    public void escrever(Connection conn, Writer out) throws SQLException, IOException {
        boolean autoCommitOriginal = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (Statement st = conn.createStatement()) {
            // Fotografia consistente: todas as tabelas refletem o mesmo instante,
            // mesmo que o sistema continue sendo usado durante o backup.
            st.execute("START TRANSACTION WITH CONSISTENT SNAPSHOT");

            out.write("-- Backup do banco '" + conn.getCatalog() + "'\n");
            out.write("-- Gerado em " + LocalDateTime.now() + "\n\n");
            out.write("SET NAMES utf8mb4;\n");
            out.write("SET FOREIGN_KEY_CHECKS=0;\n");
            out.write("SET UNIQUE_CHECKS=0;\n\n");

            for (String tabela : listarTabelas(st)) {
                escreverTabela(conn, st, tabela, out);
            }

            out.write("SET UNIQUE_CHECKS=1;\n");
            out.write("SET FOREIGN_KEY_CHECKS=1;\n");
            out.flush();
        } finally {
            try {
                conn.rollback(); // encerra a transação de leitura
            } catch (SQLException ignorada) {
                // nada a fazer
            }
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    private List<String> listarTabelas(Statement st) throws SQLException {
        List<String> tabelas = new ArrayList<>();
        try (ResultSet rs = st.executeQuery(
                "SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
            while (rs.next()) {
                tabelas.add(rs.getString(1));
            }
        }
        return tabelas;
    }

    private void escreverTabela(Connection conn, Statement st, String tabela, Writer out)
            throws SQLException, IOException {

        String nomeTabela = identificador(tabela);

        out.write("-- ----------------------------------------\n");
        out.write("-- Tabela " + nomeTabela + "\n");
        out.write("-- ----------------------------------------\n");
        out.write("DROP TABLE IF EXISTS " + nomeTabela + ";\n");

        try (ResultSet rs = st.executeQuery("SHOW CREATE TABLE " + nomeTabela)) {
            if (rs.next()) {
                out.write(rs.getString(2) + ";\n\n");
            }
        }

        try (Statement dados = conn.createStatement(
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            dados.setFetchSize(Integer.MIN_VALUE); // lê linha a linha, sem carregar tudo na memória

            try (ResultSet rs = dados.executeQuery("SELECT * FROM " + nomeTabela)) {
                ResultSetMetaData meta = rs.getMetaData();
                int colunas = meta.getColumnCount();

                StringBuilder cabecalho = new StringBuilder("INSERT INTO " + nomeTabela + " (");
                for (int i = 1; i <= colunas; i++) {
                    if (i > 1) cabecalho.append(", ");
                    cabecalho.append(identificador(meta.getColumnName(i)));
                }
                cabecalho.append(") VALUES\n");

                int linhasNoComando = 0;
                while (rs.next()) {
                    if (linhasNoComando == 0) {
                        out.write(cabecalho.toString());
                    } else {
                        out.write(",\n");
                    }

                    out.write("(");
                    for (int i = 1; i <= colunas; i++) {
                        if (i > 1) out.write(", ");
                        out.write(valor(rs, i, meta.getColumnType(i)));
                    }
                    out.write(")");

                    linhasNoComando++;
                    if (linhasNoComando == LINHAS_POR_INSERT) {
                        out.write(";\n");
                        linhasNoComando = 0;
                    }
                }
                if (linhasNoComando > 0) {
                    out.write(";\n");
                }
            }
        }
        out.write("\n");
    }

    private String valor(ResultSet rs, int i, int tipo) throws SQLException {
        switch (tipo) {
            case Types.BIT, Types.BOOLEAN,
                 Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
                 Types.FLOAT, Types.REAL, Types.DOUBLE,
                 Types.DECIMAL, Types.NUMERIC: {
                Object o = rs.getObject(i);
                if (o == null) return "NULL";
                if (o instanceof Boolean b) return b ? "1" : "0";
                if (o instanceof byte[] bytes) return hexa(bytes);
                return o.toString();
            }
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB: {
                byte[] bytes = rs.getBytes(i);
                if (bytes == null) return "NULL";
                return hexa(bytes);
            }
            default: {
                String s = rs.getString(i);
                if (s == null) return "NULL";
                return texto(s);
            }
        }
    }

    private String hexa(byte[] bytes) {
        return bytes.length == 0 ? "''" : "0x" + HexFormat.of().formatHex(bytes);
    }

    private String identificador(String nome) {
        return "`" + nome.replace("`", "``") + "`";
    }

    private String texto(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('\'');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '\0' -> sb.append("\\0");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\u001a' -> sb.append("\\Z");
                default -> sb.append(c);
            }
        }
        return sb.append('\'').toString();
    }
}
