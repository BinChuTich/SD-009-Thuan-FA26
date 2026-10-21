<script setup>
import { ref } from 'vue'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const activeTab = ref('desc')
</script>

<template>
  <div class="product-tabs-section">
    <div class="tabs-header-bar">
      <button
        class="tab-title-btn"
        :class="{ active: activeTab === 'desc' }"
        @click="activeTab = 'desc'"
      >
        Mô Tả Sản Phẩm
      </button>
      <button
        class="tab-title-btn"
        :class="{ active: activeTab === 'specs' }"
        @click="activeTab = 'specs'"
      >
        Thông Số Kỹ Thuật
      </button>
      <button
        class="tab-title-btn"
        :class="{ active: activeTab === 'policy' }"
        @click="activeTab = 'policy'"
      >
        Chính Sách Đổi Trả &amp; Bảo Hành
      </button>
    </div>

    <div class="tab-body-pane">
      <!-- 1. Mô tả chi tiết -->
      <div v-if="activeTab === 'desc'" class="pane-content">
        <h3>Đặc Điểm &amp; Chất Liệu Cao Cấp</h3>
        <p>{{ product.moTa || 'Sản phẩm áo phông nam cao cấp FF T-Shirt được may từ 100% sợi cotton tự nhiên định lượng 230 - 260gsm, mang lại form áo đứng cá tính mà vẫn mềm mịn, thông thoáng suốt ngày dài.' }}</p>
        <ul class="bullet-list">
          <li>Thấm hút mồ hôi tối đa, thoáng mát thích hợp mọi thời tiết 4 mùa.</li>
          <li>Bo cổ dệt chống bai gião, đường may 2 kim chần đè tỉ mỉ.</li>
          <li>Form dáng Regular-Fit &amp; Oversize chuẩn tỉ lệ cơ thể người Việt.</li>
        </ul>
      </div>

      <!-- 2. Thông số kỹ thuật -->
      <div v-if="activeTab === 'specs'" class="pane-content">
        <table class="specs-table-simple">
          <tbody>
            <tr>
              <td>Chất liệu sợi</td>
              <td>{{ product.tenChatLieu || '100% Cotton 2 chiều' }}</td>
            </tr>
            <tr>
              <td>Kiểu dáng</td>
              <td>{{ product.tenDanhMuc || 'Áo Phông Nam Basic' }}</td>
            </tr>
            <tr>
              <td>Kiểu cổ áo</td>
              <td>{{ product.tenCoAo || 'Cổ tròn co giãn hồi phục' }}</td>
            </tr>
            <tr>
              <td>Kiểu tay áo</td>
              <td>{{ product.tenTayAo || 'Tay ngắn viền may đôi' }}</td>
            </tr>
            <tr>
              <td>Họa tiết</td>
              <td>{{ product.tenHoaTiet || 'Trơn tối giản / In sắc nét' }}</td>
            </tr>
            <tr>
              <td>Xuất xứ</td>
              <td>{{ product.tenXuatXu || 'Việt Nam' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 3. Chính sách đổi trả -->
      <div v-if="activeTab === 'policy'" class="pane-content">
        <h3>Cam Kết Dịch Vụ Khách Hàng FF T-Shirt</h3>
        <ul class="policy-list">
          <li><strong>Đổi hàng miễn phí trong 7 ngày:</strong> Đổi size, đổi màu tận nơi nếu không vừa vặn.</li>
          <li><strong>Kiểm tra hàng trước khi thanh toán:</strong> Bạn hoàn toàn an tâm thử chất vải đúng mô tả mới nhận.</li>
          <li><strong>Bảo hành đường may 6 tháng:</strong> Cam kết hoàn tiền hoặc đổi mới nếu có lỗi từ nhà sản xuất.</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-tabs-section {
  margin-top: 50px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(73, 104, 131, 0.04);
}

.tabs-header-bar {
  display: flex;
  background: #f8fafc;
  border-bottom: 2px solid #ecd8af;
  overflow-x: auto;
}

.tab-title-btn {
  padding: 16px 26px;
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 14px;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.tab-title-btn:hover {
  color: #496883;
}

.tab-title-btn.active {
  color: #496883;
  border-bottom-color: #d2a764;
  background: #ffffff;
}

.tab-body-pane {
  padding: 30px;
}

.pane-content h3 {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 14px;
}

.pane-content p {
  color: #475569;
  line-height: 1.7;
  font-size: 14px;
  margin-bottom: 14px;
}

.bullet-list,
.policy-list {
  padding-left: 20px;
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
}

.policy-list li {
  margin-bottom: 10px;
}

.policy-list strong {
  color: #1e293b;
}

.specs-table-simple {
  width: 100%;
  max-width: 600px;
  border-collapse: collapse;
}

.specs-table-simple td {
  padding: 10px 14px;
  font-size: 14px;
  border-bottom: 1px solid #f1f5f9;
}

.specs-table-simple tr td:first-child {
  width: 35%;
  font-weight: 700;
  color: #64748b;
  background: #f8fafc;
}

.specs-table-simple tr td:last-child {
  color: #1e293b;
}
</style>

