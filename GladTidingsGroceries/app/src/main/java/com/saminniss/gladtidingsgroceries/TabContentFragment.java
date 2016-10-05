package com.saminniss.gladtidingsgroceries;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This fragment decides what content will appear on each tab.
 */
public class TabContentFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    SQLiteDatabase db = null;
    Cursor cursor = null;
    Context fragment_context;
    TextView home_text_view;

    private static final String ARG_SECTION_NUMBER = "section_number";

    public TabContentFragment() {

        Log.i("times", "fragment constructor");
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static TabContentFragment newInstance(int sectionNumber) {

        Log.i("times", "fragment new instance");
        TabContentFragment fragment = new TabContentFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("times", "on create view");

        switch (getArguments().getInt(ARG_SECTION_NUMBER))
        {
            case 1:
                View home_view = inflater.inflate(R.layout.fragment_home, container, false);
                home_text_view = (TextView) home_view.findViewById(R.id.section_label);
                String time_now;
                Calendar cal = new GregorianCalendar();
                time_now = Long.toString(cal.getTimeInMillis());
                home_text_view.setText("The time is: " + time_now + "\nWhich is "
                        +cal.getTime().toString());

                home_text_view.setTextColor(Color.parseColor("#000000"));

                new DatabaseSetup().execute("populate home");
                return home_view;

            case 2:
                View request_view = inflater.inflate(R.layout.fragment_request, container, false);
                TextView request_text_view = (TextView) request_view.findViewById(R.id.section_label);
                //request_text_view.setText("case 2");
                return request_view;

            case 3:
                View inventory_view = inflater.inflate(R.layout.fragment_inventory, container, false);
                WebView inventory_web_view = (WebView) inventory_view.findViewById(R.id.webView);
                WebSettings web_settings = inventory_web_view.getSettings();
                web_settings.setDomStorageEnabled(true);
                web_settings.setJavaScriptEnabled(true);
                inventory_web_view.loadUrl("http://www.gtcacademy.org/inventory/");
                return inventory_view;
        }
        return null;
    }

    private class DatabaseSetup extends AsyncTask<String, Void, Boolean> {

        String todo = null;

        protected Boolean doInBackground(String... input) {
            todo = input[0];
            switch (input[0]) {

                case "populate home":
                    // if we have the cursor already, don't setup again
                    if (cursor != null)
                        break;
                case "setup":
                    Db db_helper = new Db(Home.home_context);

                    // Gets the data repository in write mode
                    db = db_helper.getWritableDatabase();

                    cursor = GetRequests(db);

                    HttpURLConnection conn = null;
                    String url_str = "";
                    String data_str = "";
                    try {
                        String urlParameters  = "todo=get_requests";
                        byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
                        int    postDataLength = postData.length;
                               url_str        = "http://www.gtcacademy.org/groceries/app_data.php";
                        URL    url            = new URL( url_str );
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoOutput( true );
                        conn.setInstanceFollowRedirects( false );
                        conn.setRequestMethod( "POST" );
                        conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                        conn.setRequestProperty( "charset", "utf-8");
                        conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
                        conn.setUseCaches( false );
                        try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                            wr.write( postData );
                        }


                        InputStream in = new BufferedInputStream(conn.getInputStream());

                        for (int c; (c = in.read()) >= 0;) {
                            data_str += ((char) c);
                        }
                    }
                    catch (Exception e) {
                        Log.e("internet json", "Error getting data from "+ url_str +" "+ e.toString());
                    }
                    conn.disconnect();

                    data_str = data_str.split("<!--end of data-->")[0];
                    try {
                        JSONArray data_json = new JSONArray(data_str);
                        db.delete(RequestsDbInfo.TABLE_NAME, null, null);

                        JSONObject curr_row;

                        for (int i = 0; i < data_json.length(); i++) {
                            curr_row = data_json.getJSONObject(i);
                            System.out.println(i + " = " + curr_row);
                            InsertRequest(db,
                                    curr_row.getInt("id"),
                                    curr_row.getInt("quantity"),
                                    curr_row.getString("item"),
                                    curr_row.getString("date"),
                                    curr_row.getString("by_who"),
                                    curr_row.getInt("hidden"));

                        }


                    }
                    catch (Exception e) {
                        Log.e("internet json", "Error parsing JSON and entering into DB" + e.toString());
                    }

                    break;
            }
            return true;
        }

        protected void onProgressUpdate() {
        }

        protected void onPostExecute(Boolean input) {

            switch (todo) {
                case "populate home":
                    Boolean good_move = cursor.moveToFirst();
                    // {id, quantity, item, date, by_who, hidden};
                    String[] row;
                    home_text_view.append("\n");

                    while (good_move) {
                        row = GetRequestRow(cursor);
                        home_text_view.append("\n"+ row[0] +"\t"+ row[1] +"\t"+ row[2] +"\t"+
                                row[3] +"\t"+ row[4]);
                        good_move = cursor.moveToNext();
                    }
                    break;
            }
        }
    }

    // Database stuff
    public static class RequestsDbInfo implements BaseColumns {
        public static final String DB_NAME = "groceries.db";
        public static final String TABLE_NAME = "request";
        public static final String[] COLUMN_NAMES = {"id", "quantity", "item", "date", "by_who", "hidden"};

        private static final String SQL_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAMES[0] + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAMES[1] + " INTEGER, " +
                        COLUMN_NAMES[2] + " TEXT, " +
                        COLUMN_NAMES[3] + " TEXT, " +
                        COLUMN_NAMES[4] + " TEXT, " +
                        COLUMN_NAMES[5] + " TINYINT" + " )";

        private static final String SQL_DELETE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static class FulfillDbInfo implements BaseColumns {
        public static final String DB_NAME = "groceries.db";
        public static final String TABLE_NAME = "fulfill";
        public static final String[] COLUMN_NAMES = {"id", "quantity", "item", "date_fullfilled",
                "fullfilled_by", "date_requested", "requested_by", "hidden"};

        private static final String SQL_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAMES[0] + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAMES[1] + " INTEGER, " +
                        COLUMN_NAMES[2] + " TEXT, " +
                        COLUMN_NAMES[3] + " TEXT, " +
                        COLUMN_NAMES[4] + " TEXT, " +
                        COLUMN_NAMES[5] + " TEXT, " +
                        COLUMN_NAMES[6] + " TEXT, " +
                        COLUMN_NAMES[7] + " TINYINT " + " )";

        private static final String SQL_DELETE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public class Db extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = RequestsDbInfo.DB_NAME;

        public Db(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(RequestsDbInfo.SQL_CREATE);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(RequestsDbInfo.SQL_DELETE);
            onCreate(db);
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    public Long InsertRequest(SQLiteDatabase db, int id, int quantity, String item, String date, String name, int hidden)
    {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        // {id, quantity, item, date, by_who, hidden}
        values.put(RequestsDbInfo.COLUMN_NAMES[0], id);
        values.put(RequestsDbInfo.COLUMN_NAMES[1], quantity);
        values.put(RequestsDbInfo.COLUMN_NAMES[2], item);
        values.put(RequestsDbInfo.COLUMN_NAMES[3], date);
        values.put(RequestsDbInfo.COLUMN_NAMES[4], name);
        values.put(RequestsDbInfo.COLUMN_NAMES[5], hidden);


        try {
            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insertOrThrow(RequestsDbInfo.TABLE_NAME, null, values);
            return newRowId;
        }
        catch (SQLException e)
        {
            Log.i("db", "Inserting into db error caught: " + e.toString());
            return -1l;
        }
    }

    public Cursor GetRequests(SQLiteDatabase db)
    {
        String[] zero_string_array = {"0"};

        Cursor cursor = db.query(
                RequestsDbInfo.TABLE_NAME,                     // The table to query
                RequestsDbInfo.COLUMN_NAMES,                               // The columns to return
                "hidden = ?",                                // The columns for the WHERE clause
                zero_string_array,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                "id ASC"                                 // The sort order
        );
        return cursor;
    }

    public String[] GetRequestRow(Cursor cursor) {
        // gets the row that the cursor is currently pointing to
        // {id, quantity, item, date, by_who, hidden}
        String[] ret_array = {
                Integer.toString(cursor.getInt(
                        cursor.getColumnIndexOrThrow(RequestsDbInfo.COLUMN_NAMES[0]) )),
                Integer.toString(cursor.getInt(
                        cursor.getColumnIndexOrThrow(RequestsDbInfo.COLUMN_NAMES[1]) )),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(RequestsDbInfo.COLUMN_NAMES[2]) ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(RequestsDbInfo.COLUMN_NAMES[3]) ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow(RequestsDbInfo.COLUMN_NAMES[4]) )
        };

        return ret_array;
    }

}
