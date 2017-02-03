package eu.ensg.forester;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import dbAcces.ForesterSpatialiteOpenHelper;
import eu.ensg.commons.io.WebServices;
import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.geom.BadGeometryException;
import eu.ensg.spatialite.geom.Point;
import eu.ensg.spatialite.geom.Polygon;
import eu.ensg.spatialite.geom.XY;
import jsqlite.Exception;
import jsqlite.Stmt;

import static eu.ensg.forester.Constants.EXTRA_FORESTER_ID;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    // Gestion de la position
    private Location currentLocation;
    private Point pos;
    private TextView lblPosition;
    private LocationManager locMgr;
    private GpsListener gpsListener;

    // Gestion des polygones
    private Polygon currentDistrict;
    private boolean isRecording;
    private LinearLayout llRecording;
    private PolygonOptions polyOptions;

    // db
    private SpatialiteDatabase database;

    // ID de l'utilisateur
    private Integer foresterID;

    // Affichage
    private TextView tempTxt;
    private TextView humTxt;
    private TextView cloudTxt;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Manages location
        locMgr = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        gpsListener = new GpsListener();

        // Gérer les vues
        manageViews();

        // Gére l'intent
        // Gére si l'extra a été passé en paramètre
        foresterID = getIntent().getIntExtra(EXTRA_FORESTER_ID,0);

        Log.i("foresterID", ""+foresterID);

        // Connection à la base de données
        initDatabase();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locMgr.removeUpdates(gpsListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startListenerLoc();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case (R.id.itemPt):
                menuPt(item);
                return true;
            case (R.id.itemSt):
                menuSc(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void menuPt(MenuItem item) {
        popToast("Create point", true);
        createPt();
    }

    private void menuSc(MenuItem item) {
        popToast("Create sector", true);
        createSt();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Criteria criteria = new Criteria();
        String provider = locMgr.getBestProvider(criteria, false);

        // Initialisation de la location:
        currentLocation = new Location(provider);
        currentLocation.setLatitude(48);
        currentLocation.setLongitude(2);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locMgr.getLastKnownLocation(provider);

        if (location != null) {
            currentLocation = location;
            float lat = (float) (location.getLatitude());
            float lng = (float) (location.getLongitude());
            Log.d("loc", "lat : " + pos.getCoordinate().getY() + ", long : " + pos.getCoordinate().getX());

            pos = new Point(lng,lat);

            updateCam();
        } else {
            Log.d("Provider", "Provider not available");
        }

        // Lance les listeners sur la location
        startListenerLoc();

        // Initialise la caméra
        LatLng paris = new LatLng(48, 2);
        //mMap.addMarker(new MarkerOptions().position(paris).title("Marker in Paris").snippet("Ici c'est Paris !"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(paris));

        // Affiche les poi et le secteurs déjà présents dans la base de données et entré par l'utilisateur
        recoverPoiInDb();
        recoverSecInDb();

        // Affiche la météo
        displayMeteo();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Listener for the location
     */
    class GpsListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            currentLocation = location;
            pos = new Point(location.getLongitude(),location.getLatitude());

            // on met à jour la caméra
            updateCam();

            // on met à jour la météo
            displayMeteo();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            popToast(getResources().getString(R.string.gpsCh), true);
        }

        @Override
        public void onProviderEnabled(String s) {
            popToast(getResources().getString(R.string.gpsEn), true);
        }

        @Override
        public void onProviderDisabled(String s) {
            String gpsDis = getResources().getString(R.string.gpsDis);
            lblPosition.setText(gpsDis);
            popToast(gpsDis, true);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Manages the view
     */
    private void manageViews(){
        lblPosition = (TextView) findViewById(R.id.lblPosition);

        llRecording = (LinearLayout) findViewById(R.id.recordingMenu);
        activateRecording(false);

        Button btSave = (Button) findViewById(R.id.btSave);
        Button btAbort = (Button) findViewById(R.id.btAbort);

        btSave.setText(getString(R.string.sv));
        btAbort.setText(getString(R.string.abrt));

        btSave.setOnClickListener(new saveListener());
        btAbort.setOnClickListener(new AbortListener());

        // Récupération des textview
        tempTxt = (TextView) findViewById(R.id.tempTxt);
        humTxt = (TextView) findViewById(R.id.hmdTxt);
        cloudTxt = (TextView) findViewById(R.id.cloudTxt);

        tempTxt.setText(getString(R.string.tmp));
        humTxt.setText(getString(R.string.hmd));
        cloudTxt.setText(getString(R.string.cld));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     *  Updates the camera's position
     */
    private void updateCam(){
        //mMap.clear();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos.toLatLng(), 15));

        //mMap.addMarker(new MarkerOptions().position(pos.toLatLng()).title("Je suis ici").snippet("Ici c'est Moi !"));

        mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);

        if(isRecording){
            currentDistrict.addCoordinate((float)pos.getCoordinate().getX(),(float)pos.getCoordinate().getY());

            polyOptions.add(new LatLng(pos.getCoordinate().getY(),pos.getCoordinate().getX()));

            mMap.clear();
            mMap.addPolygon(polyOptions);
        }

        popToast("lat : " + pos.getCoordinate().getY() + ", long : " + pos.getCoordinate().getX(), true);
        Log.d("loc", "lat : " + pos.getCoordinate().getY() + ", long : " + pos.getCoordinate().getX());
        lblPosition.setText("Lat : " + pos.getCoordinate().getY() + ", Long : " + pos.getCoordinate().getX());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Functions which displays a message on the device's screen, if show = true
     *
     * @param message: String the  displayed message
     * @param show: Boolean true=> display
     */
    private void popToast(final String message, final boolean show) {
        // Simple helper method for showing toast on the main thread
        if (!show)
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MapsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a poi on the map
     */
    private void createPt(){
        mMap.clear();
        mMap.setOnMapClickListener(new MapClickListenerPt());
    }

    private class MapClickListenerPt implements GoogleMap.OnMapClickListener {
        @Override
        public void onMapClick(LatLng latLng) {
            Point poiTmp = new Point(latLng.longitude,latLng.latitude);

            // Geocodes the point, when it's done save the poi in the database
            geocodePoi(poiTmp);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a sector
     */
    private void createSt(){
        mMap.clear();
        currentDistrict = new Polygon();
        polyOptions = new PolygonOptions();
        polyOptions.strokeColor(Color.RED)
                .fillColor(Color.BLUE);
        activateRecording(true);
    }

    private class saveListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mMap.clear();
            popToast("Save", true);

            saveSecInDb("Sector","A sector",currentDistrict);

            activateRecording(false);
        }
    }

    private class AbortListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mMap.clear();
            popToast("Abort", true);
            activateRecording(false);
        }
    }

    /**
     * Activates or deactivates the recording
     * @param bool true activates the recording, false deactivates it
     */
    private void activateRecording(boolean bool){
        isRecording = bool;
        if(bool){
            llRecording.setVisibility(View.VISIBLE);
        }else{
            llRecording.setVisibility(View.GONE);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Starts the listener for the location
     */
    private void startListenerLoc(){
        // Define which provider the application will use regarding which one is available
        if (locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsListener);
        } else {
            locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initializes the connection to the database
     */
    private void initDatabase() {
        try {
            database = new ForesterSpatialiteOpenHelper(this).getDatabase();
        } catch (jsqlite.Exception | IOException e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * saves a poi in the database
     * @param name poi's name
     * @param description poi's description
     * @param poi poi's geometry
     */
    private void savePoiInDb(String name, String description, Point poi){
        String sqPoi = null;

        try {
            sqPoi = poi.toSpatialiteQuery(4326);
        } catch (BadGeometryException e) {
            e.printStackTrace();
        }

        try {
            database.exec("INSERT INTO PointOfInterest (Name, ForesterID, Description, position) " +
                    "VALUES ("+
                    DatabaseUtils.sqlEscapeString(name) + ", " +
                    foresterID + ", " +
                    DatabaseUtils.sqlEscapeString(description)+ ", " +
                    sqPoi +
                    ");");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * saves a sector in the database
     * @param name sector's name
     * @param description sector's description
     * @param sector sector's geometry
     */
    private void saveSecInDb(String name, String description, Polygon sector){
        String sqSec = null;

        try {
            sqSec = sector.toSpatialiteQuery(4326);
        } catch (BadGeometryException e) {
            e.printStackTrace();
        }

        try {
            database.exec("INSERT INTO Sector (Name, ForesterID, Description, position) " +
                    "VALUES ("+
                    DatabaseUtils.sqlEscapeString(name) + ", " +
                    foresterID + ", " +
                    DatabaseUtils.sqlEscapeString(description)+ ", " +
                    sqSec +
                    ");");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Recovers ant displays on the map the pois on the db
     */
    private void recoverPoiInDb(){
        // Prépare la requête de récupération
        try {
            Stmt stmt = database.prepare("SELECT Name, Description, ST_asText(position) FROM PointOfInterest WHERE ForesterID = " + foresterID);

            while(stmt.step()){
                // récupération des informations utiles
                String name = stmt.column_string(0);
                String comment = stmt.column_string(1);
                String coordStr = stmt.column_string(2);

                // Création du point
                Point coord = Point.unMarshall(coordStr);

                // Ajoute les points à la carte
                mMap.addMarker(new MarkerOptions().position(coord.toLatLng()).title(name).snippet(comment));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Recovers ant displays on the map the sectors on the db
     */
    private void recoverSecInDb(){
        // Prépare la requête de récupération
        try {
            Stmt stmt = database.prepare("SELECT Name, Description, ST_asText(position) FROM Sector WHERE ForesterID = " + foresterID);

            while(stmt.step()){
                String name = stmt.column_string(0);
                String comment = stmt.column_string(1);
                String geomStr = stmt.column_string(2);

                // Création du point
                Polygon poly = Polygon.unMarshall(geomStr);
                if(poly !=null) {
                    PolygonOptions polyOption = new PolygonOptions();
                    polyOption.strokeColor(Color.GREEN)
                            .fillColor(Color.YELLOW);

                    for (XY coord : poly.getCoordinates().getCoords()) {
                        polyOption.add(new LatLng(coord.getY(), coord.getX()));
                    }

                    mMap.addPolygon(polyOption);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Geocode le poi, l'affcihe sur la carte et l'ajoute à la bdd
     * @param poi le point d'interêt
     */
    private void geocodePoi(Point poi){
        final Point pt = poi;

        new AsyncTask<Location, Void, String>() {
            ProgressDialog dialog;

            @Override
            protected void onPreExecute(){
                // UI thread
                dialog = ProgressDialog.show(MapsActivity.this,
                        "Geocodage", "Geocodage en cours veuillez patienter", true, true);
            }

            @Override
            protected String doInBackground(Location... params) {

                String urlDef = "https://maps.googleapis.com/maps/api/geocode/json?latlng="+
                        pt.getCoordinate().getY()+","+pt.getCoordinate().getX()+"&key="+
                        getResources().getString(R.string.google_geocoding_key);

                URL url = null;
                try {
                    url = new URL(urlDef);

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                    return WebServices.convertStreamToString(in);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String res) {
                // UI thread
                dialog.dismiss();

                try {
                    // on parse le ficheir et on récupère la valeur qui nous intéresse
                    JSONObject json = new JSONObject(res);
                    JSONArray results = json.getJSONArray("results");
                    String adresse = ((JSONObject) results.get(0)).getString("formatted_address");

                    // On ajoute le point à la carte
                    mMap.addMarker(new MarkerOptions().position(pt.toLatLng())
                            .title("Point of interest")
                            .snippet(adresse));

                    // on sauvegarde le point dans la bdd
                    savePoiInDb("Poi",adresse,pt);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentLocation);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void displayMeteo(){
        new AsyncTask<Location, Void, String>() {
            @Override
            protected String doInBackground(Location... params) {

                String urlDef = "http://api.geonames.org/findNearByWeatherJSON?lat="+
                        currentLocation.getLatitude()+"&lng="+currentLocation.getLongitude()+
                        "&username=cyann";

                URL url = null;
                try {
                    url = new URL(urlDef);

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                    return WebServices.convertStreamToString(in);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String res) {
                try {
                    // on parse le ficheir et on récupère la valeur qui nous intéresse
                    JSONObject json = new JSONObject(res);
                    JSONObject meteoObs = new JSONObject(json.getString("weatherObservation"));

                    String clouds = meteoObs.getString("clouds");
                    String temperature = meteoObs.getString("temperature");
                    String humidity = meteoObs.getString("humidity");

                    tempTxt.setText(getString(R.string.tmp)+" " + temperature + "°C");
                    humTxt.setText(getString(R.string.hmd)+" " + humidity+ "%");
                    cloudTxt.setText(getString(R.string.cld)+" " + clouds);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentLocation);
    }
}
