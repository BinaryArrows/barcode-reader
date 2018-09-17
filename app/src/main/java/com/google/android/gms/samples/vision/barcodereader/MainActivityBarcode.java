package com.google.android.gms.samples.vision.barcodereader;

import android.content.Intent;
import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivityBarcode extends Activity
    implements EasyPermissions.PermissionCallbacks {
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    Button retrieveButton;
    ProgressDialog mProgress;


    // PRODUCT CLASS
        //UI DECLARATION
    ListView productList;
    SearchView seachproductview;

    enum SELECTIONVAL
    {
        NOSCARTON, NOSPIECES, CARTONBARCODENUM,PCSBARCODENUM,EXPIRYREMARKS,GENERALREMARKS
    }

    enum SELECTEDWAREHOUSE
    {
        WAREHOUSE1,WAREHOUSE2,WAREHOUSE3,WAREHOUSE4,WAREHOUSE5,WAREHOUSE6,WAREHOUSE7,YISHUN_WH
    }

    enum REQUESTTYPE
    {
        PASSWORD, READACCESS, CARTONBARCODENUM,PCSBARCODENUM
    }


    SELECTIONVAL selectionval;
    REQUESTTYPE requesttype;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final int RC_BARCODE_CAPTURE = 9001;

    private static final String BUTTON_TEXT = "Call Google Sheets API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };

    List<String> itemListArr;
    List<String> ctnQuantityListArr;
    List<String> pcsQuantityListArr;
    List<String> cartonbarCodeListArr;
    List<String> piecesbarCodeListArr;
    List<String> categoryListArr;
    List<String> expdtremarksArr;
    List<String> genremarksArr;

    List<String> passwordArr;
    List<String> warehouseArr;
    List<String> userarr;
    Spinner warehouseselecspinner;


    int selectedId = 1;

    int warehousespinnerPos = 0;

    // INDI PROD LIST VIEW DIALOG WITH BUTTON
    ListView indiprodlist;
    List<String> prodlistarr;
    EditText pwdtxtDial;
    EditText usertxtDial;

   // PRODUCT DIALOG UI, GLOBAL VALUES
    Dialog productDialog;

    Dialog listProductDialog;

    //TextView categtext;
    TextView warehousetext;
    TextView prodtext;
    TextView ctnvaltext;
    TextView pcsvaltext;
    TextView barvaltext;
    TextView piecesbarvaltext;
    TextView GENERALREMARKStext;
    TextView expdatevaltext;

    Button editcrtnbtn;
    Button editpcsbtn;
    Button editcartonbarcodebtn;
    Button editpcsbarcodebtn;
    Button editexpremarksbtn;
    Button editgenrmrksbtn;


    Button doneprodvalbtn;

    Button logoutbtn;


    Dialog overlayDialog;
    //val edit dial
    Dialog valeditDlg;
    Dialog pwddlg;
    Button logdlgbtn;

    Button valSubBtn;
    Button valCancelBtn;
    TextView updateValTxt;

    ArrayAdapter<String> productListAdapter;

    String spreadsheetId = "1Qyaqt3zYva9PiVBupVJzBFxGm-yfTWeKy0mNef2pMng";
    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
     //   productItem = new ProductItem();
     //   Log.d("produasf",productItem.proname);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_bar);


        prodlistarr = new ArrayList<String>();
        prodlistarr.add("EDIT CARTON BAR CODE");
        prodlistarr.add("EDIT TOTAL NOS CARTONS");
        prodlistarr.add("EDIT PIECES BARCODE");
        prodlistarr.add("EDIT TOTAL NOS PIECES");
        prodlistarr.add("EDIT DATE OF EXPIRY");


        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Sheets API ...");
        requesttype = REQUESTTYPE.PASSWORD;
     //   setContentView(activityLayout);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        checklogin();
    }

   public void checklogin() {
       requesttype = REQUESTTYPE.PASSWORD;
       disableAnyInput();
      getResultsFromApi();


   }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
          //  mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
