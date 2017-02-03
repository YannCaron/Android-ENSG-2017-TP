package eu.ensg.forester;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.support.design.widget.Snackbar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import eu.ensg.forester.data.ForesterSpatialiteOpenHelper;
import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.geom.BadGeometryException;
import eu.ensg.spatialite.geom.Point;
import eu.ensg.spatialite.geom.XY;
import jsqlite.Stmt;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LinearLayout layout;
    private Button buttonSave;
    private Button buttonCancel;
    private TextView textView;
    private String serial;
    private String firstName;
    private String lastName;

    // les préférences
    private SharedPreferences preferences;

    // database
    private SpatialiteDatabase database;

    private Point currentPosition = new Point(48.856458, 2.347882);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        serial = getIntent().getStringExtra("serial");
        firstName = getIntent().getStringExtra("firstName");
        lastName = getIntent().getStringExtra("lastName");

        Log.i(MapsActivity.class.getName(), "serial : " + serial);

        //countPOI();

        buttonSave = (Button) findViewById(R.id.save);
        buttonCancel = (Button) findViewById(R.id.cancel);
        layout = (LinearLayout) findViewById(R.id.layout);
        textView = (TextView) findViewById(R.id.serial);

        textView.setText(getString(R.string.welcome) + " " + firstName + " " + lastName);
        textView.setVisibility(View.VISIBLE);

        // gère les événements
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save_onClick(v);
            }
        });
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel_onClick(v);
            }
        });

        Point Paris = new Point(new XY(48.856458, 2.347882));
        //insertPOI("Paris", "This is a test point", 0, Paris);
    }

    private void save_onClick(View view) {
        layout.setVisibility(View.INVISIBLE);
        layout.setAlpha((float) 0.8);

        Snackbar.make(view, R.string.dataSaved, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    private void cancel_onClick(View view) {
        layout.setVisibility(View.INVISIBLE);
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

        moveTo(currentPosition);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
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
            Log.i(MapsActivity.class.getName(), String.valueOf(location.getAccuracy()));
            Log.i(MapsActivity.class.getName(), String.valueOf(lat));
            Log.i(MapsActivity.class.getName(), String.valueOf(lng));
        } else {
            Log.i(MapsActivity.class.getName(), getString(R.string.providerNotAvailable));
            Log.i(MapsActivity.class.getName(), getString(R.string.providerNotAvailable));
        }

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        LatLng paris = new LatLng(48.856458, 2.347882);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.addMarker(new MarkerOptions().position(paris).title("This is Paris !").snippet("48.856458, 2.347882").rotation(0));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(paris));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
    }

    // vérifie que la map soit bien chargée
    private boolean checkMap() {
        if (mMap == null) {
            return false;
        }
        return true;
    }

    private void moveTo(Point position) {
        if (!checkMap()) return;

        // positionnement initial
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position.toLatLng()));

    }

    private void zoomTo(float zoom) {
        if (!checkMap()) return;

        // animation
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom), 2000, null);
    }

    private void addPointOfInterest(String name, String description, Point position) {
        if (!checkMap()) return;

        // ajoute un marqueur
        mMap.addMarker(new MarkerOptions()
                .position(position.toLatLng())
                .title(name)
                .snippet(description)
        );
    }

    private void insertPOI(String name, String description, int forestId, Point position){
        Log.i(MapsActivity.class.getName(), "Point : " + String.valueOf(position));
        try {
            Log.i(MapsActivity.class.getName(), "INSERT INTO PointOfInterest (Name, Description, ForesterID, Position) VALUES('" + (name) + "', '" + (description) + "', " + (forestId) + ", " + (position.toSpatialiteQuery(4326)) + ");");
        } catch (BadGeometryException e) {
            e.printStackTrace();
        }
        try {
            database.exec("INSERT INTO PointOfInterest (Name, Description, ForesterID, Position) VALUES('" + (name) + "', '" + (description) + "', " + (forestId) + ", " + (position.toSpatialiteQuery(4326)) + ");");
            Log.i(MapsActivity.class.getName(), "insert POI into DB");

            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("serial", serial);
            startActivity(intent);
        }
        catch (jsqlite.Exception e) {
            e.printStackTrace();
        } catch (BadGeometryException e) {
            e.printStackTrace();
        }
    }

    private void countPOI(){
        try {
            Stmt stmt = database.prepare("SELECT COUNT(*) FROM PointOfInterest;");

            if (stmt.step()) {
                // stoque le serial dans la mémoire de l'appareil
                SharedPreferences.Editor editor = preferences.edit();
                editor.commit();
                editor.apply();

                int countPOI = stmt.column_int(0);

                Log.i(MapsActivity.class.getName(), "countPOI : " + countPOI);
            }
        } catch (jsqlite.Exception e) {
            e.printStackTrace();
        }
    }
}