package com.anonymous.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    INVALID_KEY(1000, "Invalid key in message validation !", HttpStatus.BAD_REQUEST),
    LOGIN_FAILED(1000, "Username or password is incorrect !", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1001, "User existed !", HttpStatus.CONFLICT),
    USER_NOT_EXIST(1002, "User isn't exist !", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD(1003, "Invalid password !", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXIST(1004, "Role isn't exist !", HttpStatus.NOT_FOUND),
    POSITION_NOT_EXIST(1005, "Position isn't exist !", HttpStatus.NOT_FOUND),
    EMPLOYEE_NOT_EXIST(1006, "Employee isn't exist !", HttpStatus.NOT_FOUND),
    CITY_NOT_EXIST(1007, "City isn't exist !", HttpStatus.NOT_FOUND),
    DISTRICT_NOT_EXIST(1008, "District isn't exist !", HttpStatus.NOT_FOUND),
    WARD_NOT_EXIST(1009, "Ward isn't exist !", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_EXIST(1010, "Address isn't exist !", HttpStatus.NOT_FOUND),
    BRAND_NOT_EXIST(1011, "Brand isn't exist !", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_EXIST(1012, "Category isn't exist !", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_EXIST(1013, "Product isn't exist !", HttpStatus.NOT_FOUND),
    UNIT_NOT_EXIST(1014, "Unit isn't exist !", HttpStatus.NOT_FOUND),
    PROMOTION_NOT_EXIST(1015, "Promotion isn't exist !", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_EXIST(1016, "Customer isn't exist !", HttpStatus.NOT_FOUND),
    STATUS_NOT_EXIST(1017, "Status isn't exist !", HttpStatus.NOT_FOUND),
    WAREHOUSE_NOT_EXIST(1018, "Warehouse isn't exist !", HttpStatus.NOT_FOUND),
    SUPPLIER_NOT_EXIST(1019, "Supplier isn't exist !", HttpStatus.NOT_FOUND),
    STOCK_IN_DETAIL_NOT_EXIST(1020, "StockIn Detail isn't exist !", HttpStatus.NOT_FOUND),
    CAN_NOT_DELETE_PRODUCT(1021, "You can't delete a product because it already has orders!", HttpStatus.BAD_REQUEST),
    STOCK_QUANTITY_INVALID(1021, "Quantity sold is greater than quantity in stock !", HttpStatus.BAD_REQUEST),
    WAREHOUSE_PRODUCT_NOT_EXIST(1022, "WarehouseProduct isn't exist !", HttpStatus.NOT_FOUND),
    STOCK_IN_NOT_EXIST(1023, "StockIn isn't exist !", HttpStatus.NOT_FOUND),
    INVOICE_NOT_EXIST(1024, "Invoice isn't exist !", HttpStatus.NOT_FOUND),
    INVOICE_DETAIL_NOT_EXIST(1025, "InvoiceDetail isn't exist !", HttpStatus.NOT_FOUND),
    STOCK_OUT_NOT_EXIST(1026, "StockOut isn't exist !", HttpStatus.NOT_FOUND),
    STOCK_OUT_DETAIL_NOT_EXIST(1027, "StockOutDetail isn't exist !", HttpStatus.NOT_FOUND),
    CAN_NOT_DELETE(1028, "You can't delete !", HttpStatus.BAD_REQUEST),
    EXPIRATION_TOKEN(9990, "Token expired !", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(9991, "Token invalid !", HttpStatus.REQUEST_TIMEOUT),
    UNAUTHORIZED(9992, "User is not permitted !", HttpStatus.UNAUTHORIZED),
    UNAUTHENTICATED(9993, "Unauthenticated error !", HttpStatus.UNAUTHORIZED),
    OPERATION_NOT_SUPPORTED(9994, "Operation not supported !", HttpStatus.BAD_REQUEST),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error !", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

}