//                    mOutputText.setText(
//                            "This app requires Google Play Services. Please install " +
//                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
            case RC_BARCODE_CAPTURE:
            if (requestCode == RC_BARCODE_CAPTURE) {
                if (resultCode == CommonStatusCodes.SUCCESS) {
                    if (data != null) {

                        Barcode barcodeTextValue = data.getParcelableExtra(MainActivity.barcodetagname);

                        Log.d("barcodevalinmain","printbarval: " + barcodeTextValue);

                        updateValTxt = (EditText) valeditDlg.findViewById(R.id.rmksvalueupnumtxt);
                        updateValTxt.setText(barcodeTextValue.displayValue);
                    } else {

                        Log.d("bar code error in main", "No barcode captured, intent data is null");
                    }
                } else {

                }
            }
            else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivityBarcode.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<List<Object>>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<List<Object>> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         * @return List of names and majors
         * @throws IOException
         */

//        if (requesttype == REQUESTTYPE.PASSWORD)
//        String requestRange = "11";}

        private List<List<Object>> getDataFromApi() throws IOException {


            String range;// = "warehouse1!A2:F";

            if (requesttype == REQUESTTYPE.PASSWORD) {
                range = "passwords!A2:C";
            }

            else {
              range = "Barcodes!A1:B";
              Log.d("Warehouse name",range);
           }

            List<String> results = new ArrayList<String>();
            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();

            Log.d("object data is",values.toString());


            return values;
        }

        @Override
        protected void onPreExecute() {
       //     mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<List<Object>> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
         //       mOutputText.setText("No results returned.");
            } else {
                //   output.add(0, "Data retrieved using the Google Sheets API:");
                //     mOutputText.setText(TextUtils.join("\n", output));


                if (requesttype == REQUESTTYPE.PASSWORD) {

                    Log.d("", output.toString());
                    if (output == null) {
                        return;
                    } else if (output != null) {

                        passwordArr = new ArrayList<String>();
                        warehouseArr = new ArrayList<String>();
                        userarr = new ArrayList<String>();
                        Log.d("the out put", output.toString());
                        for (List col : output) {
                            //results.add(row.get(0) + ", " + row.get(1));
                            passwordArr.add(col.get(1) + "");
                            warehouseArr.add(col.get(0) + "");
                            userarr.add(col.get(2) + "");
                        }

                        pwddlg = new Dialog(MainActivityBarcode.this);
                        pwddlg.setContentView(R.layout.logindlg);
                        pwdtxtDial = pwddlg.findViewById(R.id.pwdtxtdlg);
                        logdlgbtn = pwddlg.findViewById(R.id.logdlgbtn);
                        usertxtDial =  pwddlg.findViewById(R.id.useridtxtdlg);


                        warehouseselecspinner = pwddlg.findViewById(R.id.warehousespinner);
                        ArrayAdapter<String> warehouseadapter = new ArrayAdapter<String>(MainActivityBarcode.this, android.R.layout.simple_list_item_1, warehouseArr);
                        warehouseselecspinner.setAdapter(warehouseadapter);
                        pwddlg.setCancelable(false);
                        pwddlg.show();


                        logdlgbtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                warehousespinnerPos= warehouseselecspinner.getSelectedItemPosition();
                                Log.d("warehousespinnerPos","warehousespinnerPos");
                                Log.d("correct password",passwordArr.get(warehousespinnerPos));
                                Log.d("entered password",pwdtxtDial.getText().toString());
                                String temppwd = pwdtxtDial.getText().toString();
                                String tempuserid = usertxtDial.getText().toString();
                                String tempcorpwd = passwordArr.get(warehousespinnerPos);
                                String useridstr = userarr.get(warehousespinnerPos);


                                if (tempcorpwd.equals(temppwd) && tempuserid.equals(useridstr)) {

                                    pwddlg.cancel();
                                    Toast.makeText(MainActivityBarcode.this,"LOG IN SUCCESS", Toast.LENGTH_LONG).show();
                                    requesttype = REQUESTTYPE.READACCESS;
                                    getResultsFromApi();
                                }
                                else
                                {
                                    Toast.makeText(MainActivityBarcode.this,"INVALID PASSWORD FOR THE WAREHOUSE", Toast.LENGTH_LONG).show();
                                    enableAnyInput();
                                }

                            }
                        });
                   }

                }
                else
                setData(output);
                enableAnyInput();
            }
        }
        private void setData(List<List<Object>> items) {

            if (items == null) {
                return;
            }

            itemListArr = new ArrayList<String>();
            ctnQuantityListArr = new ArrayList<String>();
            pcsQuantityListArr = new ArrayList<String>();
            categoryListArr = new ArrayList<String>();
            cartonbarCodeListArr  = new ArrayList<String>();
            piecesbarCodeListArr  = new ArrayList<String>();
            expdtremarksArr = new ArrayList<String>();
            genremarksArr =  new ArrayList<String>();

            if (items != null) {

                for (List col : items) {
                    int sizeOfTheRow = col.size();
                    itemListArr.add(returnastrvalforarr(sizeOfTheRow,0,col));
                    ctnQuantityListArr.add(returnastrvalforarr(sizeOfTheRow,0,col));
                    pcsQuantityListArr.add(returnastrvalforarr(sizeOfTheRow,0,col));
                    categoryListArr.add(returnastrvalforarr(sizeOfTheRow,0,col));
                    cartonbarCodeListArr.add(returnastrvalforarr(sizeOfTheRow,1,col));
                    piecesbarCodeListArr.add(returnastrvalforarr(sizeOfTheRow,0,col));
                    expdtremarksArr.add(returnastrvalforarr(sizeOfTheRow,0,col));
                    genremarksArr.add(returnastrvalforarr(sizeOfTheRow,0,col));
                }
                Log.d("BeforeCartonArr",cartonbarCodeListArr.size()+"");
                for (int i = 0; i <cartonbarCodeListArr.size();i++ ) {

                    Log.d("Val in cartonbarCodeListArr"+i,cartonbarCodeListArr.get(i));
                }
            }
           productList = findViewById(R.id.productList);
            productListAdapter= new ArrayAdapter<String>(MainActivityBarcode.this,android.R.layout.simple_list_item_1, itemListArr);
            productList.setAdapter(productListAdapter);

            logoutbtn = MainActivityBarcode.this.findViewById(R.id.logoutbtn);

            logoutbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                    System.exit(0);
                }
            });

            warehousetext = (TextView) findViewById(R.id.warehousenametxt);
            warehousetext.setText(warehouseArr.get(warehousespinnerPos));
            seachproductview = findViewById(R.id.seachproductid);
            seachproductview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String text) {
                    productListAdapter.getFilter().filter(text);

                    for(int i=0 ; i<productListAdapter.getCount() ; i++){
                        Object obj = productListAdapter.getItem(i);
                    }

                    return false;
                }
            });


            productList.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // TODO Auto-generated method stub

                  //  Toast.makeText(this,productListAdapter.getItem(position).toString(),Toast.LENGTH_SHORT).show();
                  //  Toast.makeText(MainActivity.this,productListAdapter.getItem(position).toString(), Toast.LENGTH_LONG).show();

                    String adapterstr = productListAdapter.getItem(position).toString();

                    selectedId = position; //the position of the selected is this
                    for(int i=0 ; i<itemListArr.size() ; i++){


                        String listStr = itemListArr.get(i);

                        if(adapterstr.equals(listStr)) {

                        //    Toast.makeText(MainActivity.this,productListAdapter.getItem(position).toString()+"is at"+ i+"", Toast.LENGTH_LONG).show();
                            selectedId = i;
                        }
                    }

                    showvaleditdialog(SELECTIONVAL.CARTONBARCODENUM);


                }
            });

        }

    public String returnastrvalforarr(int size, int pos, List itemlst) {


        if (size > pos)
            return (itemlst.get(pos) + "");
      else
        return "";


    }




        public void showvaleditdialog(SELECTIONVAL type) {

            valeditDlg = new Dialog(MainActivityBarcode.this);

            int idtoshow = R.layout.valeditdlg;
            int idsubbtn = R.id.valueupdatebtn;
            int idcancelbtn = R.id.valupcancelbtn;
            int idinputtxt = R.id.valueuptxt;
            int editabletxtid =  R.id.valueupnumtxt;

            if(type == SELECTIONVAL.CARTONBARCODENUM || type == SELECTIONVAL.PCSBARCODENUM) {
                    idtoshow = R.layout.remarkseditdlg;
                    idsubbtn = R.id.rmksvalueupdatebtn;
                    idcancelbtn = R.id.rmksvalupcancelbtn;
                    idinputtxt = R.id.rmksvalueuptxt;
                    editabletxtid =  R.id.rmksvalueupnumtxt;

            }

            valeditDlg.setContentView(idtoshow);
            TextView editingType = valeditDlg.findViewById(idinputtxt);
            editingType.setText("YOU ARE UPDATING "+type);
            valeditDlg.show();

            selectionval = type;


            valSubBtn = valeditDlg.findViewById(idsubbtn);
            valCancelBtn= valeditDlg.findViewById(idcancelbtn);
            valSubBtn.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View view) {

                     Intent intent = new Intent(MainActivityBarcode.this, MainActivity.class);
                     startActivityForResult(intent, RC_BARCODE_CAPTURE);
                 }
             });

            valCancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new MainActivityBarcode.SubmitData(mCredential).execute();
               //     disableAnyInput();
                    valeditDlg.cancel();
                }
            });
        }
        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivityBarcode.REQUEST_AUTHORIZATION);
                } else {
                  //mOutputText.setText("The following error occurred:\n"
                    //        + mLastError.getMessage());
                }
            } else {
               // mOutputText.setText("Request cancelled.");
            }
        }
    }

