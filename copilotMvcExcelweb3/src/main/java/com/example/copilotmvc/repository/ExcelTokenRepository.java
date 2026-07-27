package com.example.copilotmvc.repository;

import com.example.copilotmvc.model.TokenRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelTokenRepository {

    private static final Logger log = LoggerFactory.getLogger(ExcelTokenRepository.class);

    @Value("${excel.file.path:./data/tokens.xlsx}")
    private String excelFilePath;

    private final Object lock = new Object();

    @PostConstruct
    public void init() throws Exception {
        File f = new File(excelFilePath);
        if (!f.exists()) {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (Workbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(f)) {
                Sheet sheet = wb.createSheet("tokens");
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("id");
                header.createCell(1).setCellValue("name");
                header.createCell(2).setCellValue("tokenValue");
                wb.write(out);
            }
            log.info("Created new tokens excel file at {}", f.getAbsolutePath());
        }
    }

    public List<TokenRecord> findAll() {
        synchronized (lock) {
            List<TokenRecord> out = new ArrayList<>();
            File f = new File(excelFilePath);
            if (!f.exists()) return out;
            try (FileInputStream fis = new FileInputStream(f); Workbook wb = new XSSFWorkbook(fis)) {
                Sheet sheet = wb.getSheetAt(0);
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    Cell idCell = row.getCell(0);
                    Cell nameCell = row.getCell(1);
                    Cell tokenCell = row.getCell(2);
                    long id = idCell == null ? 0L : (long) idCell.getNumericCellValue();
                    String name = nameCell == null ? "" : nameCell.getStringCellValue();
                    String token = tokenCell == null ? "" : tokenCell.getStringCellValue();
                    out.add(new TokenRecord(id, name, token));
                }
            } catch (Exception e) {
                log.error("Error reading tokens.xlsx", e);
            }
            return out;
        }
    }

    public TokenRecord add(String name, String tokenValue) {
        synchronized (lock) {
            try {
                File f = new File(excelFilePath);
                Workbook wb;
                Sheet sheet;
                if (!f.exists()) {
                    File parent = f.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    wb = new XSSFWorkbook();
                    sheet = wb.createSheet("tokens");
                    Row header = sheet.createRow(0);
                    header.createCell(0).setCellValue("id");
                    header.createCell(1).setCellValue("name");
                    header.createCell(2).setCellValue("tokenValue");
                } else {
                    try (FileInputStream fis = new FileInputStream(f)) {
                        wb = new XSSFWorkbook(fis);
                    }
                    sheet = wb.getSheetAt(0);
                }

                long maxId = 0L;
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    Cell idCell = row.getCell(0);
                    if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                        long val = (long) idCell.getNumericCellValue();
                        if (val > maxId) maxId = val;
                    }
                }
                long newId = maxId + 1;
                Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
                newRow.createCell(0).setCellValue(newId);
                newRow.createCell(1).setCellValue(name == null ? "" : name);
                newRow.createCell(2).setCellValue(tokenValue == null ? "" : tokenValue);

                try (FileOutputStream fos = new FileOutputStream(f)) {
                    wb.write(fos);
                }
                wb.close();
                return new TokenRecord(newId, name, tokenValue);
            } catch (Exception e) {
                log.error("Error adding token", e);
                return null;
            }
        }
    }

    public boolean deleteById(long id) {
        synchronized (lock) {
            File f = new File(excelFilePath);
            if (!f.exists()) return false;
            try (FileInputStream fis = new FileInputStream(f); Workbook wb = new XSSFWorkbook(fis)) {
                Sheet sheet = wb.getSheetAt(0);
                int foundRow = -1;
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    Cell idCell = row.getCell(0);
                    if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                        long val = (long) idCell.getNumericCellValue();
                        if (val == id) { foundRow = r; break; }
                    }
                }
                if (foundRow == -1) return false;
                int lastRow = sheet.getLastRowNum();
                if (foundRow >= 0 && foundRow < lastRow) {
                    sheet.shiftRows(foundRow + 1, lastRow, -1);
                } else if (foundRow == lastRow) {
                    Row rowToRemove = sheet.getRow(foundRow);
                    if (rowToRemove != null) sheet.removeRow(rowToRemove);
                }
                try (FileOutputStream fos = new FileOutputStream(f)) { wb.write(fos); }
                return true;
            } catch (Exception e) {
                log.error("Error deleting token by id", e);
                return false;
            }
        }
    }

    public boolean existsByTokenValue(String tokenValue) {
        if (tokenValue == null) return false;
        List<TokenRecord> all = findAll();
        for (TokenRecord t : all) if (tokenValue.equals(t.getTokenValue())) return true;
        return false;
    }
}
