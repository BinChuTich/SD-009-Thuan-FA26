USE master;
GO

/* =========================================================
   0. XÓA DATABASE CŨ NẾU ĐÃ TỒN TẠI
   ========================================================= */

IF DB_ID('FF_Tshirt') IS NOT NULL
BEGIN
    ALTER DATABASE FF_Tshirt SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE FF_Tshirt;
END
GO

/* =========================================================
   1. TẠO DATABASE
   ========================================================= */

CREATE DATABASE FF_Tshirt;
GO

USE FF_Tshirt;
GO


/* =========================================================
   2. CÁC BẢNG DANH MỤC THUỘC TÍNH SẢN PHẨM
   ========================================================= */

/* -------------------------
   Xuất xứ
   ------------------------- */
CREATE TABLE xuat_xu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_xuat_xu VARCHAR(50) UNIQUE,
    ten_xuat_xu NVARCHAR(255) NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_xuat_xu_trang_thai CHECK (trang_thai IN (0,1))
);
GO

/* -------------------------
   Chất liệu
   ------------------------- */
CREATE TABLE chat_lieu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_chat_lieu VARCHAR(50) UNIQUE,
    ten_chat_lieu NVARCHAR(255) NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_chat_lieu_trang_thai CHECK (trang_thai IN (0,1))
);
GO

/* -------------------------
   Thương hiệu
   ------------------------- */
CREATE TABLE thuong_hieu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_thuong_hieu VARCHAR(50) UNIQUE,
    ten_thuong_hieu NVARCHAR(255) NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_thuong_hieu_trang_thai CHECK (trang_thai IN (0,1))
);
GO

/* -------------------------
   Danh mục
   ------------------------- */
CREATE TABLE danh_muc (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_danh_muc VARCHAR(50) UNIQUE,
    ten_danh_muc NVARCHAR(255) NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_danh_muc_trang_thai CHECK (trang_thai IN (0,1))
);
GO

/* -------------------------
   Cổ áo
   ------------------------- */
CREATE TABLE co_ao (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_co_ao VARCHAR(50) UNIQUE,
    ten_co_ao NVARCHAR(255) NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_co_ao_trang_thai CHECK (trang_thai IN (0,1))
);
GO

/* -------------------------
   Tay áo
   ------------------------- */
CREATE TABLE tay_ao (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_tay_ao VARCHAR(50) UNIQUE,
    ten_tay_ao NVARCHAR(255) NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_tay_ao_trang_thai CHECK (trang_thai IN (0,1))
);
GO

/* -------------------------
   Họa tiết (Bổ sung khớp ERD)
   ------------------------- */
CREATE TABLE hoa_tiet (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_hoa_tiet VARCHAR(50) UNIQUE,
    ten_hoa_tiet NVARCHAR(255) NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_hoa_tiet_trang_thai CHECK (trang_thai IN (0,1))
);
GO

/* -------------------------
   Kích cỡ
   ------------------------- */
CREATE TABLE kich_co (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_kich_co VARCHAR(50) UNIQUE,
    ten_kich_co NVARCHAR(255) NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_kich_co_trang_thai CHECK (trang_thai IN (0,1))
);
GO

/* -------------------------
   Màu sắc (Đã sửa ma_hoa -> ma_hex)
   ------------------------- */
CREATE TABLE mau_sac (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_mau_sac VARCHAR(50) UNIQUE,
    ten_mau_sac NVARCHAR(255) NOT NULL,
    ma_hex VARCHAR(50),
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_mau_sac_trang_thai CHECK (trang_thai IN (0,1))
);
GO


/* =========================================================
   3. SẢN PHẨM & BIẾN THỂ
   ========================================================= */

