package com.example.sd009thuan.service.impl;

import com.example.sd009thuan.dto.ExcelImportResult;
import com.example.sd009thuan.dto.LichLamViecRequest;
import com.example.sd009thuan.dto.ThayTheNhanVienRequest;
import com.example.sd009thuan.entity.CaLamViec;
import com.example.sd009thuan.entity.LichLamViec;
import com.example.sd009thuan.entity.NhanVien;
import com.example.sd009thuan.repository.CaLamViecRepository;
import com.example.sd009thuan.repository.LichLamViecRepository;
import com.example.sd009thuan.repository.NhanVienRepository;
import com.example.sd009thuan.service.LichLamViecService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Transactional
public class LichLamViecServiceImpl implements LichLamViecService {

    private final LichLamViecRepository lichLamViecRepository;
    private final NhanVienRepository nhanVienRepository;
    private final CaLamViecRepository caLamViecRepository;

    public LichLamViecServiceImpl(
            LichLamViecRepository lichLamViecRepository,
            NhanVienRepository nhanVienRepository,
            CaLamViecRepository caLamViecRepository) {
        this.lichLamViecRepository = lichLamViecRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.caLamViecRepository = caLamViecRepository;
    }

    @Override
    public List<LichLamViec> getWeeklySchedule(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusDays(6);
        }
        return lichLamViecRepository.findByNgayLamViecBetweenAndTrangThaiOrderByNgayLamViecAsc(startDate, endDate, 1);
    }

    @Override
    public LichLamViec assignShift(LichLamViecRequest req) {
        if (req.getNhanVienId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn một nhân viên");
        }
        if (req.getCaLamViecId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ca làm việc");
        }
        if (req.getNgayLamViec() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày làm việc");
        }

        NhanVien nv = nhanVienRepository.findById(req.getNhanVienId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + req.getNhanVienId()));

        if (nv.getTrangThai() == null || nv.getTrangThai() != 1) {
            throw new IllegalArgumentException("Nhân viên '" + nv.getTenNhanVien() + "' đang ở trạng thái 'Ngừng hoạt động', không thể phân ca!");
        }

        CaLamViec ca = caLamViecRepository.findById(req.getCaLamViecId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca làm việc với ID: " + req.getCaLamViecId()));

        if (ca.getTrangThai() == null || ca.getTrangThai() != 1) {
            throw new IllegalArgumentException("Ca làm việc '" + ca.getTenCa() + "' đang ngừng hoạt động, không thể xếp lịch!");
        }

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        // Chặn sửa lịch quá khứ
        if (req.getNgayLamViec().isBefore(today)) {
            throw new IllegalArgumentException("Không được phép phân lịch cho những ngày trong quá khứ!");
        }
        if (req.getNgayLamViec().isEqual(today) && ca.getGioKetThuc().isBefore(nowTime)) {
            throw new IllegalArgumentException("Ca làm việc này trong ngày hôm nay đã kết thúc giờ làm!");
        }

        // Check trùng nhân viên trong ca
        Optional<LichLamViec> existingSameShift = lichLamViecRepository.findByNhanVienIdAndCaLamViecIdAndNgayLamViec(
                nv.getId(), ca.getId(), req.getNgayLamViec()
        );
        if (existingSameShift.isPresent() && existingSameShift.get().getTrangThai() == 1) {
            throw new IllegalArgumentException(String.format(
                    "Nhân viên %s (%s) đã được xếp vào ca '%s' trong ngày %s!",
                    nv.getTenNhanVien(), nv.getMaNhanVien(), ca.getTenCa(), req.getNgayLamViec()
            ));
        }

        // Check xung đột khung giờ giữa các ca trong cùng ngày của nhân viên đó
        List<LichLamViec> sameDaySchedules = lichLamViecRepository.findByNhanVienIdAndNgayLamViecAndTrangThai(
                nv.getId(), req.getNgayLamViec(), 1
        );
        for (LichLamViec item : sameDaySchedules) {
            CaLamViec otherCa = item.getCaLamViec();
            boolean isConflict = ca.getGioBatDau().isBefore(otherCa.getGioKetThuc()) &&
                                 ca.getGioKetThuc().isAfter(otherCa.getGioBatDau());
            if (isConflict) {
                throw new IllegalArgumentException(String.format(
                        "Xung đột giờ: Nhân viên %s đã có lịch ca '%s' (%s - %s) trong ngày, trùng khung giờ với ca '%s' (%s - %s)!",
                        nv.getTenNhanVien(), otherCa.getTenCa(), otherCa.getGioBatDau(), otherCa.getGioKetThuc(),
                        ca.getTenCa(), ca.getGioBatDau(), ca.getGioKetThuc()
                ));
            }
        }

        LichLamViec schedule;
        if (existingSameShift.isPresent()) {
            schedule = existingSameShift.get();
            schedule.setTrangThai(1);
            schedule.setGhiChu(req.getGhiChu());
            schedule.setNgayCapNhat(LocalDateTime.now());
        } else {
            schedule = new LichLamViec();
            schedule.setNhanVien(nv);
            schedule.setCaLamViec(ca);
            schedule.setNgayLamViec(req.getNgayLamViec());
            schedule.setGhiChu(req.getGhiChu());
            schedule.setTrangThai(1);
            schedule.setNgayTao(LocalDateTime.now());
        }

        return lichLamViecRepository.save(schedule);
    }

    @Override
    public Map<String, Object> assignBulkShifts(LichLamViecRequest req) {
        if (req.getNhanVienId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn một nhân viên");
        }
        if (req.getCaLamViecId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ca làm việc");
        }
        if (req.getDanhSachNgay() == null || req.getDanhSachNgay().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một ngày làm việc");
        }

        NhanVien nv = nhanVienRepository.findById(req.getNhanVienId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + req.getNhanVienId()));

        if (nv.getTrangThai() == null || nv.getTrangThai() != 1) {
            throw new IllegalArgumentException("Nhân viên '" + nv.getTenNhanVien() + "' đang ở trạng thái 'Ngừng hoạt động', không thể phân ca!");
        }

        CaLamViec ca = caLamViecRepository.findById(req.getCaLamViecId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca làm việc với ID: " + req.getCaLamViecId()));

        if (ca.getTrangThai() == null || ca.getTrangThai() != 1) {
            throw new IllegalArgumentException("Ca làm việc '" + ca.getTenCa() + "' đang ngừng hoạt động, không thể xếp lịch!");
        }

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        List<String> successDates = new ArrayList<>();
        List<String> skippedReasons = new ArrayList<>();

        for (LocalDate date : req.getDanhSachNgay()) {
            if (date == null) continue;

            // Chặn quá khứ
            if (date.isBefore(today)) {
                skippedReasons.add("Ngày " + date + ": Đã qua trong quá khứ");
                continue;
            }
            if (date.isEqual(today) && ca.getGioKetThuc() != null && ca.getGioKetThuc().isBefore(nowTime)) {
                skippedReasons.add("Ngày " + date + ": Ca làm việc đã kết thúc giờ làm hôm nay");
                continue;
            }

            // Check trùng ca đó
            Optional<LichLamViec> existingSameShift = lichLamViecRepository.findByNhanVienIdAndCaLamViecIdAndNgayLamViec(
                    nv.getId(), ca.getId(), date
            );
            if (existingSameShift.isPresent() && existingSameShift.get().getTrangThai() != null && existingSameShift.get().getTrangThai() == 1) {
                skippedReasons.add("Ngày " + date + ": Nhân viên đã có lịch ca này");
                continue;
            }

            // Check xung đột giờ
            List<LichLamViec> sameDaySchedules = lichLamViecRepository.findByNhanVienIdAndNgayLamViecAndTrangThai(
                    nv.getId(), date, 1
            );
            boolean conflict = false;
            for (LichLamViec item : sameDaySchedules) {
                CaLamViec otherCa = item.getCaLamViec();
                if (otherCa != null && otherCa.getGioBatDau() != null && otherCa.getGioKetThuc() != null &&
                    ca.getGioBatDau() != null && ca.getGioKetThuc() != null) {
                    if (ca.getGioBatDau().isBefore(otherCa.getGioKetThuc()) && ca.getGioKetThuc().isAfter(otherCa.getGioBatDau())) {
                        conflict = true;
                        skippedReasons.add("Ngày " + date + ": Trùng khung giờ với ca '" + otherCa.getTenCa() + "'");
                        break;
                    }
                }
            }
            if (conflict) continue;

            // Lưu lịch
            LichLamViec schedule;
            if (existingSameShift.isPresent()) {
                schedule = existingSameShift.get();
                schedule.setTrangThai(1);
                schedule.setGhiChu(req.getGhiChu());
                schedule.setNgayCapNhat(LocalDateTime.now());
            } else {
                schedule = new LichLamViec();
                schedule.setNhanVien(nv);
                schedule.setCaLamViec(ca);
                schedule.setNgayLamViec(date);
                schedule.setGhiChu(req.getGhiChu());
                schedule.setTrangThai(1);
                schedule.setNgayTao(LocalDateTime.now());
            }
            lichLamViecRepository.save(schedule);
            successDates.add(date.toString());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successDates.size());
        result.put("skippedCount", skippedReasons.size());
        result.put("successDates", successDates);
        result.put("skippedReasons", skippedReasons);
        return result;
    }

    @Override
    public void removeShift(Long id) {
        LichLamViec item = lichLamViecRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân ca với ID: " + id));

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        if (item.getNgayLamViec().isBefore(today)) {
            throw new IllegalArgumentException("Không thể xóa phân ca của các ngày trong quá khứ!");
        }
        if (item.getNgayLamViec().isEqual(today) && item.getCaLamViec().getGioKetThuc().isBefore(nowTime)) {
            throw new IllegalArgumentException("Không thể xóa ca làm việc đã kết thúc giờ làm hôm nay!");
        }

        lichLamViecRepository.delete(item);
    }

    @Override
    public List<LichLamViec> getUpcomingShiftsForEmployee(Long nhanVienId) {
        return lichLamViecRepository.findUpcomingActiveByNhanVien(nhanVienId, LocalDate.now());
    }

    @Override
    public Map<String, Object> checkSyncShiftsWhenDeactivatingEmployee(Long nhanVienId) {
        List<LichLamViec> upcoming = getUpcomingShiftsForEmployee(nhanVienId);
        Map<String, Object> res = new HashMap<>();
        res.put("hasUpcomingShifts", !upcoming.isEmpty());
        res.put("upcomingCount", upcoming.size());
        res.put("shifts", upcoming);
        return res;
    }

    @Override
    public void replaceEmployeeInShifts(ThayTheNhanVienRequest req) {
        if (req.getNhanVienMoiId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên mới để thay thế");
        }
        NhanVien newNv = nhanVienRepository.findById(req.getNhanVienMoiId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên thay thế!"));

        if (newNv.getTrangThai() == null || newNv.getTrangThai() != 1) {
            throw new IllegalArgumentException("Nhân viên thay thế phải ở trạng thái 'Hoạt động'!");
        }

        List<LichLamViec> targetShifts;
        if (req.getLichIds() != null && !req.getLichIds().isEmpty()) {
            targetShifts = lichLamViecRepository.findAllById(req.getLichIds());
        } else {
            targetShifts = lichLamViecRepository.findUpcomingActiveByNhanVien(req.getNhanVienCuId(), LocalDate.now());
        }

        for (LichLamViec shift : targetShifts) {
            CaLamViec ca = shift.getCaLamViec();
            LocalDate date = shift.getNgayLamViec();

            // Check if new employee already has this shift
            Optional<LichLamViec> duplicate = lichLamViecRepository.findByNhanVienIdAndCaLamViecIdAndNgayLamViec(newNv.getId(), ca.getId(), date);
            if (duplicate.isPresent() && duplicate.get().getTrangThai() == 1) {
                throw new IllegalArgumentException(String.format(
                        "Nhân viên thay thế %s đã có mặt trong ca '%s' ngày %s!",
                        newNv.getTenNhanVien(), ca.getTenCa(), date
                ));
            }

            // Check conflict
            List<LichLamViec> otherShifts = lichLamViecRepository.findByNhanVienIdAndNgayLamViecAndTrangThai(newNv.getId(), date, 1);
            for (LichLamViec oth : otherShifts) {
                if (ca.getGioBatDau().isBefore(oth.getCaLamViec().getGioKetThuc()) &&
                    ca.getGioKetThuc().isAfter(oth.getCaLamViec().getGioBatDau())) {
                    throw new IllegalArgumentException(String.format(
                            "Không thể thay thế: Nhân viên %s bị trùng khung giờ ca '%s' vào ngày %s!",
                            newNv.getTenNhanVien(), oth.getCaLamViec().getTenCa(), date
                    ));
                }
            }

            shift.setNhanVien(newNv);
            shift.setNgayCapNhat(LocalDateTime.now());
            shift.setGhiChu((shift.getGhiChu() != null ? shift.getGhiChu() + " - " : "") + "Thay thế nhân sự");
        }

        lichLamViecRepository.saveAll(targetShifts);
    }

    @Override
    public ExcelImportResult importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new ExcelImportResult(0, 0, 1, List.of("Vui lòng chọn file Excel để tải lên"), "File rỗng");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            return new ExcelImportResult(0, 0, 1, List.of("Định dạng file không hợp lệ. Vui lòng sử dụng file .xlsx hoặc .xls"), "File sai định dạng");
        }

        List<String> errors = new ArrayList<>();
        List<LichLamViec> toSave = new ArrayList<>();
        Set<String> internalKeys = new HashSet<>();

        LocalDate today = LocalDate.now();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                return new ExcelImportResult(0, 0, 1, List.of("File Excel không có dữ liệu để nhập"), "File không có dữ liệu");
            }

            // Row 0 là header
            int totalDataRows = 0;
            DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("d/M/yyyy")
            };

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Check empty row
                String maNv = getCellString(row.getCell(1));
                String maOrTenCa = getCellString(row.getCell(3));
                String ngayStr = getCellString(row.getCell(4));

                if ((maNv == null || maNv.isEmpty()) && (maOrTenCa == null || maOrTenCa.isEmpty()) && (ngayStr == null || ngayStr.isEmpty())) {
                    continue; // Skip dòng trống cuối file
                }

                totalDataRows++;
                int displayRow = r + 1;

                if (maNv == null || maNv.trim().isEmpty()) {
                    errors.add(String.format("Dòng %d: Mã nhân viên không được để trống", displayRow));
                    continue;
                }
                maNv = maNv.trim();

                if (maOrTenCa == null || maOrTenCa.trim().isEmpty()) {
                    errors.add(String.format("Dòng %d: Ca làm việc (Mã ca hoặc Tên ca) không được để trống", displayRow));
                    continue;
                }
                maOrTenCa = maOrTenCa.trim();

                if (ngayStr == null || ngayStr.trim().isEmpty()) {
                    errors.add(String.format("Dòng %d: Ngày làm việc không được để trống", displayRow));
                    continue;
                }
                ngayStr = ngayStr.trim();

                // Validate NhanVien
                Optional<NhanVien> nvOpt = nhanVienRepository.findByMaNhanVien(maNv);
                if (nvOpt.isEmpty()) {
                    errors.add(String.format("Dòng %d: Mã nhân viên '%s' không tồn tại trong hệ thống", displayRow, maNv));
                    continue;
                }
                NhanVien nv = nvOpt.get();
                if (nv.getTrangThai() == null || nv.getTrangThai() != 1) {
                    errors.add(String.format("Dòng %d: Nhân viên '%s' đang ở trạng thái 'Ngừng hoạt động', không thể xếp lịch", displayRow, nv.getTenNhanVien()));
                    continue;
                }

                // Validate CaLamViec
                Optional<CaLamViec> caOpt = caLamViecRepository.findByMaCa(maOrTenCa);
                if (caOpt.isEmpty()) {
                    caOpt = caLamViecRepository.findByTenCaIgnoreCase(maOrTenCa);
                }
                if (caOpt.isEmpty()) {
                    errors.add(String.format("Dòng %d: Ca làm việc '%s' không tồn tại trong hệ thống", displayRow, maOrTenCa));
                    continue;
                }
                CaLamViec ca = caOpt.get();
                if (ca.getTrangThai() == null || ca.getTrangThai() != 1) {
                    errors.add(String.format("Dòng %d: Ca làm việc '%s' đang ngừng hoạt động", displayRow, ca.getTenCa()));
                    continue;
                }

                // Parse Date
                LocalDate ngayLamViec = null;
                // Nếu ô excel là kiểu DATE numeric
                Cell dateCell = row.getCell(4);
                if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                    ngayLamViec = dateCell.getLocalDateTimeCellValue().toLocalDate();
                } else {
                    for (DateTimeFormatter fmt : formatters) {
                        try {
                            ngayLamViec = LocalDate.parse(ngayStr, fmt);
                            break;
                        } catch (DateTimeParseException ignored) {}
                    }
                }

                if (ngayLamViec == null) {
                    errors.add(String.format("Dòng %d: Ngày làm việc '%s' sai định dạng (Vui lòng nhập định dạng dd/MM/yyyy hoặc yyyy-MM-dd)", displayRow, ngayStr));
                    continue;
                }

                if (ngayLamViec.isBefore(today)) {
                    errors.add(String.format("Dòng %d: Ngày làm việc (%s) không được nhỏ hơn ngày hiện tại (%s)", displayRow, ngayLamViec, today));
                    continue;
                }

                // Check Internal duplicate
                String internalKey = nv.getId() + "_" + ca.getId() + "_" + ngayLamViec;
                if (internalKeys.contains(internalKey)) {
                    errors.add(String.format("Dòng %d: Trùng lặp nội bộ trong file Excel với nhân viên '%s', ca '%s' vào ngày %s", displayRow, nv.getTenNhanVien(), ca.getTenCa(), ngayLamViec));
                    continue;
                }
                internalKeys.add(internalKey);

                // Check DB duplicate
                Optional<LichLamViec> dbDup = lichLamViecRepository.findByNhanVienIdAndCaLamViecIdAndNgayLamViec(nv.getId(), ca.getId(), ngayLamViec);
                if (dbDup.isPresent() && dbDup.get().getTrangThai() == 1) {
                    errors.add(String.format("Dòng %d: Nhân viên '%s' đã có lịch ca '%s' ngày %s trong CSDL", displayRow, nv.getTenNhanVien(), ca.getTenCa(), ngayLamViec));
                    continue;
                }

                // Check DB conflict
                List<LichLamViec> dbSameDay = lichLamViecRepository.findByNhanVienIdAndNgayLamViecAndTrangThai(nv.getId(), ngayLamViec, 1);
                boolean conflict = false;
                for (LichLamViec item : dbSameDay) {
                    if (ca.getGioBatDau().isBefore(item.getCaLamViec().getGioKetThuc()) &&
                        ca.getGioKetThuc().isAfter(item.getCaLamViec().getGioBatDau())) {
                        errors.add(String.format("Dòng %d: Khung giờ ca '%s' bị xung đột với ca '%s' đã có sẵn của nhân viên '%s' ngày %s",
                                displayRow, ca.getTenCa(), item.getCaLamViec().getTenCa(), nv.getTenNhanVien(), ngayLamViec));
                        conflict = true;
                        break;
                    }
                }
                if (conflict) continue;

                String ghiChu = getCellString(row.getCell(5));

                LichLamViec schedule;
                if (dbDup.isPresent()) {
                    schedule = dbDup.get();
                    schedule.setTrangThai(1);
                    schedule.setGhiChu(ghiChu);
                    schedule.setNgayCapNhat(LocalDateTime.now());
                } else {
                    schedule = new LichLamViec();
                    schedule.setNhanVien(nv);
                    schedule.setCaLamViec(ca);
                    schedule.setNgayLamViec(ngayLamViec);
                    schedule.setGhiChu(ghiChu);
                    schedule.setTrangThai(1);
                    schedule.setNgayTao(LocalDateTime.now());
                }
                toSave.add(schedule);
            }

            if (!errors.isEmpty()) {
                // ALL OR NOTHING: Rollback hoàn toàn, không lưu dòng nào
                return new ExcelImportResult(
                        totalDataRows,
                        0,
                        errors.size(),
                        errors,
                        String.format("Import thất bại! Phát hiện %d lỗi. Toàn bộ file đã bị hủy để đảm bảo tính toàn vẹn.", errors.size())
                );
            }

            if (toSave.isEmpty()) {
                return new ExcelImportResult(0, 0, 0, List.of(), "Không có dòng dữ liệu hợp lệ nào để import.");
            }

            lichLamViecRepository.saveAll(toSave);
            return new ExcelImportResult(
                    totalDataRows,
                    toSave.size(),
                    0,
                    List.of(),
                    String.format("Nhập thành công 100%% (%d lịch làm việc) vào hệ thống!", toSave.size())
            );

        } catch (Exception e) {
            return new ExcelImportResult(0, 0, 1, List.of("Lỗi khi xử lý file Excel: " + e.getMessage()), "Lỗi hệ thống");
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    @Override
    public byte[] exportTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("LichLamViecTemplate");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row header = sheet.createRow(0);
            String[] headers = new String[]{
                    "STT", "MÃ NHÂN VIÊN (*)", "TÊN NHÂN VIÊN", "MÃ CA / TÊN CA (*)", "NGÀY LÀM VIỆC (YYYY-MM-DD) (*)", "GHI CHÚ"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Sample rows
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            Row sampleRow1 = sheet.createRow(1);
            sampleRow1.createCell(0).setCellValue(1);
            sampleRow1.createCell(1).setCellValue("NV001");
            sampleRow1.createCell(2).setCellValue("Admin");
            sampleRow1.createCell(3).setCellValue("CA01");
            sampleRow1.createCell(4).setCellValue(tomorrow.toString());
            sampleRow1.createCell(5).setCellValue("Trực quầy sáng");

            Row sampleRow2 = sheet.createRow(2);
            sampleRow2.createCell(0).setCellValue(2);
            sampleRow2.createCell(1).setCellValue("NV002");
            sampleRow2.createCell(2).setCellValue("Nguyễn Thị Bảo An");
            sampleRow2.createCell(3).setCellValue("Ca Chiều");
            sampleRow2.createCell(4).setCellValue(tomorrow.toString());
            sampleRow2.createCell(5).setCellValue("Trực ca chiều");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo template Excel: " + e.getMessage(), e);
        }
    }
}

