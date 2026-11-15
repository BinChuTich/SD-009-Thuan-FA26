<template>
  <div class="shipping-settings-page">
    <!-- Toast Notification -->
    <transition name="toast-fade">
      <div v-if="toast.show" class="shipping-toast" :class="toast.type">
        <span>{{ toast.message }}</span>
      </div>
    </transition>

    <!-- Top Header -->
    <div class="page-header-card">
      <div class="header-left">
        <div class="header-icon-box">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">
            <rect x="1" y="3" width="15" height="13"></rect>
            <polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon>
            <circle cx="5.5" cy="18.5" r="2.5"></circle>
            <circle cx="18.5" cy="18.5" r="2.5"></circle>
          </svg>
        </div>
        <div>
          <div class="title-row">
            <h2 class="page-title">Cấu hình phí vận chuyển (Phí Ship)</h2>
            <span class="sync-badge">Đồng bộ POS &amp; Online</span>
          </div>
          <p class="page-subtitle">
            Thiết lập cấu hình phí vận chuyển theo giá trị hóa đơn &amp; khoảng cách (Km), bảng giá khu vực và chính sách miễn phí vận chuyển (Freeship) áp dụng đồng bộ trên toàn hệ thống.
          </p>
        </div>
      </div>

      <div class="header-actions">
        <button
          v-if="isAdminUser"
          type="button"
          class="btn-secondary"
          :disabled="saving || loading"
          @click="handleOpenCreateRuleModal"
        >
          + Tạo mới cấu hình
        </button>
        <button
          v-if="isAdminUser"
          type="button"
          class="btn-outline-reset"
          :disabled="saving || loading"
          @click="handleResetDefault"
        >
          Khôi phục mặc định
        </button>
        <button
          v-if="isAdminUser"
          type="button"
          class="btn-primary-save"
          :disabled="saving || loading"
          @click="handleSaveConfig"
        >
          <span v-if="saving">Đang lưu...</span>
          <span v-else>Lưu cấu hình</span>
        </button>
      </div>
    </div>

    <!-- Staff Notice if not ADMIN -->
    <div v-if="!isAdminUser" class="readonly-banner">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
      <span>Tài khoản Nhân viên đang ở chế độ <strong>Chỉ xem</strong>. Chỉ Quản trị viên (ADMIN) mới có quyền chỉnh sửa cấu hình phí ship.</span>
    </div>

    <div class="settings-grid">
      <!-- Left Column: Shipping Rule Table + Regional Rates + Freeship -->
      <div class="settings-main-col">
        <!-- Card 1: Cấu hình phí vận chuyển theo hóa đơn & khoảng cách -->
        <div class="config-card">
          <div class="card-header">
            <div>
              <h3 class="card-title">1. Cấu hình phí vận chuyển theo hóa đơn &amp; khoảng cách</h3>
              <p class="card-desc">
                Thiết lập mức phí vận chuyển theo khoảng giá trị hóa đơn (VND) và khoảng cách giao hàng (Km). Hệ thống ưu tiên kiểm tra danh sách này trước.
              </p>
            </div>
            <button
              v-if="isAdminUser"
              type="button"
              class="btn-create-rule"
              @click="handleOpenCreateRuleModal"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" width="15" height="15">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              <span>Tạo mới cấu hình</span>
            </button>
          </div>

          <div class="table-wrapper">
            <table class="region-rate-table rule-list-table">
              <thead>
                <tr>
                  <th style="width: 56px; text-align: center;">STT</th>
                  <th>Giá trị hóa đơn</th>
                  <th>Khoảng cách</th>
                  <th style="width: 150px; text-align: center;">Hình thức tính phí</th>
                  <th style="width: 150px; text-align: right;">Phí vận chuyển</th>
                  <th v-if="isAdminUser" style="width: 130px; text-align: center;">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!form.danhSachCauHinh || form.danhSachCauHinh.length === 0">
                  <td :colspan="isAdminUser ? 6 : 5" class="empty-rule-cell">
                    Chưa có cấu hình phí vận chuyển theo khoảng giá trị hóa đơn &amp; khoảng cách. Nhấn <strong>"Tạo mới cấu hình"</strong> để thêm mới.
                  </td>
                </tr>
                <tr v-for="(rule, index) in form.danhSachCauHinh" :key="rule.id || index">
                  <td style="text-align: center; font-weight: 700; color: #64748b;">{{ index + 1 }}</td>
                  <td>
                    <div class="range-display">
                      <span class="range-tag">Từ</span>
                      <strong>{{ formatCurrencyNumber(rule.giaTriTu) }} VND</strong>
                      <span class="range-tag">Đến</span>
                      <strong>{{ formatCurrencyNumber(rule.giaTriDen) }} VND</strong>
                    </div>
                  </td>
                  <td>
                    <div class="range-display">
                      <span class="range-tag">Từ</span>
                      <strong>{{ rule.khoangCachTu }} Km</strong>
                      <span class="range-tag">Đến</span>
                      <strong>{{ rule.khoangCachDen }} Km</strong>
                    </div>
                  </td>
                  <td style="text-align: center;">
                    <span
                      class="charge-type-badge"
                      :class="rule.hinhThucTinhPhi === 'MIEN_PHI' ? 'free' : 'paid'"
                    >
                      {{ rule.hinhThucTinhPhi === 'MIEN_PHI' ? 'Miễn phí' : 'Tính phí' }}
                    </span>
                  </td>
                  <td style="text-align: right;">
                    <span class="rule-fee-text" :class="{ free: rule.hinhThucTinhPhi === 'MIEN_PHI' || Number(rule.phiVanChuyen) === 0 }">
                      {{ rule.hinhThucTinhPhi === 'MIEN_PHI' ? '0 VND' : formatCurrencyNumber(rule.phiVanChuyen) + ' VND' }}
                    </span>
                  </td>
                  <td v-if="isAdminUser" style="text-align: center;">
                    <div class="rule-actions">
                      <button
                        type="button"
                        class="btn-rule-action edit"
                        title="Chỉnh sửa cấu hình"
                        @click="handleOpenEditRuleModal(rule)"
                      >
                        Sửa
                      </button>
                      <button
                        type="button"
                        class="btn-rule-action delete"
                        title="Xóa cấu hình"
                        @click="handleDeleteRule(rule)"
                      >
                        Xóa
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Card 2: Phương thức tính phí & Bảng giá theo khu vực -->
        <div class="config-card">
          <div class="card-header">
            <div>
              <h3 class="card-title">2. Phương thức tính phí &amp; Bảng phí theo khu vực địa lý</h3>
              <p class="card-desc">Áp dụng khi đơn hàng nằm ngoài các mốc khoảng cách/giá trị đặc biệt hoặc tính theo vùng địa lý</p>
            </div>
            <span class="status-pill" :class="form.cheDoTinhPhi === 'REGION_TABLE' ? 'region' : 'ghn'">
              {{ form.cheDoTinhPhi === 'REGION_TABLE' ? 'Ưu tiên Bảng giá Cửa hàng' : 'Ưu tiên API GHN + Fallback Bảng giá' }}
            </span>
          </div>

          <div class="mode-selector-grid">
            <label
              class="mode-option-card"
              :class="{ active: form.cheDoTinhPhi === 'REGION_TABLE', disabled: !isAdminUser }"
            >
              <input
                v-model="form.cheDoTinhPhi"
                type="radio"
                value="REGION_TABLE"
                :disabled="!isAdminUser"
                @change="triggerPreviewFee"
              />
              <div class="mode-option-body">
                <div class="mode-option-title">Bảng phí chuẩn của cửa hàng (Khuyên dùng)</div>
                <div class="mode-option-sub">
                  Luôn áp dụng đúng biểu phí nội thành / ngoại thành / liên tỉnh bên dưới cho cả bán tại quầy (POS) và đặt hàng Online.
                </div>
              </div>
            </label>

            <label
              class="mode-option-card"
              :class="{ active: form.cheDoTinhPhi === 'GHN_PRIORITY', disabled: !isAdminUser }"
            >
              <input
                v-model="form.cheDoTinhPhi"
                type="radio"
                value="GHN_PRIORITY"
                :disabled="!isAdminUser"
                @change="triggerPreviewFee"
              />
              <div class="mode-option-body">
                <div class="mode-option-title">Tích hợp Giao Hàng Nhanh (GHN) + Dự phòng bảng giá</div>
                <div class="mode-option-sub">
                  Gọi API GHN khi có đủ mã Quận/Huyện &amp; Phường/Xã; tự động dùng bảng giá khu vực bên dưới nếu ngoại tuyến.
                </div>
              </div>
            </label>
          </div>

          <div class="table-wrapper" style="margin-top: 16px;">
            <table class="region-rate-table">
              <thead>
                <tr>
                  <th>Khu vực áp dụng</th>
                  <th>Phạm vi nhận diện</th>
                  <th style="width: 150px;">Thời gian dự kiến</th>
                  <th style="width: 210px;">Mức phí (VNĐ)</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <div class="region-name">Nội thành Hà Nội</div>
                    <span class="region-tag tag-green">Khu vực 1</span>
                  </td>
                  <td class="region-scope">
                    Cầu Giấy, Đống Đa, Ba Đình, Hoàn Kiếm, Hai Bà Trưng, Thanh Xuân, Hoàng Mai, Long Biên, Tây Hồ, Hà Đông, Nam/Bắc Từ Liêm
                  </td>
                  <td class="muted-cell">1 - 2 ngày</td>
                  <td>
                    <div class="currency-input-wrap">
                      <input
                        v-model.number="form.phiNoiThanhHaNoi"
                        type="number"
                        min="0"
                        step="1000"
                        class="currency-input"
                        :disabled="!isAdminUser"
                        @input="triggerPreviewFee"
                      />
                      <span class="currency-suffix">đ</span>
                    </div>
                  </td>
                </tr>

                <tr>
                  <td>
                    <div class="region-name">Ngoại thành Hà Nội</div>
                    <span class="region-tag tag-blue">Khu vực 2</span>
                  </td>
                  <td class="region-scope">
                    Gia Lâm, Đông Anh, Sóc Sơn, Thanh Trì, Hoài Đức, Đan Phượng, Thường Tín, Quốc Oai, Thạch Thất, Chương Mỹ, Sơn Tây...
                  </td>
                  <td class="muted-cell">1 - 2 ngày</td>
                  <td>
                    <div class="currency-input-wrap">
                      <input
                        v-model.number="form.phiNgoaiThanhHaNoi"
                        type="number"
                        min="0"
                        step="1000"
                        class="currency-input"
                        :disabled="!isAdminUser"
                        @input="triggerPreviewFee"
                      />
                      <span class="currency-suffix">đ</span>
                    </div>
                  </td>
                </tr>

                <tr>
                  <td>
                    <div class="region-name">Tỉnh / Thành miền Bắc</div>
                    <span class="region-tag tag-amber">Khu vực 3</span>
                  </td>
                  <td class="region-scope">
                    Hải Phòng, Quảng Ninh, Bắc Ninh, Bắc Giang, Hải Dương, Hưng Yên, Vĩnh Phúc, Thái Nguyên, Nam Định, Ninh Bình, Thái Bình...
                  </td>
                  <td class="muted-cell">2 - 3 ngày</td>
                  <td>
                    <div class="currency-input-wrap">
                      <input
                        v-model.number="form.phiMienBac"
                        type="number"
                        min="0"
                        step="1000"
                        class="currency-input"
                        :disabled="!isAdminUser"
                        @input="triggerPreviewFee"
                      />
                      <span class="currency-suffix">đ</span>
                    </div>
                  </td>
                </tr>

                <tr>
                  <td>
                    <div class="region-name">Tỉnh / Thành miền Trung</div>
                    <span class="region-tag tag-purple">Khu vực 4</span>
                  </td>
                  <td class="region-scope">
                    Thanh Hóa, Nghệ An, Hà Tĩnh, Quảng Bình, Huế, Đà Nẵng, Quảng Nam, Quảng Ngãi, Bình Định, Khánh Hòa, Lâm Đồng...
                  </td>
                  <td class="muted-cell">2 - 4 ngày</td>
                  <td>
                    <div class="currency-input-wrap">
                      <input
                        v-model.number="form.phiMienTrung"
                        type="number"
                        min="0"
                        step="1000"
                        class="currency-input"
                        :disabled="!isAdminUser"
                        @input="triggerPreviewFee"
                      />
                      <span class="currency-suffix">đ</span>
                    </div>
                  </td>
                </tr>

                <tr>
                  <td>
                    <div class="region-name">Tỉnh / Thành miền Nam</div>
                    <span class="region-tag tag-purple">Khu vực 5</span>
                  </td>
                  <td class="region-scope">
                    TP. Hồ Chí Minh, Bình Dương, Đồng Nai, Bà Rịa - Vũng Tàu, Cần Thơ, Long An, Tiền Giang và các tỉnh phía Nam
                  </td>
                  <td class="muted-cell">3 - 5 ngày</td>
                  <td>
                    <div class="currency-input-wrap">
                      <input
                        v-model.number="form.phiMienNam"
                        type="number"
                        min="0"
                        step="1000"
                        class="currency-input"
                        :disabled="!isAdminUser"
                        @input="triggerPreviewFee"
                      />
                      <span class="currency-suffix">đ</span>
                    </div>
                  </td>
                </tr>

                <tr>
                  <td>
                    <div class="region-name">Mức phí mặc định toàn quốc</div>
                    <span class="region-tag tag-gray">Dự phòng</span>
                  </td>
                  <td class="region-scope">
                    Áp dụng khi khách hàng chưa chọn chi tiết tỉnh/thành hoặc địa chỉ tự do chưa xác định vùng
                  </td>
                  <td class="muted-cell">3 - 4 ngày</td>
                  <td>
                    <div class="currency-input-wrap">
                      <input
                        v-model.number="form.phiMacDinhToanQuoc"
                        type="number"
                        min="0"
                        step="1000"
                        class="currency-input"
                        :disabled="!isAdminUser"
                        @input="triggerPreviewFee"
                      />
                      <span class="currency-suffix">đ</span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Card 3: Chính sách Miễn phí vận chuyển (Freeship) -->
        <div class="config-card">
          <div class="card-header">
            <div>
              <h3 class="card-title">3. Chính sách Miễn phí vận chuyển (Freeship tự động)</h3>
              <p class="card-desc">Tự động giảm phí ship về 0đ khi tổng tiền hàng đạt hạn mức tối thiểu</p>
            </div>
            <label class="switch-toggle">
              <input
                v-model="form.apDungMienPhiShip"
                type="checkbox"
                :disabled="!isAdminUser"
                @change="triggerPreviewFee"
              />
              <span class="switch-slider"></span>
              <span class="switch-text">{{ form.apDungMienPhiShip ? 'Đang bật Freeship' : 'Đang tắt' }}</span>
            </label>
          </div>

          <div class="freeship-config-body" :class="{ 'is-disabled': !form.apDungMienPhiShip }">
            <div class="form-row-2col">
              <div class="form-group">
                <label class="form-label">Hạn mức đơn hàng tối thiểu được Freeship (VNĐ)</label>
                <div class="currency-input-wrap large">
                  <input
                    v-model.number="form.nguongMienPhiShip"
                    type="number"
                    min="0"
                    step="10000"
                    class="currency-input"
                    :disabled="!isAdminUser || !form.apDungMienPhiShip"
                    @input="triggerPreviewFee"
                  />
                  <span class="currency-suffix">VNĐ</span>
                </div>
                <span class="field-hint">
                  Ví dụ: Nhập <strong>500.000</strong> để miễn phí giao hàng cho đơn từ {{ formatCurrency(form.nguongMienPhiShip) }} trở lên.
                </span>
              </div>

              <div class="form-group">
                <label class="form-label">Kênh bán hàng áp dụng Freeship</label>
                <div class="channel-check-list">
                  <label class="channel-check-item">
                    <input
                      v-model="form.apDungFreeshipOnline"
                      type="checkbox"
                      :disabled="!isAdminUser || !form.apDungMienPhiShip"
                      @change="triggerPreviewFee"
                    />
                    <div>
                      <div class="channel-title">Cửa hàng trực tuyến (Website Online)</div>
                      <div class="channel-sub">Hiển thị thanh tiến trình Freeship ở Giỏ hàng &amp; Thanh toán Online</div>
                    </div>
                  </label>

                  <label class="channel-check-item">
                    <input
                      v-model="form.apDungFreeshipPos"
                      type="checkbox"
                      :disabled="!isAdminUser || !form.apDungMienPhiShip"
                      @change="triggerPreviewFee"
                    />
                    <div>
                      <div class="channel-title">Bán hàng tại quầy (POS - Giao hàng)</div>
                      <div class="channel-sub">Tự động miễn phí ship khi nhân viên bật "Giao hàng" cho hóa đơn đủ điều kiện</div>
                    </div>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column: Live Shipping Fee Simulator -->
      <div class="settings-side-col">
        <div class="config-card simulator-card">
          <div class="card-header">
            <div>
              <h3 class="card-title">Bộ tính thử phí ship trực tiếp</h3>
              <p class="card-desc">Kiểm tra ngay kết quả tính phí theo cấu hình hiện tại</p>
            </div>
          </div>

          <div class="sim-form">
            <div class="form-group">
              <label class="form-label">Kênh bán hàng mô phỏng</label>
              <div class="sim-channel-tabs">
                <button
                  type="button"
                  class="sim-tab-btn"
                  :class="{ active: sim.channel === 'POS' }"
                  @click="sim.channel = 'POS'; triggerPreviewFee()"
                >
                  Tại quầy (POS)
                </button>
                <button
                  type="button"
                  class="sim-tab-btn"
                  :class="{ active: sim.channel === 'ONLINE' }"
                  @click="sim.channel = 'ONLINE'; triggerPreviewFee()"
                >
                  Đặt hàng Online
                </button>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Tỉnh / Thành phố nhận hàng</label>
              <select v-model="sim.provinceName" class="sim-select" @change="onSimProvinceChange">
                <option value="Thành phố Hà Nội">Thành phố Hà Nội</option>
                <option value="Thành phố Hải Phòng">Thành phố Hải Phòng</option>
                <option value="Tỉnh Bắc Ninh">Tỉnh Bắc Ninh</option>
                <option value="Tỉnh Quảng Ninh">Tỉnh Quảng Ninh</option>
                <option value="Tỉnh Thanh Hóa">Tỉnh Thanh Hóa</option>
                <option value="Thành phố Đà Nẵng">Thành phố Đà Nẵng</option>
                <option value="Thành phố Hồ Chí Minh">Thành phố Hồ Chí Minh</option>
                <option value="Thành phố Cần Thơ">Thành phố Cần Thơ</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Quận / Huyện</label>
              <select v-if="sim.provinceName === 'Thành phố Hà Nội'" v-model="sim.districtName" class="sim-select" @change="onSimLocationChange">
                <option value="Quận Cầu Giấy">Quận Cầu Giấy (Nội thành)</option>
                <option value="Quận Đống Đa">Quận Đống Đa (Nội thành)</option>
                <option value="Quận Hoàn Kiếm">Quận Hoàn Kiếm (Nội thành)</option>
                <option value="Quận Hà Đông">Quận Hà Đông (Nội thành)</option>
                <option value="Huyện Gia Lâm">Huyện Gia Lâm (Ngoại thành)</option>
                <option value="Huyện Đông Anh">Huyện Đông Anh (Ngoại thành)</option>
                <option value="Huyện Hoài Đức">Huyện Hoài Đức (Ngoại thành)</option>
              </select>
              <input
                v-else
                v-model="sim.districtName"
                type="text"
                class="text-input-full"
                placeholder="Nhập Quận / Huyện..."
                @input="onSimLocationChange"
              />
            </div>

            <div class="form-row-2col-compact">
              <div class="form-group">
                <label class="form-label">Giá trị hóa đơn (VND)</label>
                <div class="currency-input-wrap">
                  <input
                    v-model.number="sim.orderSubtotal"
                    type="number"
                    min="0"
                    step="10000"
                    class="currency-input"
                    @input="triggerPreviewFee"
                  />
                  <span class="currency-suffix">VND</span>
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">Khoảng cách (Km)</label>
                <div class="currency-input-wrap">
                  <input
                    v-model.number="sim.distanceKm"
                    type="number"
                    min="0"
                    step="0.5"
                    class="currency-input"
                    @input="triggerPreviewFee"
                  />
                  <span class="currency-suffix">Km</span>
                </div>
              </div>
            </div>

            <div class="quick-subtotals">
              <button type="button" class="chip-btn" @click="setQuickSim(80000, 0.8)">80k / 0.8Km</button>
              <button type="button" class="chip-btn" @click="setQuickSim(250000, 3)">250k / 3Km</button>
              <button type="button" class="chip-btn" @click="setQuickSim(550000, 8)">550k / 8Km</button>
            </div>
          </div>

          <div class="sim-result-box">
            <div class="sim-result-row">
              <span>Khu vực / Quy tắc áp dụng:</span>
              <strong>{{ previewResult.regionDescription || 'Nội thành Hà Nội' }}</strong>
            </div>
            <div class="sim-result-row">
              <span>Khoảng cách tính phí:</span>
              <span>{{ previewResult.distanceKm ?? sim.distanceKm }} Km</span>
            </div>
            <div class="sim-result-row">
              <span>Thời gian giao dự kiến:</span>
              <span>{{ previewResult.leadTime || '1 - 2 ngày' }}</span>
            </div>
            <div class="sim-result-row">
              <span>Phí vận chuyển gốc:</span>
              <span :class="{ 'line-through-muted': previewResult.freeshipApplied }">
                {{ formatCurrency(previewResult.originalFee) }}
              </span>
            </div>
            <div v-if="previewResult.freeshipApplied" class="sim-freeship-alert">
              🎉 Đơn hàng được Miễn phí vận chuyển (0 VND)!
            </div>

            <div class="sim-total-fee-bar">
              <span>Phí ship áp dụng:</span>
              <span class="final-fee-number" :class="{ free: previewResult.fee === 0 }">
                {{ previewResult.fee === 0 ? '0 VND (Miễn phí)' : formatCurrency(previewResult.fee) }}
              </span>
            </div>
          </div>

          <div class="quick-nav-pos">
            <router-link to="/ban-hang" class="link-to-pos">
              <span>Chuyển sang màn Bán hàng tại quầy (POS)</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                <line x1="5" y1="12" x2="19" y2="12"></line>
                <polyline points="12 5 19 12 12 19"></polyline>
              </svg>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal: Tạo mới / Cập nhật cấu hình phí vận chuyển (Theo đúng mẫu giao diện yêu cầu) -->
    <div v-if="ruleModal.open" class="shipping-rule-modal-backdrop" @click.self="closeRuleModal">
      <div class="shipping-rule-modal-dialog" role="dialog" aria-modal="true">
        <div class="shipping-rule-modal-header">
          <h3 class="shipping-rule-modal-title">
            {{ ruleModal.isEditing ? 'Cập nhật cấu hình phí vận chuyển' : 'Tạo mới cấu hình phí vận chuyển' }}
          </h3>
          <button type="button" class="shipping-rule-modal-close" aria-label="Đóng" @click="closeRuleModal">
            &times;
          </button>
        </div>

        <div class="shipping-rule-modal-body">
          <!-- Giá trị hóa đơn -->
          <div class="modal-field-block">
            <label class="modal-field-label">Giá trị hóa đơn</label>
            <div class="split-range-box">
              <span class="split-addon">Từ</span>
              <div class="split-input-cell">
                <input
                  v-model.number="ruleModal.form.giaTriTu"
                  type="number"
                  min="0"
                  step="1000"
                  class="split-input"
                  placeholder="0"
                />
                <span class="split-unit">VND</span>
              </div>
              <span class="split-addon middle">Đến</span>
              <div class="split-input-cell">
                <input
                  v-model.number="ruleModal.form.giaTriDen"
                  type="number"
                  min="0"
                  step="1000"
                  class="split-input"
                  placeholder="100000"
                />
                <span class="split-unit">VND</span>
              </div>
            </div>
          </div>

          <!-- Khoảng cách -->
          <div class="modal-field-block">
            <label class="modal-field-label">Khoảng cách</label>
            <div class="split-range-box">
              <span class="split-addon">Từ</span>
              <div class="split-input-cell">
                <input
                  v-model.number="ruleModal.form.khoangCachTu"
                  type="number"
                  min="0"
                  step="0.5"
                  class="split-input"
                  placeholder="0"
                />
                <span class="split-unit">Km</span>
              </div>
              <span class="split-addon middle">Đến</span>
              <div class="split-input-cell">
                <input
                  v-model.number="ruleModal.form.khoangCachDen"
                  type="number"
                  min="0"
                  step="0.5"
                  class="split-input"
                  placeholder="1"
                />
                <span class="split-unit">Km</span>
              </div>
            </div>
          </div>

          <!-- Hình thức tính phí -->
          <div class="modal-field-block">
            <label class="modal-field-label">Hình thức tính phí</label>
            <div class="modal-select-wrap">
              <select
                v-model="ruleModal.form.hinhThucTinhPhi"
                class="modal-select-control"
                @change="onRuleChargeTypeChange"
              >
                <option value="TINH_PHI">Tính phí</option>
                <option value="MIEN_PHI">Miễn phí</option>
              </select>
            </div>
          </div>

          <!-- Phí vận chuyển -->
          <div class="modal-field-block">
            <label class="modal-field-label">Phí vận chuyển</label>
            <div class="single-unit-box" :class="{ disabled: ruleModal.form.hinhThucTinhPhi === 'MIEN_PHI' }">
              <input
                v-model.number="ruleModal.form.phiVanChuyen"
                type="number"
                min="0"
                step="1000"
                class="single-unit-input"
                :disabled="ruleModal.form.hinhThucTinhPhi === 'MIEN_PHI'"
                placeholder="10000"
              />
              <span class="single-unit-suffix">VND</span>
            </div>
          </div>
        </div>

        <div class="shipping-rule-modal-footer">
          <button type="button" class="btn-modal-cancel" @click="closeRuleModal">
            Hủy
          </button>
          <button
            type="button"
            class="btn-modal-confirm"
            :disabled="ruleModal.submitting"
            @click="handleConfirmRuleModal"
          >
            {{ ruleModal.submitting ? 'Đang lưu...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '@/api.js'
import { getCurrentUser } from '@/utils/auth.js'
import {
  DEFAULT_SHIPPING_CONFIG,
  getShippingConfig,
  invalidateShippingConfigCache,
  calculateLocalRegionalFee,
  estimateDistanceKm
} from '@/services/ghnService.js'

const currentUser = computed(() => getCurrentUser())
const isAdminUser = computed(() => {
  const user = currentUser.value
  if (!user) return true
  const code = String(user.maVaiTro || '').toUpperCase()
  const roleText = String(user.role || user.tenVaiTro || '').toLowerCase()
  return code === 'ADMIN' || code === 'QL' || roleText.includes('quản trị') || roleText.includes('admin')
})

const loading = ref(false)
const saving = ref(false)

const toast = reactive({
  show: false,
  message: '',
  type: 'success'
})
let toastTimer = null

function showNotice(message, type = 'success') {
  toast.message = message
  toast.type = type
  toast.show = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.show = false
  }, 3000)
}