CREATE TABLE san_pham (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_xuat_xu BIGINT,
    id_chat_lieu BIGINT,
    id_thuong_hieu BIGINT,
    id_danh_muc BIGINT,
    id_co_ao BIGINT,
    id_tay_ao BIGINT,
    id_hoa_tiet BIGINT,
    ma_san_pham VARCHAR(50) UNIQUE,
    ten_san_pham NVARCHAR(255) NOT NULL,
    ngay_tao DATETIME2 NOT NULL DEFAULT GETDATE(),
    ngay_sua DATETIME2,
    ngay_cap_nhat DATETIME2,
    nguoi_tao NVARCHAR(100),
    nguoi_cap_nhat NVARCHAR(100),
    mo_ta NVARCHAR(MAX),
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT FK_san_pham_xuat_xu FOREIGN KEY (id_xuat_xu) REFERENCES xuat_xu(id),
    CONSTRAINT FK_san_pham_chat_lieu FOREIGN KEY (id_chat_lieu) REFERENCES chat_lieu(id),
    CONSTRAINT FK_san_pham_thuong_hieu FOREIGN KEY (id_thuong_hieu) REFERENCES thuong_hieu(id),
    CONSTRAINT FK_san_pham_danh_muc FOREIGN KEY (id_danh_muc) REFERENCES danh_muc(id),
    CONSTRAINT FK_san_pham_co_ao FOREIGN KEY (id_co_ao) REFERENCES co_ao(id),
    CONSTRAINT FK_san_pham_tay_ao FOREIGN KEY (id_tay_ao) REFERENCES tay_ao(id),
    CONSTRAINT FK_san_pham_hoa_tiet FOREIGN KEY (id_hoa_tiet) REFERENCES hoa_tiet(id),
    CONSTRAINT CK_san_pham_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE hinh_anh (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham BIGINT NOT NULL,
    ten_anh NVARCHAR(255),
    duong_dan VARCHAR(500),
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT FK_hinh_anh_san_pham FOREIGN KEY (id_san_pham) REFERENCES san_pham(id),
    CONSTRAINT CK_hinh_anh_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE chi_tiet_san_pham (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham BIGINT NOT NULL,
    id_kich_co BIGINT NOT NULL,
    id_mau_sac BIGINT NOT NULL,
    ma_chi_tiet_san_pham VARCHAR(50) UNIQUE,
    so_luong INT NOT NULL DEFAULT 0,
    gia_ban DECIMAL(18,2) NOT NULL DEFAULT 0,
    trang_thai INT NOT NULL DEFAULT 1,
    ngay_tao DATETIME2 NOT NULL DEFAULT GETDATE(),
    nguoi_tao NVARCHAR(100),
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat NVARCHAR(100),

    CONSTRAINT FK_ctsp_san_pham FOREIGN KEY (id_san_pham) REFERENCES san_pham(id),
    CONSTRAINT FK_ctsp_kich_co FOREIGN KEY (id_kich_co) REFERENCES kich_co(id),
    CONSTRAINT FK_ctsp_mau_sac FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id),
    CONSTRAINT UQ_ctsp_san_pham_size_mau UNIQUE (id_san_pham, id_kich_co, id_mau_sac),
    CONSTRAINT CK_ctsp_so_luong CHECK (so_luong >= 0),
    CONSTRAINT CK_ctsp_gia_ban CHECK (gia_ban >= 0),
    CONSTRAINT CK_ctsp_trang_thai CHECK (trang_thai IN (0,1))
);
GO


/* =========================================================
   4. ĐỢT GIẢM GIÁ
   ========================================================= */

CREATE TABLE dot_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_dot_giam_gia VARCHAR(50) UNIQUE,
    ten_dot_giam_gia NVARCHAR(255) NOT NULL,
    phan_tram_giam DECIMAL(5,2),
    ngay_bat_dau DATETIME2,
    ngay_ket_thuc DATETIME2,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_dot_giam_gia_phan_tram CHECK (phan_tram_giam IS NULL OR (phan_tram_giam > 0 AND phan_tram_giam <= 100)),
    CONSTRAINT CK_dot_giam_gia_thoi_gian CHECK (ngay_ket_thuc IS NULL OR ngay_bat_dau IS NULL OR ngay_ket_thuc > ngay_bat_dau),
    CONSTRAINT CK_dot_giam_gia_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE chi_tiet_dot_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_dot_giam_gia BIGINT NOT NULL,
    id_chi_tiet_san_pham BIGINT NOT NULL,
    phan_tram_giam_bien_the DECIMAL(5,2),
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT FK_ctdgg_dot_giam_gia FOREIGN KEY (id_dot_giam_gia) REFERENCES dot_giam_gia(id),
    CONSTRAINT FK_ctdgg_chi_tiet_san_pham FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id),
    CONSTRAINT UQ_ctdgg_dot_giam_ctsp UNIQUE (id_dot_giam_gia, id_chi_tiet_san_pham),
    CONSTRAINT CK_ctdgg_trang_thai CHECK (trang_thai IN (0,1))
);
GO


