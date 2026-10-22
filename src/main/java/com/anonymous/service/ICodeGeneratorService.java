package com.anonymous.service;

public interface ICodeGeneratorService {

    String SEQUENCE_EMPLOYEE = "EMPLOYEE";
    String SEQUENCE_PRODUCT = "PRODUCT";
    String SEQUENCE_INVOICE = "INVOICE";
    String SEQUENCE_STOCK_IN = "STOCK_IN";
    String SEQUENCE_STOCK_OUT = "STOCK_OUT";
    String SEQUENCE_SALES_RETURN = "SALES_RETURN";
    String SEQUENCE_STOCK_TAKE = "STOCK_TAKE";
    String SEQUENCE_STOCK_TRANSFER = "STOCK_TRANSFER";

    String next(String sequenceName);

}
