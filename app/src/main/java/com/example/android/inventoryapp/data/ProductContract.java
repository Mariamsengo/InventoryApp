package com.example.android.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by MariamNKinene on 16/07/2017.
 */

public class ProductContract {

    // Empty constructor.
    private ProductContract() {
    }

    // Content Provider Name
    public static final String CONTENT_AUTHORITY = "com.example.android.inventoryapp";

    // CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible path (appended to base content URI for possible URI's)
    public static final String PATH_PRODUCTS = "products";

    /**
     * Inner class that defines constant values for the products database table.
     * Each entry in the table represents a single product.
     */
    public static final class ProductEntry implements BaseColumns {

        /**
         * The content URI to access the product data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        /**
         * MIME type of the {@link #CONTENT_URI} for a list of items
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /**
         * MIME type of the {@link #CONTENT_URI} for a single item
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        // Name of the database table
        public static final String TABLE_NAME = "products";

        // The unique ID TYPE : Integer
        public final static String _ID = BaseColumns._ID;

        // The product name TYPE: Text
        public final static String COLUMN_PRODUCT_NAME = "name";

        // The product price TYPE: Integer
        public final static String COLUMN_PRODUCT_PRICE = "price";

        // The product quantity TYPE: Integer
        public final static String COLUMN_PRODUCT_QUANTITY = "quantity";

        // The supplier name TYPE: Text
        public final static String COLUMN_SUPPLIER_NAME = "suppliername";

        // The supplier email TYPE: Text
        public final static String COLUMN_SUPPLIER_EMAIL = "supplieremail";

        // The product Image TYPE: Blob
        public final static String COLUMN_PRODUCT_IMAGE = "image";


    }


}
