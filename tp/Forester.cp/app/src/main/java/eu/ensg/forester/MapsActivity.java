package eu.ensg.forester;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.Random;

import eu.ensg.forester.db.ForesterSpatialiteOpenHelper;
import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import eu.ensg.spatialite.geom.BadGeometryException;
import eu.ensg.spatialite.geom.Point;
import eu.ensg.spatialite.geom.XY;
import jsqlite.Exception;
import jsqlite.Stmt;
import pub.devrel.easypermissions.EasyPermissions;

import static eu.ensg.forester.Constants.EXTRA_SERIAL;
import static eu.ensg.forester.Constants.PREFERENCE_NAME;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private float lat = -34, lng = 151;
    private eu.ensg.spatialite.geom.Point currentPosition;
    private SpatialiteDatabase database;
    private String userid;
    private SharedPreferences preferences;
    Random rnd;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        preferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        //Menu fragment = (Menu) getFragmentManager().findFragmentById(R.menu.menumap);
        userid = getIntent().getStringExtra(EXTRA_SERIAL);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        // database
        initDatabase();
    }

    private void setPreferences() {
        SharedPreferences preferences = getSharedPreferences("MyPreferenceFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("MyKey", "me");
        editor.commit();
        editor.apply();
    }
    private String getPreferences(String value) {
        SharedPreferences preferences =
                getSharedPreferences("MyPreferenceFile", Context.MODE_PRIVATE);
        return preferences.getString("MyKey", value);
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

        updatePosition();

        // Add a marker in Sydney and move the camera
        LatLng pos = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions().position(pos).title("Here you are!"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
    }

    public void updatePosition() {
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        String p = lm.getBestProvider(c, false);

        Log.d("Provider: ", p==null?"nope":p);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            Log.d("Request perm FINE", String.valueOf(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)));
            Log.d("Request perm COARSE", String.valueOf(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)));

            p = lm.getBestProvider(c, false);

            Log.d("Provider 2: ", p==null?"nope":p);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location l = lm.getLastKnownLocation(p);

        //lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

// Initialize the location fields
            if (l != null) {
                lat = (float) (l.getLatitude());
                lng = (float) (l.getLongitude());
                currentPosition= new Point(new XY(lng, lat));
                Log.d(MapsActivity.class.getName(), String.valueOf(lat));
                Log.i(MapsActivity.class.getName(), String.valueOf(lng));
            } else {
                Log.d(MapsActivity.class.getName(), "Provider not available");
                Log.d(MapsActivity.class.getName(), "Provider not available");
            }

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {}
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 1,
                locationListener);

    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Maps Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menumap,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id= item.getItemId();
        switch (id) {
            case (R.id.add_poi) :
                Log.d("Menu", "poi selected");
                onOptionAddPOISelected(item);
                break;
            case (R.id.add_sector) :
                onOptionAddSectorSelected(item);
                Log.d("Menu", "sector selected");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onOptionAddPOISelected(MenuItem item) {
        if (currentPosition!= null) {
            //mMap.addMarker(new MarkerOptions().position(currentPosition.toLatLng()).title("POI").snippet(currentPosition.toString()));
            Toast.makeText(this, "POI avail", Toast.LENGTH_LONG).show();
            if (addPointToDatabase(currentPosition.toString())) Log.d("POI", "add ok");

        } else {
            Toast.makeText(this, "POI not avail", Toast.LENGTH_LONG).show();
            Log.d("POI", "add NOT ok");

        }

    }

    private void onOptionAddSectorSelected(MenuItem item) {

    }


    private void initDatabase() {
        try {
            SpatialiteOpenHelper helper = new ForesterSpatialiteOpenHelper(this);
            database = helper.getDatabase();
        } catch (jsqlite.Exception | IOException e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Cannot initialize database !", Toast.LENGTH_LONG).show();
            System.exit(0);
        }
    }

    private boolean addPointToDatabase(String position) {
        Log.d("Addpoint", currentPosition.getCoordinate().toString());
        Log.d("Addpoint pos", position);

        rnd= new Random();
        String name= "A" + String.valueOf(rnd.nextInt(20));
        try {
        String query= "INSERT INTO PointOfInterest "+
                "(ForesterID, Name, Description, position) VALUES("+
                userid + ", " + DatabaseUtils.sqlEscapeString(name) +","+DatabaseUtils.sqlEscapeString("toto")+","
                + currentPosition.toSpatialiteQuery(4326) + "); ";



        Log.d("Addpoint query", query);

            database.exec(query);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (BadGeometryException e) {
            e.printStackTrace();
        }
        return true;
/*
        "ID integer PRIMARY KEY AUTOINCREMENT,\n" +
                "ForesterID integer NOT NULL,\n" +
                "Name string NOT NULL,\n" +
                "Description string,\n" +
                "CONSTRAINT FK_poi_forester\n" +
                "FOREIGN KEY (foresterID)\n" +
                "REFERENCES forester (id)\n" +


         */
    }

    private void retrieveData() {
        Stmt st= null;

        try {
            st = database.prepare("SELECT name, ST_AsText(position) FROM PointOfInterest");
            while (st.step()) {
                String name = st.column_string(0);
                String pos= st.column_string(1);
                Point point= Point.unMarshall(pos);

                mMap.addMarker(new MarkerOptions().position(point.toLatLng()).title(name));
            }

        } catch (java.lang.Exception e) {}
    }

}
