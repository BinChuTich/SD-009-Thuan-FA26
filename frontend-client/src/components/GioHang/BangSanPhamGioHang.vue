<script setup>
import { formatCurrency } from '@/utils/dinhDang.js'

const props = defineProps({
  cartItems: {
    type: Array,
    required: true
  },
  selectedItemIds: {
    type: Array,
    required: true
  },
  isAllSelected: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits([
  'toggleSelectAll',
  'update:selectedItemIds',
  'qtyChange',
  'setQty',
  'removeItem',
  'removeSelected',
  'clearAll'
])

const getStock = (item) => {
  if (item.maxStock != null) return item.maxStock
  if (item.soLuongTon != null) return item.soLuongTon
  if (item.soLuong != null) return item.soLuong
  if (item.stock != null) return item.stock
  return 0
}
</script>

<template>
  <div class="cart-table-panel">
    <div class="table-card">
      <!-- Header hàng cột -->
      <div class="cart-table-head">
        <div class="th-prod">
          <label class="checkbox-custom-label">
            <input
              type="checkbox"
              :checked="isAllSelected"
              @change="emit('toggleSelectAll')"
              class="custom-chk"
            />
            <span>TẤT CẢ SẢN PHẨM ({{ cartItems.length }})</span>
          </label>
        </div>
        <div class="th-unit">ĐƠN GIÁ</div>
        <div class="th-discount">ƯU ĐÃI</div>
        <div class="th-qty">SỐ LƯỢNG</div>
        <div class="th-sum">THÀNH TIỀN</div>
        <div class="th-action">
          <button
            v-if="selectedItemIds.length > 0"
            class="btn-remove-selected-mini"
            @click="emit('removeSelected')"
            title="Xóa các mục đã chọn"
          >
            Xóa
          </button>
        </div>
      </div>

      <!-- Danh sách từng dòng sản phẩm -->
      <div class="cart-table-rows">
        <div
          v-for="item in cartItems"
          :key="item.variantId"
          class="cart-item-row"
          :class="{ 'item-selected': selectedItemIds.includes(item.variantId) }"
        >
          <!-- Cột check + ảnh + tên + size + tag màu -->
          <div class="td-prod">
            <input
              type="checkbox"
              :value="item.variantId"
              :checked="selectedItemIds.includes(item.variantId)"
              @change="(e) => {
                const next = e.target.checked
                  ? [...selectedItemIds, item.variantId]
                  : selectedItemIds.filter(id => id !== item.variantId)
                emit('update:selectedItemIds', next)
              }"
              class="custom-chk"
            />
            <div class="item-thumb-box">
              <img
                :src="item.image || '/images/products/default_tshirt.jpg'"
                :alt="item.productName"
                class="item-img"
              />
            </div>
            <div class="item-name-col">
              <router-link :to="`/san-pham/${item.productId}`" class="name-link">
                {{ item.productName }}
              </router-link>
              <div class="item-variant-pills">
                <span class="var-pill color-pill">
                  <span class="color-dot"></span> {{ item.color }}
                </span>
                <span class="var-pill size-pill">Size: {{ item.size }}</span>
              </div>
              <span class="stock-status-tag" :class="{ 'out-of-stock': getStock(item) <= 0 }">
                <span class="stock-dot"></span>
                <template v-if="getStock(item) > 0">
                  Còn lại: {{ getStock(item) }}
                </template>
                <template v-else>
                  Hết hàng
                </template>
              </span>
            </div>
          </div>

          <!-- Cột Đơn giá -->
          <div class="td-unit">
            <span class="cur-price">{{ formatCurrency(item.discountPrice > 0 ? item.discountPrice : item.price) }}</span>
            <span v-if="item.discountPrice > 0" class="old-price">{{ formatCurrency(item.price) }}</span>
          </div>

          <!-- Cột Giảm giá tag -->
          <div class="td-discount">
            <span class="badge-tag-sale">Chính hãng</span>
          </div>

          <!-- Cột Số lượng Stepper [ - ] input [ + ] -->
          <div class="td-qty">
            <div class="stepper-wrap">
              <button
                class="step-btn"
                :disabled="item.quantity <= 1"
                @click="emit('qtyChange', item.variantId, -1, item.quantity, item.maxStock)"
                title="Giảm 1"
              >
                -
              </button>
              <input
                type="number"
                class="step-num-input"
                :value="item.quantity"
                min="1"
                :max="item.maxStock || 99"
                @change="emit('setQty', item.variantId, $event.target.value, item.maxStock)"
              />
              <button
                class="step-btn"
                :disabled="item.quantity >= (item.maxStock || 99)"
                @click="emit('qtyChange', item.variantId, 1, item.quantity, item.maxStock)"
                title="Tăng 1"
              >
                +
              </button>
            </div>
          </div>

          <!-- Cột Tổng tiền -->
          <div class="td-sum">
            <span class="item-line-total">
              {{ formatCurrency((item.discountPrice > 0 ? item.discountPrice : item.price) * item.quantity) }}
            </span>
          </div>

          <!-- Cột Nút xóa -->
          <div class="td-action">
            <button
              class="btn-trash-action"
              title="Xóa khỏi giỏ hàng"
              @click="emit('removeItem', item.variantId, item.productName)"
            >
              ✕
            </button>
          </div>
        </div>
      </div>

      <!-- Chân bảng: Tiếp tục mua sắm & Xóa hết -->
      <div class="cart-bottom-actions-row">
        <router-link to="/cua-hang" class="link-continue-shop">
          <span>&larr; Tiếp tục chọn thêm sản phẩm khác</span>
        </router-link>

        <div class="bottom-right-action-group">
          <button
            v-if="selectedItemIds.length > 0"
            class="btn-clear-selection"
            @click="emit('removeSelected')"
          >
            Xóa {{ selectedItemIds.length }} mục đã chọn
          </button>
          <button
            class="btn-clear-all"
            @click="emit('clearAll')"
            title="Xóa toàn bộ sản phẩm khỏi giỏ hàng"
          >
            🗑️ Xóa toàn bộ
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-table-panel {
  width: 100%;
}

.table-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(73, 104, 131, 0.05);
  overflow: hidden;
}

