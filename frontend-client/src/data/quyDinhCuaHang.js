export const storeRules = [
  {
    slug: 'dat-hang-va-san-pham', icon: '🛍️', title: 'Đặt hàng và sản phẩm',
    summary: 'Cách chọn đúng biến thể, kiểm tra tồn kho và xác nhận đơn hàng.',
    items: [
      'Khách hàng cần kiểm tra đúng sản phẩm, màu sắc, kích cỡ và số lượng trước khi xác nhận đặt hàng.',
      'Số lượng tồn kho được cập nhật theo từng biến thể; đơn chỉ được ghi nhận khi hệ thống thông báo đặt hàng thành công.',
      'Thông tin tư vấn chọn size mang tính tham khảo; khách hàng nên đối chiếu form dáng và nhu cầu mặc thực tế.'
    ],
    sections: [
      { title: 'Lựa chọn sản phẩm', content: 'Mỗi sản phẩm có thể có nhiều biến thể màu sắc và kích cỡ với số lượng tồn kho riêng. Khách hàng cần chọn đúng biến thể trước khi thêm vào giỏ hàng.' },
      { title: 'Kiểm tra trước khi đặt', content: 'Vui lòng kiểm tra tên sản phẩm, phân loại, số lượng, đơn giá, ưu đãi và thông tin người nhận tại bước xác nhận cuối cùng.' },
      { title: 'Tư vấn kích cỡ', content: 'Gợi ý kích cỡ được xây dựng từ chiều cao, cân nặng và kiểu mặc mong muốn. Đây là thông tin tham khảo; độ vừa vặn thực tế còn phụ thuộc form áo và vóc dáng.' },
      { title: 'Xác nhận đơn hàng', content: 'Việc thêm sản phẩm vào giỏ chưa đồng nghĩa đơn hàng đã được tạo. Đơn chỉ hợp lệ sau khi hệ thống trả về thông báo đặt hàng thành công và cấp mã đơn.' }
    ]
  },
  {
    slug: 'gia-ban-va-uu-dai', icon: '🏷️', title: 'Giá bán và ưu đãi',
    summary: 'Quy tắc áp dụng giá, đợt giảm giá và phiếu giảm giá.',
    items: ['Giá chính thức là giá tại bước xác nhận đơn.', 'Ưu đãi chỉ áp dụng khi còn hiệu lực và đủ điều kiện.', 'Phiếu giảm giá không được quy đổi thành tiền mặt.'],
    sections: [
      { title: 'Giá thanh toán', content: 'Giá bán có thể thay đổi theo từng thời điểm. Mức giá chính thức của đơn là mức hiển thị tại bước xác nhận thanh toán.' },
      { title: 'Đợt giảm giá', content: 'Giá khuyến mãi chỉ áp dụng cho đúng sản phẩm hoặc biến thể thuộc chương trình và trong thời gian chương trình còn hiệu lực.' },
      { title: 'Phiếu giảm giá', content: 'Phiếu giảm giá cần còn lượt sử dụng, đúng đối tượng và đáp ứng giá trị đơn tối thiểu. Mức giảm tối đa được tính theo điều kiện của từng phiếu.' },
      { title: 'Thay đổi ưu đãi', content: 'Khi chương trình hoặc phiếu giảm giá thay đổi trước lúc xác nhận đơn, hệ thống sẽ tính lại và hiển thị mức thanh toán mới để khách hàng kiểm tra.' }
    ]
  },
  {
    slug: 'thanh-toan', icon: '💳', title: 'Thanh toán',
    summary: 'Các phương thức thanh toán và thời hạn hoàn tất giao dịch.',
    items: ['Chọn phương thức đang hiển thị tại bước thanh toán.', 'COD được thanh toán khi nhận hàng.', 'VNPAY chưa thanh toán sau 5 phút có thể tự động bị hủy.'],
    sections: [
      { title: 'Phương thức hỗ trợ', content: 'Khách hàng có thể chọn phương thức thanh toán đang được hệ thống cung cấp tại thời điểm đặt hàng, bao gồm COD hoặc các hình thức trực tuyến phù hợp.' },
      { title: 'Thanh toán COD', content: 'Tiền hàng và các khoản phí hiển thị trên đơn được thanh toán cho đơn vị vận chuyển khi giao hàng thành công.' },
      { title: 'Thanh toán trực tuyến', content: 'Khách hàng cần thực hiện đúng nội dung và số tiền do hệ thống cung cấp, sau đó chờ hệ thống xác nhận trạng thái giao dịch.' },
      { title: 'Thời hạn VNPAY', content: 'Đơn VNPAY chưa hoàn tất thanh toán trong 5 phút có thể được tự động hủy và số lượng sản phẩm sẽ được hoàn lại kho.' }
    ]
  },
  {
    slug: 'dia-chi-va-phi-giao-hang', icon: '📍', title: 'Địa chỉ và phí giao hàng',
    summary: 'Cấu trúc địa chỉ 2 cấp, thông tin người nhận và cách tính phí.',
    items: ['Địa chỉ dùng cấu trúc Tỉnh/Thành phố → Phường/Xã.', 'Cần cung cấp đúng người nhận và số điện thoại.', 'Phí vận chuyển được hiển thị trước khi đặt hàng.'],
    sections: [
      { title: 'Địa chỉ 2 cấp', content: 'Địa chỉ giao hàng gồm Tỉnh/Thành phố, Phường/Xã và phần địa chỉ cụ thể như số nhà, ngõ hoặc tên đường.' },
      { title: 'Thông tin người nhận', content: 'Tên người nhận và số điện thoại phải chính xác, đang sử dụng và có thể liên lạc trong thời gian giao hàng.' },
      { title: 'Phí vận chuyển', content: 'Phí được tính theo địa chỉ nhận hàng và cấu hình vận chuyển của cửa hàng, sau đó hiển thị trước khi khách xác nhận đơn.' },
      { title: 'Sổ địa chỉ', content: 'Khách hàng đăng nhập có thể lưu nhiều địa chỉ, chỉnh sửa, xóa và chọn một địa chỉ mặc định để sử dụng nhanh khi mua hàng.' }
    ]
  },
  {
    slug: 'thay-doi-va-huy-don', icon: '📝', title: 'Thay đổi và hủy đơn',
    summary: 'Điều kiện sửa thông tin nhận hàng và hủy đơn trực tuyến.',
    items: ['Chỉ sửa thông tin nhận hàng 1 lần khi Chờ xác nhận.', 'Đơn trả trước chỉ đổi sang địa chỉ có phí không cao hơn.', 'Chỉ tự hủy đơn khi đơn đang Chờ xác nhận.'],
    sections: [
      { title: 'Thay đổi thông tin', content: 'Khách hàng được thay đổi thông tin nhận hàng duy nhất 1 lần khi đơn còn ở trạng thái Chờ xác nhận.' },
      { title: 'Đơn đã thanh toán trước', content: 'Đơn đã thanh toán chỉ được đổi sang địa chỉ có phí vận chuyển bằng hoặc thấp hơn mức phí đã thanh toán.' },
      { title: 'Hủy đơn', content: 'Khách hàng chỉ có thể tự hủy đơn trên website khi đơn đang Chờ xác nhận. Khi đơn đã chuyển trạng thái, vui lòng liên hệ cửa hàng.' },
      { title: 'Hoàn tiền', content: 'Nếu đơn trả trước được chấp nhận hủy hoặc bị hủy do không đủ hàng, cửa hàng ghi nhận quy trình hoàn tiền theo thông tin giao dịch của đơn.' }
    ]
  },
  {
    slug: 'giao-nhan-va-ho-tro', icon: '🚚', title: 'Giao nhận và hỗ trợ',
    summary: 'Theo dõi vận chuyển, giao lại và cách yêu cầu cửa hàng hỗ trợ.',
    items: ['Giữ điện thoại liên lạc khi đơn đang vận chuyển.', 'Hỗ trợ giao lại tối đa 3 lần.', 'Cung cấp mã đơn khi cần hỗ trợ.'],
    sections: [
      { title: 'Theo dõi đơn', content: 'Khách hàng có thể theo dõi trạng thái tại mục Đơn hàng hoặc Tra cứu đơn hàng bằng thông tin được hệ thống yêu cầu.' },
      { title: 'Nhận hàng', content: 'Vui lòng giữ điện thoại liên lạc và kiểm tra thông tin đơn khi nhân viên vận chuyển giao hàng.' },
      { title: 'Giao lại', content: 'Đơn giao không thành công được hỗ trợ giao lại tối đa 3 lần. Sau 3 lần thất bại, đơn có thể bị hủy theo quy trình hệ thống.' },
      { title: 'Yêu cầu hỗ trợ', content: 'Khi có vấn đề, khách hàng nên cung cấp mã đơn, số điện thoại đặt hàng và mô tả rõ nội dung để cửa hàng kiểm tra nhanh chóng.' }
    ]
  }
]

export const findStoreRule = slug => storeRules.find(rule => rule.slug === slug)
