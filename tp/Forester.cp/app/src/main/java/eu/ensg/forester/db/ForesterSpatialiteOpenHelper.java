package eu.ensg.forester.db;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import eu.ensg.forester.Constants;
import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import jsqlite.Exception;

/**
 * Created by prof on 02/02/17.
 */

public class ForesterSpatialiteOpenHelper extends SpatialiteOpenHelper implements Constants {
    public static final String DB_NAME= "Forester.sqlite";
    public static final int DB_VERSION= 1;
    public static final String CREATE_FORESTER=
            "CREATE TABLE Forester ( ID integer PRIMARY KEY AUTOINCREMENT,\n" +
            "FirstName string NOT NULL,\n" +
            "LastName string NOT NULL,\n" +
            "Serial string NULL)";


    public static final String CREATE_POI="CREATE TABLE PointOfInterest (\n" +
            "ID integer PRIMARY KEY AUTOINCREMENT,\n" +
            "ForesterID integer NOT NULL,\n" +
            "Name string NOT NULL,\n" +
            "Description string,\n" +
            "CONSTRAINT FK_poi_forester\n" +
            "FOREIGN KEY (foresterID)\n" +
            "REFERENCES forester (id)\n" +
            ");";

    public static final String CREATE_SECTOR="CREATE TABLE Sector (\n" +
            "ID integer PRIMARY KEY AUTOINCREMENT,\n" +
            "ForesterID integer NOT NULL,\n" +
            "Name string NOT NULL,\n" +
            "Description string,\n" +
            "CONSTRAINT FK_poi_forester\n" +
            "FOREIGN KEY (foresterID)\n" +
            "REFERENCES forester (id)\n" +
            ");";

    public static final String ADD_COLPOI="SELECT \n" +
            "AddGeometryColumn('PointOfInterest'\n" +
            ", 'position', 4326, 'POINT', 'XY',\n" +
            "0);";

    public static final String ADD_COLSECTOR= "SELECT \n" +
            "AddGeometryColumn('Sector',\n" +
            "'Area', 4326, 'POLYGON', 'XY', 0);";

    public ForesterSpatialiteOpenHelper(Context context) throws Exception, IOException {
        super(context, DB_NAME, DB_VERSION);
    }

    @Override
    public void onCreate(SpatialiteDatabase db) throws Exception {
        db.exec(CREATE_FORESTER);
        db.exec(CREATE_POI);
        db.exec(ADD_COLPOI);
        db.exec(CREATE_SECTOR);
        db.exec(ADD_COLSECTOR);
        Log.d("BD", "create");
    }

    @Override
    public void onUpgrade(SpatialiteDatabase db, int oldVersion, int newVersion) throws Exception {
        /*db.exec(ADD_COLPOI);
        db.exec(ADD_COLSECTOR);*/
        Log.d("BD", "upgrade");
    }


}