const form = ref({
  ...DEFAULT_SHIPPING_CONFIG,
  danhSachCauHinh: [...(DEFAULT_SHIPPING_CONFIG.danhSachCauHinh || [])]
})

const sim = reactive({
  channel: 'POS',
  provinceName: 'Thành phố Hà Nội',
  districtName: 'Quận Cầu Giấy',
  orderSubtotal: 80000,
  distanceKm: 0.8
})

const previewResult = ref({
  fee: 10000,
  originalFee: 10000,
  freeshipApplied: false,
  distanceKm: 0.8,
  regionDescription: 'Cấu hình phí ship (0 - 100.000 VND | 0 - 1 Km)',
  leadTime: '1 - 3 ngày'
})

// State cho Modal Tạo mới / Cập nhật cấu hình phí vận chuyển
const ruleModal = reactive({
  open: false,
  isEditing: false,
  submitting: false,
  form: {
    id: null,
    giaTriTu: 0,
    giaTriDen: 100000,
    khoangCachTu: 0,
    khoangCachDen: 1,
    hinhThucTinhPhi: 'TINH_PHI',
    phiVanChuyen: 10000,
    trangThai: 1
  }
})

function formatCurrency(val) {
  const num = Number(val) || 0
  return num.toLocaleString('vi-VN') + ' đ'
}

