package com.example.sd009thuan.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Đồng bộ CSDL cũ: trước đây gioi_tinh của khách hàng là NOT NULL DEFAULT 1,
 * khiến tài khoản đăng ký chưa chọn giới tính luôn bị ghi thành Nam.
 * Migration này chạy an toàn nhiều lần và chỉ thay đổi đúng cột liên quan.
 */
@Component
public class CustomerGenderSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public CustomerGenderSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
            DECLARE @defaultConstraint sysname;
            SELECT @defaultConstraint = dc.name
            FROM sys.default_constraints dc
            JOIN sys.columns c
              ON c.default_object_id = dc.object_id
            JOIN sys.tables t
              ON t.object_id = c.object_id
            WHERE t.name = 'khach_hang' AND c.name = 'gioi_tinh';

            IF @defaultConstraint IS NOT NULL
                EXEC('ALTER TABLE khach_hang DROP CONSTRAINT [' + @defaultConstraint + ']');

            IF EXISTS (
                SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'khach_hang'
                  AND COLUMN_NAME = 'gioi_tinh'
                  AND IS_NULLABLE = 'NO'
            )
                ALTER TABLE khach_hang ALTER COLUMN gioi_tinh BIT NULL;
            """);
    }
}
