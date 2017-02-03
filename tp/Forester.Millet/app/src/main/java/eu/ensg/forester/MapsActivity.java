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

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, Constants {

    private GoogleMap mMap;
    private Point currentPoint = null;
    private Polygon sector = null;
    private PolygonOptions draw = null;
    private LinearLayout mainlayout;

    private Integer forester_id = getIntent().getIntExtra(EXTRA_FORESTER_ID, -1);
    private SpatialiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
            currentPoint = new Point(location.getLatitude(),location.getLatitude());
            Log.i(MapsActivity.class.getName(), String.valueOf(lat));
            Log.i(MapsActivity.class.getName(), String.valueOf(lng));
            LatLng last_localisation = new LatLng(lat, lng);
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                .position(last_localisation)
                .title("Localisation")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            );
            mMap.moveCamera(CameraUpdateFactory.newLatLng(last_localisation));
            TextView label = (TextView)findViewById(R.id.coordinates);
            //label.setText(lat+" , "+lng);
            label.setText(String.format("%s , %s", lat, lng));
        } else {
            Log.w(MapsActivity.class.getName(), "Provider not available");
            Log.w(MapsActivity.class.getName(), "Provider not available");
            TextView label = (TextView)findViewById(R.id.coordinates);
            label.setText("Provider not available...");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            float lat = (float) (location.getLatitude());
            float lng = (float) (location.getLongitude());
            currentPoint = new Point(location.getLatitude(),location.getLatitude());
            Log.i(MapsActivity.class.getName(), String.valueOf(lat));
            Log.i(MapsActivity.class.getName(), String.valueOf(lng));
            LatLng last_localisation = new LatLng(lat, lng);
            //mMap.clear();
            //mMap.addMarker(new MarkerOptions().position(last_localisation).title("Localisation"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(last_localisation));
            TextView label = (TextView)findViewById(R.id.coordinates);
            //label.setText(lat+" , "+lng);
            label.setText(String.format("%s , %s", lat, lng));
        } else {
            Log.w(MapsActivity.class.getName(), "Provider not available");
            Log.w(MapsActivity.class.getName(), "Provider not available");
            TextView label = (TextView)findViewById(R.id.coordinates);
            label.setText("Provider not available...");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Provider available...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Provider not available...", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case (R.id.point_of_interest):
                onPoint_of_interestSelected();
                break;
            case (R.id.sector):
                onSectorSelected();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onPoint_of_interestSelected(){
        if (currentPoint != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(currentPoint.toLatLng())
                    .title("Point of interest")
                    .snippet(String.format("%s", currentPoint.toString()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            );
            TextView label = (TextView)findViewById(R.id.coordinates);
            label.setText(String.format("%s", currentPoint.toString()));
            try {
                database.exec(
                        "INSERT INTO PointOfInterest (ForesterID, Name, Description, position "
                        + "VALUES ("
                        + forester_id
                        + ","
                        + "Point of interest"
                        + ","
                        + DatabaseUtils.sqlEscapeString(currentPoint.toString())
                        + ","
                        + currentPoint.toSpatialiteQuery(4326)
                        + ")"
                );
            } catch (Exception e) {
                e.printStackTrace();
            } catch (BadGeometryException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Provider not available...", Toast.LENGTH_LONG).show();
            TextView label = (TextView)findViewById(R.id.coordinates);
            label.setText("Provider not available...");
        }
    }

    private void onSectorSelected(){
        Button buttonsave;
        Button buttonabord;
        mainlayout = (LinearLayout)this.findViewById(R.id.ls);
        buttonsave = (Button) this.findViewById(R.id.save);
        buttonabord = (Button)this.findViewById(R.id.abort);
        mainlayout.setVisibility(LinearLayout.VISIBLE);
        if (currentPoint != null) {
            buttonsave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSaveSelected(v);
                }
            });
            buttonabord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAbortSelected();
                }
            });
        } else {
            mainlayout.setVisibility(LinearLayout.GONE);
            Toast.makeText(this, "Provider not available...", Toast.LENGTH_LONG).show();
            TextView label = (TextView)findViewById(R.id.coordinates);
            label.setText("Provider not available...");
        }
    }

    private void onSaveSelected(View v) {
        sector.addCoordinate(currentPoint.getCoordinate());
        mMap.addMarker(new MarkerOptions()
                .position(currentPoint.toLatLng())
                .title("Sector")
                .snippet(String.format("%s", currentPoint.toString()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        );
        try {
            database.exec(
                    "INSERT INTO District (ForesterID, Name, Description, Area "
                    + "VALUES ("
                    + forester_id
                    + ","
                    + "Point of interest"
                    + ","
                    + DatabaseUtils.sqlEscapeString(sector.toString())
                    + ","
                    + sector.toSpatialiteQuery(4326)
                    + ")"
            );
        } catch (Exception e) {
            e.printStackTrace();
        } catch (BadGeometryException e) {
            e.printStackTrace();
        }
        TextView label = (TextView)findViewById(R.id.coordinates);
        label.setText(String.format("%s", currentPoint.toString()));
        mainlayout.setVisibility(LinearLayout.GONE);
        Toast.makeText(this, "Point recorded !", Toast.LENGTH_LONG).show();
        for( XY coord : sector.getCoordinates().getCoords()){
            draw.add(new LatLng(coord.getX(),coord.getY()));
        }
        draw.fillColor(0x009900);
    }

    private void onAbortSelected() {
        sector = null;
        draw = null;
        mainlayout.setVisibility(LinearLayout.GONE);
        Toast.makeText(this, "Polygon emptied !", Toast.LENGTH_LONG).show();
    }

    private void initDatabase() {
        SpatialiteOpenHelper helper = null;
        try {
            helper = new ForesterSpatialiteOpenHelper(this);
            database = helper.getDatabase();
        } catch (jsqlite.Exception | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Cannot initialize database !", Toast.LENGTH_LONG).show();
            System.exit(0);
        }
    }
}