function formatCurrencyNumber(val) {
  const num = Number(val) || 0
  return num.toLocaleString('vi-VN')
}

function getAuthPayload() {
  const u = currentUser.value
  return {
    idNhanVien: u?.id || null,
    maVaiTro: u?.maVaiTro || 'ADMIN',
    nguoiCapNhat: u?.tenNhanVien || u?.hoTen || u?.tenTaiKhoan || 'Quản trị viên'
  }
}

function onSimProvinceChange() {
  if (sim.provinceName === 'Thành phố Hà Nội') {
    sim.districtName = 'Quận Cầu Giấy'
  } else {
    sim.districtName = ''
  }
  sim.distanceKm = estimateDistanceKm(sim.provinceName, sim.districtName, '', '')
  triggerPreviewFee()
}

function onSimLocationChange() {
  sim.distanceKm = estimateDistanceKm(sim.provinceName, sim.districtName, '', '')
  triggerPreviewFee()
}

function setQuickSim(subtotal, km) {
  sim.orderSubtotal = subtotal
  sim.distanceKm = km
  triggerPreviewFee()
}

function triggerPreviewFee() {
  previewResult.value = calculateLocalRegionalFee(
    sim.provinceName,
    sim.districtName,
    '',
    `${sim.districtName}, ${sim.provinceName}`,
    form.value,
    sim.orderSubtotal,
    sim.channel,
    sim.distanceKm
  )
}