/* =========================================================
   5. NGƯỜI DÙNG & VOUCHER
   ========================================================= */

CREATE TABLE vai_tro (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_vai_tro VARCHAR(50) UNIQUE,
    ten_vai_tro NVARCHAR(100) NOT NULL,
    mo_ta NVARCHAR(MAX),
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_vai_tro_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE nhan_vien (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_vai_tro BIGINT NOT NULL,
    ma_nhan_vien VARCHAR(50) UNIQUE,
    ten_tai_khoan VARCHAR(100) UNIQUE,
    ten_nhan_vien NVARCHAR(255) NOT NULL,
    mat_khau VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    so_dien_thoai VARCHAR(20),
    anh_nhan_vien VARCHAR(500),
    gioi_tinh BIT DEFAULT 1,
    ngay_sinh DATE,
    que_quan NVARCHAR(255),
    phuong NVARCHAR(255),
    dia_chi_cu_the NVARCHAR(MAX),
    trang_thai INT NOT NULL DEFAULT 1,
    ngay_tao DATETIME2 NOT NULL DEFAULT GETDATE(),
    nguoi_tao NVARCHAR(100),
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat NVARCHAR(100),

    CONSTRAINT FK_nhan_vien_vai_tro FOREIGN KEY (id_vai_tro) REFERENCES vai_tro(id),
    CONSTRAINT CK_nhan_vien_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE phieu_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_phieu_giam_gia VARCHAR(50) UNIQUE,
    ten_phieu_giam_gia NVARCHAR(255) NOT NULL,
    loai_phieu_giam_gia INT NOT NULL,
    gia_tri_giam_gia DECIMAL(18,2) NOT NULL DEFAULT 0,
    giam_toi_da DECIMAL(18,2),
    hoa_don_toi_thieu DECIMAL(18,2) DEFAULT 0,
    so_luong_su_dung INT NOT NULL DEFAULT 0,
    ngay_bat_dau DATETIME2,
    ngay_ket_thuc DATETIME2,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_pgg_loai CHECK (loai_phieu_giam_gia IN (1,2)),
    CONSTRAINT CK_pgg_gia_tri CHECK (gia_tri_giam_gia >= 0),
    CONSTRAINT CK_pgg_giam_toi_da CHECK (giam_toi_da IS NULL OR giam_toi_da >= 0),
    CONSTRAINT CK_pgg_hoa_don_toi_thieu CHECK (hoa_don_toi_thieu >= 0),
    CONSTRAINT CK_pgg_so_luong CHECK (so_luong_su_dung >= 0),
    CONSTRAINT CK_pgg_thoi_gian CHECK (ngay_ket_thuc IS NULL OR ngay_bat_dau IS NULL OR ngay_ket_thuc > ngay_bat_dau),
    CONSTRAINT CK_pgg_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE khach_hang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_khach_hang VARCHAR(50) UNIQUE,
    ten_tai_khoan VARCHAR(100) UNIQUE,
    ten_khach_hang NVARCHAR(255) NOT NULL,
    email VARCHAR(255),
    mat_khau VARCHAR(255),
    so_dien_thoai VARCHAR(20),
    ngay_sinh DATE,
    gioi_tinh BIT DEFAULT 1,
    trang_thai INT NOT NULL DEFAULT 1,
    ngay_tao DATETIME2 NOT NULL DEFAULT GETDATE(),
    nguoi_tao NVARCHAR(100),
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat NVARCHAR(100),

    CONSTRAINT CK_khach_hang_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE dia_chi_khach_hang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang BIGINT NOT NULL,
    ma_dia_chi VARCHAR(50),
    ten_dia_chi NVARCHAR(255),
    thanh_pho NVARCHAR(100),
    phuong NVARCHAR(100),
    dia_chi_cu_the NVARCHAR(MAX),
    mac_dinh BIT NOT NULL DEFAULT 0,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT FK_dia_chi_khach_hang FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    CONSTRAINT CK_dia_chi_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE khach_hang_phieu_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang BIGINT NOT NULL,
    id_phieu_giam_gia BIGINT NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT FK_kh_pgg_khach_hang FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    CONSTRAINT FK_kh_pgg_phieu FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id),
    CONSTRAINT UQ_kh_pgg UNIQUE (id_khach_hang, id_phieu_giam_gia),
    CONSTRAINT CK_kh_pgg_trang_thai CHECK (trang_thai IN (0,1))
);
GO


