<template>
  <div class="product-page-container">
    <!-- Tiêu đề trang -->
    <div class="page-header-row">
      <div class="page-breadcrumb">
        <router-link to="/san-pham" class="crumb-parent">Quản lý sản phẩm</router-link>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">Danh sách sản phẩm</span>
      </div>
      <div v-if="toast.show" :class="['toast-notification', toast.type]">
        {{ toast.message }}
      </div>
    </div>

    <!-- Khối khung trắng bao bọc toàn bộ chức năng -->
    <div class="product-card">
      <!-- 1. Hàng tìm kiếm và các nút chính -->
      <div class="search-actions-bar">
        <!-- Cột trái: Tìm kiếm full width -->
        <div class="search-input-col">
          <label class="search-label">Tìm kiếm</label>
          <div class="input-inner-wrap">
            <span class="prefix-search-icon">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </span>
            <input
                type="text"
                v-model="filters.keyword"
                @keyup.enter="handleSearch"
                @input="onKeywordChange"
                placeholder="Tìm theo mã SP / tên sản phẩm..."
                class="input-search-full"
            />
          </div>
        </div>

        <!-- Cột phải: 4 nút hành động tách biệt hoàn toàn theo yêu cầu -->
        <div class="action-buttons-col">
          <button class="btn btn-refresh" @click="resetFilters" title="Đặt lại bộ lọc">
            Đặt lại bộ lọc
          </button>
          <button class="btn btn-export" @click="handleExportExcel" :disabled="exporting" title="Xuất danh sách ra file Excel">
            {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
          </button>
          <!-- Nút Thêm sản phẩm riêng -->
          <button class="btn btn-add" @click="goToAddPage" title="Thêm mới sản phẩm">
            + Thêm sản phẩm
          </button>
        </div>
      </div>

      <!-- 2. Khối 8 tiêu chí lọc thuộc tính nâng cấp sang Select2 -->
      <div class="filter-attributes-section">
        <div class="funnel-icon-wrap" title="Bộ lọc thuộc tính">
          <span class="funnel-icon">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z"/>
            </svg>
          </span>
        </div>

        <div class="attribute-dropdowns-grid">
          <!-- 1. Thương hiệu -->
          <Select2
            v-model="filters.idThuongHieu"
            :options="thuongHieuOptions"
            placeholder="-- Thương hiệu --"
            searchPlaceholder="Tìm thương hiệu..."
            @change="handleSearch"
          />

          <!-- 2. Danh mục -->
          <Select2
            v-model="filters.idDanhMuc"
            :options="danhMucOptions"
            placeholder="-- Danh mục --"
            searchPlaceholder="Tìm danh mục..."
            @change="handleSearch"
          />

          <!-- 3. Xuất xứ -->
          <Select2
            v-model="filters.idXuatXu"
            :options="xuatXuOptions"
            placeholder="-- Xuất xứ --"
            searchPlaceholder="Tìm xuất xứ..."
            @change="handleSearch"
          />

          <!-- 4. Chất liệu -->
          <Select2
            v-model="filters.idChatLieu"
            :options="chatLieuOptions"
            placeholder="-- Chất liệu --"
            searchPlaceholder="Tìm chất liệu..."
            @change="handleSearch"
          />

          <!-- 5. Cổ áo -->
          <Select2
            v-model="filters.idCoAo"
            :options="coAoOptions"
            placeholder="-- Cổ áo --"
            searchPlaceholder="Tìm cổ áo..."
            @change="handleSearch"
          />

          <!-- 6. Tay áo -->
          <Select2
            v-model="filters.idTayAo"
            :options="tayAoOptions"
            placeholder="-- Tay áo --"
            searchPlaceholder="Tìm tay áo..."
            @change="handleSearch"
          />

          <!-- 7. Họa tiết -->
          <Select2
            v-model="filters.idHoaTiet"
            :options="hoaTietOptions"
            placeholder="-- Họa tiết --"
            searchPlaceholder="Tìm họa tiết..."
            @change="handleSearch"
          />

          <!-- 8. Trạng thái -->
          <Select2
            v-model="filters.trangThai"
            :options="trangThaiOptions"
            placeholder="-- Trạng thái --"
            searchPlaceholder="Tìm trạng thái..."
            @change="handleSearch"
          />
        </div>
      </div>

      <!-- 3. Bảng dữ liệu sản phẩm -->
      <div class="table-responsive">
        <table class="product-table">
          <thead>
          <tr>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 100px; text-align: center;">Ảnh</th>
            <th style="width: 120px;">Mã SP</th>
            <th>Tên sản phẩm</th>
            <th style="width: 180px;">Phân loại</th>
            <th style="width: 170px;">Khoảng giá & Giảm</th>
            <th style="width: 95px; text-align: center;">Tồn kho</th>
            <th style="width: 110px;">Ngày tạo</th>
            <th style="width: 130px; text-align: center;">Trạng thái</th>
            <th style="width: 130px; text-align: center;">Thao tác</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading">
            <td colspan="10" class="empty-state">
              <div class="empty-box">
                <div class="loading-spinner"></div>
                <span class="empty-text">Đang tải dữ liệu sản phẩm...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="products.length === 0">
            <td colspan="10" class="empty-state">
              <div class="empty-box">
                <span class="empty-icon">
                  <svg viewBox="0 0 24 24" width="38" height="38" fill="currentColor" opacity="0.45">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v3c0 .55.45 1 1 1h1v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8h1c.55 0 1-.45 1-1V4c0-1.1-.9-2-2-2zm-1 6v12H5V8h14zM4 4h16v2H4V4zm5 6h6v2H9v-2z"/>
                  </svg>
                </span>
                <span class="empty-text">Không tìm thấy sản phẩm nào phù hợp với bộ lọc!</span>
                <button type="button" class="btn btn-refresh-sm" @click="resetFilters">Làm mới bộ lọc</button>
              </div>
            </td>
          </tr>
          <tr v-for="(item, index) in products" :key="item.id">
            <!-- STT -->
            <td style="text-align: center;" class="stt-cell">
              {{ pageData.number * pageData.size + index + 1 }}
            </td>

            <!-- Ảnh sản phẩm -->
            <td style="text-align: center;">
              <div class="product-img-box" @click="openProductDetail(item)" title="Xem chi tiết sản phẩm">
                <img
                    :src="formatImageUrl(item.anhDaiDien) || defaultImage"
                    :alt="item.tenSanPham"
                    @error="onImgError"
                />
              </div>
            </td>

            <!-- Mã sản phẩm -->
            <td class="code-cell">
              <b>{{ item.maSanPham }}</b>
            </td>

            <!-- Tên sản phẩm -->
            <td class="product-name-cell">
              <div class="name-text" @click="openProductDetail(item)" title="Xem chi tiết sản phẩm">
                {{ item.tenSanPham }}
              </div>
              <div v-if="item.moTa" class="desc-text">{{ item.moTa }}</div>
            </td>

            <!-- Phân loại -->
            <td>
              <div class="attr-badges">
                <span class="attr-tag" v-if="item.tenThuongHieu">{{ item.tenThuongHieu }}</span>
                <span class="attr-tag" v-if="item.tenDanhMuc">{{ item.tenDanhMuc }}</span>
                <span class="attr-tag" v-if="item.tenChatLieu">{{ item.tenChatLieu }}</span>
              </div>
            </td>

            <!-- Đợt giảm giá & Giá bán thực tế -->
            <td class="price-cell">
              <div v-if="item.coGiamGia && item.phanTramGiam > 0" class="discounted-price-block">
                <!-- Giá gốc gạch ngang -->
                <div class="price-original-striked">
                  {{ formatPriceRange(item.minGia, item.maxGia) }}
                </div>
                <!-- Giá bán thực tế sau giảm giá -->
                <div class="price-actual-sale">
                  {{ formatPriceRange(item.minGiaSauGiam, item.maxGiaSauGiam) }}
                </div>
                <!-- Tag đợt giảm giá -->
                <div class="campaign-pill-badge" :title="item.tenDotGiamGia || 'Đợt giảm giá'">
                  <span class="sale-percent">-{{ item.phanTramGiam }}%</span>
                  <span class="sale-name" v-if="item.tenDotGiamGia">{{ item.tenDotGiamGia }}</span>
                </div>
              </div>
              <div v-else class="price-normal-block">
                <b>{{ formatPriceRange(item.minGia, item.maxGia) }}</b>
              </div>
            </td>

            <!-- Số lượng tồn -->
            <td style="text-align: center;">
              <span :class="['stock-badge', item.tongSoLuong > 0 ? 'in-stock' : 'out-of-stock']">
                {{ item.tongSoLuong ?? 0 }}
              </span>
            </td>

            <!-- Ngày tạo -->
            <td class="date-cell">{{ formatDate(item.ngayTao) }}</td>

            <!-- Trạng thái sản phẩm -->
            <td style="text-align: center;">
              <span :class="['status-badge', item.trangThai === 1 ? 'status-active' : 'status-inactive']">
                {{ item.trangThai === 1 ? 'Kinh doanh' : 'Ngừng KD' }}
              </span>
            </td>

            <!-- Cột Thao tác: Nút Chi tiết (icon mắt) + Nút Chỉnh sửa gom gọn -->
            <td style="text-align: center;">
              <div class="action-buttons-group">
                <!-- Nút icon con mắt: CHỈ mở modal thông tin chi tiết sản phẩm cha -->
                <button
                    class="btn-action btn-view-detail"
                    @click="openProductDetail(item)"
                    title="Xem chi tiết sản phẩm cha"
                >
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                </button>

                <!-- Nút Chỉnh sửa (Gom các nút thao tác riêng lẻ thành nút Chỉnh sửa duy nhất kèm menu) -->
                <div class="action-dropdown-wrapper">
                  <button
                      class="btn-action btn-edit-primary"
                      @click="goToEditPage(item)"
                      title="Chỉnh sửa sản phẩm"
                  >
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </button>

                  <!-- Nút menu mở rộng (3 chấm) -->
                  <button
                      class="btn-action btn-menu-more"
                      @click="toggleActionMenu(item.id)"
                      title="Thao tác khác"
                  >
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                      <circle cx="12" cy="5" r="2"></circle>
                      <circle cx="12" cy="12" r="2"></circle>
                      <circle cx="12" cy="19" r="2"></circle>
                    </svg>
                  </button>

                  <!-- Dropdown Menu -->
                  <div v-if="activeMenuId === item.id" class="dropdown-action-menu" @click.stop>
                    <button class="dropdown-menu-item" @click="goToVariants(item)">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                      </svg>
                      Quản lý biến thể ({{ item.soLuongBienThe || 0 }})
                    </button>
                    <button class="dropdown-menu-item" @click="goToVariants(item)">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                      </svg>
                      + Thêm biến thể cho SP này
                    </button>
                    <button class="dropdown-menu-item" @click="toggleStatus(item)">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                        <line x1="12" y1="2" x2="12" y2="12"></line>
                      </svg>
                      {{ item.trangThai === 1 ? 'Ngừng kinh doanh' : 'Mở kinh doanh' }}
                    </button>
                  </div>
                </div>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- 4. Thanh phân trang -->
      <div class="pagination-footer" v-if="pageData.totalPages > 0">
        <div class="pagination-left">
          <div class="page-size-selector">
            <span class="page-size-label">Hiển thị:</span>
            <select v-model.number="filters.size" @change="handlePageSizeChange" class="page-size-select">
              <option :value="5">5</option>
              <option :value="10">10</option>
              <option :value="15">15</option>
              <option :value="20">20</option>
              <option :value="25">25</option>
            </select>
            <span class="page-size-unit">/ trang</span>
          </div>
        </div>
        <div class="pagination-center">
          <button
              class="pg-box"
              :disabled="pageData.number === 0"
              @click="changePage(0)"
              title="Trang đầu"
          >«</button>
          <button
              class="pg-box"
              :disabled="pageData.number === 0"
              @click="changePage(pageData.number - 1)"
              title="Trang trước"
          >‹</button>

          <button
              v-for="p in visiblePages"
              :key="p"
              :class="['pg-box', { active: pageData.number === p }]"
              @click="changePage(p)"
          >
            {{ p + 1 }}
          </button>

          <button
              class="pg-box"
              :disabled="pageData.number >= pageData.totalPages - 1"
              @click="changePage(pageData.number + 1)"
              title="Trang sau"
          >›</button>
          <button
              class="pg-box"
              :disabled="pageData.number >= pageData.totalPages - 1"
              @click="changePage(pageData.totalPages - 1)"
              title="Trang cuối"
          >»</button>
        </div>
      </div>
    </div>

    <!-- MODAL XEM CHI TIẾT SẢN PHẨM CHA (TUYỆT ĐỐI KHÔNG MỞ BIẾN THỂ) -->
    <div class="modal-backdrop" v-if="detailModal.show" @click.self="closeDetailModal">
      <div class="modal-dialog product-detail-dialog">
        <div class="modal-header">
          <div class="header-left">
            <span class="badge-parent-product">Chi tiết sản phẩm</span>
            <h3 class="modal-title">{{ detailModal.data.tenSanPham }}</h3>
            <span class="modal-code">Mã SP: {{ detailModal.data.maSanPham }}</span>
          </div>
          <button class="btn-close-modal" @click="closeDetailModal" title="Đóng">&times;</button>
        </div>

        <div class="modal-body product-detail-body">
          <!-- Cột trái: Ảnh sản phẩm to & Khoảng giá & Đợt giảm giá -->
          <div class="detail-left-col">
            <div class="detail-img-box">
              <img
                :src="formatImageUrl(detailModal.data.anhDaiDien) || defaultImage"
                :alt="detailModal.data.tenSanPham"
                @error="onImgError"
              />
            </div>

            <!-- Giá & Đợt giảm giá -->
            <div class="detail-price-card">
              <span class="detail-price-label">Khoảng giá bán</span>
              <div v-if="detailModal.data.coGiamGia && detailModal.data.phanTramGiam > 0">
                <div class="detail-price-old">
                  {{ formatPriceRange(detailModal.data.minGia, detailModal.data.maxGia) }}
                </div>
                <div class="detail-price-actual">
                  {{ formatPriceRange(detailModal.data.minGiaSauGiam, detailModal.data.maxGiaSauGiam) }}
                </div>
                <div class="detail-discount-tag">
                  🎉 Đang áp dụng đợt giảm giá: <b>{{ detailModal.data.tenDotGiamGia }} (-{{ detailModal.data.phanTramGiam }}%)</b>
                </div>
              </div>
              <div v-else>
                <div class="detail-price-normal">
                  {{ formatPriceRange(detailModal.data.minGia, detailModal.data.maxGia) }}
                </div>
              </div>
            </div>

            <!-- Tồn kho & biến thể -->
            <div class="detail-stat-row">
              <div class="stat-box">
                <span class="stat-num">{{ detailModal.data.tongSoLuong ?? 0 }}</span>
                <span class="stat-lbl">Tổng tồn kho</span>
              </div>
              <div class="stat-box">
                <span class="stat-num">{{ detailModal.data.soLuongBienThe ?? 0 }}</span>
                <span class="stat-lbl">Số lượng biến thể</span>
              </div>
            </div>
          </div>

          <!-- Cột phải: Thông tin thuộc tính & mô tả của sản phẩm cha -->
          <div class="detail-right-col">
            <h4 class="section-title">Thuộc tính sản phẩm</h4>
            <div class="attrs-grid-info">
              <div class="attr-info-item">
                <span class="attr-k">Thương hiệu:</span>
                <span class="attr-v">{{ detailModal.data.tenThuongHieu || '—' }}</span>
              </div>
              <div class="attr-info-item">
                <span class="attr-k">Danh mục:</span>
                <span class="attr-v">{{ detailModal.data.tenDanhMuc || '—' }}</span>
              </div>
              <div class="attr-info-item">
                <span class="attr-k">Xuất xứ:</span>
                <span class="attr-v">{{ detailModal.data.tenXuatXu || '—' }}</span>
              </div>
              <div class="attr-info-item">
                <span class="attr-k">Chất liệu:</span>
                <span class="attr-v">{{ detailModal.data.tenChatLieu || '—' }}</span>
              </div>
              <div class="attr-info-item">
                <span class="attr-k">Cổ áo:</span>
                <span class="attr-v">{{ detailModal.data.tenCoAo || '—' }}</span>
              </div>
              <div class="attr-info-item">
                <span class="attr-k">Tay áo:</span>
                <span class="attr-v">{{ detailModal.data.tenTayAo || '—' }}</span>
              </div>
              <div class="attr-info-item">
                <span class="attr-k">Họa tiết:</span>
                <span class="attr-v">{{ detailModal.data.tenHoaTiet || '—' }}</span>
              </div>
              <div class="attr-info-item">
                <span class="attr-k">Trạng thái:</span>
                <span class="attr-v">
                  <span :class="['status-badge', detailModal.data.trangThai === 1 ? 'status-active' : 'status-inactive']">
                    {{ detailModal.data.trangThai === 1 ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
                  </span>
                </span>
              </div>
            </div>

            <h4 class="section-title" style="margin-top: 1.25rem;">Mô tả sản phẩm</h4>
            <div class="detail-desc-box">
              {{ detailModal.data.moTa || 'Chưa có mô tả chi tiết cho sản phẩm này.' }}
            </div>

            <div class="audit-info-row">
              <span>Ngày tạo: <b>{{ formatDate(detailModal.data.ngayTao) }}</b></span>
              <span v-if="detailModal.data.nguoiTao">Người tạo: <b>{{ detailModal.data.nguoiTao }}</b></span>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeDetailModal">Đóng</button>
          <button class="btn btn-primary" @click="goToEditFromDetail">
            Chỉnh sửa sản phẩm cha
          </button>
        </div>
      </div>
    </div>



    <!-- MODAL XÁC NHẬN HÀNH ĐỘNG (XÓA / ĐỔI TRẠNG THÁI) -->
    <transition name="fade">
      <div v-if="confirmDialog.show" class="confirm-modal-overlay" @click="closeConfirmDialog">
        <div class="confirm-modal-box" @click.stop>
          <div class="confirm-icon-wrap" :class="confirmDialog.type">
            <span v-if="confirmDialog.type === 'danger'">⚠️</span>
            <span v-else-if="confirmDialog.type === 'info'">ℹ️</span>
            <span v-else>❓</span>
          </div>

          <h3 class="confirm-title">{{ confirmDialog.title }}</h3>
          <p class="confirm-message">{{ confirmDialog.message }}</p>

          <div class="confirm-actions">
            <button type="button" class="btn btn-confirm-cancel" @click="closeConfirmDialog">
              Hủy bỏ
            </button>
            <button
                type="button"
                :class="['btn', confirmDialog.type === 'danger' ? 'btn-confirm-danger' : 'btn-confirm-primary']"
                @click="executeConfirmedAction"
            >
              {{ confirmDialog.confirmText || 'Đồng ý' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'
import Select2 from '@/components/Select2.vue'

const router = useRouter()

const defaultImage = 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300&auto=format&fit=crop&q=80'

// Bộ lọc
const filters = ref({
  keyword: '',
  idThuongHieu: '',
  idDanhMuc: '',
  idXuatXu: '',
  idChatLieu: '',
  idCoAo: '',
  idTayAo: '',
  idHoaTiet: '',
  trangThai: '',
  page: 0,
  size: 10
})

// Dữ liệu thuộc tính
const attributes = ref({
  thuongHieu: [],
  danhMuc: [],
  xuatXu: [],
  chatLieu: [],
  coAo: [],
  tayAo: [],
  hoaTiet: [],
  mauSac: [],
  kichCo: []
})

// Options chuẩn hóa cho Select2
const thuongHieuOptions = computed(() => [
  { value: '', label: '-- Thương hiệu --' },
  ...(attributes.value.thuongHieu || []).map(i => ({ value: i.id, label: i.tenThuongHieu }))
])

const danhMucOptions = computed(() => [
  { value: '', label: '-- Danh mục --' },
  ...(attributes.value.danhMuc || []).map(i => ({ value: i.id, label: i.tenDanhMuc }))
])

const xuatXuOptions = computed(() => [
  { value: '', label: '-- Xuất xứ --' },
  ...(attributes.value.xuatXu || []).map(i => ({ value: i.id, label: i.tenXuatXu }))
])

const chatLieuOptions = computed(() => [
  { value: '', label: '-- Chất liệu --' },
  ...(attributes.value.chatLieu || []).map(i => ({ value: i.id, label: i.tenChatLieu }))
])

const coAoOptions = computed(() => [
  { value: '', label: '-- Cổ áo --' },
  ...(attributes.value.coAo || []).map(i => ({ value: i.id, label: i.tenCoAo }))
])

const tayAoOptions = computed(() => [
  { value: '', label: '-- Tay áo --' },
  ...(attributes.value.tayAo || []).map(i => ({ value: i.id, label: i.tenTayAo }))
])

const hoaTietOptions = computed(() => [
  { value: '', label: '-- Họa tiết --' },
  ...(attributes.value.hoaTiet || []).map(i => ({ value: i.id, label: i.tenHoaTiet }))
])

const trangThaiOptions = [
  { value: '', label: '-- Trạng thái --' },
  { value: 1, label: 'Đang kinh doanh' },
  { value: 0, label: 'Ngừng kinh doanh' }
]

const mauSacOptions = computed(() => [
  { value: '', label: '-- Chọn màu sắc --' },
  ...(attributes.value.mauSac || []).map(i => ({ value: i.id, label: i.tenMauSac }))
])

const kichCoOptions = computed(() => [
  { value: '', label: '-- Chọn kích cỡ --' },
  ...(attributes.value.kichCo || []).map(i => ({ value: i.id, label: `Size ${i.tenKichCo}` }))
])



// Danh sách sản phẩm & phân trang
const products = ref([])
const loading = ref(false)
const pageData = ref({
  number: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
})

// Action dropdown active ID
const activeMenuId = ref(null)
const toggleActionMenu = (id) => {
  activeMenuId.value = activeMenuId.value === id ? null : id
}

// Đóng action menu khi click ngoài
const closeActionMenu = () => {
  activeMenuId.value = null
}

// Modal Xem chi tiết sản phẩm cha
const detailModal = ref({
  show: false,
  data: {}
})

const openProductDetail = (item) => {
  closeActionMenu()
  detailModal.value.data = { ...item }
  detailModal.value.show = true
}

const closeDetailModal = () => {
  detailModal.value.show = false
}

const goToEditFromDetail = () => {
  const item = detailModal.value.data
  closeDetailModal()
  goToEditPage(item)
}



// Hộp thoại xác nhận tương tác
const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  type: 'primary',
  confirmText: 'Đồng ý',
  onConfirm: null
})

const openConfirm = ({ title, message, type = 'primary', confirmText = 'Đồng ý', onConfirm }) => {
  confirmDialog.value = {
    show: true,
    title,
    message,
    type,
    confirmText,
    onConfirm
  }
}

const closeConfirmDialog = () => {
  confirmDialog.value.show = false
  confirmDialog.value.onConfirm = null
}

const executeConfirmedAction = async () => {
  const action = confirmDialog.value.onConfirm
  closeConfirmDialog()
  if (typeof action === 'function') {
    await action()
  }
}

// Chuyển trang Thêm sản phẩm
const goToAddPage = () => {
  router.push('/san-pham/them')
}

// Chuyển trang Sửa sản phẩm
const goToEditPage = (item) => {
  closeActionMenu()
  router.push(`/san-pham/chinh-sua/${item.id}`)
}

// Chuyển trang Biến thể sản phẩm
const goToVariants = (item) => {
  closeActionMenu()
  router.push({ path: '/san-pham/bien-the', query: { idSanPham: item.id } })
}

// Toast
const toast = ref({
  show: false,
  message: '',
  type: 'success'
})

const showToast = (message, type = 'success') => {
  toast.value = { show: true, message, type }
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

// Lấy danh sách thuộc tính
const fetchAttributes = async () => {
  try {
    const res = await api.get('/api/thuoc-tinh')
    if (res.data) {
      attributes.value = res.data
    }
  } catch (err) {
    console.error('Lỗi tải thuộc tính:', err)
  }
}

// Lấy danh sách toàn bộ sản phẩm đơn giản để chọn trong Modal thêm biến thể
const fetchAllProductsSimple = async () => {
  try {
    const res = await api.get('/api/san-pham', { params: { size: 1000 } })
    if (res.data && res.data.content) {
      allProductsList.value = res.data.content
    }
  } catch (e) {
    console.error('Lỗi tải danh sách sản phẩm:', e)
  }
}

// Lấy danh sách sản phẩm phân trang
const fetchProducts = async () => {
  loading.value = true
  closeActionMenu()
  try {
    const params = {
      page: filters.value.page,
      size: filters.value.size,
      sortBy: 'id',
      sortDir: 'desc'
    }
    if (filters.value.keyword?.trim()) params.keyword = filters.value.keyword.trim()
    if (filters.value.idThuongHieu) params.idThuongHieu = filters.value.idThuongHieu
    if (filters.value.idDanhMuc) params.idDanhMuc = filters.value.idDanhMuc
    if (filters.value.idXuatXu) params.idXuatXu = filters.value.idXuatXu
    if (filters.value.idChatLieu) params.idChatLieu = filters.value.idChatLieu
    if (filters.value.idCoAo) params.idCoAo = filters.value.idCoAo
    if (filters.value.idTayAo) params.idTayAo = filters.value.idTayAo
    if (filters.value.idHoaTiet) params.idHoaTiet = filters.value.idHoaTiet
    if (filters.value.trangThai !== '') params.trangThai = filters.value.trangThai

    const res = await api.get('/api/san-pham', { params })
    if (res.data) {
      products.value = res.data.content || []
      pageData.value = {
        number: res.data.pageNumber || 0,
        size: res.data.pageSize || 10,
        totalElements: res.data.totalElements || 0,
        totalPages: res.data.totalPages || 0
      }
    }
  } catch (err) {
    console.error('Lỗi tải sản phẩm:', err.response?.data || err)
    showToast(err.response?.data?.message || 'Lỗi khi tải danh sách sản phẩm!', 'error')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  filters.value.page = 0
  fetchProducts()
}

let searchTimeout = null
const onKeywordChange = () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    handleSearch()
  }, 350)
}

const resetFilters = () => {
  filters.value = {
    keyword: '',
    idThuongHieu: '',
    idDanhMuc: '',
    idXuatXu: '',
    idChatLieu: '',
    idCoAo: '',
    idTayAo: '',
    idHoaTiet: '',
    trangThai: '',
    page: 0,
    size: 10
  }
  fetchProducts()
}

const exporting = ref(false)

const handleExportExcel = async () => {
  exporting.value = true
  try {
    const params = {}
    if (filters.value.keyword?.trim()) params.keyword = filters.value.keyword.trim()
    if (filters.value.idThuongHieu) params.idThuongHieu = filters.value.idThuongHieu
    if (filters.value.idDanhMuc) params.idDanhMuc = filters.value.idDanhMuc
    if (filters.value.idXuatXu) params.idXuatXu = filters.value.idXuatXu
    if (filters.value.idChatLieu) params.idChatLieu = filters.value.idChatLieu
    if (filters.value.idCoAo) params.idCoAo = filters.value.idCoAo
    if (filters.value.idTayAo) params.idTayAo = filters.value.idTayAo
    if (filters.value.idHoaTiet) params.idHoaTiet = filters.value.idHoaTiet
    if (filters.value.trangThai !== '') params.trangThai = filters.value.trangThai

    const res = await api.get('/api/san-pham/export-excel', {
      params,
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `danh_sach_san_pham_${Date.now()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    showToast('Xuất file Excel thành công!')
  } catch (err) {
    console.error(err)
    showToast('Lỗi khi xuất file Excel!', 'error')
  } finally {
    exporting.value = false
  }
}

const handlePageSizeChange = () => {
  filters.value.page = 0
  fetchProducts()
}

const changePage = (p) => {
  if (p < 0 || p >= pageData.value.totalPages) return
  filters.value.page = p
  fetchProducts()
}

const visiblePages = computed(() => {
  const current = pageData.value.number
  const total = pageData.value.totalPages
  const pages = []
  const start = Math.max(0, current - 2)
  const end = Math.min(total - 1, current + 2)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// Định dạng giá tiền
const formatPriceRange = (min, max) => {
  if (min == null && max == null) return 'Chưa có biến thể'
  if (min != null && max != null && min === max) {
    return Number(min).toLocaleString('vi-VN') + ' đ'
  }
  const minStr = min != null ? Number(min).toLocaleString('vi-VN') : '0'
  const maxStr = max != null ? Number(max).toLocaleString('vi-VN') : '0'
  return `${minStr} - ${maxStr} đ`
}

// Định dạng ngày
const formatDate = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  return d.toLocaleDateString('vi-VN')
}

const formatImageUrl = (url) => {
  if (!url) return ''
  let cleanUrl = url
  if (cleanUrl.includes('|')) {
    cleanUrl = cleanUrl.split('|')[1]
  }
  if (cleanUrl.startsWith('data:') || cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) return cleanUrl
  return `http://localhost:8080${cleanUrl.startsWith('/') ? '' : '/'}${cleanUrl}`
}

const onImgError = (e) => {
  e.target.src = defaultImage
}

// Thay đổi trạng thái sản phẩm có xác nhận
const toggleStatus = (item) => {
  closeActionMenu()
  const newStatus = item.trangThai === 1 ? 0 : 1
  const actionText = newStatus === 1 ? 'mở kinh doanh lại' : 'ngừng kinh doanh'
  openConfirm({
    title: 'Xác nhận thay đổi trạng thái',
    message: `Bạn có chắc chắn muốn ${actionText} sản phẩm "${item.tenSanPham}" không?`,
    type: 'info',
    confirmText: 'Xác nhận',
    onConfirm: async () => {
      try {
        await api.patch(`/api/san-pham/${item.id}/status`, null, {
          params: { trangThai: newStatus }
        })
        item.trangThai = newStatus
        showToast(`Đã chuyển trạng thái sang "${newStatus === 1 ? 'Kinh doanh' : 'Ngừng KD'}"`)
      } catch (err) {
        console.error('Lỗi cập nhật trạng thái:', err)
        showToast('Lỗi cập nhật trạng thái!', 'error')
      }
    }
  })
}

onMounted(() => {
  fetchAttributes()
  fetchProducts()
  document.addEventListener('click', closeActionMenu)
})
</script>

<style scoped>
.product-page-container {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* Breadcrumb */
.page-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.crumb-parent {
  color: #64748b;
  text-decoration: none;
}
.crumb-parent:hover {
  color: var(--blue, #496883);
}

.crumb-separator {
  color: #94a3b8;
}

.crumb-current {
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 0.3px;
  margin: 0;
}

/* Toast */
.toast-notification {
  padding: 0.6rem 1.2rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.9rem;
  animation: slideIn 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.toast-notification.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}
.toast-notification.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Card trắng */
.product-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.5rem 1.75rem;
}

/* 1. Hàng tìm kiếm & nút */
.search-actions-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.25rem;
  width: 100%;
}

.search-input-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 0;
}

.search-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
  margin: 0;
}

.input-inner-wrap {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.prefix-search-icon {
  position: absolute;
  left: 0.95rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  pointer-events: none;
}

.input-search-full {
  width: 100%;
  height: 2.5rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0 1rem 0 2.5rem;
  font-size: 0.92rem;
  color: #1e293b;
  outline: none;
  transition: all 0.2s ease;
}
.input-search-full:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.action-buttons-col {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-shrink: 0;
}

/* Nút bấm */
.btn {
  height: 2.5rem;
  padding: 0 1.1rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border: none;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-refresh {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}
.btn-refresh:hover {
  background-color: #faeed7;
}

.btn-export {
  background-color: #edf5ef;
  color: #558764;
  border: 1px solid #cfe2d3;
}
.btn-export:hover {
  background-color: #dceada;
}

/* Nút Thêm biến thể riêng */
.btn-add-variant {
  background-color: #f0fdf4;
  color: #16a34a;
  border: 1.5px solid #86efac;
}
.btn-add-variant:hover {
  background-color: #dcfce7;
  border-color: #4ade80;
}

/* Nút Thêm sản phẩm riêng */
.btn-add {
  background-color: var(--blue, #496883);
  color: #ffffff;
}
.btn-add:hover {
  background-color: #385269;
}

/* 2. Bộ lọc 8 tiêu chí Select2 */
.filter-attributes-section {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background-color: #fcfbf9;
  border: 1px solid #eeebe2;
  border-radius: 10px;
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
}

.funnel-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  flex-shrink: 0;
}

.attribute-dropdowns-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.65rem;
  width: 100%;
}
@media (max-width: 1200px) {
  .attribute-dropdowns-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .attribute-dropdowns-grid {
    grid-template-columns: 1fr;
  }
}

/* 3. Bảng sản phẩm */
.table-responsive {
  width: 100%;
  overflow-x: auto;
  border-radius: 8px;
  border: 1px solid #edf2f7;
}

.product-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.88rem;
}

.product-table thead th {
  background-color: #f8fafc;
  color: #475569;
  font-weight: 700;
  padding: 0.85rem 0.75rem;
  border-bottom: 1.5px solid #e2e8f0;
  white-space: nowrap;
}

.product-table tbody tr {
  border-bottom: 1px solid #f1f5f9;
  transition: background-color 0.15s ease;
}
.product-table tbody tr:hover {
  background-color: #f8fafc;
}

.product-table td {
  padding: 0.75rem;
  vertical-align: middle;
}

.stt-cell {
  color: #94a3b8;
  font-weight: 600;
}

.product-img-box {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background-color: #f8fafc;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.product-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s ease;
}
.product-img-box:hover img {
  transform: scale(1.08);
}

.code-cell b {
  color: var(--blue, #496883);
  font-weight: 700;
}

.product-name-cell {
  max-width: 260px;
}
.name-text {
  font-weight: 700;
  color: #1e293b;
  cursor: pointer;
}
.name-text:hover {
  color: var(--blue, #496883);
  text-decoration: underline;
}
.desc-text {
  font-size: 0.78rem;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.attr-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.attr-tag {
  background: #f1f5f9;
  color: #475569;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 0.75rem;
  font-weight: 500;
}

/* Đợt giảm giá & Giá bán */
.price-cell {
  font-size: 0.88rem;
}

.discounted-price-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.price-original-striked {
  font-size: 0.78rem;
  color: #94a3b8;
  text-decoration: line-through;
}
.price-actual-sale {
  color: #dc2626;
  font-weight: 800;
  font-size: 0.95rem;
}
.campaign-pill-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 0.72rem;
  font-weight: 700;
  margin-top: 2px;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-normal-block {
  color: #1e293b;
  font-weight: 700;
}

.stock-badge {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.8rem;
}
.stock-badge.in-stock {
  background: #f0fdf4;
  color: #16a34a;
}
.stock-badge.out-of-stock {
  background: #fef2f2;
  color: #dc2626;
}

.date-cell {
  color: #64748b;
  font-size: 0.82rem;
}

.status-badge {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 0.78rem;
  font-weight: 700;
}
.status-active {
  background: #ecfdf5;
  color: #059669;
}
.status-inactive {
  background: #f1f5f9;
  color: #64748b;
}

/* Thao tác gom gọn */
.action-buttons-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-action {
  height: 32px;
  padding: 0 8px;
  border-radius: 6px;
  border: 1px solid transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 0.82rem;
  font-weight: 600;
  transition: all 0.15s ease;
  background: transparent;
}

/* Nút Chi tiết (icon mắt) */
.btn-view-detail {
  background-color: #f1f5f9;
  color: #334155;
  border-color: #e2e8f0;
}
.btn-view-detail:hover {
  background-color: #e2e8f0;
  color: #0f172a;
}

/* Nút Sửa chính */
.btn-edit-primary {
  width: 32px;
  padding: 0;
  background-color: #eff6ff;
  color: #2563eb;
  border-color: #bfdbfe;
}
.btn-edit-primary:hover {
  background-color: #dbeafe;
}

.action-dropdown-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.btn-menu-more {
  padding: 0 4px;
  color: #64748b;
  border-radius: 4px;
}
.btn-menu-more:hover {
  background-color: #f1f5f9;
  color: #1e293b;
}

.dropdown-action-menu {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 10px 20px rgba(0,0,0,0.08);
  min-width: 190px;
  z-index: 100;
  padding: 4px 0;
  animation: fadeIn 0.15s ease;
}

.dropdown-menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: transparent;
  border: none;
  font-size: 0.82rem;
  color: #334155;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.1s ease;
}
.dropdown-menu-item:hover {
  background-color: #f8fafc;
  color: var(--blue, #496883);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Phân trang */
.pagination-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1.25rem;
}
.page-size-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: #64748b;
}
.page-size-select {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 4px 6px;
  background: #fff;
}
.pagination-center {
  display: flex;
  align-items: center;
  gap: 4px;
}
.pg-box {
  min-width: 32px;
  height: 32px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
}
.pg-box.active {
  background-color: var(--blue, #496883);
  color: #fff;
  border-color: var(--blue, #496883);
}
.pg-box:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Modals */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 1rem;
}

.modal-dialog {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.product-detail-dialog {
  max-width: 820px;
}

.add-variant-dialog {
  max-width: 600px;
}

.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  background-color: #f8fafc;
}
.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.badge-parent-product {
  display: inline-block;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  width: fit-content;
}
.badge-add-variant {
  display: inline-block;
  background: #dcfce7;
  color: #15803d;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  width: fit-content;
}
.modal-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
}
.modal-code {
  font-size: 0.82rem;
  color: #64748b;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
}
.btn-close-modal:hover {
  color: #0f172a;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  background: #f8fafc;
}

.btn-secondary {
  background: #e2e8f0;
  color: #334155;
}
.btn-primary {
  background: var(--blue, #496883);
  color: #fff;
}

/* Chi tiết sản phẩm cha */
.product-detail-body {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 1.5rem;
}
@media (max-width: 768px) {
  .product-detail-body {
    grid-template-columns: 1fr;
  }
}
.detail-left-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.detail-img-box {
  width: 100%;
  height: 240px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}
.detail-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.detail-price-card {
  background: #fdfaf3;
  border: 1px solid #f2e9d2;
  border-radius: 8px;
  padding: 0.85rem 1rem;
}
.detail-price-label {
  font-size: 0.78rem;
  color: #78643c;
  font-weight: 600;
  display: block;
}
.detail-price-old {
  font-size: 0.82rem;
  color: #94a3b8;
  text-decoration: line-through;
}
.detail-price-actual {
  font-size: 1.25rem;
  font-weight: 800;
  color: #dc2626;
}
.detail-price-normal {
  font-size: 1.2rem;
  font-weight: 700;
  color: #1e293b;
}
.detail-discount-tag {
  font-size: 0.78rem;
  color: #b91c1c;
  margin-top: 4px;
}
.detail-stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.stat-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px;
  text-align: center;
}
.stat-num {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--blue, #496883);
  display: block;
}
.stat-lbl {
  font-size: 0.75rem;
  color: #64748b;
}

.section-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.65rem 0;
  padding-bottom: 4px;
  border-bottom: 1.5px solid #f1f5f9;
}
.attrs-grid-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
}
.attr-info-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  padding: 4px 0;
  border-bottom: 1px dashed #f1f5f9;
}
.attr-k {
  color: #64748b;
}
.attr-v {
  font-weight: 600;
  color: #1e293b;
}
.detail-desc-box {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 6px;
  padding: 0.85rem;
  font-size: 0.85rem;
  color: #475569;
  line-height: 1.5;
  min-height: 60px;
}
.audit-info-row {
  display: flex;
  justify-content: space-between;
  margin-top: 1rem;
  font-size: 0.78rem;
  color: #94a3b8;
}

/* Modal Form Add Variant */
.form-row {
  margin-bottom: 1rem;
}
.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.form-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: #475569;
}
.form-label.required::after {
  content: ' *';
  color: #ef4444;
}
.form-input {
  height: 38px;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.88rem;
  color: #1e293b;
  outline: none;
}
.form-input:focus {
  border-color: #2563eb;
}
.err-text {
  font-size: 0.75rem;
  color: #ef4444;
}

.variant-img-uploader-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.variant-mini-img-preview {
  position: relative;
  width: 50px;
  height: 50px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}
.variant-mini-img-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.btn-remove-mini-img {
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  cursor: pointer;
}
.btn-upload-file {
  padding: 6px 14px;
  background: #f1f5f9;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.82rem;
  color: #475569;
  font-weight: 600;
}
.btn-upload-file:hover {
  background: #e2e8f0;
}

/* Confirm modal */
.confirm-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}
.confirm-modal-box {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
  width: 100%;
  max-width: 400px;
  text-align: center;
}
.confirm-icon-wrap {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}
.confirm-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.15rem;
  font-weight: 700;
}
.confirm-message {
  color: #64748b;
  font-size: 0.9rem;
  margin: 0 0 1.25rem 0;
}
.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}
.btn-confirm-cancel {
  background: #f1f5f9;
  color: #475569;
}
.btn-confirm-primary {
  background: var(--blue, #496883);
  color: #fff;
}
.btn-confirm-danger {
  background: #ef4444;
  color: #fff;
}
</style>