function handleOpenCreateRuleModal() {
  ruleModal.isEditing = false
  ruleModal.submitting = false
  ruleModal.form = {
    id: null,
    giaTriTu: 0,
    giaTriDen: 100000,
    khoangCachTu: 0,
    khoangCachDen: 1,
    hinhThucTinhPhi: 'TINH_PHI',
    phiVanChuyen: 10000,
    trangThai: 1
  }
  ruleModal.open = true
}

function handleOpenEditRuleModal(rule) {
  ruleModal.isEditing = true
  ruleModal.submitting = false
  ruleModal.form = {
    id: rule.id != null ? Number(rule.id) : null,
    giaTriTu: Number(rule.giaTriTu) || 0,
    giaTriDen: Number(rule.giaTriDen) || 100000,
    khoangCachTu: Number(rule.khoangCachTu) || 0,
    khoangCachDen: Number(rule.khoangCachDen) || 1,
    hinhThucTinhPhi: rule.hinhThucTinhPhi || 'TINH_PHI',
    phiVanChuyen: rule.hinhThucTinhPhi === 'MIEN_PHI' ? 0 : (Number(rule.phiVanChuyen) || 0),
    trangThai: rule.trangThai ?? 1
  }
  ruleModal.open = true
}

function closeRuleModal() {
  ruleModal.open = false
}