/* =========================================================
   6. HÓA ĐƠN & THANH TOÁN
   ========================================================= */

CREATE TABLE hoa_don (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_nhan_vien BIGINT NULL,
    id_khach_hang BIGINT NULL,
    id_phieu_giam_gia BIGINT NULL,
    ma_hoa_don VARCHAR(50) UNIQUE,
    loai_don INT NOT NULL,
    phi_van_chuyen DECIMAL(18,2) NOT NULL DEFAULT 0,
    tong_tien DECIMAL(18,2) NOT NULL DEFAULT 0,
    tien_sau_giam_gia DECIMAL(18,2) NOT NULL DEFAULT 0,
    ten_khach_hang NVARCHAR(255),
    so_dien_thoai_khach_hang VARCHAR(20),
    dia_chi_nhan_hang NVARCHAR(MAX),
    ngay_tao DATETIME2 NOT NULL DEFAULT GETDATE(),
    nguoi_tao NVARCHAR(100),
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat NVARCHAR(100),
    trang_thai INT NOT NULL DEFAULT 1,
    ghi_chu NVARCHAR(MAX),

    CONSTRAINT FK_hoa_don_nhan_vien FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    CONSTRAINT FK_hoa_don_khach_hang FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    CONSTRAINT FK_hoa_don_phieu_giam_gia FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id),
    CONSTRAINT CK_hoa_don_loai_don CHECK (loai_don IN (1,2)),
    CONSTRAINT CK_hoa_don_phi_van_chuyen CHECK (phi_van_chuyen >= 0),
    CONSTRAINT CK_hoa_don_tong_tien CHECK (tong_tien >= 0),
    CONSTRAINT CK_hoa_don_tien_sau_giam_gia CHECK (tien_sau_giam_gia >= 0),
    CONSTRAINT CK_hoa_don_trang_thai CHECK (trang_thai BETWEEN 1 AND 6)
);
GO

CREATE TABLE chi_tiet_hoa_don (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    id_chi_tiet_san_pham BIGINT NOT NULL,
    ma_hoa_don_chi_tiet VARCHAR(50),
    don_gia DECIMAL(18,2) NOT NULL DEFAULT 0,
    so_luong INT NOT NULL DEFAULT 1,
    thanh_tien DECIMAL(18,2) NOT NULL DEFAULT 0,
    ghi_chu NVARCHAR(MAX),
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT FK_cthd_hoa_don FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    CONSTRAINT FK_cthd_ctsp FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id),
    CONSTRAINT CK_cthd_don_gia CHECK (don_gia >= 0),
    CONSTRAINT CK_cthd_so_luong CHECK (so_luong > 0),
    CONSTRAINT CK_cthd_thanh_tien CHECK (thanh_tien >= 0),
    CONSTRAINT CK_cthd_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE lich_su_hoa_don (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    trang_thai INT,
    thoi_gian DATETIME2 NOT NULL DEFAULT GETDATE(),
    ghi_chu NVARCHAR(MAX),
    nguoi_thuc_hien NVARCHAR(100),
    vai_tro_nguoi_thuc_hien NVARCHAR(100),

    CONSTRAINT FK_lich_su_hoa_don FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id)
);
GO

