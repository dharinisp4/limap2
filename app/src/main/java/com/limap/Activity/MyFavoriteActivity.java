package com.limap.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.limap.Adapter.CategoryAddRVAdapter;
import com.limap.BaseController;
import com.limap.BuildConfig;
import com.limap.Interface.APIService;
import com.limap.Model.APIUrl;
import com.limap.Model.SetterAllPostDetails;
import com.limap.Pref.Pref;
import com.limap.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyFavoriteActivity extends AppCompatActivity {
    private SwipeRefreshLayout swipeContainer;;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager recyclerViewlayoutManager;
    private ArrayList<SetterAllPostDetails> listing;
    private CategoryAddRVAdapter recyclerAdapter;

    //public JSONArray jsonArray;

    private boolean loadMore = true;
    private int index = 0;

    private int cnt=0;

    String category;

    private BottomNavigationView navigation1;

    // location last updated time
    private String mLastUpdateTime;
    private Double lat=0.0;
    private Double longi=0.0;
    private String pincode ="";
    // location updates interval - 10sec
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final int REQUEST_CHECK_SETTINGS = 100;

    // bunch of location related apis
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates=false;
    Toolbar toolbar;
    // for scrolls
    boolean continue_request;
    int page = 0;
    private int currentVisibleItemCount;
    private int currentFirstVisibleItem;
    private int totalItem;
    LinearLayoutManager manager ;

    private boolean shouldRefreshOnResume = false;
    private boolean isFirst = true;
    List<SetterAllPostDetails> datumList1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_add);
        //setTitle("MY FAVOURITE");
        toolbar     =   findViewById(R.id.toolbar);
        toolbar.setTitle("MY FAVOURITE");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Your code
                finish();
            }
        });
        init();
        datumList1=new ArrayList<>();
        // restore the values from saved instance state
//        restoreValuesFromBundle(savedInstanceState);
//        dexter();
//        startLocationUpdates();
        swipeContainer = findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                page=0;
                datumList1.clear();
                readAddsWithPaging(page);

            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        navigation1 = (BottomNavigationView) findViewById(R.id.navigation);
        navigation1.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        Intent c = new Intent(MyFavoriteActivity.this,MainActivity.class);
                        startActivity(c);
                        break;
                    case R.id.navigation_favorite:

                        break;
                    case R.id.navigation_sell:
                        Intent b = new Intent(MyFavoriteActivity.this, CheckPostActivity.class);
                        startActivity(b);
                        break;
                    case R.id.navigation_adds:
                        Intent a = new Intent(MyFavoriteActivity.this,MyPostAdvertisementActivity.class);
                        startActivity(a);
                        break;
                    case R.id.navigation_account:
                        Intent d = new Intent(MyFavoriteActivity.this,CheckAccountActivity.class);
                        startActivity(d);
                        break;
                }
                return false;
            }
        });
        //   navigation1.setSelectedItemId(R.id.navigation_account);
        navigation1.getMenu().findItem(R.id.navigation_favorite).setChecked(true);
        navigation1.getMenu().performIdentifierAction(R.id.navigation_favorite, 0);
        //list of history
        recyclerView = findViewById(R.id.recyclerView);
        //RecyclerView Layout
        manager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(manager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerAdapter = new CategoryAddRVAdapter(datumList1, getApplicationContext());
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                    continue_request=true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                currentFirstVisibleItem = manager.findFirstVisibleItemPosition();
                currentVisibleItemCount = manager.getChildCount();
                totalItem = manager.getItemCount();

                if (continue_request && (currentFirstVisibleItem + currentVisibleItemCount == totalItem)) {
                    continue_request = false;
                    page = page + 1;
                    readAddsWithPaging(page);

                }
            }
        });

        index = 0;

        readAddsWithPaging(page);