function onRuleChargeTypeChange() {
  if (ruleModal.form.hinhThucTinhPhi === 'MIEN_PHI') {
    ruleModal.form.phiVanChuyen = 0
  } else if (!ruleModal.form.phiVanChuyen) {
    ruleModal.form.phiVanChuyen = 10000
  }
}

async function handleConfirmRuleModal() {
  const giaTriTu = Math.max(0, Number(ruleModal.form.giaTriTu) || 0)
  const giaTriDen = Math.max(0, Number(ruleModal.form.giaTriDen) || 0)
  const khoangCachTu = Math.max(0, Number(ruleModal.form.khoangCachTu) || 0)
  const khoangCachDen = Math.max(0, Number(ruleModal.form.khoangCachDen) || 0)
  const hinhThucTinhPhi = ruleModal.form.hinhThucTinhPhi === 'MIEN_PHI' ? 'MIEN_PHI' : 'TINH_PHI'
  const phiVanChuyen = hinhThucTinhPhi === 'MIEN_PHI' ? 0 : Math.max(0, Number(ruleModal.form.phiVanChuyen) || 0)

  if (giaTriDen < giaTriTu) {
    showNotice('Giá trị hóa đơn "Đến" phải lớn hơn hoặc bằng giá trị "Từ".', 'warning')
    return
  }
  if (khoangCachDen < khoangCachTu) {
    showNotice('Khoảng cách "Đến" (Km) phải lớn hơn hoặc bằng khoảng cách "Từ".', 'warning')
    return
  }

  const rulePayload = {
    id: ruleModal.form.id != null ? Number(ruleModal.form.id) : null,
    giaTriTu,
    giaTriDen,
    khoangCachTu,
    khoangCachDen,
    hinhThucTinhPhi,
    phiVanChuyen,
    trangThai: 1,
    ...getAuthPayload()
  }

  ruleModal.submitting = true
  try {
    const res = await api.post('/api/cau-hinh-phi-ship/rules', rulePayload)
    const updatedCfg = res?.data?.config || res?.data
    if (updatedCfg && Array.isArray(updatedCfg.danhSachCauHinh)) {
      form.value = { ...DEFAULT_SHIPPING_CONFIG, ...updatedCfg }
    } else {
      upsertRuleLocally(rulePayload)
    }
    invalidateShippingConfigCache()
    await getShippingConfig({ forceRefresh: true })
    triggerPreviewFee()
    window.dispatchEvent(new CustomEvent('shipping-config-updated', { detail: form.value }))
    showNotice(
      ruleModal.isEditing
        ? 'Đã cập nhật cấu hình phí vận chuyển!'
        : 'Đã tạo mới cấu hình phí vận chuyển!',
      'success'
    )
    closeRuleModal()
  } catch (err) {
    upsertRuleLocally(rulePayload)
    triggerPreviewFee()
    showNotice(
      ruleModal.isEditing
        ? 'Đã cập nhật cấu hình phí vận chuyển!'
        : 'Đã tạo mới cấu hình phí vận chuyển!',
      'success'
    )
    closeRuleModal()
  } finally {
    ruleModal.submitting = false
  }
}

