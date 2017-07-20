package com.example.android.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract;


public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = CatalogActivity.class.getName();

    // Identifier for the product data loader
    private static final int PRODUCT_LOADER = 0;

    // Adapter for the ListView
    ProductCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the list data
        ListView productListView = (ListView) findViewById(R.id.list_product);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        // Set up adapter to create a list item for each row of data in the Cursor
        mCursorAdapter = new ProductCursorAdapter(this, null);
        productListView.setAdapter(mCursorAdapter);

        // Setup the item click listener
        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);

                // Form the content URI that represents the specific list item that was clicked on,
                // by appending the "id" onto the {@link ProductEntry#CONTENT_URI}.
                // Example => content://com.example.android.storeinventory/products/2, for product id = 2
                Uri currentProductUri = ContentUris.withAppendedId(ProductContract.ProductEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentProductUri);

                // Launch the {@link EditorActivity} to display the data for the current item
                startActivity(intent);
            }
        });

        // Kick off the loader
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
    }

    /**
     * Helper method to delete all products in the database.
     */
    private void deleteAllProducts() {
        int rowsDeleted = getContentResolver().delete(ProductContract.ProductEntry.CONTENT_URI, null, null);
        if (rowsDeleted > 0) {
            Toast.makeText(CatalogActivity.this, getString(R.string.confirm_delete_all_entries),
                    Toast.LENGTH_SHORT).show();
        } else {
            Log.e(LOG_TAG, getString(R.string.error_delete_all_entries));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllProducts();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                ProductContract.ProductEntry._ID,
                ProductContract.ProductEntry.COLUMN_PRODUCT_NAME,
                ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductContract.ProductEntry.COLUMN_SUPPLIER_NAME,
                ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL,
                ProductContract.ProductEntry.COLUMN_PRODUCT_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                ProductContract.ProductEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link ProductCursorAdapter} with this new cursor containing updated pet data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }
}



