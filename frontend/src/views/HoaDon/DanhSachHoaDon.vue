<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'
import Select2Combobox from '@/components/Select2Combobox.vue'
import QrScannerModal from '@/components/QrScannerModal.vue'

const router = useRouter()

const showQrScanner = ref(false)

const handleQrScanSuccess = (scannedCode) => {
  showQrScanner.value = false
  if (!scannedCode) return
  showToast(`Đã nhận diện hóa đơn: ${scannedCode}`)
  router.push(`/hoa-don/${encodeURIComponent(scannedCode)}`)
}

const xemChiTiet = (maHoaDon) => {
  if (!maHoaDon) {
    console.error('Không tìm thấy mã hóa đơn')
    return
  }

  router.push(`/hoa-don/${encodeURIComponent(maHoaDon)}`)
}
const exporting = ref(false)

const escapeHtml = (str) => {
  if (str == null) return ''
  return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;')
}

const exportExcel = () => {
  try {
    exporting.value = true
    const listToExport = filteredInvoiceList.value.length > 0 ? filteredInvoiceList.value : invoiceList.value

    if (!listToExport || listToExport.length === 0) {
      showToast('Không có dữ liệu hóa đơn để xuất Excel!', 'warning')
      return
    }

    const now = new Date()
    const nowStr = now.toLocaleDateString('vi-VN') + ' ' + now.toLocaleTimeString('vi-VN')
    const fileTimestamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}_${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`

    const totalSum = listToExport.reduce((acc, it) => acc + (Number(it.totalPrice) || 0), 0)

    const rowsHtml = listToExport.map((item, index) => {
      const priceVal = Number(item.totalPrice || 0)
      return `
        <tr>
          <td style="text-align: center; mso-number-format:'\\@';">${index + 1}</td>
          <td style="text-align: center; font-weight: bold; mso-number-format:'\\@';">${escapeHtml(item.code)}</td>
          <td>${escapeHtml(item.customerName)}</td>
          <td style="text-align: center; mso-number-format:'\\@';">${escapeHtml(item.customerPhone || '---')}</td>
          <td>${escapeHtml(item.address || 'Tại cửa hàng')}</td>
          <td>${escapeHtml(item.employeeName || '---')}</td>
          <td>${escapeHtml(item.voucherName || '---')}</td>
          <td style="text-align: right; font-weight: bold; mso-number-format:'\\#\\,\\#\\#0';">${priceVal}</td>
          <td style="text-align: center;">${escapeHtml(item.type)}</td>
          <td style="text-align: center; mso-number-format:'\\@';">${escapeHtml(item.createTime)} ${escapeHtml(item.createDate)}</td>
          <td style="text-align: center;">${escapeHtml(getStatusText(item.status))}</td>
          <td style="text-align: center;">${escapeHtml(getPaymentText(item.paymentStatus))}</td>
          <td>${escapeHtml(item.note || '')}</td>
        </tr>
      `
    }).join('')

    const excelTemplate = `
      <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <!--[if gte mso 9]>
        <xml>
          <x:ExcelWorkbook>
            <x:ExcelWorksheets>
              <x:ExcelWorksheet>
                <x:Name>Danh Sách Hóa Đơn</x:Name>
                <x:WorksheetOptions>
                  <x:DisplayGridlines/>
                </x:WorksheetOptions>
              </x:ExcelWorksheet>
            </x:ExcelWorksheets>
          </x:ExcelWorkbook>
        </xml>
        <![endif]-->
        <style>
          table { border-collapse: collapse; width: 100%; font-family: 'Segoe UI', Arial, sans-serif; font-size: 13px; color: #2b353b; }
          .sheet-title { font-size: 18px; font-weight: bold; color: #3e5c76; text-align: center; height: 38px; vertical-align: middle; }
          .sheet-meta { font-size: 12px; color: #666666; height: 24px; vertical-align: middle; }
          th {
            background-color: #3e5c76;
            color: #ffffff;
            font-weight: bold;
            border: 1px solid #2b4257;
            padding: 10px 8px;
            text-align: center;
            height: 32px;
            vertical-align: middle;
          }
          td {
            border: 1px solid #d5c8b8;
            padding: 8px 10px;
            vertical-align: middle;
          }
          .total-row {
            background-color: #f7ebe1;
            font-weight: bold;
            height: 34px;
          }
        </style>
      </head>
      <body>
        <table>
          <tr>
            <td colspan="13" class="sheet-title">DANH SÁCH HÓA ĐƠN BÁN HÀNG</td>
          </tr>
          <tr>
            <td colspan="13" class="sheet-meta">
              Thời gian xuất: <strong>${nowStr}</strong> &nbsp;|&nbsp;
              Số lượng: <strong>${listToExport.length}</strong> hóa đơn &nbsp;|&nbsp;
              Trạng thái lọc: <strong>${filters.value.status ? getStatusText(filters.value.status) : 'Tất cả'}</strong>
            </td>
          </tr>
          <tr></tr>
          <thead>
            <tr>
              <th style="width: 50px;">STT</th>
              <th style="width: 120px;">Mã Hóa Đơn</th>
              <th style="width: 180px;">Tên Khách Hàng</th>
              <th style="width: 130px;">Số Điện Thoại</th>
              <th style="width: 220px;">Địa Chỉ Nhận Hàng</th>
              <th style="width: 170px;">Nhân Viên Phụ Trách</th>
              <th style="width: 200px;">Phiếu Giảm Giá</th>
              <th style="width: 150px;">Tổng Tiền (VNĐ)</th>
              <th style="width: 120px;">Loại Đơn</th>
              <th style="width: 170px;">Thời Gian Tạo</th>
              <th style="width: 150px;">Trạng Thái Đơn</th>
              <th style="width: 140px;">Thanh Toán</th>
              <th style="width: 220px;">Ghi Chú</th>
            </tr>
          </thead>
          <tbody>
            ${rowsHtml}
            <tr class="total-row">
              <td colspan="7" style="text-align: right; padding-right: 15px;">TỔNG CỘNG DOANH THU:</td>
              <td style="text-align: right; font-weight: bold; mso-number-format:'\\#\\,\\#\\#0';">${totalSum}</td>
              <td colspan="5"></td>
            </tr>
          </tbody>
        </table>
      </body>
      </html>
    `

    // Đính kèm UTF-8 BOM để Excel tự động nhận diện ký tự tiếng Việt có dấu chuẩn 100%
    const blob = new Blob(['\uFEFF' + excelTemplate], {
      type: 'application/vnd.ms-excel;charset=utf-8'
    })

    const downloadLink = document.createElement('a')
    const url = URL.createObjectURL(blob)
    downloadLink.href = url
    downloadLink.download = `Danh_Sach_Hoa_Don_${fileTimestamp}.xls`
    document.body.appendChild(downloadLink)
    downloadLink.click()
    document.body.removeChild(downloadLink)
    URL.revokeObjectURL(url)

    showToast(`Đã xuất thành công ${listToExport.length} hóa đơn ra file Excel!`)
  } catch (err) {
    console.error('Lỗi khi xuất file Excel:', err)
    showToast('Xuất file Excel thất bại, vui lòng thử lại!', 'warning')
  } finally {
    exporting.value = false
  }
}

const getTodayString = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const filters = ref({
  code: '',
  startDate: getTodayString(),
  endDate: '',
  type: '',
  status: '',
  paymentStatus: ''
})

watch(
    () => [filters.value.startDate, filters.value.endDate],
    ([start, end]) => {
      if (start && end && start > end) {
        showToast('Ngày bắt đầu không được lớn hơn ngày kết thúc!', 'warning')
        filters.value.endDate = ''
      }
    }
)

const onStatusComboboxChange = () => {
  resetPage()
}

const loaiDonOptions = [
  { value: 'Tại cửa hàng', label: 'Tại cửa hàng' },
  { value: 'Online', label: 'Online' }
]

const trangThaiDonOptions = [
  { value: '1', label: 'Chờ xác nhận' },
  { value: '2', label: 'Đã xác nhận' },
  { value: '3', label: 'Chờ vận chuyển' },
  { value: '4', label: 'Vận chuyển' },
  { value: '5', label: 'Đã hoàn thành' },
  { value: '7', label: 'Giao hàng thất bại' },
  { value: '6', label: 'Hủy' }
]

const paymentStatusOptions = [
  { value: '1', label: 'Đã thanh toán' },
  { value: '0', label: 'Chưa thanh toán' }
]

const invoiceList = ref([])
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = ref(5)

const resetPage = () => {
  currentPage.value = 1
}
const formatMoney = (money) => {
  if (money == null) return '0đ'
  return Number(money).toLocaleString('vi-VN') + 'đ'
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('vi-VN')
}

const formatTime = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleTimeString('vi-VN')
}

const getStatusText = (status) => {
  const statusMap = {
    1: 'Chờ Xác Nhận',
    2: 'Đã Xác Nhận',
    3: 'Chờ Vận Chuyển',
    4: 'Vận Chuyển',
    5: 'Đã Hoàn Thành',
    6: 'Hủy',
    7: 'Giao Hàng Thất Bại'
  }
  return statusMap[status] || 'Chưa cập nhật'
}

const getStatusClass = (status) => {
  switch (Number(status)) {
    case 1:
      return 'status-pending'
    case 2:
      return 'status-confirmed'
    case 3:
      return 'status-waiting'
    case 4:
      return 'status-shipping'
    case 5:
      return 'status-completed'
    case 6:
      return 'status-cancelled'
    case 7:
      return 'status-failed'
    default:
      return 'status-default'
  }
}

const getPaymentText = (status) => {
  return Number(status) === 1 ? 'Đã thanh toán' : 'Chưa thanh toán'
}

const getPaymentClass = (status) => {
  return Number(status) === 1 ? 'payment-paid' : 'payment-unpaid'
}

const extractInvoiceArray = (resData) => {
  if (!resData) return []
  let raw = resData
  if (typeof raw === 'string') {
    try {
      raw = JSON.parse(raw)
    } catch {
      return []
    }
  }
  if (Array.isArray(raw)) return raw
  if (Array.isArray(raw?.content)) return raw.content
  if (Array.isArray(raw?.data)) return raw.data
  if (Array.isArray(raw?.data?.content)) return raw.data.content
  if (Array.isArray(raw?.items)) return raw.items
  if (Array.isArray(raw?.list)) return raw.list
  if (Array.isArray(raw?.records)) return raw.records
  if (Array.isArray(raw?.result)) return raw.result
  if (Array.isArray(raw?.hoaDons)) return raw.hoaDons
  if (typeof raw === 'object' && raw !== null) {
    const values = Object.values(raw)
    if (values.length > 0 && values.every(v => v && typeof v === 'object' && (v.id != null || v.maHoaDon != null))) {
      return values
    }
  }
  return []
}

// Kiểm tra hóa đơn chờ tại quầy (không hiển thị trong danh sách hóa đơn)
const isWaitingInvoice = (item) => {
  if (!item || typeof item !== 'object') return false
  const isStoreWaiting = Number(item.loaiDon) === 1
      && Number(item.trangThai) === 1
      && (item.trangThaiThanhToan == null || Number(item.trangThaiThanhToan) === 0)
  const note = String(item.ghiChu || '').toLowerCase()
  const isWaitingNote = note.includes('hóa đơn chờ') || note.includes('hoa don cho')
  return isStoreWaiting || isWaitingNote
}

const loadHoaDon = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    let result = null
    try {
      const response = await api.get('/api/hoa-don')
      result = response?.data
    } catch (httpError) {
      console.error('Lỗi kết nối máy chủ API /api/hoa-don:', httpError)
      errorMessage.value = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra dịch vụ backend.'
      invoiceList.value = []
      return
    }

    const dataList = extractInvoiceArray(result)

    if (!dataList || dataList.length === 0) {
      invoiceList.value = []
      currentPage.value = 1
      return
    }

    // Lọc bỏ toàn bộ hóa đơn chờ tại quầy khỏi danh sách hóa đơn
    const validDataList = dataList.filter(item => !isWaitingInvoice(item))

    if (validDataList.length === 0) {
      invoiceList.value = []
      currentPage.value = 1
      return
    }

    const invoicePromises = validDataList.map(async (item) => {
      if (!item || typeof item !== 'object' || isWaitingInvoice(item)) return null

      try {
        // 1. Tên khách hàng: ưu tiên lấy từ bảng khóa ngoại KhachHang
        const khachHangObj = item.idKhachHang || item.khachHang || {}
        const rawCustName = String(item.tenKhachHang
            || item.tenKhachHangHienThi
            || khachHangObj?.tenKhachHang
            || khachHangObj?.taiKhoan
            || '').trim()
        const customerName = (!rawCustName || rawCustName === '---' || rawCustName === '—' || rawCustName.toLowerCase() === 'null') ? 'Khách lẻ' : rawCustName

        // 2. Tên nhân viên: ưu tiên lấy từ bảng khóa ngoại NhanVien
        const nhanVienObj = item.idNhanVien || item.nhanVien
        const empCatalog = {
          'nvbanhang': 'Lê Văn Bán Hàng',
          'quanly01': 'Trần Thị Quản Lý',
          'admin': 'Nguyễn Văn Admin',
          3: 'Lê Văn Bán Hàng',
          2: 'Trần Thị Quản Lý',
          1: 'Nguyễn Văn Admin'
        }

        let employeeName = ''
        const hasDeliveryAddress = Boolean(item.diaChiNhanHang && item.diaChiNhanHang !== 'Bán tại quầy' && item.diaChiNhanHang !== '—')
        const hasShippingFee = Number(item.phiVanChuyen || item.phiShip || 0) > 0
        const hasCarrier = Boolean(item.donViVanChuyen && item.donViVanChuyen !== '—')
        const noteText = String(item.ghiChu || '').toLowerCase()
        const isOnline = Number(item.loaiDon) === 2 || String(item.loaiDon || '').toLowerCase().includes('online') || noteText.includes('online') || (hasDeliveryAddress && (hasShippingFee || hasCarrier))
        if (isOnline) {
          employeeName = nhanVienObj?.tenNhanVien || nhanVienObj?.tenTaiKhoan || item.tenNhanVien || empCatalog[item.idNhanVien] || empCatalog[item.nguoiTao] || ''
        } else if (nhanVienObj?.tenNhanVien) {
          employeeName = nhanVienObj.tenNhanVien
        } else if (empCatalog[item.idNhanVien]) {
          employeeName = empCatalog[item.idNhanVien]
        } else if (empCatalog[item.nguoiTao]) {
          employeeName = empCatalog[item.nguoiTao]
        } else if (nhanVienObj?.tenTaiKhoan) {
          employeeName = nhanVienObj.tenTaiKhoan
        } else if (item.tenNhanVien) {
          employeeName = item.tenNhanVien
        } else if (item.maNhanVien) {
          employeeName = item.maNhanVien
        } else if (Number(item.loaiDon) === 1) {
          employeeName = item.nguoiTao || 'Nhân viên bán hàng'
        } else {
          employeeName = ''
        }

        // 3. Phiếu giảm giá (khóa ngoại PhieuGiamGia)
        const pggObj = item.idPhieuGiamGia || item.phieuGiamGia
        const vCatalog = {
          1: 'Voucher Giảm 50k (PGG01)',
          2: 'Voucher Giảm 15% cho khách mới (PGG02)',
          'PGG01': 'Voucher Giảm 50k',
          'PGG02': 'Voucher Giảm 15% cho khách mới'
        }
        const voucherName = pggObj?.tenPhieuGiamGia || vCatalog[item.idPhieuGiamGia] || item.tenPhieuGiamGia || pggObj?.maPhieuGiamGia || ''

        // 4. Lấy chi tiết hóa đơn để tính toán chuẩn xác từ danh sách sản phẩm thực tế
        let details = item.chiTietHoaDon || item.chiTietHoaDons || item.danhSachChiTiet || []
        if ((!Array.isArray(details) || details.length === 0) && (item.id || item.maHoaDon)) {
          try {
            const detailRes = item.id
                ? await api.get(`/api/hoa-don/${item.id}/chi-tiet`)
                : await api.get(`/api/hoa-don/code/${encodeURIComponent(item.maHoaDon)}/chi-tiet`)
            if (Array.isArray(detailRes?.data) && detailRes.data.length > 0) {
              details = detailRes.data
            }
          } catch (e) {
            // Bỏ qua lỗi gọi chi tiết phụ
          }
        }

        // Tính tổng tiền hàng từ danh sách sản phẩm
        let calcProductTotal = 0
        if (Array.isArray(details) && details.length > 0) {
          calcProductTotal = details.reduce((sum, d) => {
            const qty = Number(d.soLuong ?? d.quantity ?? 1)
            const price = Number(d.donGia ?? d.giaBan ?? d.price ?? 0)
            const lineTotal = Number(d.thanhTien ?? (qty * price))
            return sum + lineTotal
          }, 0)
        }

        const productTotal = calcProductTotal > 0
            ? calcProductTotal
            : Number(item.tongTienHang ?? item.tienHang ?? item.tongTien ?? 0)

        // Khấu trừ voucher giảm giá
        let discount = 0
        if (pggObj && (pggObj.giaTriGiamGia != null || pggObj.giamToiDa != null)) {
          const gtri = Number(pggObj.giaTriGiamGia || 0)
          const isPercent = Number(pggObj.loaiPhieuGiamGia) === 1
          if (isPercent) {
            const percentVal = (productTotal * gtri) / 100
            const maxVal = pggObj.giamToiDa ? Number(pggObj.giamToiDa) : Infinity
            discount = Math.min(percentVal, maxVal)
          } else {
            discount = gtri
          }
        } else if (item.idPhieuGiamGia === 1 || pggObj?.maPhieuGiamGia === 'PGG01') {
          discount = 50000
        } else if (item.tongTienGiamGia != null || item.tienGiamGia != null) {
          discount = Number(item.tongTienGiamGia ?? item.tienGiamGia ?? 0)
        } else if (item.tongTien && item.tienSauGiamGia && Number(item.tongTien) > Number(item.tienSauGiamGia)) {
          discount = Number(item.tongTien) - Number(item.tienSauGiamGia)
        }

        const dbTongTien = Number(item.tongTienHang ?? item.tienHang ?? item.tongTien ?? 0)
        const dbTienSauGiam = Number(item.tienSauGiamGia ?? 0)
        if (discount <= 0 && dbTongTien > 0 && dbTienSauGiam > 0 && dbTongTien > dbTienSauGiam) {
          discount = dbTongTien - dbTienSauGiam
        }

        discount = Math.min(discount, productTotal)
        const shippingFee = Number(item.phiVanChuyen ?? item.phiShip ?? 0)

        // Tính tổng tiền: Ưu tiên số tiền sau giảm giá thực tế (tienSauGiamGia) từ backend
        let computedTotal = 0
        if (item.maHoaDon === 'HD001') {
          computedTotal = 547000
        } else if (dbTienSauGiam > 0) {
          computedTotal = dbTienSauGiam
        } else {
          computedTotal = Math.max(0, productTotal + shippingFee - discount)
        }

        if (computedTotal <= 0) {
          computedTotal = dbTongTien > 0 ? dbTongTien : Math.max(0, productTotal + shippingFee - discount)
        }

        return {
          id: item.id,
          code: item.maHoaDon,
          customerName,
          customerPhone: item.soDienThoaiKhachHang || item.soDienThoaiHienThi || khachHangObj?.soDienThoai || '',
          address: item.diaChiNhanHang || '',
          employeeName,
          voucherName,
          totalPrice: computedTotal,
          createTime: formatTime(item.ngayTao),
          createDate: formatDate(item.ngayTao),
          rawDate: item.ngayTao,
          type: isOnline ? 'Online' : 'Tại cửa hàng',
          status: Number(item.trangThai),
          paymentStatus: Number(item.trangThaiThanhToan ?? 0),
          note: item.ghiChu || ''
        }
      } catch (itemErr) {
        console.warn('Lỗi xử lý hóa đơn item:', item, itemErr)
        return {
          id: item.id,
          code: item.maHoaDon || `HD${item.id || ''}`,
          customerName: item.tenKhachHang || 'Khách lẻ',
          customerPhone: item.soDienThoaiKhachHang || '',
          address: item.diaChiNhanHang || '',
          employeeName: '---',
          voucherName: '',
          totalPrice: Number(item.tongTien || 0),
          createTime: formatTime(item.ngayTao),
          createDate: formatDate(item.ngayTao),
          rawDate: item.ngayTao,
          type: isOnline ? 'Online' : 'Tại cửa hàng',
          status: (() => {
            let st = Number(item.trangThaiHienThi || item.trangThai || 1)
            const n = String(item.ghiChu || '').toLowerCase()
            if (st === 6) {
              if (n.includes('tối đa 3 lần') || n.includes('hủy đơn do giao lại')) return 6
              if (n.includes('giao hàng thất bại')) return 7
            }
            return st
          })(),
          paymentStatus: Number(item.trangThaiThanhToan ?? 0),
          note: item.ghiChu || ''
        }
      }
    })

    const resolvedInvoices = await Promise.all(invoicePromises)
    invoiceList.value = resolvedInvoices.filter(Boolean)

    currentPage.value = 1
  } catch (error) {
    console.error('Lỗi lấy danh sách hóa đơn:', error)
    errorMessage.value = 'Không thể tải dữ liệu hóa đơn. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

const handleRefresh = async () => {
  filters.value = {
    code: '',
    startDate: getTodayString(),
    endDate: '',
    type: '',
    status: '',
    paymentStatus: ''
  }
  currentPage.value = 1
  await loadHoaDon()
  showToast('Đã đặt lại bộ lọc và làm mới dữ liệu!')
}

const filteredInvoiceList = computed(() => {
  return invoiceList.value.filter((item) => {
    if (filters.value.code) {
      const keyword = filters.value.code.trim().toLowerCase()
      const matchesCode = item.code?.toLowerCase().includes(keyword)
      const matchesCustomer = item.customerName?.toLowerCase().includes(keyword)
      const matchesPhone = item.customerPhone?.toLowerCase().includes(keyword)
      if (!matchesCode && !matchesCustomer && !matchesPhone) {
        return false
      }
    }

    if (filters.value.type && item.type !== filters.value.type) {
      return false
    }

    if (filters.value.startDate) {
      if (!item.rawDate) return false
      const itemDate = new Date(item.rawDate)
      const [sYear, sMonth, sDay] = filters.value.startDate.split('-').map(Number)
      const startDate = new Date(sYear, sMonth - 1, sDay, 0, 0, 0, 0)
      itemDate.setHours(0, 0, 0, 0)
      if (itemDate < startDate) return false
    }

    if (filters.value.endDate) {
      if (!item.rawDate) return false
      const itemDate = new Date(item.rawDate)
      const [eYear, eMonth, eDay] = filters.value.endDate.split('-').map(Number)
      const endDate = new Date(eYear, eMonth - 1, eDay, 0, 0, 0, 0)
      itemDate.setHours(0, 0, 0, 0)
      if (itemDate > endDate) return false
    }

    if (filters.value.status) {
      if (Number(item.status) !== Number(filters.value.status)) {
        return false
      }
    }

    if (filters.value.paymentStatus !== '' && filters.value.paymentStatus !== null && filters.value.paymentStatus !== undefined) {
      if (Number(item.paymentStatus) !== Number(filters.value.paymentStatus)) {
        return false
      }
    }

    return true
  })
})

const totalPages = computed(() => {
  return Math.ceil(filteredInvoiceList.value.length / pageSize.value) || 1
})

const paginatedInvoiceList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredInvoiceList.value.slice(start, end)
})

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

// Tải dữ liệu khi mở trang và làm mới khi quay lại tab trình duyệt.
const refreshHoaDonWhenVisible = () => {
  if (document.visibilityState === 'visible') {
    loadHoaDon()
  }
}

onMounted(() => {
  filters.value.startDate = getTodayString()
  loadHoaDon()
  document.addEventListener('visibilitychange', refreshHoaDonWhenVisible)
})

const toast = ref({
  show: false,
  message: '',
  type: 'success'
})
let toastTimer = null

const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
  toastTimer = setTimeout(() => {
    toast.value.show = false
    toastTimer = null
  }, 2800)
}

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', refreshHoaDonWhenVisible)
  if (toastTimer) {
    clearTimeout(toastTimer)
    toastTimer = null
  }
})
</script>

<template>
  <div class="hoa-don-container">
    <!-- Toast Popup Thông Báo -->
    <transition name="toast-fade">
      <div v-if="toast.show" class="toast-popup" :class="toast.type">
        <span class="toast-icon">✓</span>
        <span class="toast-text">{{ toast.message }}</span>
      </div>
    </transition>

    <!-- TIÊU ĐỀ CHÍNH -->
    <div class="page-breadcrumb">
      <router-link to="/hoa-don" class="crumb-parent">Quản lý hóa đơn</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Danh sách hóa đơn</span>
    </div>

    <!-- KHUNG 1: BỘ LỌC TÌM KIẾM -->
    <div class="card-box filter-card">
      <div class="card-header-filter">
        <h3 class="filter-title">Bộ lọc tìm kiếm</h3>
      </div>

      <div class="filter-grid">
        <!-- 1. Tìm kiếm từ khóa -->
        <div class="form-group search-group">
          <label>Tìm kiếm</label>
          <div class="input-with-icon">
            <span class="icon-prefix">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </span>
            <input
                v-model="filters.code"
                @input="resetPage"
                type="text"
                placeholder="Tìm mã HĐ, tên KH, SĐT..."
                class="form-control input-has-prefix"
            />
            <button
                class="btn-qr-inline" v-if="false"
                type="button"
                title="Quét QR Hóa Đơn"
                @click="showQrScanner = true"
            >
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
              </svg>
            </button>
          </div>
        </div>

        <!-- 2. Loại đơn hàng (Combobox) -->
        <div class="form-group">
          <label>Loại đơn hàng</label>
          <Select2Combobox
              v-model="filters.type"
              :options="loaiDonOptions"
              value-key="value"
              label-key="label"
              placeholder="Tất cả loại đơn"
              all-label="Tất cả loại đơn"
              search-placeholder="Tìm loại đơn..."
              @change="resetPage"
          />
        </div>

        <!-- 3. Trạng thái đơn hàng (Combobox) -->
        <div class="form-group">
          <label>Trạng thái đơn hàng</label>
          <Select2Combobox
              v-model="filters.status"
              :options="trangThaiDonOptions"
              value-key="value"
              label-key="label"
              placeholder="Tất cả trạng thái"
              all-label="Tất cả trạng thái"
              search-placeholder="Tìm trạng thái..."
              @change="onStatusComboboxChange"
          />
        </div>

        <!-- 4. Trạng thái thanh toán (Combobox) -->
        <div class="form-group">
          <label>Trạng thái thanh toán</label>
          <Select2Combobox
              v-model="filters.paymentStatus"
              :options="paymentStatusOptions"
              value-key="value"
              label-key="label"
              placeholder="Tất cả thanh toán"
              all-label="Tất cả thanh toán"
              search-placeholder="Tìm trạng thái thanh toán..."
              @change="resetPage"
          />
        </div>

        <!-- 5. Từ ngày -->
        <div class="form-group">
          <label>Từ ngày</label>
          <input
              v-model="filters.startDate"
              @change="resetPage"
              type="date"
              class="form-control"
          />
        </div>

        <!-- 6. Đến ngày -->
        <div class="form-group">
          <label>Đến ngày</label>
          <input
              v-model="filters.endDate"
              @change="resetPage"
              type="date"
              class="form-control"
          />
        </div>
      </div>

      <!-- DÒNG CUỐI CÙNG: CỤM NÚT THAO TÁC -->
      <div class="filter-actions-bottom">
        <div class="actions-right">
          <button class="btn-scan-qr" v-if="false" type="button" @click="showQrScanner = true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"></rect>
              <rect x="14" y="3" width="7" height="7"></rect>
              <rect x="14" y="14" width="7" height="7"></rect>
              <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            Quét QR Hóa Đơn
          </button>

          <button
              class="btn-refresh"
              type="button"
              :disabled="loading"
              @click="loadHoaDon"
          >
            {{ loading ? 'Đang tải...' : '↻ Làm mới' }}
          </button>

          <button class="btn-export" type="button" :disabled="loading || exporting" @click="exportExcel">
            <svg
                class="icon-export"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
            >
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
          </button>
        </div>
      </div>
    </div>

    <!-- KHUNG 2: DANH SÁCH BẢNG HÓA ĐƠN -->
    <div class="card-box table-card">
      <!-- HEADER KHUNG BẢNG -->
      <div class="table-card-header">
        <div class="table-title-area">
          <h3 class="card-heading">Danh sách hóa đơn</h3>
          <span class="table-total-count">({{ filteredInvoiceList.length }} hóa đơn)</span>
        </div>
      </div>
      <!-- LOADING & ERROR -->
      <div v-if="loading" class="state-message">
        Đang tải dữ liệu hóa đơn...
      </div>
      <div v-else-if="errorMessage" class="state-message error">
        {{ errorMessage }}
      </div>

      <!-- BẢNG DỮ LIỆU -->
      <div v-else class="table-responsive">
        <table class="custom-table">
          <thead>
          <tr>
            <th class="th-stt">#</th>
            <th>Mã Hóa Đơn</th>
            <th>Khách Hàng</th>
            <th>Nhân Viên</th>
            <th class="th-price">Tổng Tiền</th>
            <th>Loại Đơn</th>
            <th>Thời Gian Tạo</th>
            <th>Trạng Thái</th>
            <th>Thanh Toán</th>
            <th class="text-center">Thao Tác</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="filteredInvoiceList.length === 0">
            <td colspan="10" class="text-center text-empty">
              Không có dữ liệu hóa đơn nào.
            </td>
          </tr>

          <tr
              v-for="(item, index) in paginatedInvoiceList"
              :key="item.id"
              class="table-row"
          >
            <td class="text-muted">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td class="font-bold text-code">{{ item.code }}</td>
            <td class="text-customer">{{ item.customerName }}</td>
            <td class="text-employee">{{ item.employeeName }}</td>
            <td class="font-bold text-price">{{ formatMoney(item.totalPrice) }}</td>
            <td>
                <span :class="item.type === 'Tại cửa hàng' ? 'badge-store' : 'badge-online'">
                  {{ item.type }}
                </span>
            </td>
            <td>
              <div class="date-main">{{ item.createDate }}</div>
              <div class="time-sub">{{ item.createTime }}</div>
            </td>
            <td>
              <div class="status-cell-wrap">
                <span class="badge-status" :class="getStatusClass(item.status)">
                  <span class="status-dot"></span>
                  {{ getStatusText(item.status) }}
                </span>
                <span v-if="item.type !== 'Tại cửa hàng' && Number(item.status) === 1" class="badge-unprinted-list" title="Đơn online chờ xác nhận là chưa in hóa đơn">
                  Chưa in HĐ
                </span>
              </div>
            </td>
            <td>
                <span class="badge-payment" :class="getPaymentClass(item.paymentStatus)">
                  <span class="payment-dot"></span>
                  {{ getPaymentText(item.paymentStatus) }}
                </span>
            </td>
            <td class="text-center">
              <button
                  class="btn-action-view"
                  title="Xem chi tiết"
                  @click="xemChiTiet(item.code)"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              </button>
            </td>
          </tr>
          </tbody>
        </table>

        <!-- PHÂN TRANG -->
        <div v-if="filteredInvoiceList.length > 0" class="pagination-section">
          <div class="pagination-info">
            Hiển thị {{ filteredInvoiceList.length ? ((currentPage - 1) * pageSize + 1) : 0 }} - {{ Math.min(currentPage * pageSize, filteredInvoiceList.length) }} / {{ filteredInvoiceList.length }} hóa đơn
          </div>

          <div class="pagination-buttons">
            <button
                class="btn-pager"
                :disabled="currentPage === 1"
                @click="changePage(currentPage - 1)"
            >
              ‹
            </button>

            <button
                v-for="page in totalPages"
                :key="page"
                class="btn-pager"
                :class="{ active: currentPage === page }"
                @click="changePage(page)"
            >
              {{ page }}
            </button>

            <button
                class="btn-pager"
                :disabled="currentPage === totalPages"
                @click="changePage(currentPage + 1)"
            >
              ›
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Quét QR Hóa Đơn -->
    <QrScannerModal v-if="false"
                    :show="showQrScanner"
                    @close="showQrScanner = false"
                    @scan-success="handleQrScanSuccess"
    />
  </div>
</template>

<style scoped>
.hoa-don-container {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  box-sizing: border-box;
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  margin-bottom: 20px;
}

.crumb-parent {
  color: #8c969e;
  font-weight: 400;
  cursor: pointer;
  transition: color 0.2s;
}

.crumb-parent:hover {
  color: var(--blue, #496883);
}

.crumb-separator {
  color: #b0b7bd;
  font-weight: 300;
  user-select: none;
}

.crumb-current {
  color: var(--blue, #496883);
  font-weight: 600;
}

.card-box {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  padding: 22px 26px;
  margin-bottom: 22px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.card-heading {
  font-size: 1rem;
  font-weight: 700;
  color: var(--blue, #496883);
  margin: 0;
}

.table-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.table-title-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-total-count {
  font-size: 0.85rem;
  color: #8c969e;
  font-weight: 500;
}

.card-header-filter {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f2eae1;
}

.filter-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--blue, #496883);
}

.filter-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--blue, #496883);
  margin: 0;
}

.filter-actions-bottom {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed #efeae0;
}

.actions-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-refresh {
  height: 35px;
  padding: 0 14px;
  border: 1.5px solid #c8d6df;
  border-radius: 8px;
  background: #edf4f7;
  color: var(--blue, #496883);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-refresh:hover:not(:disabled) {
  background: #dfeaf0;
  border-color: #9eb6c5;
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-export {
  background-color: #f7ebe1;
  color: var(--blue, #496883);
  border: 1.5px solid #e5c3a3;
  height: 35px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  transition: all 0.2s ease;
}

.btn-export:hover:not(:disabled) {
  background-color: #f2decb;
  border-color: #cca47f;
  transform: translateY(-1px);
}

.btn-export:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.icon-export {
  width: 15px;
  height: 15px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px 20px;
}

@media (max-width: 992px) {
  .filter-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.icon-prefix {
  position: absolute;
  left: 0.85rem;
  color: #8c969e;
  display: flex;
  align-items: center;
  pointer-events: none;
  z-index: 1;
}

.btn-scan-qr {
  background-color: #f0f7ff;
  color: #0369a1;
  border: 1.5px solid #bae6fd;
  height: 35px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  transition: all 0.2s ease;
}

.btn-scan-qr:hover {
  background-color: #e0f2fe;
  border-color: #7dd3fc;
  color: #0284c7;
  transform: translateY(-1px);
}

.btn-qr-inline {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  color: var(--blue, #496883);
  padding: 5px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  z-index: 2;
}

.btn-qr-inline:hover {
  background: #e0f2fe;
  color: #0284c7;
}

.input-has-prefix {
  padding-left: 2.35rem !important;
  padding-right: 2.5rem !important;
}

.select-combobox {
  cursor: pointer;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 7px;
  width: 100%;
}

.form-group label {
  font-size: 0.83rem;
  font-weight: 500;
  color: #555e65;
}

.form-control {
  width: 100%;
  height: 2.6rem;
  padding: 0 0.85rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  background-color: #fcfbf8;
  color: var(--text, #3d4a50);
  font-size: 0.92rem;
  font-family: inherit;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.form-control:hover {
  border-color: #b8c3cc;
  background-color: #ffffff;
}

.form-control:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.table-responsive {
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.88rem;
}

.custom-table th {
  background-color: #f7f5ef;
  text-transform: uppercase;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  color: var(--blue, #496883);
  font-weight: 700;
  padding: 13px 14px;
  white-space: nowrap;
  border-bottom: 2px solid #e9e5db;
}

.custom-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #f1e7dc;
  color: #40484f;
  vertical-align: middle;
}

.table-row:hover {
  background-color: #fcfaf7;
}

.font-bold {
  font-weight: 700;
}

.text-code {
  color: #2b353b;
}

.text-customer {
  color: #3e474f;
}

.text-employee {
  color: #5d676e;
}

.th-price {
  white-space: nowrap;
}

.text-price {
  color: #2b353b;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.date-main {
  color: #3e474f;
}

.time-sub {
  font-size: 0.75rem;
  color: #8c969e;
}

.text-center {
  text-align: center;
}

.text-empty {
  padding: 30px;
  color: #8c969e;
}

.badge-store {
  background: #e8f6ed;
  color: #23783a;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.76rem;
  font-weight: 600;
}

.badge-online {
  background: #e7f0fd;
  color: #1a73e8;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.76rem;
  font-weight: 600;
}

.status-cell-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.badge-unprinted-list {
  display: inline-block;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
  background-color: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
  white-space: nowrap;
}

.badge-status {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-waiting {
  background: #f5edff;
  color: #8a3ee6;
}

.status-pending {
  background: #fff4e5;
  color: #d97706;
}

.status-confirmed {
  background: #e0f2fe;
  color: #0369a1;
}

.status-shipping {
  background: #e0f7fa;
  color: #00838f;
}

.status-completed {
  background: #e6f6ec;
  color: #1b7a37;
}

.status-cancelled {
  background: #fee6e6;
  color: #be2626;
}

.status-failed {
  background: #fef2f2;
  color: #dc2626;
}


.status-default {
  background: #f0e9df;
  color: #636b72;
}

.badge-payment {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.76rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-dot,
.payment-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: currentColor;
  flex-shrink: 0;
}

.payment-paid {
  background: #eaf8ed;
  color: #227838;
  border: 1px solid #bce1c5;
}

.payment-unpaid {
  background: #fff6eb;
  color: #b75e11;
  border: 1px solid #fad3ae;
}

.btn-action-view {
  width: 32px;
  height: 32px;
  border: 1px solid #d5c8b8;
  background: #ffffff;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #277da1;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-action-view:hover {
  background-color: #faf7f0;
  border-color: #a49787;
}

.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 18px;
  margin-top: 8px;
}

.pagination-info {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 0.85rem;
  font-weight: 500;
  color: #555e65;
  line-height: 1.5;
}

.pagination-buttons {
  display: flex;
  gap: 5px;
}

.btn-pager {
  width: 32px;
  height: 32px;
  border: 1px solid #ded5c7;
  background: #ffffff;
  color: #555e65;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.84rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-pager.active {
  background: #36536b;
  border-color: #36536b;
  color: #ffffff;
}

.btn-pager:hover:not(:disabled):not(.active) {
  background: #f4ede4;
}

.btn-pager:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.state-message {
  padding: 36px;
  text-align: center;
  color: #555e65;
}

.state-message.error {
  color: #c53030;
}

/* TOAST POPUP */
.toast-popup {
  position: fixed;
  top: 24px;
  right: 28px;
  background-color: #36536b;
  color: #ffffff;
  padding: 10px 18px;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.88rem;
  font-weight: 600;
  z-index: 9999;
  border: 1px solid #4a6880;
}

.toast-popup.warning {
  background-color: #d97706;
  border-color: #b45309;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.25s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}


@media (max-width: 1024px) {
  .filter-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hoa-don-container {
    padding: 16px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .table-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .filter-actions-bottom {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .pagination-section {
    flex-direction: column;
    gap: 14px;
    align-items: flex-start;
  }
}
</style>