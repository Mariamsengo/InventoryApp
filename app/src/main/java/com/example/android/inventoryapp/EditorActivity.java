package com.example.android.inventoryapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by MariamNKinene on 16/07/2017.
 */

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = EditorActivity.class.getSimpleName();

    private static final int EXISTING_ITEM_LOADER = 0;

    //Content URI for existing product (and if null it's a new product)
    private Uri mCurrentProductUri;
    private Uri mImageUri;

    // Edit TextVIEWS
    private EditText mEditTextProductName;
    private EditText mEditTextProductPrice;
    private EditText mEditQuantityView;
    private EditText mEditTextSupplierName;
    private EditText mEditTextSupplierEmail;


    // Buttons
    private Button mPlusButton;
    private Button mMinusButton;
    private Button mOrderButton;

    private static final int REQUEST_CODE = 1;
    private static final int MY_PERMISSIONS_REQUEST = 2;
    private Bitmap bitmap;
    private ImageView mImageProduct;
    private ImageButton btnCaptureImage;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String CAMERA_DIR = "/dcim/";

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.myfileprovider";

    // Boolean flag that keeps track of whether the product has been edited (true) or not (false)
    private boolean mProductHasChanged = false;

    // OnTouchListener that listens for any user touches on a View, implying that they are modifying view
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        mImageProduct = (ImageView) findViewById(R.id.image_product);
        btnCaptureImage = (ImageButton) findViewById(R.id.camerabutton);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if adding a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // Check if CurrentProductUri is null (for new product) or not (for existing product)
        // Set action bar title accordingly
        // Also, for existing product, initialize a loader to fetch data from the database and
        // display the current values in the editor
        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));
            invalidateOptionsMenu();
            hideOrderButton();
            setQuantity();

        } else {
            setTitle(getString(R.string.editor_activity_title_edit_product));
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }

        // Find the data views
        initializeUIElements();

        //Plus and minus buttons
        mPlusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int quantity = Integer.parseInt(mEditQuantityView.getText().toString().trim());
                quantity++;
                mEditQuantityView.setText(Integer.toString(quantity));
            }
        });

        mMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.parseInt(mEditQuantityView.getText().toString().trim());
                if (quantity == 0) {
                    Toast.makeText(getApplicationContext(), getString(R.string.detail_quantity_negative),
                            Toast.LENGTH_SHORT).show();
                } else {
                    quantity = quantity - 1;
                    mEditQuantityView.setText(Integer.toString(quantity));
                }
            }
        });
        mOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get text for product name and price to put in email
                String nameEmail = mEditTextProductName.getText().toString().trim();

                //Create email message
                String message = getString(R.string.email_message) +
                        "\n" + nameEmail;

                //Send intent
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.email_subject));
                intent.putExtra(Intent.EXTRA_TEXT, message);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
        // Capture image button click event
        btnCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // capture picture
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    File f = createImageFile();

                    Log.d(LOG_TAG, "File: " + f.getAbsolutePath());

                    mImageUri = FileProvider.getUriForFile(
                            EditorActivity.this, FILE_PROVIDER_AUTHORITY, f);

                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY);
                        for (ResolveInfo resolveInfo : resInfoList) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            grantUriPermission(packageName, mImageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    }

                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(cameraIntent, REQUEST_CODE);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        });

    }

    private void setQuantity() {
        mEditQuantityView = (EditText) findViewById(R.id.detail_quantity_view_edit);
        String quantityText = "0";
        mEditQuantityView.setText(quantityText);
    }


    //This method initializes all UI components used in the Activity
    public void initializeUIElements() {
        // Find the data views
        mEditTextProductName = (EditText) findViewById(R.id.text_product_edit);
        mEditTextProductPrice = (EditText) findViewById(R.id.text_price_edit);
        mEditQuantityView = (EditText) findViewById(R.id.detail_quantity_view_edit);
        mEditTextSupplierName = (EditText) findViewById(R.id.text_supplier_email_edit);
        mEditTextSupplierEmail = (EditText) findViewById(R.id.text_supplier_name_edit);
        mImageProduct = (ImageView) findViewById(R.id.image_product);


        //Identify all the button views
        mPlusButton = (Button) findViewById(R.id.detail_plus_button);
        mMinusButton = (Button) findViewById(R.id.detail_minus_button);
        mOrderButton = (Button) findViewById(R.id.button_order_more);
        btnCaptureImage = (ImageButton) findViewById(R.id.camerabutton);
        btnCaptureImage.setEnabled(false);

        requestPermissions();

        // Setup OnTouchListeners
        mEditTextProductName.setOnTouchListener(mTouchListener);
        mEditTextProductPrice.setOnTouchListener(mTouchListener);
        mEditQuantityView.setOnTouchListener(mTouchListener);
        mEditTextSupplierName.setOnTouchListener(mTouchListener);
        mEditTextSupplierEmail.setOnTouchListener(mTouchListener);
        mImageProduct.setOnTouchListener(mTouchListener);
    }

    /**
     * Receiving activity result method will be called after closing the camera
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                mImageProduct.setImageBitmap(imageBitmap);
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File(Environment.getExternalStorageDirectory()
                    + CAMERA_DIR
                    + getString(R.string.app_name));

            Log.d(LOG_TAG, "Dir: " + storageDir);

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d(LOG_TAG, "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }


    /**
     * Method to add a new product to database
     */
    private void saveProduct() {
        String nameString = mEditTextProductName.getText().toString().trim();
        String priceString = mEditTextProductPrice.getText().toString().trim();
        String quantityString = mEditQuantityView.getText().toString().trim();
        String suppliernameString = mEditTextSupplierName.getText().toString().trim();
        String supplierEmailString = mEditTextSupplierEmail.getText().toString().trim();
        String imageString = mImageProduct.toString().trim();

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE, priceString);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityString);
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER_NAME, suppliernameString);
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL, supplierEmailString);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_IMAGE, mImageUri.toString() );

        bitmap = getBitmapFromUri(mImageUri);
        mImageProduct.setImageBitmap(bitmap);

        // Check if this is supposed to be a new product
        // and check if all the fields in the editor are blank
        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(nameString) || TextUtils.isEmpty(priceString) ||
                TextUtils.isEmpty(quantityString) || TextUtils.isEmpty(suppliernameString) ||
                TextUtils.isEmpty(supplierEmailString) || TextUtils.isEmpty(imageString)) {

            Toast.makeText(this, "Please, insert all required information.",
                    Toast.LENGTH_SHORT).show();


            //Since nothing was edited no need to do anything
            return;
        }


        // Determine if this is a new or existing product by checking if mCurrentProductUri is null or not
        if (mCurrentProductUri == null) {
            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(ProductContract.ProductEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.new_product_fail_message),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.new_product_success_message),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            //Otherwise if an existing lipstick update with content URI
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.update_product_fail_message),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.update_product_success_message),
                        Toast.LENGTH_SHORT).show();
            }
        }
        Log.v("EditorActivity", "id - " + mImageProduct);
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                saveProduct();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
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
                mCurrentProductUri,     // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int suppliernameColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_SUPPLIER_NAME);
            int supplieremailColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL);
            int imageColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_IMAGE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String suppliername = cursor.getString(suppliernameColumnIndex);
            String supplieremail = cursor.getString(supplieremailColumnIndex);
            String image = cursor.getString(imageColumnIndex);


            // Update the views on the screen with the values from the database
            mEditTextProductName.setText(name);
            mEditTextProductPrice.setText(String.valueOf(price));
            mEditQuantityView.setText(String.valueOf(quantity));
            mEditTextSupplierName.setText(suppliername);
            mEditTextSupplierEmail.setText(supplieremail);
            bitmap = getBitmapFromUri(mImageUri);
            mImageProduct.setImageBitmap(bitmap);


        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mEditTextProductName.setText("");
        mEditTextProductPrice.setText("");
        mEditQuantityView.setText("0");
        mEditTextSupplierName.setText("");
        mEditTextSupplierEmail.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the pet at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentPetUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }
    public void requestPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            btnCaptureImage.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    btnCaptureImage.setEnabled(true);
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void hideOrderButton() {
        Button OrderButton = (Button) findViewById(R.id.button_order_more);
        OrderButton.setVisibility(OrderButton.GONE);
    }
}