function upsertRuleLocally(rulePayload) {
  const list = Array.isArray(form.value.danhSachCauHinh) ? [...form.value.danhSachCauHinh] : []
  if (rulePayload.id != null) {
    const idx = list.findIndex((item) => Number(item.id) === Number(rulePayload.id))
    if (idx >= 0) {
      list[idx] = { ...rulePayload }
      form.value.danhSachCauHinh = list
      return
    }
  }
  const nextId = list.reduce((max, item) => Math.max(max, Number(item.id) || 0), 0) + 1
  list.push({ ...rulePayload, id: rulePayload.id || nextId })
  form.value.danhSachCauHinh = list
}

async function handleDeleteRule(rule) {
  if (!rule?.id) return
  try {
    const auth = getAuthPayload()
    const res = await api.delete(`/api/cau-hinh-phi-ship/rules/${encodeURIComponent(rule.id)}`, {
      params: auth
    })
    const updatedCfg = res?.data?.config || res?.data
    if (updatedCfg && Array.isArray(updatedCfg.danhSachCauHinh)) {
      form.value = { ...DEFAULT_SHIPPING_CONFIG, ...updatedCfg }
    } else {
      form.value.danhSachCauHinh = (form.value.danhSachCauHinh || []).filter((r) => Number(r.id) !== Number(rule.id))
    }
    invalidateShippingConfigCache()
    await getShippingConfig({ forceRefresh: true })
    triggerPreviewFee()
    window.dispatchEvent(new CustomEvent('shipping-config-updated', { detail: form.value }))
    showNotice('Đã xóa cấu hình phí vận chuyển.', 'success')
  } catch (err) {
    form.value.danhSachCauHinh = (form.value.danhSachCauHinh || []).filter((r) => Number(r.id) !== Number(rule.id))
    triggerPreviewFee()
    showNotice('Đã xóa cấu hình phí vận chuyển.', 'success')
  }
}

