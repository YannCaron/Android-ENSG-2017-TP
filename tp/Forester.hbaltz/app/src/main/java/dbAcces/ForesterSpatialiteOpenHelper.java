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

    public ForesterSpatialiteOpenHelper(Context context, String name, int version) throws Exception, IOException {
        super(context, name, version);
    }

    public static String getQueryCreateForester() {
        return "CREATE TABLE Forester ("
                + "id Integer PRIMARY KEY AUTOINCREMENT, "
                + "FirstName string NOT NULL, "
                + "LastName string NOT NULL, "
                + "Serial string NULL "
                + ");";
    }
    public static String getQueryDropForester() {
        return "DROP TABLE IF EXISTS Forester;";
    }

    @Override
    public void onCreate(SpatialiteDatabase db) throws Exception {
        db.exec(getQueryCreateForester());
    }

    @Override
    public void onUpgrade(SpatialiteDatabase db, int oldVersion, int newVersion) throws Exception {

    }
}
