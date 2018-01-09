package eu.ensg.forester.data;

import android.content.Context;

import java.io.IOException;

import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import jsqlite.Exception;

public class ForesterSpatialiteOpenHelper extends SpatialiteOpenHelper {

    public static final String CREATE_FORESTER=""+
            "CREATE TABLE Forester(" +
            "   id Integer PRIMARY KEY AUTOINCREMENT," +
            "   firstName String NOT NULL," +
            "   lastName String NOT NULL," +
            "   serial String NOT NULL" +
            ");";

    public static final String CREATE_POI=""+
            "CREATE TABLE PointOfInterest(" +
            "   id Integer PRIMARY KEY AUTOINCREMENT," +
            "   foresterID Integer NOT NULL," +
            "   name String NOT NULL," +
            "   description String," +
            "   CONSTRAINT FK_poi_forester" +
            "       FOREIGN KEY (foresterID)" +
            "       REFERENCES forester (id)" +
            ");";

    public static final String ADD_POI_POSITION=""+
            "SELECT AddGeometryColumn('PointOfInterest', 'position', 4326, 'POINT', 'XY',0);";

    public static final String CREATE_SECTOR=""+
            "CREATE TABLE District(" +
            "   id Integer PRIMARY KEY AUTOINCREMENT," +
            "   foresterID Integer NOT NULL," +
            "   name String NOT NULL," +
            "   description String," +
            "   CONSTRAINT FK_poi_forester" +
            "       FOREIGN KEY (foresterID)" +
            "       REFERENCES forester (id)" +
            ");";

    public static final String ADD_SECTOR_POSITION=""+
            "SELECT AddGeometryColumn('District','Area', 4326, 'POLYGON', 'XY', 0);";

    public static final String UPGRADE_FORESTER="";

    public ForesterSpatialiteOpenHelper(Context context) throws Exception, IOException {
        super(context, "forester.sqlite", 1);
    }

    @Override
    public void onCreate(SpatialiteDatabase db) throws Exception {
        db.exec(CREATE_FORESTER);
        db.exec(CREATE_POI);
        db.exec(ADD_POI_POSITION);
        db.exec(CREATE_SECTOR);
        db.exec(ADD_SECTOR_POSITION);


    }

    @Override
    public void onUpgrade(SpatialiteDatabase db, int oldVersion, int newVersion) throws Exception {
        //db.exec(UPGRADE_FORESTER);

    }
}
