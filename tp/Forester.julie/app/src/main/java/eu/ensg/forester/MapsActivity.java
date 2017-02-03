package eu.ensg.forester;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.IOException;

import eu.ensg.forester.data.ForesterSpatialiteOpenHelper;
import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import eu.ensg.spatialite.geom.Point;
import eu.ensg.spatialite.geom.Polygon;
import eu.ensg.spatialite.geom.XY;
import jsqlite.Exception;
import jsqlite.Stmt;

import static eu.ensg.forester.Constants.EXTRA_FORESTER_ID;
import static eu.ensg.forester.Constants.EXTRA_SERIAL;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,LocationListener {

    private GoogleMap mMap;
    private Point currentPosition;

    private TextView editPosition;
    private LinearLayout sectorMenu;
    private Button save;
    private Button abort;

    private Polygon sector = null;;
    private boolean isRecording = false;
    private com.google.android.gms.maps.model.Polygon currentDrawPolygon;

    // DB
    private SpatialiteDatabase database;

    // ID user
    private Integer idUser;

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
        abort = (Button)findViewById(R.id.button_abort);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save_onClick(view);
            }
        });

        abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abort_onClick(view);
            }
        });

        //idUser = getIntent().getStringExtra(EXTRA_SERIAL);
        idUser = getIntent().getIntExtra(EXTRA_FORESTER_ID, -1);

        // database
        initDatabase();
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

        load_Point();
        load_Sector();

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

        if (isRecording) {
            sector.addCoordinate(currentPosition.getCoordinate());

            PolygonOptions poly = new PolygonOptions();
            for (XY elmt : sector.getCoordinates().getCoords()) {
                poly.add(new LatLng(elmt.getX(), elmt.getY()));
            }

            if (currentDrawPolygon != null)
                currentDrawPolygon.remove();
            currentDrawPolygon = mMap.addPolygon(poly);
            currentDrawPolygon.setVisible(true);

            Log.i(this.getClass().getName(), currentDrawPolygon.toString());

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
            try {
                database.exec("INSERT INTO PointOfInterest " +
                        "(foresterID, name, description, position) " +
                        "VALUES ('" + idUser + "', '" +
                        "Interest" + "', '" +
                        currentPosition.toString() + "', '" +
                        currentPosition + "')");
                Log.i(this.getClass().getName(), "Save");

            } catch (jsqlite.Exception e) {
                e.printStackTrace();
            }
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
        try {
            database.exec("INSERT INTO Sector " +
                    "(foresterID, name, Area) " +
                    "VALUES ('" + idUser + "', '" +
                    "Sector" + "',' " +
                    sector + "')");
            Log.i(this.getClass().getName(), "Save");
            sector = null;

        } catch (jsqlite.Exception e) {
            e.printStackTrace();
        }
        isRecording = false;
        sectorMenu.setVisibility(view.GONE);
        //Log.i(this.getClass().getName(), sector.toString());
    }

    private void abort_onClick(View view) {
        isRecording = false;
        sectorMenu.setVisibility(view.GONE);
        //Log.i(this.getClass().getName(), sector.toString());
    }

    private void initDatabase() {
        try {
            SpatialiteOpenHelper helper = new ForesterSpatialiteOpenHelper(this);
            database = helper.getDatabase();
        } catch (jsqlite.Exception | IOException e) {
            e.printStackTrace();
        }
    }

    private void load_Point() {
        Stmt stmt = null;
        try {
            stmt = database.prepare("SELECT name, description, position FROM PointOfInterest where foresterID="+ idUser);
            while (stmt.step()) {
                String name = stmt.column_string(0);
                String description = stmt.column_string(1);
                Point position = Point.unMarshall(stmt.column_string(2));

                mMap.addMarker(new MarkerOptions().position(position.toLatLng())
                        .title(name)
                        .snippet(description));
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void load_Sector() {
        Stmt stmt = null;
        try {
            stmt = database.prepare("SELECT name, description, Area FROM Sector where foresterID=\"+ idUser");
            while (stmt.step()) {
                String name = stmt.column_string(0);
                String description = stmt.column_string(1);
                Polygon poly = Polygon.unMarshall(stmt.column_string(2));
                PolygonOptions polyOpt = new PolygonOptions();

                for (XY xy : poly.getCoordinates().getCoords()) {
                    polyOpt.add(new LatLng(xy.getY(), xy.getX()));
                }
                polyOpt.strokeColor(ContextCompat.getColor(this, R.color.color_stroke_polygon));
                mMap.addPolygon(polyOpt);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
