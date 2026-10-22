package com.anonymous.constant;

// Who may call what; used in @PreAuthorize so every endpoint states its rule explicitly.
// Mirrors the menu rules of the frontend (dashboard quick actions / sidebar).
public final class Permissions {

    public static final String AUTHENTICATED = "isAuthenticated()";
    public static final String ADMIN = "hasRole('ADMIN')";

    // Selling: invoices and customers
    public static final String SALES = "hasAnyRole('ADMIN','SALESMAN')";
    public static final String SALES_READ = "hasAnyRole('ADMIN','SALESMAN','ACCOUNTING_STAFF')";

    // Warehouse work: stock documents, suppliers, product catalogue
    public static final String STOCK = "hasAnyRole('ADMIN','STOREKEEPER')";
    public static final String STOCK_READ = "hasAnyRole('ADMIN','STOREKEEPER','ACCOUNTING_STAFF')";

    // Reports: management and accounting (inventory reports use STOCK_READ)
    public static final String REPORT = "hasAnyRole('ADMIN','ACCOUNTING_STAFF')";

    private Permissions() {
        throw new UnsupportedOperationException("Cannot be instantiated !");
    }

}
