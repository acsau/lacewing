package com.csa.ttn.firefly1;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity {

    public static String PARENT_DIRECTORY = "Firefly"; /**< Name of the directory that stores CSV files */
    public static String userName = null;
    Toast currentToast = null;

    /*
	 * The elements of the list view stored in "ListElements". Update rather
	 * than starting a new list each time a new entry is added/removed as this
	 * re-orders the list
	 */
    List<Map<String, String>> userNamesList = new ArrayList<>();
    SimpleAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Create string for 'about this app' TextView
        String text = "Firefly app v";
        try {
            text = text + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        text = text + " for Pixie v1.3; April 2018\nAnselm Au, Imperial College London";

        // Update the TextView
        TextView tv = (TextView)findViewById(R.id.about_app);
        tv.setText(text);

        /*
		 * Create an adapter for the listview and set the adapter. userNamesList
		 * is populated in onResume (when the activity is resumed, i.e. on
		 * returning to the app)
		 */
        listAdapter = new SimpleAdapter(this, userNamesList, android.R.layout.simple_list_item_1, new String[] { "name" },
                new int[] { android.R.id.text1 });

        ListView listView = (ListView) findViewById(R.id.listView_Users);
        listView.setAdapter(listAdapter);

        //Start MainActivity upon selection of Patient Reference Code
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {

                Map<String, String> map;
                map = (Map<String, String>) parent.getItemAtPosition(pos);
                userName = map.get("name");

                Intent access = new Intent(StartActivity.this, MainActivity.class);
                startActivity(access);
            }
        });

        //When swiping a user name, show a dialog asking the user if they wish to delete the user
        SwipeDismiss touchListener = new SwipeDismiss(listView, new SwipeDismiss.DismissCallbacks() {

            @Override
            public void onDismiss(ListView listView, int[] positions) {
                final String userName;
                final Object listItem;
                userName = ((Map<String, String>) listAdapter.getItem(positions[0])).get("name");
                listItem = listAdapter.getItem(positions[0]);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                File userDirectory = new File(Environment.getExternalStorageDirectory(), "/" + PARENT_DIRECTORY + "/"
                                        + userName);
                                DeleteDirectory(userDirectory);
                                userNamesList.remove(listItem);
                                listAdapter.notifyDataSetChanged();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
                builder.setMessage("Confirm to delete all data associated with reference " + "\"" + userName + "\"?")
                        .setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
            }

            @Override
            public boolean canDismiss(int position) {
                return true;
            }
        });
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
    }

    @Override
    protected void onResume() {
        super.onResume();

        userNamesList.clear();
        for (String name : GetUserNames()) {
            Map<String, String> map = new HashMap<>();
            map.put("name", name);
            userNamesList.add(map);
        }
        listAdapter.notifyDataSetChanged();
    }

    void DeleteDirectory(File file) {
        if (file.isDirectory())
            for (File child : file.listFiles())
                DeleteDirectory(child);

        file.delete();
    }

    /** Get user names from the parent directory that stores user data */
    public List<String> GetUserNames() {

        List<String> userNames = new ArrayList<>();

        File parentDirectory = new File(Environment.getExternalStorageDirectory(), "/" + PARENT_DIRECTORY);

        parentDirectory.mkdir();
        File[] files = parentDirectory.listFiles();

        if (files != null)
            for (File file : files) {
                if (file.isDirectory()) {
                    userNames.add(file.getName());
                }
            }

        return userNames;

    }
    /** Create Dialog to add a user (*@\label{mainActivity:addUser}@*) */
    public void AddUser(View view) {
        Builder dialogBuilder = new Builder(this);
        final EditText editText = new EditText(this);

        showToast("New directory will not be viewable on a computer until the phone is rebooted");

        // set up linear layout that contains an edit text and has padding at the sides
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setPadding(getResources().getDimensionPixelOffset(R.dimen.activity_vertical_margin), 0, getResources()
                .getDimensionPixelOffset(R.dimen.activity_vertical_margin), 0);
        editText.setHint("Enter Full Reference");
        editText.setSingleLine();
        linearLayout.addView(editText);
        dialogBuilder.setView(linearLayout);
        dialogBuilder.setTitle("New Reference Code");

        dialogBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {


                String userName = editText.getText().toString();
                if (!userName.equals("")) {
                    Map<String, String> map = new HashMap<>();
                    map.put("name", userName);
                    userNamesList.add(map);
                    listAdapter.notifyDataSetChanged();

                    File newDirectory = new File(Environment.getExternalStorageDirectory(), "/" + PARENT_DIRECTORY + "/" + userName);
                    newDirectory.mkdir();

                    showToast("Tap entry to begin \nSwipe to delete records");
                }
                else{
                    showToast("Please enter a valid code");
                }
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        dialogBuilder.show();
    }

    /**
     * Function to immediately update the contents of a toast, bypassing the SHORT and LONG intervals
     * @param text
     */
    public void showToast (String text){
        if(currentToast == null)
        {
            currentToast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        }
        currentToast.setText(text);
        currentToast.setDuration(Toast.LENGTH_SHORT);
        currentToast.show();
    }

}
