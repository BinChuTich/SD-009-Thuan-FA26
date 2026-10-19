package com.example.sd009thuan;

import com.example.sd009thuan.service.ExcelImportService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;

public class GenerateExcelTemplateFileTest {

    @Test
    public void generateFile() throws Exception {
        ExcelImportService service = new ExcelImportService(
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null, null
        );
        byte[] bytes = service.generateProductTemplate();
        File outFile = new File("Mau_Import_SanPham.xlsx");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(bytes);
        }
        System.out.println("FILE_CREATED_SUCCESS: " + outFile.getAbsolutePath() + " (" + bytes.length + " bytes)");
    }
}

