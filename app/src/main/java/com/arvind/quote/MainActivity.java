package com.arvind.quote;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.arvind.quote.Auth.APP_KEY_QUOTE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GibQuote";
    private RequestQueue requestQueue;
    private DrawerLayout drawerLayout;
    private final ArrayList<QuoteData> quoteArrayList = new ArrayList<>();
    private QuoteAdapter quoteAdapter;
    private RecyclerView quoteRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set Theme unless it is a fresh start
        if (getIntent().getStringExtra("Theme") != null) {
            switch (getIntent().getStringExtra("Theme")) {
                case "Dark":
                    setTheme(R.style.AnotherAppTheme);
                    break;
                case "Light":
                    setTheme(R.style.AppTheme);
                    break;
            }
        }

        setContentView(R.layout.activity_main);

        // Get the Animation Drawable from RelativeLayout
        // Currently 'animation_list.xml'
        //
        // final RelativeLayout rootLayout = (RelativeLayout) findViewById(R.id.root_layout);
        // AnimationDrawable anim = (AnimationDrawable) rootLayout.getBackground();
        // anim.setEnterFadeDuration(1000);
        // anim.setExitFadeDuration(1000);
        // // Start the animating background
        // anim.start();

        // RequestQueue executes any number of requests, asynchronously
        // More of a Queue Manager for Volley
        requestQueue = Volley.newRequestQueue(this);

        // RecyclerView Object
        quoteRecyclerView = (RecyclerView) findViewById(R.id.quote_list_view);

        // RecyclerView's Adapter - Detects change on DataSet
        quoteAdapter = new QuoteAdapter(this, quoteArrayList);

        // Allows Recycler to perform actions on the Layout
        // whenever the particular adapter detects a change
        quoteRecyclerView.setAdapter(quoteAdapter);

        // RecyclerView's Layout Manager
        // Used for viewing RecyclerView's nodes
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        // Reverses the Layout
        // Newer nodes come at the top
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        // Set Layout Manager for RecyclerView
        // Two Managers available: 1. LinearLayoutManager 2. StaggeredGridLayoutManager
        quoteRecyclerView.setLayoutManager(linearLayoutManager);

        // FAB to generate quote onClick
        final FloatingActionButton gibQuoteFab = (FloatingActionButton) findViewById(R.id.gib_quote_fab);

        if (isNetworkAvailable()) {
            Log.v(TAG, "INTERNET CONNECTIVITY : OK");
            Toast.makeText(this, "Connected to Internet", Toast.LENGTH_SHORT).show();
        } else {
            Log.v(TAG, "DAM SON :(");
            Toast.makeText(this, "Internet Connection Required", Toast.LENGTH_LONG).show();
            // Greet the user with some inspirational quotes
            quoteArrayList.add(new QuoteData("Always pay your Internet Bills on-time",
                    "YetAnotherN00b",
                    "free-advice, no internet, y u do dis"));
            quoteAdapter.notifyItemInserted(quoteArrayList.size() - 1);
            quoteRecyclerView.smoothScrollToPosition(quoteArrayList.size() - 1);
            gibQuoteFab.setEnabled(false);
        }

        // Generate the first Quote
        generateStuffs();

        // Adding an Listener which ...
        // Hides the FAB when RecyclerView is scrolled down
        // Gets the FAB back when RecyclerView is scrolled up
        quoteRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                // dy - Instantaneous change in 'y' co-ordinate
                // Origin is at Top-Left corner
                // scrollDown -> Final 'y' co-ordinate is lesser than (or below) Initial 'y' co-ordinate
                //                which results in a positive difference
                // scrollUp -> Final 'y' Co-ordinate is greater than (or above) Initial 'y' co-ordinate
                //             which results in a Negative difference

                // If scrollDown && the FAB is visible
                if (dy > 0 && gibQuoteFab.getVisibility() == View.VISIBLE) {
                    // then hide it
                    gibQuoteFab.hide();
                    // Create a separate thread, which sleeps for 1000ms
                    // And shows the FAB after it wakes up :P
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    gibQuoteFab.show();
                                }
                            });
                        }
                    }).start();
                } // else If scrollUp && the FAB is not Visible
                else if (dy < 0 && gibQuoteFab.getVisibility() != View.VISIBLE) {
                    // then show that FAB
                    gibQuoteFab.show();
                }
            }
        });

        // generateStuffs on receiving a click on FAB
        gibQuoteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateStuffs();
            }
        });

        // ToolBar Stuffs
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);

        // Set 'toolbar' as an ActionBar
        setSupportActionBar(toolbar);

        // Root Layout of the Application
        // DrawerLayout consists of
        // 1. The Toolbar (ActionBar)
        // 2. The Navigation (Drawer) View
        // 3. RelativeLayout, which occupies rest of the screen
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // MainActivity Context
        // Required, since shareQuote() is called under the context
        // of RecyclerView
        final Context mainActivityContext = this;

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_bar_view);

        // Change CardView theme on the basis of the theme selected
        if (getIntent().getStringExtra("Theme") != null) {
            switch (getIntent().getStringExtra("Theme")) {
                case "Dark":
                    navigationView.setBackgroundColor(getResources().getColor(R.color.cardview_dark_background));
                    break;
                case "Light":
                    navigationView.setBackgroundColor(getResources().getColor(R.color.cardview_light_background));
                    break;
            }
        }

        // Listen for clicks on any MenuItem present in the Navigation Drawer
        // MenuItem is Identified by its position
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.theme_selector_item:
                        // Creating an AlertDialog which would look like
                        // Title : Select a Theme
                        // Options : Light, Dark, TODO: Gradient
                        // Selecting any of the options would restart the application
                        // with the selected theme applied
                        AlertDialog.Builder themeSelectorDialogBuilder = new AlertDialog.Builder(mainActivityContext);
                        themeSelectorDialogBuilder.setTitle("Select a Theme");
                        themeSelectorDialogBuilder.setCancelable(true);
                        themeSelectorDialogBuilder.setItems(
                                new CharSequence[]{"Light", "Dark"},
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = getIntent();
                                        // TODO: Check if the selected theme is same as current theme
                                        // boolean isSameTheme = false;
                                        Integer themeStyleId = R.style.AnotherAppTheme;
                                        Log.d(TAG, themeStyleId.toString());
                                        switch (which) {
                                            case 0:
                                                intent.putExtra("Theme", "Light");
                                                break;
                                            case 1:
                                                intent.putExtra("Theme", "Dark");
                                                break;
                                        }
                                        finish();
                                        startActivity(intent);
                                    }
                                });

                        // Create AlertDialog after it has been built
                        AlertDialog themeSelectorDialog = themeSelectorDialogBuilder.create();
                        // Show it!
                        themeSelectorDialog.show();
                        break;
                    case R.id.about_item:
                        // TODO: Implement About Dialog/Activity
                        break;
                }
                // Close drawers present in this DrawerLayout
                drawerLayout.closeDrawers();
                return true;
            }
        });

        // TODO: Hamburger icon Indicator for Navigation Drawer
        // drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        // drawerLayout.addDrawerListener(drawerToggle);

    }

    /* To Check if we're connected to the Internet */
    // TODO: Get isNetworkAvailable explained
    private boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = null;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager != null)
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /* Generates a Random number to be sent as the Quote's ID */
    private Integer generateRandomNumber() {
        int MIN = 1;
        // Yeah! This was the Number of Quotes as on 19/09/17
        // But, some quotes are 404, dunno why
        int MAX = 62024;
        return new Random().nextInt((MAX - MIN) + 1) + MIN;
    }

    /* Allows the user to share currently displayed quote */
    public void shareQuote(Context context, QuoteData quote) {

        // My intention is to send (throw) a piece of Text (ball)
        Intent quoteIntent = new Intent(Intent.ACTION_SEND);
        // Piece of Text (the Ball)
        String quoteMessage = quote.getQuoteText() + "\n\n-- " + quote.getAuthorText();
        // Specify the Text to be thrown
        quoteIntent.putExtra(Intent.EXTRA_TEXT, quoteMessage);
        // Specify the MIME type of the object to be thrown
        quoteIntent.setType("text/plain");
        // Send an Acknowledgement
        Toast.makeText(context, "Select the App to gibQuote", Toast.LENGTH_SHORT).show();
        // Throw the Ball!
        context.getApplicationContext().startActivity(Intent.createChooser(quoteIntent, "Share this Quote"));
    }

    private void generateStuffs() {

        // Creating a new GET JSONObject request
        //
        // Params
        // The Request method - GET
        // The Request URL - a random quote's URL
        // A Listener for performing actions on successfully obtaining a JSON Object
        // A Listener for performing actions if an error occurs while requesting
        //
        // Additionally, a method of JsonObjectRequest - getHeaders() - has been overridden to provide
        // the request headers (Content Type & Authorization token)

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                "https://favqs.com/api/quotes/" + generateRandomNumber().toString(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, response.toString());

                        try {
                            String quoteText = response.getString("body");
                            String authorText = response.getString("author");
                            JSONArray tagText = response.getJSONArray("tags");

                            StringBuilder tags = new StringBuilder("");
                            for (int i = 0; i < tagText.length(); i++) {
                                tags.append(tagText.get(i));
                                if (i != tagText.length() - 1) {
                                    tags.append(", ");
                                }
                            }

                            // Set Obtained details
                            quoteArrayList.add(new QuoteData(quoteText, authorText, tags.toString()));
                            // Notify the Adapter which in turn notifies the RecyclerView
                            // that a new item has been inserted
                            quoteAdapter.notifyItemInserted(quoteArrayList.size());

                            // Scroll the RecyclerView to given position, smooothly
                            quoteRecyclerView.smoothScrollToPosition(quoteAdapter.getItemCount() - 1);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "404 Quote NOT FOUND. KEK");
                        Log.e(TAG, error.toString());
                    }
                }
        ) {
            // Optional method to pass Request Headers
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Authorization", "Token token=\"" + APP_KEY_QUOTE + "\"");
                return params;
            }
        };

        // Add the request to Volley's Request Queue
        requestQueue.add(jsonObjectRequest);

    }
}