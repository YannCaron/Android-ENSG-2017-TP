package eu.ensg.forester;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;

import eu.ensg.forester.data.ForesterSpatialiteOpenHelper;
import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;


public class LoginActivity extends AppCompatActivity implements Constants {

    // les vues
    private EditText editSerial;
    private Button buttonLogin;
    private Button buttonCreate;

    // les préférences
    private SharedPreferences preferences;

    // la database
    private SpatialiteDatabase database;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // renvoie l'activité de l'application
    private void login_onClick(View view) {
        String serial = editSerial.getText().toString();
        Stmt stmt = null;
        try {
            // test si le serial est dans la base de données
            stmt = database.prepare("SELECT * FROM Forester where Serial = " +
                    DatabaseUtils.sqlEscapeString(serial));
            if (stmt.step()) {
                int foresterID = stmt.column_int(0);


                // garder le serial en mémoire de l'appareil
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PREFERENCE_SERIAL, serial);
                editor.commit();
                editor.apply();

                // envoie de l'activité si c'est le cas
                Intent intent = new Intent(this, MapsActivity.class);
                intent.putExtra(EXTRA_FORESTER_ID, foresterID);
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.usernotfind, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, CreateUserActivity.class);
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    // renvoie l'activité de création de compte
    private void create_onClick(View view) {
        Intent intent = new Intent(this, CreateUserActivity.class);
        startActivity(intent);
    }

    private void initDatabase() {
        SpatialiteOpenHelper helper = null;
        try {
            helper = new ForesterSpatialiteOpenHelper(this);
            database = helper.getDatabase();
        } catch (Exception | IOException e) {
            e.printStackTrace();
        }

    }

}