CREATE TABLE phuong_thuc_thanh_toan (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten_phuong_thuc NVARCHAR(100) NOT NULL,
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT CK_pttt_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE hinh_thuc_thanh_toan (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    id_phuong_thuc BIGINT NOT NULL,
    so_tien DECIMAL(18,2) NOT NULL DEFAULT 0,
    ma_giao_dich VARCHAR(100),
    thoi_gian DATETIME2 NOT NULL DEFAULT GETDATE(),
    trang_thai INT NOT NULL DEFAULT 1,

    CONSTRAINT FK_httt_hoa_don FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    CONSTRAINT FK_httt_phuong_thuc FOREIGN KEY (id_phuong_thuc) REFERENCES phuong_thuc_thanh_toan(id),
    CONSTRAINT CK_httt_so_tien CHECK (so_tien >= 0),
    CONSTRAINT CK_httt_trang_thai CHECK (trang_thai IN (0,1))
);
GO

CREATE TABLE lich_su_thanh_toan (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    so_tien DECIMAL(18,2) NOT NULL DEFAULT 0,
    ma_giao_dich VARCHAR(100),
    thoi_gian DATETIME2 NOT NULL DEFAULT GETDATE(),
    trang_thai INT NOT NULL DEFAULT 1,
    mo_ta NVARCHAR(MAX),

    CONSTRAINT FK_lstt_hoa_don FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    CONSTRAINT CK_lstt_so_tien CHECK (so_tien >= 0),
    CONSTRAINT CK_lstt_trang_thai CHECK (trang_thai IN (0,1))
);
GO


/* =========================================================
   7. INDEX TĂNG TỐC TRUY VẤN
   ========================================================= */

CREATE INDEX IX_san_pham_hoa_tiet ON san_pham(id_hoa_tiet);
GO
CREATE INDEX IX_hinh_anh_san_pham ON hinh_anh(id_san_pham);
GO
CREATE INDEX IX_ctsp_san_pham ON chi_tiet_san_pham(id_san_pham);
GO
CREATE INDEX IX_ctsp_kich_co ON chi_tiet_san_pham(id_kich_co);
GO
CREATE INDEX IX_ctsp_mau_sac ON chi_tiet_san_pham(id_mau_sac);
GO
CREATE INDEX IX_ctdgg_dot_giam_gia ON chi_tiet_dot_giam_gia(id_dot_giam_gia);
GO
CREATE INDEX IX_ctdgg_ctsp ON chi_tiet_dot_giam_gia(id_chi_tiet_san_pham);
GO
CREATE INDEX IX_dia_chi_khach_hang ON dia_chi_khach_hang(id_khach_hang);
GO
CREATE INDEX IX_hoa_don_khach_hang ON hoa_don(id_khach_hang);
GO
CREATE INDEX IX_hoa_don_nhan_vien ON hoa_don(id_nhan_vien);
GO
CREATE INDEX IX_hoa_don_trang_thai ON hoa_don(trang_thai);
GO
CREATE INDEX IX_hoa_don_ngay_tao ON hoa_don(ngay_tao);
GO
CREATE INDEX IX_cthd_hoa_don ON chi_tiet_hoa_don(id_hoa_don);
GO
CREATE INDEX IX_cthd_ctsp ON chi_tiet_hoa_don(id_chi_tiet_san_pham);
GO
CREATE INDEX IX_httt_hoa_don ON hinh_thuc_thanh_toan(id_hoa_don);
GO
CREATE INDEX IX_lstt_hoa_don ON lich_su_thanh_toan(id_hoa_don);
GO

/* =========================================================
   8. NHẬP DỮ LIỆU MẪU CHO TẤT CẢ CÁC BẢNG (26 BẢNG)
   ========================================================= */

-- 1. xuat_xu
INSERT INTO xuat_xu (ma_xuat_xu, ten_xuat_xu) VALUES
('XX01', N'Việt Nam'),
('XX02', N'Thái Lan'),
('XX03', N'Trung Quốc'),
('XX04', N'Hàn Quốc');
GO

-- 2. chat_lieu
INSERT INTO chat_lieu (ma_chat_lieu, ten_chat_lieu) VALUES
('CL01', N'Cotton 100%'),
('CL02', N'Cá sấu'),
('CL03', N'Cotton Compact'),
('CL04', N'Polyester');
GO

-- 3. thuong_hieu
INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu) VALUES
('TH01', N'FF Shirt'),
('TH02', N'Local Brand'),
('TH03', N'Basic Wear');
GO

