package com.anonymous.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

// Codes are grouped by area (1000 request, 1100 account, 1200 people, 1300 address, 1400 catalogue,
// 1500 warehouse, 1600 documents, 1700 operations, 1800 chatbot, 99xx security/system) and must stay unique.
@Getter
public enum ErrorCode {

    INVALID_INPUT(1000, "Dữ liệu không hợp lệ !", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_BODY(1001, "Nội dung yêu cầu không đúng định dạng !", HttpStatus.BAD_REQUEST),
    DATA_CONFLICT(1002, "Dữ liệu bị trùng hoặc đang được sử dụng !", HttpStatus.CONFLICT),
    INVALID_DATE_RANGE(1003, "Khoảng thời gian không hợp lệ (tối đa 366 ngày) !", HttpStatus.BAD_REQUEST),

    LOGIN_FAILED(1100, "Tên đăng nhập hoặc mật khẩu không đúng !", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(1101, "Tài khoản đã bị khóa !", HttpStatus.FORBIDDEN),
    USER_EXISTED(1102, "Tên đăng nhập đã tồn tại !", HttpStatus.CONFLICT),
    USER_NOT_EXIST(1103, "Tài khoản không tồn tại !", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD(1104, "Mật khẩu không đúng !", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXIST(1105, "Quyền không tồn tại !", HttpStatus.NOT_FOUND),

    POSITION_NOT_EXIST(1200, "Vị trí làm việc không tồn tại !", HttpStatus.NOT_FOUND),
    EMPLOYEE_NOT_EXIST(1201, "Nhân viên không tồn tại !", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_EXIST(1202, "Khách hàng không tồn tại !", HttpStatus.NOT_FOUND),

    CITY_NOT_EXIST(1300, "Tỉnh / thành phố không tồn tại !", HttpStatus.NOT_FOUND),
    DISTRICT_NOT_EXIST(1301, "Quận / huyện không thuộc tỉnh / thành phố đã chọn !", HttpStatus.NOT_FOUND),
    WARD_NOT_EXIST(1302, "Phường / xã không thuộc quận / huyện đã chọn !", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_EXIST(1303, "Địa chỉ không tồn tại !", HttpStatus.NOT_FOUND),

    BRAND_NOT_EXIST(1400, "Thương hiệu không tồn tại !", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_EXIST(1401, "Loại sản phẩm không tồn tại !", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_EXIST(1402, "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh !", HttpStatus.NOT_FOUND),
    UNIT_NOT_EXIST(1403, "Đơn vị tính không tồn tại !", HttpStatus.NOT_FOUND),
    PROMOTION_NOT_EXIST(1404, "Khuyến mãi không tồn tại !", HttpStatus.NOT_FOUND),
    BARCODE_EXISTED(1405, "Mã vạch đã được dùng cho sản phẩm khác !", HttpStatus.CONFLICT),

    WAREHOUSE_NOT_EXIST(1500, "Kho không tồn tại !", HttpStatus.NOT_FOUND),
    SUPPLIER_NOT_EXIST(1501, "Nhà cung cấp không tồn tại !", HttpStatus.NOT_FOUND),
    STOCK_QUANTITY_INVALID(1502, "Số lượng tồn kho không đủ !", HttpStatus.CONFLICT),

    STATUS_NOT_EXIST(1600, "Trạng thái không tồn tại !", HttpStatus.NOT_FOUND),
    INVOICE_NOT_EXIST(1601, "Hóa đơn không tồn tại !", HttpStatus.NOT_FOUND),
    INVOICE_DETAIL_NOT_EXIST(1602, "Dòng hóa đơn không tồn tại !", HttpStatus.NOT_FOUND),
    STOCK_IN_NOT_EXIST(1603, "Phiếu nhập kho không tồn tại !", HttpStatus.NOT_FOUND),
    STOCK_IN_DETAIL_NOT_EXIST(1604, "Dòng phiếu nhập không tồn tại !", HttpStatus.NOT_FOUND),
    STOCK_OUT_NOT_EXIST(1605, "Phiếu xuất kho không tồn tại !", HttpStatus.NOT_FOUND),
    STOCK_OUT_DETAIL_NOT_EXIST(1606, "Dòng phiếu xuất không tồn tại !", HttpStatus.NOT_FOUND),
    DOCUMENT_NOT_EDITABLE(1607, "Chỉ sửa được chứng từ đang chờ xử lý !", HttpStatus.CONFLICT),
    INVALID_STATUS_TRANSITION(1608, "Không thể chuyển sang trạng thái này !", HttpStatus.CONFLICT),
    DUPLICATE_PRODUCT_LINE(1609, "Mỗi sản phẩm chỉ được xuất hiện một lần trong chứng từ !", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_COMPLETED(1610, "Chứng từ đã hoàn thành, hãy hủy thay vì xóa !", HttpStatus.CONFLICT),
    DISCOUNT_EXCEEDS_TOTAL(1611, "Giảm giá lớn hơn tổng tiền hàng !", HttpStatus.BAD_REQUEST),
    AMOUNT_PAID_NOT_ENOUGH(1612, "Tiền khách đưa chưa đủ !", HttpStatus.BAD_REQUEST),
    INVOICE_NOT_COMPLETED(1613, "Chỉ trả hàng cho hóa đơn đã hoàn thành !", HttpStatus.CONFLICT),
    RETURN_QUANTITY_EXCEEDED(1614, "Số lượng trả vượt quá số lượng còn được trả !", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_IN_INVOICE(1615, "Sản phẩm không có trong hóa đơn !", HttpStatus.BAD_REQUEST),
    INVOICE_HAS_RETURNS(1616, "Hóa đơn đã có phiếu trả hàng, hãy hủy phiếu trả trước !", HttpStatus.CONFLICT),
    SALES_RETURN_NOT_EXIST(1617, "Phiếu trả hàng không tồn tại !", HttpStatus.NOT_FOUND),
    STOCK_TAKE_NOT_EXIST(1618, "Phiếu kiểm kê không tồn tại !", HttpStatus.NOT_FOUND),
    STOCK_TRANSFER_NOT_EXIST(1619, "Phiếu chuyển kho không tồn tại !", HttpStatus.NOT_FOUND),
    SAME_WAREHOUSE_TRANSFER(1620, "Kho nhận phải khác kho chuyển !", HttpStatus.BAD_REQUEST),
    EXPIRY_DATE_INVALID(1621, "Hạn sử dụng đã qua, không thể nhập kho !", HttpStatus.BAD_REQUEST),

    SHIFT_NOT_EXIST(1700, "Ca làm việc không tồn tại !", HttpStatus.BAD_REQUEST),
    CODE_SEQUENCE_NOT_EXIST(1701, "Chưa cấu hình bộ sinh mã !", HttpStatus.INTERNAL_SERVER_ERROR),
    CHECKLIST_ITEM_NOT_EXIST(1702, "Mục checklist không tồn tại !", HttpStatus.NOT_FOUND),
    CHECKLIST_ITEM_CODE_EXISTED(1703, "Mã mục checklist đã tồn tại !", HttpStatus.CONFLICT),

    CONVERSATION_NOT_EXIST(1800, "Cuộc trò chuyện không tồn tại !", HttpStatus.NOT_FOUND),
    CHAT_MESSAGE_NOT_EXIST(1801, "Tin nhắn không tồn tại !", HttpStatus.NOT_FOUND),
    CHAT_RATE_LIMITED(1802, "Bạn hỏi quá nhanh, vui lòng thử lại sau ít giây !", HttpStatus.TOO_MANY_REQUESTS),
    KB_DOCUMENT_NOT_EXIST(1803, "Tài liệu không tồn tại !", HttpStatus.NOT_FOUND),
    KB_FILE_TYPE_UNSUPPORTED(1804, "Chỉ hỗ trợ tệp .md, .txt, .pdf, .html !", HttpStatus.BAD_REQUEST),
    KB_FILE_EMPTY(1805, "Tệp không có nội dung văn bản !", HttpStatus.BAD_REQUEST),
    KB_ROLES_REQUIRED(1806, "Chọn ít nhất một vai trò được xem tài liệu !", HttpStatus.BAD_REQUEST),

    EXPIRATION_TOKEN(9990, "Phiên đăng nhập đã hết hạn !", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(9991, "Phiên đăng nhập không hợp lệ !", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(9992, "Bạn không có quyền thực hiện thao tác này !", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED(9993, "Vui lòng đăng nhập !", HttpStatus.UNAUTHORIZED),
    OPERATION_NOT_SUPPORTED(9994, "Thao tác không được hỗ trợ !", HttpStatus.BAD_REQUEST),
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống, vui lòng thử lại sau !", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

}
