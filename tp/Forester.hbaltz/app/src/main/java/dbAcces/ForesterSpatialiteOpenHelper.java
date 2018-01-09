package dbAcces;

import android.content.Context;

import java.io.IOException;

import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import jsqlite.Exception;

/**
 * Created by prof on 01/02/17.
 */

public class ForesterSpatialiteOpenHelper extends SpatialiteOpenHelper {

    public static final String DB_NAME = "Forester.db";
    public static final int DB_VERSION = 1;

    public ForesterSpatialiteOpenHelper(Context context) throws Exception, IOException {
        super(context, DB_NAME, DB_VERSION);
    }

    @Override
    public void onCreate(SpatialiteDatabase db) throws jsqlite.Exception {
        // Drops if exists
        db.exec(getQueryDropForester());
        db.exec(getQueryDropPOI());
        db.exec(getQueryDropSec());

        db.exec(getQueryCreateForester());
        db.exec(getQueryCreatePOI());
        db.exec(getQueryCreateSec());

        db.exec(getQueryIndexForPoi());
        db.exec(getQueryIndexForSec());

        db.exec(getQueryPoiGeometryColumn());
        db.exec(getQuerySecGeometryColumn());
    }

    @Override
    public void onUpgrade(SpatialiteDatabase db, int oldVersion, int newVersion) throws Exception {

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getQueryCreateForester() {
        return "CREATE TABLE Forester ("
                + "id Integer PRIMARY KEY AUTOINCREMENT, "
                + "FirstName string NOT NULL, "
                + "LastName string NOT NULL, "
                + "Serial string NULL "
                + ");";
    }

    public static String getQueryCreatePOI(){
        return "CREATE TABLE PointOfInterest ("+
                "ID integer PRIMARY KEY AUTOINCREMENT, "+
                "ForesterID integer NOT NULL, "+
                "Name string NOT NULL, "+
                "Description string, "+
                "CONSTRAINT FK_poi_forester "+
                "FOREIGN KEY (foresterID) " +
                "REFERENCES forester (id) "+
                ");";
    }



    public static String getQueryCreateSec(){
        return "CREATE TABLE Sector ("+
                "ID integer PRIMARY KEY AUTOINCREMENT, "+
                "ForesterID integer NOT NULL, "+
                "Name string NOT NULL, "+
                "Description string, "+
                "CONSTRAINT FK_sec_forester "+
                "FOREIGN KEY (foresterID) " +
                "REFERENCES forester (id) "+
                ");";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getQueryIndexForPoi(){
        return "CREATE INDEX IDX_poi_forester " +
                "ON PointOfInterest (ForesterID);";
    }

    public static String getQueryIndexForSec(){
        return "CREATE INDEX IDX_sec_forester " +
                "ON Sector (ForesterID);";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getQueryPoiGeometryColumn(){
        return "SELECT AddGeometryColumn(" +
                "'PointOfInterest', 'position', 4326, 'POINT', 'XY', 0);";
    }

    public static String getQuerySecGeometryColumn(){
        return "SELECT AddGeometryColumn(" +
                "'Sector', 'position', 4326, 'POLYGON', 'XY', 0);";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getQueryDropForester() {
        return "DROP TABLE IF EXISTS Forester;";
    }

    public static String getQueryDropPOI() {
        return "DROP TABLE IF EXISTS PointOfInterest;";
    }

    public static String getQueryDropSec() {
        return "DROP TABLE IF EXISTS Sector;";
    }

}
