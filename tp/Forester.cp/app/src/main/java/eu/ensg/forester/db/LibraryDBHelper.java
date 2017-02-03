package eu.ensg.forester.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by prof on 02/02/17.
 */

public class LibraryDBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME= "Library.db";
    public static final int DB_VERSION= 1;

    public LibraryDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static String getQueryCreate() {
        return "CREATE...";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(getQueryCreate());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int delta= newVersion-oldVersion;

    }
}
