package eu.ensg.forester;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.view.ViewGroupCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import eu.ensg.spatialite.geom.BadGeometryException;
import eu.ensg.spatialite.geom.Point;
import eu.ensg.spatialite.geom.Polygon;
import eu.ensg.spatialite.geom.XY;
import jsqlite.Exception;
import jsqlite.Stmt;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, Constants {

    private GoogleMap mMap;

    private TextView position;
    private ViewGroup recordLayout;
    private Button save, abort;

    private Point currentPoint;
    private Polygon currentSector;
    private com.google.android.gms.maps.model.Polygon currentDrawnPolygon;

    private boolean isRecording = false;
    private int foresterId;

    private SpatialiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        foresterId = getIntent().getIntExtra(EXTRA_FORESTER_ID, -1);

        position = (TextView)findViewById(R.id.position);
        recordLayout = (ViewGroup)findViewById(R.id.record_layout);
        save = (Button)findViewById(R.id.record_save);
        abort = (Button)findViewById(R.id.record_abort);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSaveRecordClicked(v);
            }
        });

        abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAbortRecordClicked(v);
            }
        });

        initDatabase();
    }

    private void initDatabase() {
        SpatialiteOpenHelper helper = null;
        try {
            helper = new ForesterSpatialiteOpenHelper(this);
            database = helper.getDatabase();

        } catch (Exception | IOException e) {
            e.printStackTrace();
        }
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

        LocationManager locationManager =
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(true);

        String provider = locationManager.getBestProvider(criteria, true);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Log.i(MapsActivity.class.getName(), "Request location update");
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,  this);

        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if (location != null) {

            float lat = (float) (location.getLatitude());
            float lng = (float) (location.getLongitude());

            currentPoint = new Point(location.getLongitude(), location.getLatitude());
            position.setText(currentPoint.toString());
            Log.i(this.getClass().getName(), "Initial position" + currentPoint.toString());

            // Add a marker in Sydney and move the camera
            LatLng lastCoord = new LatLng(lat, lng);
            //mMap.addMarker(new MarkerOptions().position(lastCoord).title("Position initiale"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(lastCoord));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

            Log.i(MapsActivity.class.getName(), String.valueOf(lat));
            Log.i(MapsActivity.class.getName(), String.valueOf(lng));
        } else {
            Log.w(MapsActivity.class.getName(), "Provider not available");
            position.setText("Provider not available");
        }

        try {
            Stmt stmt = database.prepare("SELECT name, description, ST_AsText(position) FROM PointOfInterest;");

            while(stmt.step()) {
                String name = stmt.column_string(0);
                String description = stmt.column_string(1);
                String position = stmt.column_string(2);
                Point point = Point.unMarshall(position);

                mMap.addMarker(new MarkerOptions().position(
                        point.toLatLng()).title(name)
                        .snippet(description));

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Stmt stmt = database.prepare("SELECT name, description, ST_AsText(area) FROM Sector");

            while(stmt.step()) {
                String name = stmt.column_string(0);
                String description = stmt.column_string(1);
                String area = stmt.column_string(2);
                Polygon polygon = Polygon.unMarshall(area);

                PolygonOptions poly = new PolygonOptions();
                for (XY coord : polygon.getCoordinates().getCoords()) {
                    poly.add(new LatLng(coord.getY(), coord.getX()));
                }
                poly.strokeColor(R.color.color_primary_dark);
                mMap.addPolygon(poly);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentPoint = new Point(location.getLongitude(), location.getLatitude());
        position.setText(currentPoint.toString());
        Log.i(this.getClass().getName(), "Position changed " + currentPoint.toString());

        if (isRecording) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPoint.toLatLng()));
            currentSector.addCoordinate((float)location.getLongitude(), (float)location.getLatitude());

            PolygonOptions poly = new PolygonOptions();
            for (XY coord : currentSector.getCoordinates().getCoords()) {
                poly.add(new LatLng(coord.getY(), coord.getX()));
            }
            poly.strokeColor(R.color.color_primary);

            if (currentDrawnPolygon != null) currentDrawnPolygon.remove();
            currentDrawnPolygon = mMap.addPolygon(poly);
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

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case (R.id.add_poi) :
                onOptionAddPOISelected(item);
                break;
            case (R.id.add_sector) :
                onOptionAddSectorSelected(item);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onOptionAddPOISelected(MenuItem item) {

        if (currentPoint != null) {
            mMap.addMarker(new MarkerOptions().position(
                    currentPoint.toLatLng()).title("Point of interest")
                    .snippet(currentPoint.toString()));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPoint.toLatLng()));

            try {

                database.exec("" +
                        "INSERT INTO PointOfInterest " +
                        "(foresterID, name, description, position) " +
                        "VALUES " +
                        "(" + foresterId + ", " +
                        DatabaseUtils.sqlEscapeString("Point of interest") + ", " +
                        DatabaseUtils.sqlEscapeString(currentPoint.toString()) + ", " +
                        currentPoint.toSpatialiteQuery(4326) + ")");

            } catch (jsqlite.Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Sql Error !!!!", Toast.LENGTH_LONG).show();
            } catch (BadGeometryException e) {
                e.printStackTrace();
                Toast.makeText(this, "Point marshalling Error !!!!", Toast.LENGTH_LONG).show();
            }

        } else
            Toast.makeText(this, "Not available !", Toast.LENGTH_LONG).show();

    }

    private void onOptionAddSectorSelected(MenuItem item) {
        currentSector = new Polygon();
        isRecording = true;
        recordLayout.setVisibility(View.VISIBLE);
    }

    private void onSaveRecordClicked(View view) {
        isRecording = false;
        recordLayout.setVisibility(View.GONE);

        try {

            database.exec("" +
                    "INSERT INTO Sector " +
                    "(foresterID, name, description, area) " +
                    "VALUES " +
                    "(" + foresterId + ", " +
                    DatabaseUtils.sqlEscapeString("Sector") + ", " +
                    DatabaseUtils.sqlEscapeString(currentSector.toString()) + ", " +
                    currentSector.toSpatialiteQuery(4326) + ")");

            currentSector = null;

        } catch (jsqlite.Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Sql Error !!!!", Toast.LENGTH_LONG).show();
        } catch (BadGeometryException e) {
            e.printStackTrace();
            Toast.makeText(this, "Polygon marshalling Error !!!!", Toast.LENGTH_LONG).show();
        }
    }

    private void onAbortRecordClicked(View view) {
        isRecording = false;
        recordLayout.setVisibility(View.GONE);
        currentSector = null;
    }

}
