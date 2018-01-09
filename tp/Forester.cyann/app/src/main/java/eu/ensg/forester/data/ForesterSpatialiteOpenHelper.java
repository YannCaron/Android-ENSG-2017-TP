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
            "   id Integer PRIMARY KEY AUTOINCREMENT," +
            "   firstName String NOT NULL," +
            "   lastName String NOT NULL," +
            "   serial String NOT NULL" +
            ");";

    public static final String CREATE_POI = "" +
            "CREATE TABLE PointOfInterest (" +
            "   id integer PRIMARY KEY AUTOINCREMENT," +
            "   foresterID integer NOT NULL," +
            "   name string NOT NULL," +
            "   description string," +
            "   CONSTRAINT FK_poi_forester" +
            "       FOREIGN KEY (foresterID) REFERENCES forester (id)" +
            ");";

    public static final String CREATE_POI_GEOMETRY = "" +
            "SELECT " +
            "AddGeometryColumn('PointOfInterest', 'position', 4326, 'POINT', 'XY', 0);";

    public static final String CREATE_SECTOR = "" +
            "CREATE TABLE Sector (" +
            "   id integer PRIMARY KEY AUTOINCREMENT," +
            "   foresterID integer NOT NULL," +
            "   name string NOT NULL," +
            "   description string," +
            "   CONSTRAINT FK_sector_forester" +
            "       FOREIGN KEY (foresterID) REFERENCES forester (id)" +
            ");";

    public static final String CREATE_SECTOR_GEOMETRY = "" +
            "SELECT " +
            "AddGeometryColumn('Sector', 'area', 4326, 'POLYGON', 'XY', 0);";


    public ForesterSpatialiteOpenHelper(Context context) throws Exception, IOException {
        super(context, "forester.sqlite", 1);
    }

    @Override
    public void onCreate(SpatialiteDatabase db) throws Exception {
        db.exec(CREATE_FORESTER);
        db.exec(CREATE_POI);
        db.exec(CREATE_POI_GEOMETRY);
        db.exec(CREATE_SECTOR);
        db.exec(CREATE_SECTOR_GEOMETRY);
    }

    @Override
    public void onUpgrade(SpatialiteDatabase db, int oldVersion, int newVersion) throws Exception {

    }
}
