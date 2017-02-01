package eu.ensg.forester;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import eu.ensg.spatialite.geom.Point;
import eu.ensg.spatialite.geom.Polygon;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,LocationListener {

    private GoogleMap mMap;
    private Point currentPosition;

    private TextView editPosition;
    private LinearLayout sectorMenu;
    private Button save;
    private Button abrot;

    private Polygon sector = new Polygon();;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        editPosition = (TextView)findViewById(R.id.position);
        sectorMenu = (LinearLayout) findViewById(R.id.sector_menu);
        save = (Button)findViewById(R.id.button_save);
        abrot = (Button)findViewById(R.id.button_abrot);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save_onClick(view);
            }
        });

        abrot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abort_onClick(view);
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Service
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Dernière position connue
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if (location != null) {
            float lat = (float) (location.getLatitude());
            float lng = (float) (location.getLongitude());

            currentPosition = new Point(location.getLongitude(), location.getLatitude());

            // Add a marker  and move the camera
            LatLng lastCoord = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions().position(lastCoord).title("Here"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(lastCoord));
            editPosition.setText(currentPosition.toString());

            Log.i(this.getClass().getName(), String.valueOf(lat));
            Log.i(this.getClass().getName(), String.valueOf(lng));
        } else {
            Log.i(this.getClass().getName(), "Provider not available");
            Log.i(this.getClass().getName(), "Provider not available");
        }

        // Ecoute tout changement de location
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 1, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        currentPosition = new Point(location.getLongitude(), location.getLatitude());
        //mMap.clear();
        //mMap.addMarker(new MarkerOptions().position(currentPosition.toLatLng()).title("Here"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition.toLatLng()));
        editPosition.setText(currentPosition.toString());
        Log.i(this.getClass().getName(), "Update location");

        if (isRecording == true) {
            sector.addCoordinate(currentPosition.getCoordinate());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.menu_addPoint):
                onOptionsItemSelectedPoint(item);
                break;
            case (R.id.menu_addSector):
                onOptionsItemSelectedSector(item);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onOptionsItemSelectedPoint(MenuItem item) {
        if (currentPosition != null) {
            mMap.addMarker(new MarkerOptions().position(currentPosition.toLatLng())
                                            .title("Point of interest")
                                            .snippet(currentPosition.toString()));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition.toLatLng()));
        }
        else
            Toast.makeText(this, "Not available",Toast.LENGTH_LONG).show();

    }

    public void onOptionsItemSelectedSector(MenuItem item) {
        sectorMenu.setVisibility(View.VISIBLE);
        sector = new Polygon();
        isRecording = true;
    }

    private void save_onClick(View view) {
        isRecording = false;
        sectorMenu.setVisibility(view.GONE);
        Log.i(this.getClass().getName(), sector.toString());
    }

    private void abort_onClick(View view) {
        isRecording = false;
        sectorMenu.setVisibility(view.GONE);
        Log.i(this.getClass().getName(), sector.toString());
    }

}
