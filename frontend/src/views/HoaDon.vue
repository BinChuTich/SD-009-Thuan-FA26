<template>
  <div class="pos-container">
    <!-- Tiêu đề trang -->
    <div class="pos-header">
      <div class="pos-title">
        <span class="bar-accent"></span>
        <div>
          <h2>Bán hàng tại quầy</h2>
          <p>Tạo hóa đơn trực tiếp tại cửa hàng · Nhân viên: Admin</p>
        </div>
      </div>

      <!-- Danh sách Tabs hóa đơn chờ -->
      <div class="invoice-tabs">
        <button
            v-for="(tab, index) in tabs"
            :key="tab.id"
            class="invoice-tab"
            :class="{ active: currentTab === index }"
            @click="currentTab = index"
        >
          {{ tab.name }}
          <span class="badge">{{ tab.count }}</span>
        </button>
        <button class="add-tab-btn" @click="addNewTab" title="Tạo thêm hóa đơn mới">+</button>
      </div>
    </div>

    <!-- Bố cục chính 2 cột -->
    <div class="pos-layout">
      <!-- Cột trái: Giỏ hàng + Thông tin khách & Thanh toán -->
      <div class="pos-left">
        <!-- Khối Giỏ hàng -->
        <div class="pos-card cart-card">
          <div class="card-head">
            <div>
              <h3>🛒 Giỏ hàng</h3>
              <p>Danh sách sản phẩm trong hóa đơn</p>
            </div>
            <div class="card-actions">
              <button class="icon-btn" title="Chọn sản phẩm">📋</button>
              <button class="icon-btn" title="Quét mã">📷</button>
            </div>
          </div>

          <!-- Bảng giỏ hàng -->
          <div class="cart-table-wrap">
            <table class="cart-table">
              <thead>
              <tr>
                <th style="width: 40px">#</th>
                <th>SẢN PHẨM</th>
                <th>SỐ LƯỢNG</th>
                <th>ĐƠN GIÁ</th>
                <th>THÀNH TIỀN</th>
                <th style="width: 70px; text-align: center">THAO TÁC</th>
              </tr>
              </thead>
              <tbody>
              <tr v-if="cartItems.length === 0">
                <td colspan="6" class="empty-cart">
                  <div class="empty-state">
                    <span class="empty-icon">🛒</span>
                    <p>Chưa có sản phẩm</p>
                    <small>Nhấn "Chọn sản phẩm" để thêm vào giỏ hàng</small>
                  </div>
                </td>
              </tr>
              <tr v-for="(item, idx) in cartItems" :key="item.id">
                <td>{{ idx + 1 }}</td>
                <td><b>{{ item.name }}</b></td>
                <td>{{ item.qty }}</td>
                <td>{{ item.price }}</td>
                <td><b>{{ item.total }}</b></td>
                <td style="text-align: center">
                  <button class="del-btn" @click="removeItem(idx)">🗑</button>
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div class="card-footer">
            <span>Hiển thị {{ cartItems.length ? '1-' + cartItems.length : '0' }} / {{ cartItems.length }} dữ liệu</span>
            <div class="mini-pagination">
              <button disabled>‹</button>
              <button class="active">1</button>
              <button disabled>›</button>
            </div>
          </div>
        </div>

        <!-- Khối Thông tin thanh toán & Khách hàng -->
        <div class="pos-card customer-card">
          <div class="card-head">
            <div class="title-with-bar">
              <span class="bar-accent-sm"></span>
              <div>
                <h3>Thông tin thanh toán</h3>
                <p>Thông tin khách hàng và phương thức nhận hàng</p>
              </div>
            </div>
            <button class="btn-select-user">👤 Chọn tài khoản</button>
          </div>

          <div class="form-split">
            <!-- Cột trái: Form thông tin nhận hàng -->
            <div class="form-fields">
              <div class="form-row">
                <div class="form-group">
                  <label>TÊN TÀI KHOẢN</label>
                  <input type="text" value="Khách lẻ" />
                </div>
                <div class="form-group">
                  <label>EMAIL</label>
                  <input type="text" placeholder="Nhập email" />
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>HỌ VÀ TÊN *</label>
                  <input type="text" placeholder="Nhập họ tên" />
                </div>
                <div class="form-group">
                  <label>SỐ ĐIỆN THOẠI *</label>
                  <input type="text" placeholder="Nhập số điện thoại" />
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>TỈNH/THÀNH PHỐ *</label>
                  <select>
                    <option value="">Chọn tỉnh/thành phố</option>
                    <option value="HN">Hà Nội</option>
                    <option value="HCM">TP. Hồ Chí Minh</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>QUẬN/HUYỆN *</label>
                  <select>
                    <option value="">Chọn quận/huyện</option>
                  </select>
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>PHƯỜNG/XÃ *</label>
                  <select>
                    <option value="">Chọn phường/xã</option>
                  </select>
                </div>
              </div>

              <div class="form-group">
                <label>ĐỊA CHỈ CỤ THỂ</label>
                <input type="text" placeholder="Nhập địa chỉ cụ thể" />
              </div>
            </div>

            <!-- Cột phải: Tính tiền & Nút xác nhận -->
            <div class="checkout-summary">
              <div class="summary-line">
                <span>Thành tiền</span>
                <b>0 ₫</b>
              </div>
              <div class="summary-line">
                <span>Giảm giá</span>
                <b>0 ₫</b>
              </div>

              <div class="total-line">
                <span>Tổng cộng</span>
                <strong class="total-amount">0 ₫</strong>
              </div>

              <div class="switch-row">
                <label class="toggle-switch">
                  <input type="checkbox" v-model="isShipping" />
                  <span class="slider"></span>
                </label>
                <span class="switch-label">Giao hàng</span>
              </div>

              <div class="voucher-box">
                <input type="text" placeholder="Mã voucher" />
                <button class="btn-voucher">Chọn mã giảm giá</button>
              </div>

              <textarea class="note-area" placeholder="Ghi chú..."></textarea>

              <div class="pay-methods">
                <button class="pay-opt active">💵 Tiền mặt</button>
                <button class="pay-opt">⇄ Chuyển khoản</button>
                <button class="pay-opt">💳 Cả 2</button>
              </div>

              <button class="btn-confirm-checkout">Xác nhận thanh toán</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Cột phải: Danh sách 10 đơn hàng gần nhất -->
      <div class="pos-right">
        <div class="pos-card recent-orders-card">
          <div class="recent-head">
            <h3>Đơn hàng hôm nay</h3>
            <p>10 hóa đơn gần nhất</p>
          </div>

          <div class="recent-list">
            <div
                v-for="order in recentOrders"
                :key="order.code"
                class="recent-item"
            >
              <div class="recent-info">
                <b class="order-code">{{ order.code }}</b>
                <span class="customer-name">{{ order.customer }}</span>
              </div>
              <strong class="order-total">{{ order.total }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const currentTab = ref(0)
const isShipping = ref(false)

const tabs = ref([
  { id: 1, name: 'Hóa đơn: HD_633AA2', count: 0 },
  { id: 2, name: 'Hóa đơn: HD_633AA3', count: 0 }
])

const addNewTab = () => {
  const newId = tabs.value.length + 1
  tabs.value.push({
    id: newId,
    name: `Hóa đơn: HD_633AA${newId + 1}`,
    count: 0
  })
}

const cartItems = ref([])

const recentOrders = ref([
  { code: 'HD_633AA2', customer: 'Nguyễn Minh Anh', total: '1.875.000 ₫' },
  { code: 'HD_633AA1', customer: 'Trần Quốc Bảo', total: '825.000 ₫' },
  { code: 'HD_633AA0', customer: 'Lê Hoàng Nam', total: '1.240.000 ₫' },
  { code: 'HD_632ZZ9', customer: 'Phạm Gia Hân', total: '560.000 ₫' },
  { code: 'HD_632ZZ8', customer: 'Đặng Tuấn Kiệt', total: '2.480.000 ₫' },
  { code: 'HD_632ZZ7', customer: 'Vũ Ngọc Linh', total: '375.000 ₫' },
  { code: 'HD_632ZZ6', customer: 'Nguyễn Đức Anh', total: '990.000 ₫' },
  { code: 'HD_632ZZ5', customer: 'Trần Minh Khoa', total: '1.350.000 ₫' },
  { code: 'HD_632ZZ4', customer: 'Bùi Thanh Hà', total: '450.000 ₫' },
  { code: 'HD_632ZZ3', customer: 'Mai Khánh Vy', total: '740.000 ₫' }
])
</script>