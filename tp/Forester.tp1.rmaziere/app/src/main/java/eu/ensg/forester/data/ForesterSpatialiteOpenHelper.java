package eu.ensg.forester.data;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import jsqlite.Exception;

/**
 * Created by prof on 02/02/17.
 */

public class ForesterSpatialiteOpenHelper extends SpatialiteOpenHelper{

    public static final String DB_FILE_NAME = "Spatialite.sqlite";
    public static final int VERSION = 1;
    public static final int GPS_SRID = 4326;

    public ForesterSpatialiteOpenHelper(Context context, String name, int version) throws Exception, IOException {
        super(context, DB_FILE_NAME, VERSION);
    }

    public static String getQueryDrop(String tableName) {
        return "DROP TABLE IF EXISTS " + tableName + ";";
    }

    public ForesterSpatialiteOpenHelper(Context context) throws IOException, Exception {
        super(context, DB_FILE_NAME, VERSION);
    }

    @Override
    public void onCreate(SpatialiteDatabase db) throws Exception {
        Log.i(ForesterSpatialiteOpenHelper.class.getName(), "Database Creation");
        // table forester
        getDatabase().exec("CREATE TABLE Forester (\n" +
                "ID integer PRIMARY KEY AUTOINCREMENT, \n" +
                "FirstName string NOT NULL, \n" +
                "LastName string NOT NULL, \n" +
                "Serial string NO NULL,\n" +
                "CONSTRAINT Ukey UNIQUE (Serial)\n" +
                ");");

        // table point of interest
        getDatabase().exec("CREATE TABLE PointOfInterest (\n" +
                "ID integer PRIMARY KEY AUTOINCREMENT, \n" +
                "ForesterID integer NOT NULL,\n" +
                "Name string NOT NULL, \n" +
                "Description string,\n" +
                "CONSTRAINT FK_poi_forester\n" +
                "   FOREIGN KEY (foresterID)\n" +
                "   REFERENCES forester (id)\n" +
                ");");

        getDatabase().exec("SELECT AddGeometryColumn('PointOfInterest', 'Position', " + GPS_SRID + ", 'POINT', 'XY', 1);");

        getDatabase().exec("INSERT INTO PointOfInterest (Name, Description, ForesterID, Position) VALUES('Paris', 'This is a test point', 0, ST_GeomFromText('POINT (48.856458 2.347882)', 4326));");

        // table district
        getDatabase().exec("CREATE TABLE District (\n" +
                "ID integer PRIMARY KEY AUTOINCREMENT, \n" +
                "ForesterID integer NOT NULL,\n" +
                "Name string NOT NULL, \n" +
                "Description string,\n" +
                "CONSTRAINT FK_district_forester\n" +
                "   FOREIGN KEY (foresterID)\n" +
                "   REFERENCES forester (id)\n" +
                ");");

        getDatabase().exec("SELECT AddGeometryColumn('District', 'Area', " + GPS_SRID + ", 'POLYGON', 'XY', 1);");

        //Create First user
        getDatabase().exec("INSERT INTO Forester (Serial, FirstName, LastName)\n" +
                "VALUES (1, 'Romain', 'MAZIERE');");
    }

    @Override
    public void onUpgrade(SpatialiteDatabase db, int oldVersion, int newVersion) throws Exception {

    }
}