async function loadConfig() {
  loading.value = true
  try {
    const cfg = await getShippingConfig({ forceRefresh: true })
    form.value = {
      ...DEFAULT_SHIPPING_CONFIG,
      ...cfg,
      danhSachCauHinh: Array.isArray(cfg?.danhSachCauHinh)
        ? cfg.danhSachCauHinh
        : [...DEFAULT_SHIPPING_CONFIG.danhSachCauHinh]
    }
    triggerPreviewFee()
  } catch (e) {
    showNotice('Không thể tải cấu hình phí vận chuyển', 'error')
  } finally {
    loading.value = false
  }
}

async function handleSaveConfig() {
  if (!isAdminUser.value) return
  saving.value = true
  try {
    const payload = {
      ...form.value,
      ...getAuthPayload()
    }
    const res = await api.put('/api/cau-hinh-phi-ship', payload)
    const savedConfig = res.data?.config || res.data || form.value
    form.value = { ...DEFAULT_SHIPPING_CONFIG, ...savedConfig }
    invalidateShippingConfigCache()
    await getShippingConfig({ forceRefresh: true })
    triggerPreviewFee()
    window.dispatchEvent(new CustomEvent('shipping-config-updated', { detail: form.value }))
    showNotice('Đã lưu cấu hình phí vận chuyển thành công!', 'success')
  } catch (err) {
    showNotice(err?.response?.data?.message || 'Lỗi khi lưu cấu hình phí vận chuyển', 'error')
  } finally {
    saving.value = false
  }
}

async function handleResetDefault() {
  if (!isAdminUser.value) return
  saving.value = true
  try {
    const res = await api.post('/api/cau-hinh-phi-ship/reset', getAuthPayload())
    const resetCfg = res.data?.config || res.data || DEFAULT_SHIPPING_CONFIG
    form.value = { ...DEFAULT_SHIPPING_CONFIG, ...resetCfg }
    invalidateShippingConfigCache()
    await getShippingConfig({ forceRefresh: true })
    triggerPreviewFee()
    window.dispatchEvent(new CustomEvent('shipping-config-updated', { detail: form.value }))
    showNotice('Đã khôi phục bảng phí ship về mặc định!', 'success')
  } catch (err) {
    form.value = {
      ...DEFAULT_SHIPPING_CONFIG,
      danhSachCauHinh: [...DEFAULT_SHIPPING_CONFIG.danhSachCauHinh]
    }
    triggerPreviewFee()
    showNotice('Đã nạp giá trị mặc định trên biểu mẫu, nhấn Lưu cấu hình để áp dụng.', 'info')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.shipping-settings-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1440px;
  margin: 0 auto;
  position: relative;
}

.shipping-toast {
  position: fixed;
  top: 22px;
  right: 24px;
  z-index: 3000;
  padding: 12px 18px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  color: #ffffff;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.2);
}

.shipping-toast.success {
  background: #059669;
}

.shipping-toast.warning {
  background: #d97706;
}

.shipping-toast.error {
  background: #dc2626;
}

.shipping-toast.info {
  background: #2563eb;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.2s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.page-header-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 20px 24px;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.header-icon-box {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.sync-badge {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 13.5px;
  color: #64748b;
  max-width: 780px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-secondary {
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: #dbeafe;
  border-color: #93c5fd;
}

.btn-outline-reset {
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  font-weight: 600;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.btn-outline-reset:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #94a3b8;
}

.btn-primary-save {
  padding: 10px 20px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
  color: #ffffff;
  font-weight: 600;
  font-size: 13.5px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.22);
  transition: all 0.18s ease;
}

.btn-primary-save:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}

.btn-primary-save:disabled,
.btn-outline-reset:disabled,
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.readonly-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 13.5px;
}

.settings-grid {
  display: grid;
  grid-template-columns: 1fr 390px;
  gap: 20px;
  align-items: start;
}

.settings-main-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.config-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 20px 22px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.card-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}

.btn-create-rule {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  border: none;
  background: #0b66b2;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s ease;
}

.btn-create-rule:hover {
  background: #095392;
}