.cart-table-head {
  display: grid;
  grid-template-columns: 2.2fr 1fr 0.8fr 1.1fr 1fr 45px;
  align-items: center;
  background: #f8fafc;
  padding: 14px 20px;
  border-bottom: 2px solid #ecd8af;
  font-size: 11px;
  font-weight: 800;
  color: #496883;
  letter-spacing: 0.5px;
}

.checkbox-custom-label {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.custom-chk {
  width: 17px;
  height: 17px;
  cursor: pointer;
  accent-color: #496883;
}

.cart-item-row {
  display: grid;
  grid-template-columns: 2.2fr 1fr 0.8fr 1.1fr 1fr 45px;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.15s;
}

.cart-item-row:hover {
  background: #fbfcfe;
}

.td-prod {
  display: flex;
  align-items: center;
  gap: 14px;
}

.item-thumb-box {
  width: 64px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.item-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.name-link {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  text-decoration: none;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.35;
  margin-bottom: 6px;
}

.name-link:hover {
  color: #d2a764;
}

.item-variant-pills {
  display: flex;
  gap: 6px;
  margin-bottom: 4px;
}

.var-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
}

.stock-status-tag {
  font-size: 11.5px;
  color: #16a34a;
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 500;
}

.stock-status-tag.out-of-stock {
  color: #dc2626;
}

.stock-dot {
  width: 6px;
  height: 6px;
  background: #16a34a;
  border-radius: 50%;
  flex-shrink: 0;
}

.stock-status-tag.out-of-stock .stock-dot {
  background: #dc2626;
}

.cur-price {
  font-weight: 800;
  color: #1e293b;
  font-size: 14px;
}

.old-price {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  text-decoration: line-through;
}

.badge-tag-sale {
  display: inline-block;
  background: rgba(210, 167, 100, 0.15);
  color: #496883;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}

.stepper-wrap {
  display: inline-flex;
  align-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #ffffff;
}

.step-btn {
  width: 28px;
  height: 28px;
  background: #f8fafc;
  border: none;
  cursor: pointer;
  font-weight: 700;
  color: #334155;
}

.step-btn:hover:not(:disabled) {
  background: #e2e8f0;
}

.step-num {
  width: 32px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
}

.step-num-input {
  width: 38px;
  height: 28px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  border: none;
  background: transparent;
  color: #1e293b;
  outline: none;
  -moz-appearance: textfield;
}

.step-num-input::-webkit-outer-spin-button,
.step-num-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.bottom-right-action-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-clear-all {
  background: none;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-clear-all:hover {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fecaca;
}

.item-line-total {
  font-size: 15px;
  font-weight: 800;
  color: #e11d48;
}

.btn-trash-action {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
}

.btn-trash-action:hover {
  color: #e11d48;
}

.cart-bottom-actions-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fafbfc;
  border-top: 1px solid #f1f5f9;
}

.link-continue-shop {
  color: #496883;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.link-continue-shop:hover {
  color: #d2a764;
}

.btn-clear-selection {
  background: transparent;
  border: 1px solid #cbd5e1;
  color: #64748b;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.btn-clear-selection:hover {
  background: #fee2e2;
  border-color: #ef4444;
  color: #dc2626;
}

@media (max-width: 860px) {
  .cart-table-head {
    display: none;
  }
  .cart-item-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
</style>

