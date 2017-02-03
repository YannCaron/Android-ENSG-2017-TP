package eu.ensg.forester.data;

import android.content.Context;

import java.io.IOException;

import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import jsqlite.Exception;

/**
 * Created by prof on 02/02/17.
 */

public class ForesterSpatialiteOpenHelper extends SpatialiteOpenHelper{

    public static final String CREATE_FORESTER = "" +
            "CREATE TABLE Forester ( " +
            "id integer PRIMARY KEY AUTOINCREMENT, " +
            "firstName string NOT NULL, " +
            "lastName string NOT NULL, " +
            "serial string NOT NULL " +
            ");";
    public static final String CREATE_POINT = "" +
            "CREATE TABLE PointOfInterest ( " +
            "id integer PRIMARY KEY AUTOINCREMENT, " +
            "foresterID integer NOT NULL, " +
            "name string NOT NULL, " +
            "description string, " +
            "CONSTRAINT FK_poi_forester " +
            "FOREIGN KEY (foresterID) " +
            "REFERENCES forester (id) " +
            ");";
    public static final String CREATE_POINT_GEOM = "" +
            "SELECT AddGeometryColumn('PointOfInterest', " +
            "'position', 4326, 'POINT', 'XY', 0); ";
    public static final String CREATE_SECTOR = "" +
            "CREATE TABLE Sector ( " +
            "id integer PRIMARY KEY AUTOINCREMENT, " +
            "foresterID integer NOT NULL, " +
            "name string NOT NULL, " +
            "description string, " +
            "CONSTRAINT FK_poi_forester " +
            "FOREIGN KEY (foresterID) " +
            "REFERENCES forester (id) " +
            ");";
    public static final String CREATE_SECTOR_GEOM = "" +
            "SELECT AddGeometryColumn('Sector', " +
            "'Area', 4326, 'POLYGON', 'XY', 0);";

    public ForesterSpatialiteOpenHelper(Context context) throws Exception, IOException {
        super(context, "Forester.sqlite", 1);
    }

    @Override
    public void onCreate(SpatialiteDatabase db) throws Exception {
        db.exec(CREATE_FORESTER);
        db.exec(CREATE_POINT);
        db.exec(CREATE_POINT_GEOM);
        db.exec(CREATE_SECTOR);
        db.exec(CREATE_SECTOR_GEOM);

    }

    @Override
    public void onUpgrade(SpatialiteDatabase db, int oldVersion, int newVersion) throws Exception {

    }
}