-- 4. danh_muc
INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc) VALUES
('DM01', N'Áo phông nam'),
('DM02', N'Áo phông nữ'),
('DM03', N'Áo phông unisex'),
('DM04', N'Áo polo');
GO

-- 5. co_ao
INSERT INTO co_ao (ma_co_ao, ten_co_ao) VALUES
('CA01', N'Cổ tròn'),
('CA02', N'Cổ tim'),
('CA03', N'Cổ bẻ (Polo)');
GO

-- 6. tay_ao
INSERT INTO tay_ao (ma_tay_ao, ten_tay_ao) VALUES
('TA01', N'Tay ngắn'),
('TA02', N'Tay dài'),
('TA03', N'Tay lỡ');
GO

-- 7. hoa_tiet
INSERT INTO hoa_tiet (ma_hoa_tiet, ten_hoa_tiet) VALUES
('HT01', N'Trơn'),
('HT02', N'In hình mặt trước'),
('HT03', N'Kẻ sọc ngang');
GO

-- 8. kich_co
INSERT INTO kich_co (ma_kich_co, ten_kich_co) VALUES
('KC01', N'S'),
('KC02', N'M'),
('KC03', N'L'),
('KC04', N'XL'),
('KC05', N'XXL');
GO

-- 9. mau_sac
INSERT INTO mau_sac (ma_mau_sac, ten_mau_sac, ma_hex) VALUES
('MS01', N'Đen', '#000000'),
('MS02', N'Trắng', '#FFFFFF'),
('MS03', N'Xám', '#808080'),
('MS04', N'Đỏ', '#FF0000'),
('MS05', N'Xanh navy', '#000080');
GO

-- 10. vai_tro
INSERT INTO vai_tro (ma_vai_tro, ten_vai_tro, mo_ta) VALUES
('ADMIN', N'Quản trị viên', N'Toàn quyền hệ thống'),
('QL', N'Quản lý', N'Quản lý cửa hàng và kho hàng'),
('NV', N'Nhân viên', N'Nhân viên bán hàng tại quầy');
GO

-- 11. phuong_thuc_thanh_toan
INSERT INTO phuong_thuc_thanh_toan (ten_phuong_thuc) VALUES
(N'Tiền mặt'),
(N'Chuyển khoản ngân hàng'),
(N'Quét mã QR'),
(N'Thẻ ATM / VISA');
GO

-- 12. dot_giam_gia
INSERT INTO dot_giam_gia (ma_dot_giam_gia, ten_dot_giam_gia, phan_tram_giam, ngay_bat_dau, ngay_ket_thuc) VALUES
('DGG01', N'Chương trình Giảm giá Mùa hè', 10.00, '2026-06-01 00:00:00', '2026-08-31 23:59:59'),
('DGG02', N'Khuyến mãi Khai trương Store', 20.00, '2026-09-01 00:00:00', '2026-09-15 23:59:59');
GO

-- 13. phieu_giam_gia
INSERT INTO phieu_giam_gia (ma_phieu_giam_gia, ten_phieu_giam_gia, loai_phieu_giam_gia, gia_tri_giam_gia, giam_toi_da, hoa_don_toi_thieu, so_luong_su_dung, ngay_bat_dau, ngay_ket_thuc) VALUES
('PGG01', N'Voucher Giảm 50k', 2, 50000.00, 50000.00, 200000.00, 100, '2026-01-01 00:00:00', '2026-12-31 23:59:59'),
('PGG02', N'Voucher Giảm 15% cho khách mới', 1, 15.00, 100000.00, 300000.00, 50, '2026-01-01 00:00:00', '2026-12-31 23:59:59');
GO

