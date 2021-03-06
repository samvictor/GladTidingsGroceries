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
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
    static SQLiteDatabase db = null;
    public static Cursor cursor = null;
    TextView home_text_view;
    static View home_view;
    static boolean first_fire_attach = true;
    static boolean home_fragment_alive = false;

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

        switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
            case 1:
                home_fragment_alive = true;
                home_view = inflater.inflate(R.layout.fragment_home, container, false);
                home_text_view = (TextView) home_view.findViewById(R.id.section_label);
                String time_now;
                Calendar cal = new GregorianCalendar();
                time_now = Long.toString(cal.getTimeInMillis());
                //home_text_view.setText("The time is: " + time_now + "\nWhich is "
                //        +cal.getTime().toString());
                home_text_view.setText("Here is the Glad Tidings grocery list:");


                home_text_view.setTextColor(Color.parseColor("#000000"));

                CheckAndUpdate();

                // FireBase
                /*FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("request/last_post");

                if (first_fire_attach) {
                    // Read from the database
                    myRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            Long value = dataSnapshot.getValue(Long.class);
                            Log.d("firebase", "Value is: " + value);

                            CheckAndUpdate();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            Log.w("firebase", "Failed to read value. ", error.toException());
                        }
                    });

                    first_fire_attach = false;
                }*/

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

    @Override
    public void onResume() {
        home_fragment_alive = true;
        CheckAndUpdate();
        super.onResume();
    }

    @Override
    public void onPause() {
        home_fragment_alive = false;
        super.onPause();
    }

    public static void CheckAndUpdate() {
        if (cursor == null || db == null)
            new DatabaseSetup().execute("setup");
        else
            new DatabaseSetup().execute("populate home");
    }

    public static class DatabaseSetup extends AsyncTask<String, Void, Boolean> {

        String todo = null;

        protected Boolean doInBackground(String... input) {
            todo = input[0];
            switch (todo) {

                case "setup":
                    DbHelper db_helper = new DbHelper(Home.home_context);

                    // Gets the data repository in write mode
                    db = db_helper.getWritableDatabase();

                    cursor = GetRequests(db);

                case "populate home":

                    UpdateDatabase(db);
                    break;

                default:
                    Log.e("internet json", "nothing to do for background. todo = " + todo);
            }
            return true;
        }

        protected void onProgressUpdate() {
        }

        protected void onPostExecute(Boolean input) {

            switch (todo) {
                case "setup":
                case "populate home":
                    UpdateUI();
                    break;

                default:
                    Log.e("internet json", "nothing to do for post execute. todo = " + todo);
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

    public static class DbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = RequestsDbInfo.DB_NAME;

        public DbHelper(Context context) {
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

    public static Long InsertRequest(SQLiteDatabase db, int id, int quantity, String item, String date, String name, int hidden)
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

    public static Cursor GetRequests(SQLiteDatabase db)
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

    public static String[] GetRequestRow(Cursor cursor) {
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



    public static void UpdateDatabase(SQLiteDatabase db) {
        //HttpURLConnection conn = null;
        URLConnection conn = null;
        String url_str = "";
        String data_str = "";
        BufferedReader reader=null;

        try {
            /*
            String urlParameters  = "todo=get_requests";
            byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
            int    postDataLength = postData.length;
            url_str        = "http://www.gtcacademy.org/groceries/app_data.php";
            URL    url            = new URL( url_str );
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput( true );
            conn.setChunkedStreamingMode(0);
            conn.setInstanceFollowRedirects( false );
            conn.setRequestMethod( "POST" );
            //conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty( "charset", "utf-8");
            //conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
            conn.setUseCaches( false );
            //try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
            //    wr.write( postData );
            //}

            */

            String data = URLEncoder.encode("todo", "UTF-8")
                    + "=" + URLEncoder.encode("get_requests", "UTF-8");

            String text = "";

            URL url = new URL("http://www.gtcacademy.org/groceries/app_data.php");

            // Send POST data request

            conn = url.openConnection();
            conn.setDoOutput(true);


            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write( data );
            wr.flush();

            // Get the server response

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;

            // Read Server Response
            while((line = reader.readLine()) != null)
            {
                // Append server response in string
                sb.append(line + "\n");
            }


            text = sb.toString();

            data_str = text;

            //OutputStream out = new BufferedOutputStream(conn.getOutputStream());
            //out.write(postData);

            /*
            InputStream in = new BufferedInputStream(conn.getInputStream());

            data_str = "";
            for (int c; (c = in.read()) >= 0;) {
                data_str += ((char) c);
            }


            if (in != null) {
                in.close();
            } */

        }
        catch (Exception e) {
            Log.e("internet json", "Error getting data from "+ url_str +" "+ e.toString());
        }
        try {
            //conn.disconnect();
            reader.close();
        }

        catch(Exception ex) {}

        data_str = data_str.split("<!--end of data-->")[0];
        try {
            JSONArray data_json = new JSONArray (data_str);
            db.delete(RequestsDbInfo.TABLE_NAME, null, null);

            JSONObject curr_row;

            for (int i = 0; i < data_json.length(); i++) {
                curr_row = data_json.getJSONObject(i);
                Log.i("internet json", "inserting into db");
                Log.i("internet json", i + " = " + curr_row);
                InsertRequest(db,
                        curr_row.getInt("id"),
                        curr_row.getInt("quantity"),
                        curr_row.getString("item"),
                        curr_row.getString("date"),
                        curr_row.getString("by_who"),
                        curr_row.getInt("hidden"));

            }

            cursor = GetRequests(db);
        }
        catch (Exception e) {
            Log.e("internet json", "Error parsing JSON and entering into DB" + e.toString());
        }

    }

    public static void UpdateUI() {

        Log.i("update ui", "Starting 'UpdateUI()' ");

        Boolean good_move = cursor.moveToFirst();
        // {id, quantity, item, date, by_who, hidden};
        String[] row;

        TableLayout home_table = (TableLayout) home_view.findViewById(R.id.table);

        int count = home_table.getChildCount();
        for (int i = 1; i < count; i++) {
            View child = home_table.getChildAt(i);
            if (child instanceof TableRow)
                ((ViewGroup) child).removeAllViews();
        }
        //home_table.removeAllViews();

        Log.i("update ui", "going into loop");

        while (good_move) {
            row = GetRequestRow(cursor);

            Log.i("update ui", "appending " + row[0] +"\t"+ row[1] +"\t"+ row[2] +"\t"+
                    row[3] +"\t"+ row[4]);

            Context local_home_context = Home.home_context;


            TextView new_view1 = new TextView(local_home_context);
            TextView new_view2 = new TextView(local_home_context);
            TextView new_view3 = new TextView(local_home_context);
            TextView new_view4 = new TextView(local_home_context);

            new_view1.setText(row[1]);
            new_view1.setTextColor(ContextCompat.getColor(local_home_context, R.color.home_table_cell_color));
            //new_view1.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            TableRow.LayoutParams new_view1_params = new TableRow.LayoutParams();
            new_view1_params.column = 0;
            new_view1_params.weight = 1;

            new_view2.setText(row[2]);
            new_view2.setTextColor(ContextCompat.getColor(local_home_context, R.color.home_table_cell_color));
            //new_view2.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            TableRow.LayoutParams new_view2_params = new TableRow.LayoutParams();
            new_view2_params.column = 1;
            new_view2_params.weight = 1;

            new_view3.setText(row[3]);
            new_view3.setTextColor(ContextCompat.getColor(local_home_context, R.color.home_table_cell_color));
            //new_view3.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            TableRow.LayoutParams new_view3_params = new TableRow.LayoutParams();
            new_view3_params.column = 2;
            new_view3_params.weight = 1;

            new_view4.setText(row[4]);
            new_view4.setTextColor(ContextCompat.getColor(local_home_context, R.color.home_table_cell_color));
            //.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            TableRow.LayoutParams new_view4_params = new TableRow.LayoutParams();
            new_view4_params.column = 3;
            new_view4_params.weight = 1;

            TableRow new_row = new TableRow(Home.home_context);
            new_row.addView(new_view1, new_view1_params);
            new_row.addView(new_view2, new_view2_params);
            new_row.addView(new_view3, new_view3_params);
            new_row.addView(new_view4, new_view4_params);

            home_table.addView(new_row);

            good_move = cursor.moveToNext();
        }
    }

}
