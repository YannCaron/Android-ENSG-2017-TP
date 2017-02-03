package eu.ensg.forester;

import android.content.Intent;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

import dbAcces.ForesterSpatialiteOpenHelper;
import eu.ensg.spatialite.SpatialiteDatabase;
import jsqlite.Exception;

public class CreateUserActivity extends AppCompatActivity implements Constants {

    // les vues
    private EditText editFirstName;
    private EditText editLastName;
    private EditText editSerial;
    private Button buttonCreate;

    // db
    private SpatialiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_user);

        // Récupère les instances des vues
        editFirstName = (EditText)findViewById(R.id.first_name);
        editLastName = (EditText)findViewById(R.id.last_name);
        editSerial = (EditText)findViewById(R.id.serial);
        buttonCreate = (Button)findViewById(R.id.create);

        // gère les événements
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                create_onClick(v);
            }
        });

        // database
        initDatabase();

    }

    private void create_onClick(View view) {
        // Récupération des attributs
        String frstName = editFirstName.getText().toString();
        String lstName = editLastName.getText().toString();
        String serialNB = editSerial.getText().toString();

        if(!frstName.isEmpty() && !lstName.isEmpty() && !serialNB.isEmpty()){
            try {
                database.exec("INSERT INTO Forester (FirstName, LastName, Serial) " +
                                "VALUES ("+
                                DatabaseUtils.sqlEscapeString(frstName) + ", " +
                                DatabaseUtils.sqlEscapeString(lstName) + ", " +
                                DatabaseUtils.sqlEscapeString(serialNB) +
                            ");");

                // On lance l'activité login
                Intent i = new Intent(this, LoginActivity.class);
                i.putExtra(EXTRA_SERIAL, serialNB); // Voir Constants.java
                startActivity(i);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Initializes the connection to the database
     */
    private void initDatabase() {
        try {
            database = new ForesterSpatialiteOpenHelper(this).getDatabase();
        } catch (jsqlite.Exception | IOException e) {
            e.printStackTrace();
        }
    }
}
