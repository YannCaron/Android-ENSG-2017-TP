package eu.ensg.forester;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import eu.ensg.forester.data.ForesterSpatialiteOpenHelper;
import eu.ensg.spatialite.SpatialiteDatabase;
import eu.ensg.spatialite.SpatialiteOpenHelper;
import jsqlite.Stmt;

public class CreateUserActivity extends AppCompatActivity implements Constants {

    // les vues
    private EditText editFirstName;
    private EditText editLastName;
    private EditText editSerial;
    private Button buttonCreate;
    private String serial;

    private String firstName;
    private String lastName;

    // les préférences
    private SharedPreferences preferences;

    // database
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

        serial = getIntent().getStringExtra("serial");
        Log.i(CreateUserActivity.class.getName(), "serial : " + serial);
        editSerial.setText(serial);

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
        serial = editSerial.getText().toString();
        firstName = editFirstName.getText().toString();
        lastName= editLastName.getText().toString();
        createUser();
    }

    private void initDatabase() {
        SpatialiteOpenHelper helper = null;
        try {
            helper = new ForesterSpatialiteOpenHelper(this);
            database = helper.getDatabase();
        } catch (jsqlite.Exception | IOException e) {
            e.printStackTrace();
            Toast.makeText(this,
                    R.string.databaseInitialiastionError, Toast.LENGTH_LONG).show();
            System.exit(0);
        }
    }

    private void createUser(){
        Log.i(CreateUserActivity.class.getName(), "createUser into DB");

        Log.i(CreateUserActivity.class.getName(), "serial : " + serial);
        Log.i(CreateUserActivity.class.getName(), "firstName : " + firstName);
        Log.i(CreateUserActivity.class.getName(), "lastName : " + lastName);


        Log.i(CreateUserActivity.class.getName(), "INSERT INTO Forester (Serial, FirstName, LastName) VALUES(" + (serial) + ", " + (firstName) + ", " + (lastName) + ")");

        try {
            //Stmt stmt = database.prepare("INSERT INTO Forester (Serial, FirstName, LastName) VALUES(" + DatabaseUtils.sqlEscapeString(serial) + ", '" + DatabaseUtils.sqlEscapeString(firstName) + "', '" + DatabaseUtils.sqlEscapeString(lastName) + "');");
            database.exec("INSERT INTO Forester (Serial, FirstName, LastName) VALUES(" + (serial) + ", '" + (firstName) + "', '" + (lastName) + "');");

            Log.i(CreateUserActivity.class.getName(), "User created into DB");

            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("serial", serial);
            startActivity(intent);
        }
        catch (jsqlite.Exception e) {
            e.printStackTrace();
        }
    }
}
