package eu.ensg.forester.data;

import android.content.Context;

import java.io.IOException;

import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import jsqlite.Exception;

/**
 * Created by prof on 02/02/17.
 */

public class ForesterSpatialiteOpenHelper extends SpatialiteOpenHelper {

    public static final String CREATE_FORESTER = "" +
            "CREATE TABLE Forester (" +
            "Id Integer PRIMARY KEY AUTOINCREMENT, " +
            "FirstName String NOT NULL, " +
            "LastName String NOT NULL, " +
            "Serial String NOT NULL" +
            ");";

    public static final String CREATE_POI = "" +
            "CREATE TABLE PointOfInterest (" +
            "ID Integer PRIMARY KEY AUTOINCREMENT, " +
            "ForesterID Integer NOT NULL, " +
            "Name String NOT NULL, " +
            "Description String, " +
            "CONSTRAINT FK_poi_forester FOREIGN KEY (foresterID) REFERENCES forester (id)" +
            ");";

    public static final String CREATE_DISTRICT = "" +
            "CREATE TABLE District (" +
            "ID Integer PRIMARY KEY AUTOINCREMENT, " +
            "ForesterID Integer NOT NULL, " +
            "Name String NOT NULL, " +
            "Description String, " +
            "CONSTRAINT FK_poi_forester FOREIGN KEY (foresterID) REFERENCES forester (id)" +
            ");";

    public static final String ADD_POI = "" +
            "SELECT AddGeometryColumn('PointOfInterest', 'position', 4326, 'POINT', 'XY',0);";

    public static final String ADD_DISTRICT = "" +
            "SELECT AddGeometryColumn('District', 'Area', 4326, 'POLYGON', 'XY',0);";

    public ForesterSpatialiteOpenHelper(Context context) throws Exception, IOException {
        super(context, "forester.sqlite", 1);
    }

    @Override
    public void onCreate(SpatialiteDatabase db) throws Exception {
        db.exec(CREATE_FORESTER);
        db.exec(CREATE_POI);
        db.exec(CREATE_DISTRICT);
        db.exec(ADD_POI);
        db.exec(ADD_DISTRICT);
    }

    @Override
    public void onUpgrade(SpatialiteDatabase db, int oldVersion, int newVersion) throws Exception {

    }
}
