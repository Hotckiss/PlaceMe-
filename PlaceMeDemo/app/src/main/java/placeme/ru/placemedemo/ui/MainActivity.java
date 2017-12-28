package placeme.ru.placemedemo.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.tomergoldst.tooltips.ToolTip;
import com.tomergoldst.tooltips.ToolTipsManager;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import geo.GeoObj;
import gl.GL1Renderer;
import gl.GLFactory;
import placeme.ru.placemedemo.R;
import placeme.ru.placemedemo.core.Controller;
import placeme.ru.placemedemo.core.database.DatabaseManager;
import placeme.ru.placemedemo.ui.dialogs.AlertDialogCreator;
import system.ArActivity;
import system.MySetup;
import worldData.World;

import static com.google.android.gms.location.places.ui.PlaceAutocomplete.getPlace;

/**
 * Main activity of the app
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final int GALLERY_INTENT = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final int CAMERA_RESULT = 0;
    private static final String MY_PLACE = "My place";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private GoogleMap mGoogleMap;

    private Location mLastKnownLocation;
    private LatLng myPosition;

    private EditText mSearch;

    private ImageView mImageView;
    private Bitmap mBitmap;
    private Uri mUri;
    private int mLastAction = 0;
    private ToolTipsManager mToolTipsManager;
    //AR
    private static ArrayList<LatLng> points = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkLogin();
        int permission4 = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permission4 != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        checkPermission();
        int permission2 = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission2 != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }


        int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        int permission3 = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);

        if (permission3 != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        initializeGeolocation();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        View hView = navigationView.getHeaderView(0);

        mToolTipsManager = new ToolTipsManager();

        loadProfileAvatar(hView);
        navigationView.setNavigationItemSelectedListener(this);

        initializeInputWindow();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            initializeCamera();
        }

        initGooglePlacesButton();

        FloatingActionButton actionButton = findViewById(R.id.fab);

        actionButton.setOnClickListener(v -> {
            Controller.getUserRoutesLength2(Controller.getLoggedInAsString(MainActivity.this), MainActivity.this);
            saveRoute().show();
        });

        ToolTip.Builder builder1 = new ToolTip.Builder(this, actionButton, findViewById(R.id.root_t), "Take a route snapshot!", ToolTip.POSITION_LEFT_TO);

        actionButton.setOnLongClickListener(v -> {
            mToolTipsManager.show(builder1.build());
            return true;
        });

        initializeSearchParameters();

        FloatingActionButton searchFriendsButton = findViewById(R.id.search_friends);

        ToolTip.Builder builder = new ToolTip.Builder(this, searchFriendsButton, findViewById(R.id.root_t), "Find friends\nhere!", ToolTip.POSITION_ABOVE);

        searchFriendsButton.setOnClickListener(v -> {
            searchFriends().show();
        });

        searchFriendsButton.setOnLongClickListener(v -> {
            mToolTipsManager.show(builder.build());
            return true;
        });

    }

    private AlertDialog searchFriends() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.save_route, null);

        final TextView textView = layout.findViewById(R.id.route_title);

        textView.setText("Search friends!");

        //builder.setTitle("Search friends");
        builder.setPositiveButton("Search!", (dialog, id) -> {
            EditText editTextDescription = layout.findViewById(R.id.route_description);
            String description = editTextDescription.getText().toString();
            if (description == null || description.length() == 0) {
                dialog.dismiss();
            }
            AlertDialogCreator.createAlertDialogFoundedFriends(MainActivity.this, editTextDescription.getText().toString()).show();
        }).setNegativeButton(R.string.answer_back, (dialog, arg1) -> {
        });

        builder.setCancelable(true);
        builder.setView(layout);
        return builder.create();
    }

    private void loadProfileAvatar(View view) {
        CircleImageView circleImageView = view.findViewById(R.id.profile_image);
        if (circleImageView != null) {
            Controller.loadAvatar(circleImageView, MainActivity.this, Controller.getLoggedInAsString(MainActivity.this));
        }
    }

    private void initGooglePlacesButton() {
        FloatingActionButton floatingActionButton = findViewById(R.id.google_places_button);
        ToolTip.Builder builder = new ToolTip.Builder(this, floatingActionButton, findViewById(R.id.root_t), "Import places\nfrom big base", ToolTip.POSITION_BELOW);

        floatingActionButton.setOnClickListener(v -> alertDialogAskGooglePlacesUsage(MainActivity.this).show());

        floatingActionButton.setOnLongClickListener(v -> {
            mToolTipsManager.show(builder.build());
            return true;
        });
    }

    private AlertDialog alertDialogAskGooglePlacesUsage(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(R.string.places_question_gp);
        builder.setMessage(R.string.places_question_gp_message);

        builder.setPositiveButton(R.string.answer_yes, (dialog, which) -> {

            PlacePicker.IntentBuilder builder1 = new PlacePicker.IntentBuilder();

            try {
                startActivityForResult(builder1.build(MainActivity.this), PLACE_PICKER_REQUEST);
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
        });

        builder.setNegativeButton(R.string.answer_no, (dialog, which) -> dialog.cancel());
        builder.setCancelable(true);

        return builder.create();
    }

    @Override
    public void onMapLongClick(LatLng point) {
        mGoogleMap.addMarker(new MarkerOptions().position(point).title(MY_PLACE));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap map) {
        mGoogleMap = map;
        Log.d("ddddd", "dvd");
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        mGoogleMap.getUiSettings().setCompassEnabled(false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        Controller.addAllMarkers(mGoogleMap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            initializeCamera();
        }
        try {
            mGoogleMap.setMyLocationEnabled(true);
        } catch (SecurityException se) {
            se.printStackTrace();
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        if (marker.getTitle() == null) {
            return false;
        }
        if (marker.getTitle().equals(MY_PLACE)) {
            AlertDialog alert = createAlertDialogNewPlace(marker.getPosition());
            alert.show();

            Controller.refreshMarkers(mGoogleMap);
        }
        else {
            showDescriptionDialog(marker);
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            Intent profile = new Intent(this, ProfileActivity.class);
            startActivity(profile);
        } else if (id == R.id.nav_places) {
            Intent places = new Intent(this, FavouritePlacesActivity.class);
            startActivity(places);
        } else if (id == R.id.nav_routes) {
            Intent routes = new Intent(this, RoutesActivity.class);
            startActivity(routes);
        } else if (id == R.id.nav_settings) {
            Intent settings = new Intent(this, PlanActivity.class);
            startActivity(settings);
        } else if (id == R.id.nav_share) {
            shareApplication();
        } else if (id == R.id.nav_exit) {
            Controller.setLoggedOut(MainActivity.this);
            login();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void shareApplication() {
        //TODO:PICTURE
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Я пользуюсь приложением PlaceMe! Присоединяйся и ты: placeme.com :)");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share using..."));
    }

    private void initializeSearchParameters() {
        FloatingActionButton searchParamButton  = findViewById(R.id.button_search_parameters);
        ToolTip.Builder builder = new ToolTip.Builder(this, searchParamButton, findViewById(R.id.root_t), "Customize your search!", ToolTip.POSITION_LEFT_TO );
        searchParamButton.setOnClickListener(v -> {
            AlertDialog alert = AlertDialogCreator.createSearchParametersDialog(MainActivity.this);
            alert.show();
        });

        searchParamButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mToolTipsManager.show(builder.build());
                return true;
            }
        });

    }

    @Override
    public void onStop () {
        super.onStop();
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_INTENT && resultCode == RESULT_OK) {
            mUri = data.getData();
            mImageView.setImageURI(mUri);
            mLastAction = 1;
        }

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                //TODO: ?
                Place place = getPlace(this, data);
                placeme.ru.placemedemo.elements.Place toAdd = convertGooglePlaceToPlace(place);

                Controller.saveConvertedPlace(null, toAdd);
                String toastMsg = String.format("Place: %s", place.getName().toString());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == CAMERA_RESULT) {
            mBitmap = (Bitmap) data.getExtras().get("data");
            mLastAction = 2;

            mImageView.setImageBitmap(mBitmap);
        }
    }

    private placeme.ru.placemedemo.elements.Place convertGooglePlaceToPlace(Place place) {
        placeme.ru.placemedemo.elements.Place result = new placeme.ru.placemedemo.elements.Place();
        String name = "Some place";
        String address = "";
        String phone = "";
        if (place.getName() != null) {
            name = place.getName().toString();
        }

        if (place.getAddress() != null) {
            address = place.getAddress().toString();
        }

        if (place.getPhoneNumber() != null) {
            phone = place.getPhoneNumber().toString();
        }

        StringBuilder description = new StringBuilder();
        description.append(name);

        if (!address.equals("")) {
            description.append('\n');
            description.append(address);
        }

        if (!phone.equals("")) {
            description.append('\n');
            description.append(phone);
        }

        result.setName(place.getName().toString());
        result.setDescription(description.toString());

        result = addTags(result, place);

        result.setLatitude(place.getLatLng().latitude);

        result.setLongitude(place.getLatLng().longitude);
        return result;
    }

    private placeme.ru.placemedemo.elements.Place addTags(placeme.ru.placemedemo.elements.Place destination, Place src) {
        List<Integer> placeTags = src.getPlaceTypes();
        StringBuilder tags = new StringBuilder();
        //tags.append(",");

        for (Integer id : placeTags) {
            if (id.equals(Place.TYPE_AIRPORT)) {
                tags.append("аэропорт,");
                tags.append("самолёт,");
            } else if (id.equals(Place.TYPE_ART_GALLERY)) {
                tags.append("галерея,");
                tags.append("арт,");
                tags.append("искусство,");
                tags.append("культура,");
                tags.append("картины,");
            } else if (id.equals(Place.TYPE_ATM)) {
                tags.append("банк,");
                tags.append("банкомат,");
                tags.append("деньги,");
            } else if (id.equals(Place.TYPE_BAKERY)) {
                tags.append("пекарня,");
                tags.append("выпечка,");
                tags.append("хлеб,");
                tags.append("булочная,");
            } else if (id.equals(Place.TYPE_BANK)) {
                tags.append("банк,");
                tags.append("деньги,");
            } else if (id.equals(Place.TYPE_BAR)) {
                tags.append("бар,");
                tags.append("кафе,");
                tags.append("досуг,");
            } else if (id.equals(Place.TYPE_BEAUTY_SALON)) {
                tags.append("салон красоты,");
                tags.append("красота,");
                tags.append("спа,");
                tags.append("салон,");
                tags.append("парикмахерская,");
            } else if (id.equals(Place.TYPE_BICYCLE_STORE)) {
                tags.append("велосипед,");
                tags.append("магазин,");
            } else if (id.equals(Place.TYPE_BOOK_STORE)) {
                tags.append("книги,");
                tags.append("магазин книг,");
                tags.append("магазин,");
                tags.append("канцелярия,");
            } else if (id.equals(Place.TYPE_BUS_STATION)) {
                tags.append("автобус,");
                tags.append("остановка,");
                tags.append("троллейбус,");
                tags.append("транспорт,");
            } else if (id.equals(Place.TYPE_CAFE)) {
                tags.append("кафе,");
                tags.append("еда,");
                tags.append("ресторан,");
            } else if (id.equals(Place.TYPE_CAR_REPAIR)) {
                tags.append("машина,");
                tags.append("ремонт,");
                tags.append("ремонт машин,");
                tags.append("запчасти,");
            } else if (id.equals(Place.TYPE_CAR_WASH)) {
                tags.append("машина,");
                tags.append("мойка,");
                tags.append("мойка машин,");
            } else if (id.equals(Place.TYPE_CHURCH)) {
                tags.append("церковь,");
            } else if (id.equals(Place.TYPE_CLOTHING_STORE)) {
                tags.append("магазин,");
                tags.append("одежда,");
                tags.append("магазин одежды,");
            } else if (id.equals(Place.TYPE_COUNTRY)) {
                tags.append("страна,");
                tags.append("государство,");
            } else if (id.equals(Place.TYPE_DENTIST)) {
                tags.append("дантист,");
                tags.append("стоматология,");
                tags.append("клиника,");
                tags.append("стоматолог,");
            } else if (id.equals(Place.TYPE_DEPARTMENT_STORE)) {
                tags.append("универмаг,");
            } else if (id.equals(Place.TYPE_DOCTOR)) {
                tags.append("доктор,");
                tags.append("врач,");
                tags.append("больница,");
                tags.append("поликлиника,");
            } else if (id.equals(Place.TYPE_EMBASSY)) {
                tags.append("консул,");
                tags.append("консульство,");
                tags.append("посольство,");
                tags.append("посол,");
            } else if (id.equals(Place.TYPE_FIRE_STATION)) {
                tags.append("пожарная,");
                tags.append("пожарная станция,");
                tags.append("пожарная часть,");
            } else if (id.equals(Place.TYPE_FLORIST)) {
                tags.append("цветы,");
                tags.append("цветочный магазин,");
                tags.append("магазин цветов,");
                tags.append("флорист,");
            } else if (id.equals(Place.TYPE_FLORIST)) {
                tags.append("цветы,");
                tags.append("цветочный магазин,");
                tags.append("магазин цветов,");
                tags.append("флорист,");
            } else if (id.equals(Place.TYPE_FOOD)) {
                tags.append("еда,");
            } else if (id.equals(Place.TYPE_GROCERY_OR_SUPERMARKET)) {
                tags.append("магазин,");
                tags.append("супермаркет,");
                tags.append("еда,");
            } else if (id.equals(Place.TYPE_GYM)) {
                tags.append("фитнес,");
                tags.append("спорт,");
                tags.append("спортзал,");
                tags.append("тренажёры,");
            } else if (id.equals(Place.TYPE_HAIR_CARE)) {
                tags.append("парикмахерская,");
                tags.append("волосы,");
                tags.append("стрижка,");
                tags.append("парикмахер,");
            } else if (id.equals(Place.TYPE_HEALTH)) {
                tags.append("здоровье,");
            } else if (id.equals(Place.TYPE_LIBRARY)) {
                tags.append("книги,");
                tags.append("библиотека,");
                tags.append("чтение,");
                tags.append("досуг,");
            } else if (id.equals(Place.TYPE_MOVIE_THEATER)) {
                tags.append("кино,");
                tags.append("кинотеатр,");
                tags.append("фильмы,");
                tags.append("досуг,");
            } else if (id.equals(Place.TYPE_MUSEUM)) {
                tags.append("музей,");
                tags.append("культура,");
                tags.append("образование,");
            } else if (id.equals(Place.TYPE_PARK)) {
                tags.append("парк,");
                tags.append("прогулка,");
                tags.append("сад,");
                tags.append("деревья,");
            } else if (id.equals(Place.TYPE_PARKING)) {
                tags.append("парковка,");
                tags.append("стоянка,");
            } else if (id.equals(Place.TYPE_PET_STORE)) {
                tags.append("зоомагазин,");
                tags.append("магазин,");
                tags.append("питомец,");
            } else if (id.equals(Place.TYPE_PHARMACY)) {
                tags.append("аптека,");
                tags.append("лекарства,");
            } else if (id.equals(Place.TYPE_POLICE)) {
                tags.append("полиция,");
                tags.append("полицеский участок,");
            } else if (id.equals(Place.TYPE_POST_OFFICE)) {
                tags.append("почта,");
                tags.append("посылка,");
                tags.append("письмо,");
            } else if (id.equals(Place.TYPE_RESTAURANT)) {
                tags.append("ресторан,");
                tags.append("кафе,");
                tags.append("еда,");
                tags.append("уют,");
            } else if (id.equals(Place.TYPE_SCHOOL)) {
                tags.append("школа,");
                tags.append("образование,");
            } else if (id.equals(Place.TYPE_SHOE_STORE)) {
                tags.append("обувь,");
                tags.append("магазин,");
                tags.append("магазин обуви,");
            } else if (id.equals(Place.TYPE_SHOPPING_MALL)) {
                tags.append("торговый центр,");
                tags.append("магазин,");
                tags.append("тц,");
                tags.append("шопинг,");
            } else if (id.equals(Place.TYPE_SPA)) {
                tags.append("спа,");
                tags.append("отдых,");
                tags.append("красота,");
            } else if (id.equals(Place.TYPE_STADIUM)) {
                tags.append("спорт,");
                tags.append("стадион,");
            } else if (id.equals(Place.TYPE_STORE)) {
                tags.append("магазин,");
            } else if (id.equals(Place.TYPE_STREET_ADDRESS)) {
                tags.append("улица,");
            } else if (id.equals(Place.TYPE_SUBWAY_STATION)) {
                tags.append("метро,");
                tags.append("станция метро,");
                tags.append("транспорт,");
            } else if (id.equals(Place.TYPE_TRAIN_STATION)) {
                tags.append("поезд,");
                tags.append("транспорт,");
                tags.append("вокзал,");
            } else if (id.equals(Place.TYPE_UNIVERSITY)) {
                tags.append("университет,");
                tags.append("обучение,");
                tags.append("образование,");
                tags.append("институт,");
                tags.append("вуз,");
            } else if (id.equals(Place.TYPE_VETERINARY_CARE)) {
                tags.append("ветеринар,");
                tags.append("питомец,");
            } else if (id.equals(Place.TYPE_ZOO)) {
                tags.append("зоопарк,");
                tags.append("животные,");
                tags.append("досуг,");
            }
        }

        if(tags.toString().length() > 0) {
            tags.deleteCharAt(tags.lastIndexOf(","));
        }

        destination.setTags(tags.toString());

        return destination;
    }

    private void putLineToPoint(GL1Renderer renderer, final World world, GLFactory objectFactory, LatLng start, LatLng finish) {
        double maxx = Math.max(start.latitude, finish.latitude);
        double maxy = Math.max(start.longitude, finish.longitude);
        double minx = Math.min(start.latitude, finish.latitude);
        double miny = Math.min(start.longitude, finish.longitude);
        double dx = maxx - minx;
        double dy = maxy - miny;
        int iter = (int)(100 * Math.max(dx, dy)) + 3;
        if (dx * dx + dy * dy < 1e-14) {
            iter = 1;
        }
        for (int i = 0; i <= iter; i++) {
            Location l = new Location("");
            l.setLatitude(start.latitude + (finish.latitude - start.latitude) * i / iter);
            l.setLongitude(start.longitude + (finish.longitude - start.longitude) * i / iter);
            GeoObj next = new GeoObj(l);
            next.setComp(objectFactory.newArrow());

            world.add(next);
        }
    }

    private void initializeCamera() {
        Button b = findViewById(R.id.button4);
        ToolTip.Builder builder2 = new ToolTip.Builder(this, b, findViewById(R.id.root_t), "Try AR route!", ToolTip.POSITION_ABOVE );
        b.setOnLongClickListener(v -> {
            mToolTipsManager.show(builder2.build());
            return true;
        });
        b.setOnClickListener(v -> ArActivity.startWithSetup(MainActivity.this, new MySetup() {
            @Override
            public void addObjectsTo(GL1Renderer renderer, final World world, GLFactory objectFactory) {

                points = AlertDialogCreator.getPoints();
                if (points == null) {
                }
                else {
                    if (points.size() == 0) {
                    } else {
                        putLineToPoint(renderer, world, objectFactory, new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), points.get(0));

                        for (int i = 0; i < points.size() - 1; i++) {
                            putLineToPoint(renderer, world, objectFactory, points.get(i), points.get(i + 1));
                        }
                        Location ls = new Location("");
                        LatLng end = points.get(points.size() - 1);
                        ls.setLatitude(end.latitude);
                        ls.setLongitude(end.longitude);
                        GeoObj endObj = new GeoObj(ls);
                        endObj.setComp(objectFactory.newArrow());
                        world.add(endObj);
                    }
                }
            }
        }));
    }

    private void initializeGeolocation() {
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    mLastKnownLocation = task.getResult();
                    myPosition = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                }
            });
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void initializeInputWindow() {
        mSearch = findViewById(R.id.search);
        ToolTip.Builder builder2 = new ToolTip.Builder(this, mSearch, findViewById(R.id.root_t), "Search places\nhere!", ToolTip.POSITION_BELOW );
        mSearch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mToolTipsManager.show(builder2.build());
                return true;
            }
        });
        mSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (mGoogleMap != null) {
                mGoogleMap.clear();
            }
            final String query = mSearch.getText().toString();
            Controller.addFoundedMarkers(mGoogleMap, query);
            AlertDialogCreator.createAlertDialogFounded(MainActivity.this, query, mGoogleMap, myPosition).show();

            return false;
        });
    }

    private void checkLogin() {
        if (Controller.getLoggedIn(this) == -1) {
            login();
        }
    }

    private void login() {
        Intent login = new Intent(this, LoginActivity.class);
        login.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(login);
        finish();
    }

    private AlertDialog createAlertDialogNewPlace(final LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle(R.string.places_creating);
        builder.setMessage(R.string.places_creating_question);
        builder.setPositiveButton(R.string.answer_yes, (dialog, arg1) -> {
            AlertDialog alertNewPlace = createNewPlace(latLng);
            alertNewPlace.show();
        });

        builder.setNegativeButton(R.string.answer_no, (dialog, arg1) -> dialog.dismiss());
        builder.setCancelable(true);

        return builder.create();
    }

    private AlertDialog saveRoute() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        final View layout = inflater.inflate(R.layout.save_route, null);

        builder.setPositiveButton(R.string.answer_finish, (dialog, id) -> {
            EditText editTextDescription = layout.findViewById(R.id.route_description);
            String description = editTextDescription.getText().toString();
            if (description == null || description.length() == 0) {
                description = "No description given.";
            }
            Controller.saveRouteInfo(Controller.getLoggedInAsString(MainActivity.this), Controller.getRoutesLength(MainActivity.this), description);
            Controller.sendRoute(mGoogleMap, "tmp", MainActivity.this);
            Controller.updateRoutesLength(Controller.getLoggedInAsString(MainActivity.this), Controller.getRoutesLength(MainActivity.this));
        }).setNegativeButton(R.string.answer_back, (dialog, arg1) -> {});

        builder.setCancelable(true);
        builder.setView(layout);
        return builder.create();
    }

    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ){//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                    123);
        }
    }

    private AlertDialog createNewPlace(final LatLng latLng) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        final View layout = inflater.inflate(R.layout.dialog_new_place, null);

        mBitmap = null;
        mUri = null;
        mLastAction = 0;

        mImageView = layout.findViewById(R.id.new_place_image);
        mImageView.setOnClickListener(v -> {
            AlertDialog.Builder builderInner = new AlertDialog.Builder(MainActivity.this);
            builderInner.setMessage("Do you want to open gallery or take photo?");
            builderInner.setTitle("Adding photo");
            builderInner.setPositiveButton("Gallery", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_PICK);

                mLastAction = 1;
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY_INTENT);
            });
            builderInner.setNeutralButton("Camera", (dialog, which) -> {
                mLastAction = 2;
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                startActivityForResult(cameraIntent, CAMERA_RESULT);
            });
            builderInner.setNegativeButton(R.string.answer_cancel, (dialog, which) -> Toast.makeText(MainActivity.this, "cancel", Toast.LENGTH_SHORT).show());
            builderInner.show();

        });

        builder.setPositiveButton(R.string.answer_finish, (dialog, id) -> {
            String[] placeInfo = getPlaceInfo(layout);
            if (mLastAction == 1) {
                Controller.saveCreatedPlace(mUri, placeInfo, latLng);
            } else {
                DatabaseManager.saveCreatedPlace2(mBitmap, placeInfo, latLng);
            }
        }).setNegativeButton(R.string.answer_back, (dialog, arg1) -> {});

        builder.setCancelable(true);
        builder.setView(layout);
        return builder.create();
    }

    private String[] getPlaceInfo(final View layout) {
        EditText edName = layout.findViewById(R.id.place_name);
        EditText edDescription = layout.findViewById(R.id.place_description);
        EditText edTags = layout.findViewById(R.id.place_tags);

        String[] result = new String[3];
        result[0] = edName.getText().toString();
        result[1] = edDescription.getText().toString();
        result[2] = edTags.getText().toString();
        return result;

    }

    private void showDescriptionDialog(final Marker marker) {
        Controller.runDescriptionDialog(MainActivity.this, marker, myPosition, mGoogleMap);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}
}