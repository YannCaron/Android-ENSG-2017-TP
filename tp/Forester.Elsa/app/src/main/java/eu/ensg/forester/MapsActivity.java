package eu.ensg.forester;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

import static eu.ensg.forester.Constants.EXTRA_FORESTER_ID;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    // la database
    private SpatialiteDatabase database;

    private GoogleMap mMap;
    private Point currentPosition;
    private TextView lblPosition;
    private Polygon currentSector;
    private com.google.android.gms.maps.model.Polygon currentDrawPolygon;

    private int foresterID;

    boolean isRecording = false;

    Button buttonSave;
    Button buttonAbort;
    View layoutSector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Récupère les instances des vues
        lblPosition = (TextView) findViewById(R.id.gps);
        buttonSave = (Button)findViewById (R.id.save);
        buttonAbort = (Button)findViewById (R.id.abort);
        layoutSector = findViewById(R.id.layout_sector);

        // On récupère le serial
        foresterID = getIntent().getIntExtra(EXTRA_FORESTER_ID, -1);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save_onClick(v);
            }
        });
        buttonAbort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abort_onClick(v);
            }
        });

        // Initialisation de la database
        initDatabase();
    }


    // ajout du menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
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


        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Recherche de la derniere localisation
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

            // Add a marker at our position and move the camera
            LatLng lastCoord = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions().position(lastCoord).title("Last time we were here"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(lastCoord));

            lblPosition.setText("Last Position: "+location.getLatitude()+", "+location.getLongitude());

            Log.i(this.getClass().getName(), String.valueOf(lat));
            Log.i(this.getClass().getName(), String.valueOf(lng));
        } else {
            Log.w(this.getClass().getName(), "Provider not available");
        }

        // dessine points intérets et sector de la database
        loadPOI();
        loadSector();

        // attache le callback
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 1, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        currentPosition = new Point(location.getLongitude(), location.getLatitude());
        //mMap.addMarker(new MarkerOptions().position(currentPosition.toLatLng()).title("Here!"));
        lblPosition.setText("Position changed: "+location.getLatitude()+", "+location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition.toLatLng()));
        Log.i(this.getClass().getName(), "Position changed" + currentPosition.toString());

        if (isRecording){
            currentSector.addCoordinate(currentPosition.getCoordinate());
            /*mMap.addMarker(new MarkerOptions().position(currentPosition.toLatLng())
                    .snippet(currentPosition.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));*/
            PolygonOptions poly = new PolygonOptions();
            for (XY coord:currentSector.getCoordinates().getCoords()){
               poly.add(new LatLng(coord.getY(), coord.getX()));
            }
            if(currentDrawPolygon != null) currentDrawPolygon.remove();
            currentDrawPolygon = mMap.addPolygon(poly);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case (R.id.menu_addpoint):
                // menu add point of interest
                onSelectedAddPoint(item);
                break;
            case (R.id.menu_addsector):
                // menu sector
                onSelectedAddSector(item);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onSelectedAddPoint(MenuItem item){
        Log.i(this.getClass().getName(), "Add point of interest");
        if (currentPosition != null) {
            try {
                database.exec("INSERT INTO PointOfInterest (foresterID, name, description, position) " +
                        "VALUES (" +
                        foresterID + ", " +
                        DatabaseUtils.sqlEscapeString("Point of interest") + ", " +
                        DatabaseUtils.sqlEscapeString(currentPosition.toString()) + ", " +
                        currentPosition.toSpatialiteQuery(4326) + ")");

                markPOI(currentPosition);

                Toast.makeText(this, "Point registered", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Sql error", Toast.LENGTH_LONG).show();
            } catch (BadGeometryException e) {
                e.printStackTrace();
                Toast.makeText(this, "Polygon marshalling error", Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(this, "Not available !", Toast.LENGTH_LONG).show();
        }
    }

    private void onSelectedAddSector(MenuItem item){
        Log.i(this.getClass().getName(), "Add sector");
        currentSector = new Polygon();
        isRecording = true;
        layoutSector.setVisibility(View.VISIBLE);

    }

    private void save_onClick(View v) {
        Log.i(this.getClass().getName(), "Add sector");
        if (currentPosition != null) {
            try {
                database.exec("INSERT INTO District (foresterID, name, description, Area) " +
                        "VALUES (" +
                        foresterID + ", " +
                        DatabaseUtils.sqlEscapeString("Sector") + ", " +
                        DatabaseUtils.sqlEscapeString(currentSector.toString()) + ", " +
                        currentSector.toSpatialiteQuery(4326) + ")");

                isRecording = false; currentSector = null;
                layoutSector.setVisibility(View.GONE);
                currentDrawPolygon.setFillColor(0x7F0000FF);
                currentDrawPolygon.setStrokeColor(Color.BLUE);

                Toast.makeText(this, "Sector registered", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Sql error", Toast.LENGTH_LONG).show();
            } catch (BadGeometryException e) {
                e.printStackTrace();
                Toast.makeText(this, "Polygon marshalling error", Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(this, "Not available !", Toast.LENGTH_LONG).show();
        }

    }

    private void abort_onClick(View v) {
        isRecording = false;
        layoutSector.setVisibility(View.GONE);

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

    private void markPOI(Point position){
        mMap.addMarker(new MarkerOptions().position(position.toLatLng())
                .title("Point of interest")
                .snippet(position.toString())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position.toLatLng()));
    }

    private void drawSector(Polygon sector){
        PolygonOptions poly = new PolygonOptions();
        for (XY coord:sector.getCoordinates().getCoords()){
            poly.add(new LatLng(coord.getY(), coord.getX()));
        }
        com.google.android.gms.maps.model.Polygon polygonDraw = mMap.addPolygon(poly);
        polygonDraw.setFillColor(0x7F0000FF);
        polygonDraw.setStrokeColor(Color.BLUE);
    }


    private void loadPOI() {
        try {
            Stmt stmt = database.prepare("SELECT name, description, ST_asText(position) FROM PointOfInterest WHERE foresterID = " + foresterID);
            while (stmt.step()) {
                Point position = Point.unMarshall(stmt.column_string(2));
                markPOI(position);
            }
        } catch (jsqlite.Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Sql error ", Toast.LENGTH_LONG).show();
        }
    }

    private void loadSector() {
        try {
            Stmt stmt = database.prepare("SELECT name, ST_asText(Area) as Area FROM District WHERE foresterID = " + foresterID);
            while (stmt.step()) {
                Polygon polygon = Polygon.unMarshall(stmt.column_string(1));
                drawSector(polygon);

            }
        } catch (jsqlite.Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Sql error ", Toast.LENGTH_LONG).show();
        }
    }
}