-- 14. nhan_vien
INSERT INTO nhan_vien (id_vai_tro, ma_nhan_vien, ten_tai_khoan, ten_nhan_vien, mat_khau, email, so_dien_thoai, gioi_tinh, ngay_sinh, que_quan, phuong, dia_chi_cu_the, nguoi_tao) VALUES
(1, 'NV001', 'admin', N'Nguyễn Văn Admin', '123456', 'admin@ffshirt.com', '0900000001', 1, '1995-05-10', N'Hà Nội', N'Phường Cống Vị', N'Số 123 Đường Đội Cấn', N'System'),
(2, 'NV002', 'quanly01', N'Trần Thị Quan Lý', '123456', 'quanly@ffshirt.com', '0900000002', 0, '1998-08-20', N'Hà Nội', N'Phường Hàng Bông', N'Số 45 Phố Hàng Bông', N'admin'),
(3, 'NV003', 'nvbanhang', N'Lê Văn Bán Hàng', '123456', 'nhanvien@ffshirt.com', '0900000003', 1, '2001-02-15', N'Bắc Ninh', N'Phường Tiền An', N'Số 12 Đường Ngô Gia Tự', N'admin');
GO

-- 15. khach_hang
INSERT INTO khach_hang (ma_khach_hang, ten_tai_khoan, ten_khach_hang, email, mat_khau, so_dien_thoai, ngay_sinh, gioi_tinh, nguoi_tao) VALUES
('KH001', 'khachhang01', N'Phạm Văn An', 'an.pham@gmail.com', '123456', '0911111111', '2000-01-01', 1, N'System'),
('KH002', 'khachhang02', N'Nguyễn Thị Bình', 'binh.nguyen@gmail.com', '123456', '0922222222', '2002-03-15', 0, N'System'),
('KH003', 'khachhang03', N'Hoàng Anh Cường', 'cuong.hoang@gmail.com', '123456', '0933333333', '1999-11-20', 1, N'System');
GO

-- 16. dia_chi_khach_hang
INSERT INTO dia_chi_khach_hang (id_khach_hang, ma_dia_chi, ten_dia_chi, thanh_pho, phuong, dia_chi_cu_the, mac_dinh) VALUES
(1, 'DC01', N'Nhà riêng', N'Hà Nội', N'Phường Dịch Vọng', N'Số 15 Ngõ 86 Cầu Giấy', 1),
(2, 'DC02', N'Cơ quan', N'Hà Nội', N'Phường Mễ Trì', N'Tầng 5 Tòa nhà Sông Đà', 1),
(3, 'DC03', N'Nhà riêng', N'Hồ Chí Minh', N'Phường 1', N'Số 123 Đường Lê Lợi, Quận 1', 1);
GO

-- 17. khach_hang_phieu_giam_gia
INSERT INTO khach_hang_phieu_giam_gia (id_khach_hang, id_phieu_giam_gia) VALUES
(1, 1),
(1, 2),
(2, 1);
GO

-- 18. san_pham
INSERT INTO san_pham (id_xuat_xu, id_chat_lieu, id_thuong_hieu, id_danh_muc, id_co_ao, id_tay_ao, id_hoa_tiet, ma_san_pham, ten_san_pham, mo_ta, nguoi_tao) VALUES
(1, 1, 1, 1, 1, 1, 1, 'SP001', N'Áo Phông Nam Basic Cotton Cổ Tròn', N'Áo phông nam chất liệu 100% cotton thoáng mát, thấm hút mồ hôi tốt', N'admin'),
(1, 3, 1, 4, 3, 1, 1, 'SP002', N'Áo Polo Nam Cổ Bẻ Cao Cấp', N'Áo Polo phong cách lịch lãm, chất liệu cotton compact co giãn 4 chiều', N'admin'),
(2, 1, 2, 3, 1, 3, 2, 'SP003', N'Áo Phông Unisex In Hình Graphic', N'Áo phông unisex form rộng tay lỡ in hình độc đáo', N'admin');
GO

-- 19. hinh_anh
INSERT INTO hinh_anh (id_san_pham, ten_anh, duong_dan) VALUES
(1, N'Ảnh mặt trước SP001', '/images/products/sp01_front.jpg'),
(1, N'Ảnh mặt sau SP001', '/images/products/sp01_back.jpg'),
(2, N'Ảnh mặt trước SP002', '/images/products/sp02_front.jpg'),
(3, N'Ảnh mặt trước SP003', '/images/products/sp03_front.jpg');
GO

