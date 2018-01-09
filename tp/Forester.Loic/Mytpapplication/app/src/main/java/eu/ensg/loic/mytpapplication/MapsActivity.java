package eu.ensg.loic.mytpapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static eu.ensg.loic.mytpapplication.R.id.map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_ACCESS_COARSE_LOCATION = 0; /*> Id to identity ACCESS_COARSE_LOCATION permission request */

    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private String mProvider;
    private Location mLocation;
    private Criteria mCriteria;
    private boolean mSpy;
    private Polygon mPolygon;
    private PolygonOptions mPolygonOptions;
    private boolean mPolygonSetup;
    private android.location.LocationListener mLocationListened;


    /**
     * Generate the view and its default configuration
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Default configuration
        defaultSettings();
        defaultPermissions();
        buttonsSettings();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Set defaults settings
     */
    private void defaultSettings() {
        mSpy = false;
        mPolygonSetup = false;
    }

    /**
     * Set permissions
     */
    private void defaultPermissions() {
        if (!mayRequestLocation()) {
            return;
        }
        Log.d("Permissions", "ACCESS_COARSE_LOCATION ok");
        startLocationServices();
    }


    /**
     * Replace mLocation when new location is changed.
     */
    private void setLocationListener() {
        this.mLocationListened = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d("Location", "new location detected");
                mLocation = location;
                if (mSpy){
                    marketPositionToPolygon(5);
                    changeMarketPosition(5);
                }
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {}
        };
    }

    /**
     * Add a marker to the map if accuracy is less than a specific value.
     */
    private void changeMarketPosition(double minAccuracy){
        if (mLocation.getAccuracy() < minAccuracy) {
            LatLng marker = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(marker)
                    .title(
                            "Accuracy : " + String.valueOf(mLocation.getAccuracy()) +
                                    "\nTime : " + String.valueOf(mLocation.getTime())
                    ));
        }
    }

    /**
     * Draw a polygon on the map. Add a new point if accuracy is less than a specific value.
     */
    private void marketPositionToPolygon(double minAccuracy){
        if (mPolygonSetup){
            if (mPolygon != null){
                mPolygon.remove();
            }
            if (mLocation.getAccuracy() < minAccuracy) {
                LatLng marker = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                mPolygonOptions.add(marker);
            }
            mPolygon = mMap.addPolygon(mPolygonOptions);
        } else {
            configurePolygon();
        }
    }

    /**
     * Configure style option for the polygon.
     */
    private void configurePolygon(){
        mPolygonOptions = new PolygonOptions();
        mPolygonOptions.fillColor(Color.BLUE);
        mPolygonSetup = true;
    }

    /**
     * Toggle the spy option and display information to user
     */
    private void recording(){
        mSpy = !mSpy;
        displayToast("Draw sector : "+String.valueOf(mSpy));

        View layoutAddSector = findViewById(R.id.layoutAddSector);
        if(mSpy){
            layoutAddSector.setVisibility(layoutAddSector.VISIBLE);
        } else {
            layoutAddSector.setVisibility(layoutAddSector.GONE);
        }
    }

    /**
     * Display a toast with the content the string choice value.
     * @param choice : string to display
     */
    private void displayToast(String choice){
        Toast toast = Toast.makeText(getActivity(), choice, 3);
        toast.show();
    }

    /**
     * Add a marker to the map if accuracy is less than a specific value. Or ask confirmation to the user.
     */
    private void addPoi(double minAccuracy){
        if (mLocation.getAccuracy() < minAccuracy){
            LatLng marker = new LatLng( mLocation.getLatitude(), mLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(marker)
                    .title(
                            "Accuracy : "+String.valueOf(mLocation.getAccuracy())+
                                    "\nTime : "+ String.valueOf(mLocation.getTime())
                    ));
            displayToast(getString(R.string.addPOI));
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmation")
                    .setMessage(
                            "La précision de votre position est supérieure à 5 mètres. \n \n " +
                            "Position actuelle : "+String.valueOf(mLocation.getAccuracy())+" m. \n \n " +
                            "Ajouter quand même ?"
                    )
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LatLng marker = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(marker)
                                    .title(
                                            "Accuracy : "+String.valueOf(mLocation.getAccuracy())+"\n" +
                                                    "Time : "+ String.valueOf(mLocation.getTime())
                                    ));
                            displayToast(getString(R.string.addPOI));
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    /**
     * Start location callback when permissions are granted
     */
    private void startLocationListener(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            defaultPermissions();
            return;
        }
        this.mLocationManager.requestLocationUpdates(this.mProvider, 10,1, this.mLocationListened);
        mLocation = this.mLocationManager.getLastKnownLocation(mProvider);
        Log.d("Location", "Location service started");
    }

    /**
     * Start location services
     */
    private void startLocationServices() {
        this.mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        mCriteria = new Criteria();
        mProvider = this.mLocationManager.getBestProvider(mCriteria, true);

        setLocationListener();
        startLocationListener();
    }

    /**
     * return true when permission is granted
     */
    private boolean mayRequestLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                defaultSettings();
            }
        }
    }

    /**
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    /**
     * Get the current activity
     */
    public Context getActivity() {
        return this;
    }


    /**
     * Add a menu to this activity
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maps,menu);
        return true;
    }

    /**
     * Map the corresponding function with the item clicked in the menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addPOI:
                Log.d("Menu", "AddPOI button clicked");
                addPoi(5);
                break;
            case R.id.addSector:
                Log.d("Menu", "Sector button clicked");
                recording();
                item.setChecked(mSpy);
                break;
            case R.id.logoff :
                Log.d("Menu", "LogOff button clicked");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Map the corresponding function with the view clicked in the menu
     */
    public void buttonsSettings(){
        View btn;

        btn = findViewById(R.id.abort);
        btn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        abort();
                    }
                }
        );

        btn = findViewById(R.id.save);
        btn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        save();
                    }
                }
        );
    }

    /**
     * Function to abort the polygon draw.
     */
    private void abort(){
        Log.d("Buttons", "abort button pressed");
    }

    /**
     * Function to save the polygon draw.
     */
    private void save(){
        Log.d("Buttons", "save button pressed");
    }

}
