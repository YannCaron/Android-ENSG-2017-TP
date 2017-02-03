package eu.ensg.forester;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import eu.ensg.forester.db.ForesterSpatialiteOpenHelper;
import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import jsqlite.Exception;
import jsqlite.Stmt;


public class LoginActivity extends AppCompatActivity implements Constants {

    // les vues
    private EditText editSerial;
    private Button buttonLogin;
    private Button buttonCreate;

    // les préférences
    private SharedPreferences preferences;

    private SpatialiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Récupère les instances des vues
        editSerial = (EditText) findViewById(R.id.serial);
        buttonLogin = (Button) findViewById(R.id.login);
        buttonCreate = (Button) findViewById(R.id.create);

        // Récupère les préférences
        preferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);

        // gére si l'extra a été passé en paramètre
        String s = getIntent().getStringExtra(EXTRA_SERIAL);

        if (s != null) {
            editSerial.setText(s);
        } else {
            editSerial.setText(preferences.getString(PREFERENCE_SERIAL, ""));
        }

        // gère les événements
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login_onClick(v);
            }
        });

        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                create_onClick(v);
            }
        });

        // database
        initDatabase();

    }

    private void login_onClick(View view) {
        // TODO check if in database

        if (isInDatabase("toto", "titi", editSerial.getText().toString())) {

            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Nope!", Toast.LENGTH_LONG);
            Intent intent = new Intent(this, CreateUserActivity.class);
            startActivity(intent);
        }

    }

    private void create_onClick(View view) {
        Intent intent = new Intent(this, CreateUserActivity.class);
        startActivity(intent);
    }

    private void initDatabase() {
            try {
                SpatialiteOpenHelper helper = new ForesterSpatialiteOpenHelper(this);
                database = helper.getDatabase();
            } catch (jsqlite.Exception | IOException e) {
                e.printStackTrace();
                Toast.makeText(this,
                        "Cannot initialize database !", Toast.LENGTH_LONG).show();
                System.exit(0);
            }
    }

    private boolean isInDatabase(String name, String surname, String serial) {
/*
*             "CREATE TABLE Forester ( ID integer PRIMARY KEY AUTOINCREMENT,\n" +
            "FirstName string NOT NULL,\n" +
            "LastName string NOT NULL,\n" +
            "Serial string NULL)";*/

        String[] cols= new String[]{"Serial"};
        Stmt st= null;
        try {
            st= database.prepare("SELECT id FROM Forester WHERE Serial="+serial+";");
            if (st.step()) {
                int no= st.column_int(0);
                Log.d("QUERY executed return=", String.valueOf(no));
                if(no>0) return true;

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (st != null)
                    st.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }
/*
    public POJO read(Book POJO) {
// columns
        String[] allColumns = new String[]{COL_ID, COL_TITLE, COL_ISBN,
                COL_NBPAGE};
        Expression de recherche
// clause
        String clause = COL_ID + " = ?";
        String[] clauseArgs = new String[]{String.valueOf(POJO.getId())};
// select query
        Cursor cursor = getDB().query(TABLE_NAME, allColumns, "ID = ?",
// read cursor
                cursor.moveToFirst();
        POJO.setTitle(cursor.getString(1));
        POJO.setIsbn(cursor.getString(2));
        POJO.setNbPage(cursor.getInt(3));
        cursor.close();
        clauseArgs, null, null, null);
        return POJO;
    }
    */

}