-- 20. chi_tiet_san_pham
INSERT INTO chi_tiet_san_pham (id_san_pham, id_kich_co, id_mau_sac, ma_chi_tiet_san_pham, so_luong, gia_ban, nguoi_tao) VALUES
(1, 1, 1, 'CTSP01', 50, 199000.00, N'admin'),  -- Áo Basic / S / Đen
(1, 2, 1, 'CTSP02', 100, 199000.00, N'admin'), -- Áo Basic / M / Đen
(1, 2, 2, 'CTSP03', 80, 199000.00, N'admin'),  -- Áo Basic / M / Trắng
(2, 2, 1, 'CTSP04', 60, 299000.00, N'admin'),  -- Áo Polo / M / Đen
(2, 3, 5, 'CTSP05', 40, 299000.00, N'admin'),  -- Áo Polo / L / Xanh navy
(3, 3, 2, 'CTSP06', 70, 250000.00, N'admin');  -- Áo Unisex / L / Trắng
GO

-- 21. chi_tiet_dot_giam_gia
INSERT INTO chi_tiet_dot_giam_gia (id_dot_giam_gia, id_chi_tiet_san_pham, phan_tram_giam_bien_the) VALUES
(1, 1, 10.00),
(1, 2, 10.00),
(1, 3, 10.00),
(2, 4, 20.00);
GO

-- 22. hoa_don
INSERT INTO hoa_don (id_nhan_vien, id_khach_hang, id_phieu_giam_gia, ma_hoa_don, loai_don, phi_van_chuyen, tong_tien, tien_sau_giam_gia, ten_khach_hang, so_dien_thoai_khach_hang, dia_chi_nhan_hang, trang_thai, ghi_chu, nguoi_tao) VALUES
(3, 1, 1, 'HD001', 1, 0.00, 398000.00, 348000.00, N'Phạm Văn An', '0911111111', N'Bán tại quầy', 5, N'Khách thanh toán đủ tại quầy', N'nvbanhang'),
(2, 2, 2, 'HD002', 2, 30000.00, 299000.00, 284150.00, N'Nguyễn Thị Bình', '0922222222', N'Tầng 5 Tòa nhà Sông Đà, Phường Mễ Trì, Hà Nội', 5, N'Đơn hàng giao thành công', N'khachhang02');
GO

-- 23. chi_tiet_hoa_don
INSERT INTO chi_tiet_hoa_don (id_hoa_don, id_chi_tiet_san_pham, ma_hoa_don_chi_tiet, don_gia, so_luong, thanh_tien, ghi_chu) VALUES
(1, 1, 'CTHD01', 199000.00, 1, 199000.00, N'Sản phẩm 1'),
(1, 2, 'CTHD02', 199000.00, 1, 199000.00, N'Sản phẩm 2'),
(2, 4, 'CTHD03', 299000.00, 1, 299000.00, N'Đơn online Polo');
GO

-- 24. lich_su_hoa_don
INSERT INTO lich_su_hoa_don (id_hoa_don, trang_thai, ghi_chu, nguoi_thuc_hien, vai_tro_nguoi_thuc_hien) VALUES
(1, 1, N'Tạo mới hóa đơn tại quầy', N'Lê Văn Bán Hàng', N'Nhân viên'),
(1, 5, N'Hoàn thành thanh toán hóa đơn', N'Lê Văn Bán Hàng', N'Nhân viên'),
(2, 1, N'Khách hàng đặt hàng Online', N'Nguyễn Thị Bình', N'Khách hàng'),
(2, 5, N'Giao hàng thành công', N'Trần Thị Quan Lý', N'Quản lý');
GO

-- 25. hinh_thuc_thanh_toan
INSERT INTO hinh_thuc_thanh_toan (id_hoa_don, id_phuong_thuc, so_tien, ma_giao_dich) VALUES
(1, 1, 348000.00, 'CASH_HD001'),
(2, 2, 284150.00, 'BANK_TRANS_HD002_98765');
GO

-- 26. lich_su_thanh_toan
INSERT INTO lich_su_thanh_toan (id_hoa_don, so_tien, ma_giao_dich, mo_ta) VALUES
(1, 348000.00, 'CASH_HD001', N'Thanh toán tiền mặt thành công tại quầy'),
(2, 284150.00, 'BANK_TRANS_HD002_98765', N'Thanh toán chuyển khoản qua ngân hàng');
GO