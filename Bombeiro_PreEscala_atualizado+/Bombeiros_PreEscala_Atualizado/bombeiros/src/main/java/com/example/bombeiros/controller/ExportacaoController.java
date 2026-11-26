package com.example.bombeiros.controller;

import com.example.bombeiros.model.Participacao;
import com.example.bombeiros.repository.ParticipacaoRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;
import java.time.YearMonth;
import java.util.List;

@Controller
public class ExportacaoController {
    private final ParticipacaoRepository repository;
    public ExportacaoController(ParticipacaoRepository repository) { this.repository = repository; }

    @GetMapping("/exportar/escalas.xlsx")
    @Transactional(readOnly = true)
    public void exportar(@RequestParam("mes") String mes, HttpSession session, HttpServletResponse response) throws IOException {
        if (session.getAttribute("usuarioLogadoId") == null || !"ADMINISTRADOR".equalsIgnoreCase(String.valueOf(session.getAttribute("usuarioLogadoPerfil")))) {
            response.sendError(403, "Acesso permitido somente ao administrador."); return;
        }
        YearMonth periodo = YearMonth.parse(mes);
        List<Participacao> registros = repository.findAll().stream()
                .filter(p -> p.getEscalaPlantao() != null && p.getEscalaPlantao().getData() != null)
                .filter(p -> YearMonth.from(p.getEscalaPlantao().getData()).equals(periodo))
                .toList();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Reservas");
            String[] titulos = {"Bombeiro", "Tipo", "Data", "Turno", "Status"};
            Row header = sheet.createRow(0);
            CellStyle estilo = wb.createCellStyle(); Font fonte = wb.createFont(); fonte.setBold(true); estilo.setFont(fonte);
            for (int i=0;i<titulos.length;i++) { Cell c=header.createCell(i); c.setCellValue(titulos[i]); c.setCellStyle(estilo); }
            int linha=1;
            for (Participacao p : registros) {
                Row r=sheet.createRow(linha++);
                r.createCell(0).setCellValue(p.getBombeiro()==null?"":p.getBombeiro().getNome());
                r.createCell(1).setCellValue(p.getBombeiro()==null?"":p.getBombeiro().getTipo());
                r.createCell(2).setCellValue(p.getEscalaPlantao().getData().toString());
                r.createCell(3).setCellValue(p.getEscalaPlantao().getTurno());
                r.createCell(4).setCellValue(p.getStatusReserva());
            }
            for(int i=0;i<titulos.length;i++) sheet.autoSizeColumn(i);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=escalas-"+mes+".xlsx");
            wb.write(response.getOutputStream());
        }
    }
}
