package com.rp.myapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rp.myapplication.model.Contact;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ManageEmergencyContactsActivity extends AppCompatActivity {

    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    private static final int PICK_CONTACT = 2;
    private static final String EMERGENCY_CONTACTS_LIST = "emergencyContactsList";

    private ArrayList<Contact> emergencyContactsArrayList;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_emergency_contacts);

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        getArrayList();

        //Checa se temos a permissão de acessar os contatos
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }

        Button registerEmergencyContactButton = (Button) findViewById(R.id.register_emergency_contact_button);
        ListView emergencyContactsList = (ListView) findViewById(R.id.emergencyContactsList);

        registerEmergencyContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentResolver resolver = getContentResolver();
                resolver.query(ContactsContract.Contacts.CONTENT_URI, null ,null , null, null);
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT);
            }
        });
    }

    protected void onActivityResult(int reqCode, int resultCode, Intent data){
        super.onActivityResult(reqCode, resultCode, data);
        if(resultCode != RESULT_CANCELED && data != null) {
            if (reqCode == PICK_CONTACT) {
                if (resultCode == RESULT_OK) {
                        Uri contactData = data.getData();
                        Cursor contacts = getContentResolver().query(contactData, null, null, null, null);

                    if (contacts.moveToFirst()) {
                        String contactId = contacts.getString(contacts.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String name = contacts.getString(contacts.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                        String number = new String();

                        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);

                        if (phones == null)
                            displayMessage(ErrorMessages.noPhoneNumber);


                        while (phones.moveToNext()) {
                            int type = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                                number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                break;
                            }
                        }

                        if(number.isEmpty())
                            displayMessage(ErrorMessages.noPhoneNumber);
                        else if (!checkIfNewPhone(number)) {
                            displayMessage(ErrorMessages.contactAlreadyExists);
                        } else {
                            emergencyContactsArrayList.add(new Contact(name, number));
                            saveArrayList();
                        }
                        number = "";
                        name = "";
                        phones.close();
                    }
                    contacts.close();
                }

            }
        }
    }

    protected boolean checkIfNewPhone(String number){
        for(Contact contact : emergencyContactsArrayList){
            if(contact.getPhoneNumber().equals(number))
                return false;
        }
        return true;
    }

    protected void displayMessage(CharSequence message){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }


    public void saveArrayList(){
        Gson gson = new Gson();
        String json = gson.toJson(emergencyContactsArrayList);
        editor.putString(EMERGENCY_CONTACTS_LIST, json);
        editor.apply();
    }

    public void getArrayList(){
        Gson gson = new Gson();
        String json = sharedPref.getString(EMERGENCY_CONTACTS_LIST, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        if (gson.fromJson(json, type) == null){
            emergencyContactsArrayList = new ArrayList<Contact>();
        } else {
            emergencyContactsArrayList = gson.fromJson(json, type);
            updateContactsListView();
        }
    }

    public void updateContactsListView(){


    }
}