.status-pill {
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-pill.region {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.status-pill.ghn {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.mode-selector-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.mode-option-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1.5px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.18s ease;
  background: #f8fafc;
}

.mode-option-card.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.mode-option-card.disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.mode-option-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.mode-option-sub {
  margin-top: 4px;
  font-size: 12.5px;
  color: #475569;
  line-height: 1.45;
}

.table-wrapper {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.region-rate-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
}

.region-rate-table th {
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
  text-align: left;
  padding: 12px 14px;
  border-bottom: 1px solid #e2e8f0;
}

.region-rate-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.region-rate-table tr:last-child td {
  border-bottom: none;
}

.empty-rule-cell {
  text-align: center;
  color: #64748b;
  padding: 24px 16px !important;
}

.muted-cell {
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
}

.range-display {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  color: #1e293b;
}

.range-tag {
  display: inline-block;
  padding: 2px 7px;
  background: #f1f5f9;
  color: #475569;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.charge-type-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.charge-type-badge.paid {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.charge-type-badge.free {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.rule-fee-text {
  font-weight: 700;
  color: #0f172a;
}

.rule-fee-text.free {
  color: #059669;
}

.rule-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-rule-action {
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s ease;
}

.btn-rule-action.edit {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #bfdbfe;
}

.btn-rule-action.edit:hover {
  background: #dbeafe;
}

.btn-rule-action.delete {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fecaca;
}

.btn-rule-action.delete:hover {
  background: #fee2e2;
}

.region-name {
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.region-scope {
  color: #475569;
  font-size: 12.5px;
  line-height: 1.45;
}

.region-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
}

.tag-green {
  background: #dcfce7;
  color: #15803d;
}

.tag-blue {
  background: #dbeafe;
  color: #1d4ed8;
}

.tag-amber {
  background: #fef3c7;
  color: #b45309;
}

.tag-purple {
  background: #f3e8ff;
  color: #7e22ce;
}

.tag-gray {
  background: #f1f5f9;
  color: #475569;
}

.text-input-sm,
.text-input-full,
.sim-select {
  width: 100%;
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  font-size: 13.5px;
  color: #0f172a;
  background: #ffffff;
}

.currency-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.currency-input {
  width: 100%;
  padding: 9px 44px 9px 12px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.currency-input-wrap.large .currency-input {
  padding: 11px 52px 11px 14px;
  font-size: 15px;
}

.currency-suffix {
  position: absolute;
  right: 12px;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  pointer-events: none;
}

.switch-toggle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.switch-toggle input {
  display: none;
}

.switch-slider {
  width: 44px;
  height: 24px;
  background: #cbd5e1;
  border-radius: 999px;
  position: relative;
  transition: 0.2s;
}

.switch-slider::after {
  content: '';
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ffffff;
  position: absolute;
  top: 3px;
  left: 3px;
  transition: 0.2s;
}

.switch-toggle input:checked + .switch-slider {
  background: #10b981;
}

.switch-toggle input:checked + .switch-slider::after {
  transform: translateX(20px);
}

.switch-text {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.freeship-config-body.is-disabled {
  opacity: 0.55;
}

.form-row-2col {
  display: grid;
  grid-template-columns: 1fr 1.15fr;
  gap: 18px;
}

.form-row-2col-compact {
  display: grid;
  grid-template-columns: 1.25fr 0.9fr;
  gap: 10px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.field-hint {
  font-size: 12px;
  color: #64748b;
}

.channel-check-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.channel-check-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
}

.channel-title {
  font-size: 13.5px;
  font-weight: 600;
  color: #0f172a;
}

.channel-sub {
  font-size: 12px;
  color: #64748b;
}

.simulator-card {
  position: sticky;
  top: 16px;
}

.sim-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sim-channel-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.sim-tab-btn {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}

.sim-tab-btn.active {
  background: #1e3a8a;
  color: #ffffff;
  border-color: #1e3a8a;
}

.quick-subtotals {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip-btn {
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
}

.chip-btn:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.sim-result-box {
  margin-top: 16px;
  padding: 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sim-result-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #475569;
  gap: 8px;
}

.line-through-muted {
  text-decoration: line-through;
  color: #94a3b8;
}

.sim-freeship-alert {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #047857;
  font-size: 12.5px;
  font-weight: 600;
  padding: 8px 10px;
  border-radius: 8px;
}

.sim-total-fee-bar {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px dashed #cbd5e1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.final-fee-number {
  font-size: 16px;
  color: #1d4ed8;
}

.final-fee-number.free {
  color: #059669;
}

.quick-nav-pos {
  margin-top: 14px;
}

.link-to-pos {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #bfdbfe;
}

.link-to-pos:hover {
  background: #dbeafe;
}

/* ============================================================
   MODAL: TẠO MỚI CẤU HÌNH PHÍ VẬN CHUYỂN (THEO MẪU ẢNH)
   ============================================================ */
.shipping-rule-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.shipping-rule-modal-dialog {
  width: 100%;
  max-width: 720px;
  background: #ffffff;
  border-radius: 6px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.22);
  padding: 22px 28px 24px;
  animation: ruleModalFadeIn 0.16s ease-out;
}

@keyframes ruleModalFadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.99);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.shipping-rule-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.shipping-rule-modal-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.shipping-rule-modal-close {
  border: none;
  background: transparent;
  color: #9ca3af;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
  padding: 0 4px;
}

.shipping-rule-modal-close:hover {
  color: #374151;
}

.shipping-rule-modal-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.modal-field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.modal-field-label {
  font-size: 13.5px;
  font-weight: 700;
  color: #4b5563;
}

/* Split input group: [Từ] [ 0 ... VND ] [Đến] [ 100000 ... VND ] */
.split-range-box {
  display: grid;
  grid-template-columns: 46px 1fr 48px 1fr;
  align-items: stretch;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  background: #ffffff;
  height: 40px;
}

.split-addon {
  background: #ebebeb;
  color: #4b5563;
  font-size: 13.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #e5e7eb;
  user-select: none;
}

.split-addon.middle {
  border-left: 1px solid #e5e7eb;
}

.split-input-cell {
  display: flex;
  align-items: center;
  padding: 0 12px;
  background: #ffffff;
}

.split-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  color: #374151;
  background: transparent;
}

.split-unit {
  font-size: 13px;
  color: #9ca3af;
  margin-left: 8px;
  user-select: none;
}

.modal-select-wrap {
  position: relative;
}

.modal-select-control {
  width: 100%;
  height: 40px;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 14px;
  color: #4b5563;
  background-color: #ffffff;
  outline: none;
}

.modal-select-control:focus {
  border-color: #0b66b2;
}

.single-unit-box {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #ffffff;
}

.single-unit-box.disabled {
  background: #f3f4f6;
}

.single-unit-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  color: #374151;
  background: transparent;
}

.single-unit-suffix {
  font-size: 13px;
  color: #9ca3af;
  margin-left: 8px;
  user-select: none;
}

.shipping-rule-modal-footer {
  margin-top: 26px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.btn-modal-cancel {
  min-width: 74px;
  height: 38px;
  padding: 0 18px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #ffffff;
  color: #4b5563;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.btn-modal-cancel:hover {
  background: #f9fafb;
}

.btn-modal-confirm {
  min-width: 108px;
  height: 38px;
  padding: 0 22px;
  border: none;
  border-radius: 4px;
  background: #0b66b2;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-modal-confirm:hover:not(:disabled) {
  background: #095392;
}

.btn-modal-confirm:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 1100px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
  .mode-selector-grid,
  .form-row-2col {
    grid-template-columns: 1fr;
  }
  .split-range-box {
    grid-template-columns: 42px 1fr 44px 1fr;
  }
}
</style>