//        readAdds();
    }

    private void readAddsWithPaging(int pg) {
        cnt++;
        //final ProgressDialog progressDialog = new ProgressDialog(this);
        //progressDialog.setMessage("Wait...");
        //progressDialog.show();
        try {

            if (BaseController.isNetworkAvailable(getApplicationContext())) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                // set your desired log level
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                //   OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
                // add your other interceptors …
                OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
                httpClient.connectTimeout(300, TimeUnit.SECONDS);
                httpClient.readTimeout(300, TimeUnit.SECONDS);
                httpClient.writeTimeout(300, TimeUnit.SECONDS);
                // add logging as last interceptor
                httpClient.addInterceptor(logging);  // <-- this is the important line

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(APIUrl.BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(httpClient.build())
                        .build();

                APIService service = retrofit.create(APIService.class);
                Log.e("post_data", "readAdds: "+Pref.getInstance(getApplicationContext()).getUserId()+" : "+lat+" :: "+longi );
                Call<List<SetterAllPostDetails>> call = service.myFavourite(Pref.getInstance(getApplicationContext()).getUserId(),lat,longi,String.valueOf(pg));
                call.enqueue(new Callback<List<SetterAllPostDetails>>()
                {
                    @Override
                    public void onResponse(Call<List<SetterAllPostDetails>> call, Response<List<SetterAllPostDetails>> response)
                    {
                       // progressDialog.dismiss();
                        continue_request=false;
                        datumList1.addAll(response.body());
                        if(datumList1.size()>0) {
                            recyclerView.setVisibility(View.VISIBLE);
                            recyclerAdapter.notifyDataSetChanged();
                           if(swipeContainer.isRefreshing()){
                               swipeContainer.setRefreshing(false);
                           }

                        }
                        else
                        {

                                swipeContainer.setRefreshing(false);
                             //   progressDialog.dismiss();
                                recyclerView.setVisibility(View.GONE);


                        }
                    }
                    @Override
                    public void onFailure(Call<List<SetterAllPostDetails>> call, Throwable t)
                    {
                      //  progressDialog.dismiss();
                        //   Toast.makeText(getApplicationContext(),"Invalid contact number", Toast.LENGTH_LONG).show();
                    }
                });

            } else {

                swipeContainer.setRefreshing(false);
                recyclerView.setVisibility(View.GONE);
                //emptyview.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Connect to network and refresh", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            swipeContainer.setRefreshing(false);
            Log.e("ERORR", "" + e);
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    private void init()
//    {
//        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//        mSettingsClient = LocationServices.getSettingsClient(this);
//        mLocationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult)
//            {
//                super.onLocationResult(locationResult);
//                // location is received
//                mCurrentLocation = locationResult.getLastLocation();
//                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
//                updateLocationUI();
//            }
//        };
//
//        mRequestingLocationUpdates = false;
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
//        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
//        builder.addLocationRequest(mLocationRequest);
//        mLocationSettingsRequest = builder.build();
//    }  private void init() {
//        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//        mSettingsClient = LocationServices.getSettingsClient(this);
//        mLocationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                super.onLocationResult(locationResult);
//                // location is received
//                mCurrentLocation = locationResult.getLastLocation();
//                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
//                updateLocationUI();
//            }
//        };
//
//        mRequestingLocationUpdates = false;
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
//        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
//        builder.addLocationRequest(mLocationRequest);
//        mLocationSettingsRequest = builder.build();
//    }

    private void init()
    {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationUI();
            }
        };

        mRequestingLocationUpdates = false;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

    }


    /**
     * Restoring values from saved instance state
     */
//    private void restoreValuesFromBundle(Bundle savedInstanceState)
//    {
//        if (savedInstanceState != null)
//        {
//            if (savedInstanceState.containsKey("is_requesting_updates")) {
//                mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
//            }
//            if (savedInstanceState.containsKey("last_known_location")) {
//                mCurrentLocation = savedInstanceState.getParcelable("last_known_location");
//            }
//            if (savedInstanceState.containsKey("last_updated_on")) {
//                mLastUpdateTime = savedInstanceState.getString("last_updated_on");
//            }
//        }
//        updateLocationUI();
//    }
    /**
     * Update the UI displaying the location data
     * and toggling the buttons
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            lat = mCurrentLocation.getLatitude();
            longi = mCurrentLocation.getLongitude();
            List<Address> addresses = getMapAddress();
            if (addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                pincode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName();

            }
            Log.e("location", "lat--"+lat+"--long---"+longi +"---pin---"+pincode);
            Pref.getInstance(getApplicationContext()).setLocation(String.valueOf(lat),String.valueOf(longi),pincode);
            stopLocationUpdates();

        }
    }
    private List<Address> getMapAddress() {
        List<Address> addresses = new ArrayList<>();
        String zip = "";
        try {
            Geocoder geocoder = new Geocoder(MyFavoriteActivity.this, Locale.getDefault());

            addresses = geocoder.getFromLocation(lat, longi, 1);
            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            zip = addresses.get(0).getPostalCode();
            String country = addresses.get(0).getCountryName();
            Log.e("map_data_main", "getMapAddress: " + "" + address + "\n" + city + "\n" + state + "\n" + zip + "\n" + country);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return addresses;

    }
    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */

//    private void startLocationUpdates()
//    {
//        mSettingsClient
//                .checkLocationSettings(mLocationSettingsRequest)
//                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
//                    @Override
//                    public void onSuccess(LocationSettingsResponse locationSettingsResponse)
//                    {
//                        Log.i("LOC", "All location settings are satisfied.");
//                        //    Toast.makeText(getApplicationContext(), "Started location updates!", Toast.LENGTH_SHORT).show();
//                        //noinspection MissingPermission
//                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
//                        updateLocationUI();
//                    }
//                })
//                .addOnFailureListener(this, new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e)
//                    {
//                        int statusCode = ((ApiException) e).getStatusCode();
//                        switch (statusCode)
//                        {
//                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                                Log.i("LOCATION", "Location settings are not satisfied. Attempting to upgrade " +
//                                        "location settings ");
//                                try
//                                {
//                                    // Show the dialog by calling startResolutionForResult(), and check the
//                                    // result in onActivityResult().
//                                    ResolvableApiException rae = (ResolvableApiException) e;
//                                    rae.startResolutionForResult(MyFavoriteActivity.this, REQUEST_CHECK_SETTINGS);
//                                }
//                                catch (IntentSender.SendIntentException sie)
//                                {
//                                    Log.i("LOCATION", "Pending Intent unable to execute request.");
//                                }
//                                break;
//                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                                String errorMessage = "Location settings are inadequate, and cannot be fixed here. Fix in Settings.";
//                                Log.e("LOCATION", errorMessage);
//                                Toast.makeText(MyFavoriteActivity.this, errorMessage, Toast.LENGTH_LONG).show();
//                        }
//                        updateLocationUI();
//                    }
//                });
//    }
//    private void openSettings()
//    {
//        Intent intent = new Intent();
//        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
//        intent.setData(uri);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//    }
//
//    public void stopLocationUpdates()
//    {
//        // Removing location updates
//        mFusedLocationClient
//                .removeLocationUpdates(mLocationCallback)
//                .addOnCompleteListener(this, new OnCompleteListener<Void>()
//                {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task)
//                    {
//                        //  Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
    private boolean checkPermissions()
    {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
//    private  void dexter()
//    {
//        // Requesting ACCESS_FINE_LOCATION using Dexter library
//        Dexter.withActivity(this)
//                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
//                .withListener(new PermissionListener() {
//                    @Override
//                    public void onPermissionGranted(PermissionGrantedResponse response) {
//                        mRequestingLocationUpdates = true;
//                        startLocationUpdates();
//                    }
//                    @Override
//                    public void onPermissionDenied(PermissionDeniedResponse response)
//                    {
//                        if (response.isPermanentlyDenied())
//                        {
//                            // open device settings when the permission is
//                            // denied permanently
//                            openSettings();
//                        }
//                    }
//                    @Override
//                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token)
//                    {
//                        token.continuePermissionRequest();
//                    }
//                }).check();
//    }
//    @Override
//    protected void onPause()
//    {
//        super.onPause();
//        if (mRequestingLocationUpdates)
//        {
//            // pausing location updates
//            stopLocationUpdates();
//        }
//    }

    /**
     * Restoring values from saved instance state
     */
    private void restoreValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("is_requesting_updates")) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
            }
            if (savedInstanceState.containsKey("last_known_location")) {
                mCurrentLocation = savedInstanceState.getParcelable("last_known_location");
            }
            if (savedInstanceState.containsKey("last_updated_on")) {
                mLastUpdateTime = savedInstanceState.getString("last_updated_on");
            }
        }
        updateLocationUI();
    }

    /**
     * Update the UI displaying the location data
     * and toggling the buttons
     */

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */

    private void startLocationUpdates() {
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i("LOC", "All location settings are satisfied.");
                        //    Toast.makeText(getApplicationContext(), "Started location updates!", Toast.LENGTH_SHORT).show();
                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i("LOCATION", "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MyFavoriteActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i("LOCATION", "Pending Intent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be fixed here. Fix in Settings.";
                                Log.e("LOCATION", errorMessage);
                                Toast.makeText(MyFavoriteActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                        updateLocationUI();
                    }
                });
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void stopLocationUpdates() {
        // Removing location updates
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //  Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void dexter() {
        // Requesting ACCESS_FINE_LOCATION using Dexter library
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mRequestingLocationUpdates = true;
                        startLocationUpdates();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            // open device settings when the permission is
                            // denied permanently
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRequestingLocationUpdates) {
            // pausing location updates
            stopLocationUpdates();
        }
    }
    private void requestStoragePermission() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            //        Toast.makeText(getApplicationContext(), "All permissions are granted!", Toast.LENGTH_SHORT).show();
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }
    private void showSettingsDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MyFavoriteActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
                openSettings1();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void openSettings1() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }
}