// COMMON PROCESSING

    public String showpreviousvalofdedittxt(SELECTIONVAL type) {

        switch(type) {
            case NOSCARTON:
                    return ctnQuantityListArr.get(selectedId);

            case NOSPIECES:
                return pcsQuantityListArr.get(selectedId);


            case CARTONBARCODENUM:
                return cartonbarCodeListArr.get(selectedId);

            case PCSBARCODENUM:
                return piecesbarCodeListArr.get(selectedId);

            case EXPIRYREMARKS:
                return expdtremarksArr.get(selectedId);

            case GENERALREMARKS:
                return genremarksArr.get(selectedId);
            default:
                return "";

        }


    }

    public void disableAnyInput(){
        Toast.makeText(getApplicationContext(), "PROCESSING", Toast.LENGTH_LONG).show();
        overlayDialog = new Dialog(MainActivityBarcode.this, android.R.style.Theme_Panel);
        overlayDialog.setCancelable(true);
        overlayDialog.show();
    }

    public void enableAnyInput(){
        Toast.makeText(getApplicationContext(), "CLICK ENABLED", Toast.LENGTH_LONG).show();
       overlayDialog.cancel();
    }

    //SUBMIT DATA TO GOOGLE SHEET
    private class SubmitData extends AsyncTask<Void, Void, Void> {

        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;

        SubmitData(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
        }



        @Override
        protected Void doInBackground(Void... voids) {
            try {
                submitData();

            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
            return null;
        }

        private void submitData() throws IOException {
         //   String spreadsheetId = "1hRJ_7LljMuf4jApUFTGeXKX-G-JZMTuwKTvYUez7gzA"; //production

            String range ="";
            String sheetrange =  warehouseArr.get(warehousespinnerPos);

            List<List<Object>> values = new ArrayList<>();

            //Where each value represents the list of objects that is to be written to a range
            //I simply want to edit a single column, so I use a single list of objects
            List<Object> data1 = new ArrayList<>();

            if(updateValTxt.getText().toString() == "")
            {
                if(selectionval == SELECTIONVAL.EXPIRYREMARKS || selectionval == SELECTIONVAL.GENERALREMARKS) {

                    updateValTxt.setText("NO REMARKS ADDED");

                }

                else {
                    updateValTxt.setText(0+"");
                }

            }

            if(selectionval == SELECTIONVAL.NOSCARTON) {
                range = sheetrange+"!B2:B";
         //       ctnQuantityListArr.set(selectedId, "CARTON UPDATED");
                   for (int i = 0; i < ctnQuantityListArr.size(); i++) {
                    if (i == selectedId) {
                        data1.add(updateValTxt.getText().toString());
                    } else {
                        data1.add(ctnQuantityListArr.get(i));
                    }
                }
            }
             else   if(selectionval == SELECTIONVAL.PCSBARCODENUM){
                range = sheetrange+"!F2:F";
                for (int i = 0; i < piecesbarCodeListArr.size(); i++) {
                    if (i == selectedId) {
                        data1.add(updateValTxt.getText().toString());
                    } else {
                        data1.add(piecesbarCodeListArr.get(i));
                        //data1.add("23");
                    }
                }
            }
            else if(selectionval == SELECTIONVAL.EXPIRYREMARKS){
                range = sheetrange+"!G2:G";
                for (int i = 0; i < expdtremarksArr.size(); i++) {
                    if (i == selectedId) {
                        //data1.add(inputQuantity.getText());
                        //data1.add(ctnvaltext.getText().toString());
                        data1.add(updateValTxt.getText().toString());
                    } else {
                        data1.add(expdtremarksArr.get(i));
                        //data1.add("23");
                    }
                }
            }
            else if(selectionval == SELECTIONVAL.GENERALREMARKS){
                range = sheetrange+"!H2:H";
                for (int i = 0; i < genremarksArr.size(); i++) {
                    if (i == selectedId) {
                        //data1.add(inputQuantity.getText());
                        //data1.add(ctnvaltext.getText().toString());
                        data1.add(updateValTxt.getText().toString());
                    } else {
                        data1.add(genremarksArr.get(i));
                        //data1.add("23");
                    }
                }
            }

             else   if(selectionval == SELECTIONVAL.CARTONBARCODENUM){
                range = "Barcodes!B1:B";
                for (int i = 0; i < cartonbarCodeListArr.size(); i++) {
                    if (i == selectedId) {
                        //data1.add(inputQuantity.getText());
                        //data1.add(ctnvaltext.getText().toString());
                        data1.add(updateValTxt.getText().toString());
                    } else {
                        data1.add(cartonbarCodeListArr.get(i));
                        //data1.add("23");
                    }
                }
                }

                else if (selectionval == SELECTIONVAL.NOSPIECES) {
                range = sheetrange+"!C2:C";
                for (int i = 0; i < pcsQuantityListArr.size(); i++) {
                    if (i == selectedId) {
                        //data1.add(inputQuantity.getText());
                        //data1.add(ctnvaltext.getText().toString());
                        data1.add(updateValTxt.getText().toString());
                    //    data1.add("PIECES SELECTED");
                    } else {
                        data1.add(pcsQuantityListArr.get(i));
                        //data1.add("23");
                    }
                }



            }

            //There are obviously more dynamic ways to do these, but you get the picture
            values.add(data1);

            Log.d("MainActivityBarcode","printing message output");
            for(Object row : data1){
                Log.d("VALUE ADDED IS",row+"");
            }

            //Create the valuerange object and set its fields
            ValueRange body = new ValueRange()
                    .setValues(values).setMajorDimension("COLUMNS");

            UpdateValuesResponse result =
                    mService.spreadsheets().values().update(spreadsheetId, range, body)
                            .setValueInputOption("USER_ENTERED")
                            .execute();

        }


        @Override
        protected void onPreExecute() {
          //  statusText.setText("Submitting Data");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
        //    super.onPostExecute(aVoid);
            mProgress.hide();
            getResultsFromApi();
            Toast.makeText(MainActivityBarcode.this,"UPDATED SUCCESSFULLY", Toast.LENGTH_LONG).show();

            if(selectionval == SELECTIONVAL.NOSCARTON) {
                ctnvaltext.setText(updateValTxt.getText().toString());

            }
            else if(selectionval == SELECTIONVAL.CARTONBARCODENUM) {
             //   barvaltext.setText(updateValTxt.getText().toString());
            }
            else if(selectionval == SELECTIONVAL.PCSBARCODENUM) {
                piecesbarvaltext.setText(updateValTxt.getText().toString());
            }
            else if(selectionval == SELECTIONVAL.NOSPIECES) {
                pcsvaltext.setText(updateValTxt.getText().toString());
            }
            else if(selectionval == SELECTIONVAL.GENERALREMARKS) {
                GENERALREMARKStext.setText(updateValTxt.getText().toString());
            }
            else if(selectionval == SELECTIONVAL.EXPIRYREMARKS) {
                expdatevaltext.setText(updateValTxt.getText().toString());
            }

            enableAnyInput();
         //   valeditDlg.cancel();

        }


        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivityBarcode.REQUEST_AUTHORIZATION);
                } else {
                   // statusText.setText("The following error occurred:\n"+ mLastError.getMessage());

                    Toast.makeText(MainActivityBarcode.this,"The following error occurred:\n"+ mLastError.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(MainActivityBarcode.this,"Got Cancelled but dunno why", Toast.LENGTH_LONG).show();
            }
        }
    }



}
