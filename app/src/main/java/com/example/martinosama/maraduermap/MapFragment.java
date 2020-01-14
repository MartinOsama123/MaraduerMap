package com.example.martinosama.maraduermap;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.martinosama.maraduermap.SQLite.MapContract;
import com.example.martinosama.maraduermap.SQLite.MapDbHelper;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;


@SuppressLint("ValidFragment")
public class MapFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private View v;
    @BindView(R.id.mapView)
     PhotoView imageView;
    @BindView(R.id.spinnerFloor)
     Spinner spinner;
    @BindView(R.id.upButton)
     ImageButton upBtn;
    @BindView(R.id.downButton)
     ImageButton downBtn;
    @BindView(R.id.currentLocationSpinner)
     Spinner source;
    @BindView(R.id.DestinationSpinner)
     Spinner destination;
    @BindView(R.id.legendButton)
     ImageButton legendBtn;
    @BindView(R.id.getLocation)
    ImageButton floatingActionButton;
    SwitchCompat iconSwitch;

    final String ROOM212 = "00:24:a8:b1:9e:11";
    final String ITDEAN = "c8:cb:b8:9f:86:71";
    final String SQUARE = "00:24:a8:b1:ee:51";

    WifiManager wifiManager;
    WifiReceiver receiver;
    List<ScanResult> wifiList;
    WifiInfo wifiInfo;

    private boolean flagWifi=false;

    private SQLiteDatabase mDb;
    private ArrayAdapter<CharSequence> floor1Points,floor2Points,floor3Points,floor4Points,basementPoints;
    private Bitmap bitmap,tempBitmap, elevatorIcon,stairsIcon,WC,currentLocation,dest,ATM,bank,college,friendBitmap,offlineFriend;
    private Canvas canvas;
    private float scale;
    private Cursor cursor;
    private int sourceID=-1,destID=-1,floorLevel=2;
    private PointF sourcePoint= null,destPoint = null;
    private boolean sourceSwitch=false,destSwitch = false;
    private ShortestPath shortestPath;
    private ArrayList<Integer> paths;
    private boolean checkSwitch;
    private int points = -1,temp = -1;
    private boolean flagForFriend = false;
    private String destinationString;
    private String friendName,friendLastKnownLocation,friendID;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    MainActivity mainActivity;
    public MapFragment(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.map,container,false);
        ButterKnife.bind(this,v);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        checkSwitch = sharedPreferences.getBoolean("ICONS",false);
        scale = getResources().getDisplayMetrics().density;
        ATM = BitmapFactory.decodeResource(getResources(),R.drawable.atm);
        bank = BitmapFactory.decodeResource(getResources(),R.drawable.bank);
        elevatorIcon = BitmapFactory.decodeResource(getResources(),R.drawable.elevator);
        stairsIcon = BitmapFactory.decodeResource(getResources(),R.drawable.stairs);
        WC =  BitmapFactory.decodeResource(getResources(),R.drawable.bathroom);
        currentLocation = BitmapFactory.decodeResource(getResources(),R.drawable.current_location);
        college = BitmapFactory.decodeResource(getResources(),R.drawable.college_service);
        offlineFriend = BitmapFactory.decodeResource(getResources(),R.drawable.friend_location);
        dest = BitmapFactory.decodeResource(getResources(),R.drawable.dest);
        wifiManager = (WifiManager)getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()== false){
            Toast.makeText(getActivity(), "Enable wifi to get your current location.", Toast.LENGTH_SHORT).show();
            flagWifi = wifiManager.isWifiEnabled();
            floatingActionButton.setBackground(getActivity().getResources().getDrawable(R.drawable.off_shape));
            floatingActionButton.setImageResource(R.drawable.wifi_off);

        }else {
            Toast.makeText(getActivity(), "Current location enabled.", Toast.LENGTH_SHORT).show();
            flagWifi = wifiManager.isWifiEnabled();
            floatingActionButton.setBackground(getActivity().getResources().getDrawable(R.drawable.on_shape));
            floatingActionButton.setImageResource(R.drawable.wifi_on);

        }

        Timer timer = new Timer();
        timer.schedule(new StartScan(), 0, 3000);
        receiver = new WifiReceiver();
        getActivity().registerReceiver(receiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        MapDbHelper dbHelper = new MapDbHelper(getContext());
        mDb = dbHelper.getWritableDatabase();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        if (getAllFloorRooms().getCount()==0) {
            insertFakeDataFloor1(mDb);
            insertFakeDataFloor2(mDb);
            insertFakeDataFloor3(mDb);
            insertFakeDataFloor4(mDb);
            insertFakeDataFloor0(mDb);
        }
        final ArrayAdapter<CharSequence> floorLevelsSpinner = ArrayAdapter.createFromResource(this.getActivity(),R.array.dropdown,android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(floorLevelsSpinner);

        shortestPath = new ShortestPath();
        routing4();
        source.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                cursor = null;
                if(sourceSwitch && destSwitch) {
                    fetchMap(floorLevel);
                    sourceSwitch = false;
                }else if(sourceSwitch && !destSwitch){
                    fetchMap(floorLevel);
                    sourceSwitch = false; destSwitch = false;
                }
                if (floorLevel==0)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_THREE+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                else if (floorLevel==1)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_TWO+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                else if (floorLevel==2)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_ONE+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                else if (floorLevel==3)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_GROUND+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                else if (floorLevel==4)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_BASEMENT+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                if (cursor.getCount()!=0) {
                    cursor.moveToFirst();
                    float x = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_X))) * scale;
                    float y = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_Y))) * scale;
                    sourceID = Integer.parseInt(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry._ID)));
                    sourcePoint = new PointF(x,y);
                    canvas.drawBitmap(currentLocation,x-(currentLocation.getWidth()/(int)scale)/2*scale,y-(currentLocation.getHeight()/(int)scale)/2*scale,null);
                    imageView.setImageBitmap(tempBitmap);
                    sourceSwitch = true;
                }
                if(sourceSwitch && destSwitch){
                    cursor = getAllFloorRooms();
                    canvas.drawBitmap(dest,destPoint.x-(currentLocation.getWidth()/(int)scale)/2*scale,destPoint.y-(currentLocation.getHeight()/(int)scale)/2*scale,null);
                    imageView.setImageBitmap(tempBitmap);
                    paths =   shortestPath.printShortestDistance(sourceID-1,destID-1,50);
                    drawPath(0,0);
                }

                }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        destination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(sourceSwitch && destSwitch) {
                    fetchMap(floorLevel);
                     destSwitch = false;
                } else if(!sourceSwitch && destSwitch){
                fetchMap(floorLevel);
                    sourceSwitch = false; destSwitch = false;
            }
                if (floorLevel==0)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_THREE+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                else if (floorLevel==1)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_TWO+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                else if (floorLevel==2)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_ONE+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                else if (floorLevel==3)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_GROUND+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                else if (floorLevel==4)
                    cursor = mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_BASEMENT+" Where "+ MapContract.MapEntry.COLUMN_ROOM_TYPE+" = '"+ adapterView.getItemAtPosition(i).toString()+"'",null);
                if (cursor.getCount()!=0) {
                    cursor.moveToFirst();
                    float x = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_X))) * scale;
                    float y = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_Y))) * scale;
                    destID = Integer.parseInt(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry._ID)));
                    destPoint = new PointF(x,y);
                    canvas.drawBitmap(dest,x-(dest.getWidth()/(int)scale)/2*scale,y-(dest.getHeight()/(int)scale)/2*scale,null);
                    imageView.setImageBitmap(tempBitmap);
                    destSwitch = true;
                }
                if(sourceSwitch && destSwitch){
                    cursor = getAllFloorRooms();
                    canvas.drawBitmap(currentLocation,sourcePoint.x-(currentLocation.getWidth()/(int)scale)/2*scale,sourcePoint.y-(currentLocation.getHeight()/(int)scale)/2*scale,null);
                    imageView.setImageBitmap(tempBitmap);
                    paths =   shortestPath.printShortestDistance(sourceID-1,destID-1,50);
                    destinationString = adapterView.getItemAtPosition(i).toString();
                    if (FriendsFragment.currentStudentID !=null) {
                        DatabaseReference updateLastKnownLocation = FirebaseDatabase.getInstance().getReference("StudentInfo").child(FriendsFragment.currentStudentID).child("lastKnownLocation");
                        updateLastKnownLocation.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                dataSnapshot.getRef().setValue(destinationString);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                    drawPath(0,0);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner.setOnItemSelectedListener(this);
        spinner.setSelection(3);
//        cursor = getAllFloorRooms();
        upBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sourcePoint = destPoint = null;
                if (floorLevel<=4)
                    downBtn.setImageResource(R.drawable.arrow_down);
                if(floorLevel > 0){
                    fetchMap(--floorLevel);
                    setBothSpinners(floorLevel);
                    upBtn.setImageResource(R.drawable.arrow_up);
                    spinner.setSelection(floorLevel);
                }
                if(floorLevel == 0)
                    upBtn.setImageResource(R.drawable.disable_arrow_up);

            }
        });
        downBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sourcePoint = destPoint = null;
                if (floorLevel>=0)
                    upBtn.setImageResource(R.drawable.arrow_up);
                if(floorLevel < 4){
                    fetchMap(++floorLevel);
                    setBothSpinners(floorLevel);
                    downBtn.setImageResource(R.drawable.arrow_down);
                    spinner.setSelection(floorLevel);
                }
                if(floorLevel == 4)
                    downBtn.setImageResource(R.drawable.disable_arrow_down);
            }
        });
        legendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog legendDialog = new Dialog(getActivity());
                legendDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                legendDialog.setContentView(R.layout.legend_dialog);
                legendDialog.show();
                iconSwitch = (SwitchCompat)legendDialog.findViewById(R.id.iconSwitch);
                iconSwitch.setChecked(sharedPreferences.getBoolean("ICONS",false));
                iconSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        sharedPreferences.edit().putBoolean("ICONS",isChecked).apply();
                        checkSwitch = isChecked;
                        fetchMap(floorLevel);
                        if(sourcePoint != null) {
                            canvas.drawBitmap(currentLocation, sourcePoint.x - (currentLocation.getWidth() / (int) scale) / 2 * scale, sourcePoint.y - (currentLocation.getHeight() / (int) scale) / 2 * scale, null);
                            imageView.setImageBitmap(tempBitmap);
                        }if(destPoint != null){
                            canvas.drawBitmap(dest,destPoint.x-(dest.getWidth()/(int)scale)/2*scale,destPoint.y-(dest.getHeight()/(int)scale)/2*scale,null);
                            imageView.setImageBitmap(tempBitmap);
                        }
                        if(sourcePoint != null && destPoint != null){
                            cursor = getAllFloorRooms();
                            paths =   shortestPath.printShortestDistance(sourceID-1,destID-1,50);
                            drawPath(0,0);
                        }
                    }
                });
            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (flagWifi){
                    flagWifi=false;
                    Toast.makeText(getActivity(), "Current location disabled.", Toast.LENGTH_SHORT).show();
                    floatingActionButton.setBackground(getActivity().getResources().getDrawable(R.drawable.off_shape));
                    floatingActionButton.setImageResource(R.drawable.wifi_off);
                }else {
                    flagWifi=true;
                    Toast.makeText(getActivity(), "Current location enabled.", Toast.LENGTH_SHORT).show();
                    floatingActionButton.setBackground(getActivity().getResources().getDrawable(R.drawable.on_shape));
                    floatingActionButton.setImageResource(R.drawable.wifi_on);
                    wifiManager.setWifiEnabled(true);
                }
//                wifiInfo = wifiManager.getConnectionInfo();
//                if (wifiInfo!=null) {
//                    double exp = 0;
//                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//                        exp = (27.55 - (20 * Math.log10(wifiInfo.getFrequency())) + Math.abs(wifiInfo.getRssi())) / 20.0;
//
//                        double distanceM = Math.pow(10.0, exp);
//                        if (wifiInfo.getBSSID().toString().equals(ROOM212)) {
//
//                            int dbm = wifiInfo.getRssi();
//                            int percent = WifiManager.calculateSignalLevel(dbm, 101);
//                            int level = WifiManager.calculateSignalLevel(dbm, 5);
//                            String s1 = "Room 212";
//                            Toast.makeText(getActivity(), "dbm: " + wifiInfo.getRssi() + " Percent: " + percent + " dist: " + distanceM + " BSSID: " + s1, Toast.LENGTH_SHORT).show();
//                        } else if (wifiInfo.getBSSID().toString().equals(ITDEAN)) {
//
//                            int dbm = wifiInfo.getRssi();
//                            int percent = WifiManager.calculateSignalLevel(dbm, 101);
//                            int level = WifiManager.calculateSignalLevel(dbm, 5);
//                            String s1 = "3meed";
//                            Toast.makeText(getActivity(), "dbm: " + wifiInfo.getRssi() + " Percent: " + percent + " dist: " + distanceM + " BSSID: " + s1, Toast.LENGTH_SHORT).show();
//                        } else if (wifiInfo.getBSSID().toString().equals(SQUARE)) {
//
//                            int dbm = wifiInfo.getRssi();
//                            int percent = WifiManager.calculateSignalLevel(dbm, 101);
//                            int level = WifiManager.calculateSignalLevel(dbm, 5);
//                            String s1 = "Square";
//                            Toast.makeText(getActivity(), "dbm: " + wifiInfo.getRssi() + " Percent: " + percent + " dist: " + distanceM + " BSSID: " + s1, Toast.LENGTH_SHORT).show();
//
//                        } else {
//                            int dbm = wifiInfo.getRssi();
//                            int percent = WifiManager.calculateSignalLevel(dbm, 101);
//                            int level = WifiManager.calculateSignalLevel(dbm, 5);
//                            String s1 = wifiInfo.getBSSID();
//                            Toast.makeText(getActivity(), "dbm: " + wifiInfo.getRssi() + " Percent: " + percent + " dist: " + distanceM + " BSSID: " + s1, Toast.LENGTH_SHORT).show();
//
//                        }
//                    }
//                } else{
//                    Toast.makeText(getActivity(), "Current Location is not available for your mobile Version", Toast.LENGTH_SHORT).show();
//                }
            }
        });

      return v;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        sourcePoint= destPoint = null;
        floorLevel = i;
        if(floorLevel == 0)
            upBtn.setImageResource(R.drawable.disable_arrow_up);
        if(floorLevel == 4)
            downBtn.setImageResource(R.drawable.disable_arrow_down);
        if (floorLevel>0)
            upBtn.setImageResource(R.drawable.arrow_up);
        if (floorLevel<4)
            downBtn.setImageResource(R.drawable.arrow_down);
        fetchMap(i);
        setBothSpinners(i);
    }

    private void setBothSpinners(int i){
        if(i == 0){
            floor4Points = ArrayAdapter.createFromResource(this.getActivity(),R.array.Locations4,android.R.layout.simple_spinner_dropdown_item);
            source.setAdapter(floor4Points); destination.setAdapter(floor4Points);
            source.setSelection(-1); destination.setSelection(-1);
        }else if(i == 2){
            floor2Points = ArrayAdapter.createFromResource(this.getActivity(),R.array.Locations2,android.R.layout.simple_spinner_dropdown_item);
            source.setAdapter(floor2Points); destination.setAdapter(floor2Points);
            source.setSelection(-1); destination.setSelection(-1);
        }else if(i == 3){
            floor1Points = ArrayAdapter.createFromResource(this.getActivity(),R.array.Locations1,android.R.layout.simple_spinner_dropdown_item);
            source.setAdapter(floor1Points); destination.setAdapter(floor1Points);
            source.setSelection(-1); destination.setSelection(-1);
        }else if(i == 1){
            floor3Points = ArrayAdapter.createFromResource(this.getActivity(),R.array.Locations3,android.R.layout.simple_spinner_dropdown_item);
            source.setAdapter(floor3Points); destination.setAdapter(floor3Points);
            source.setSelection(-1); destination.setSelection(-1);
        }else if(i == 4){
            basementPoints= ArrayAdapter.createFromResource(this.getActivity(),R.array.Locations0,android.R.layout.simple_spinner_dropdown_item);
            source.setAdapter(basementPoints); destination.setAdapter(basementPoints);
            source.setSelection(-1); destination.setSelection(-1);
        }
    }

    private void fetchMap(int i){

        if(i == 0){
            if (flagForFriend==false) {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing4();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.third_floor);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                if (checkSwitch)
                    drawIcons(0, 0, floorLevel);
                //drawCircles(0, 0);
                imageView.setImageBitmap(tempBitmap);
            }
            else {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing4();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.third_floor);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                if (checkSwitch)
                    drawIcons(0, 0, floorLevel);
                //drawCircles(0, 0);
                drawFriend();
                imageView.setImageBitmap(tempBitmap);
                flagForFriend= false;
            }
        }
        else if(i == 1) {
            if (flagForFriend == false) {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing3();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.second_floor);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                if (checkSwitch)
                    drawIcons(5, 7, floorLevel);
                imageView.setImageBitmap(tempBitmap);
            }else {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing3();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.second_floor);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                if (checkSwitch)
                    drawIcons(5, 7, floorLevel);
                drawFriend();
                imageView.setImageBitmap(tempBitmap);
                flagForFriend=false;
            }
        }
        else if(i == 2){
            if (flagForFriend == false) {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing2();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.first_floor);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                if (checkSwitch)
                    drawIcons(0, 5, floorLevel);
                //drawCircles(0, 0);
                imageView.setImageBitmap(tempBitmap);
            }
            else {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing2();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.first_floor);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                if (checkSwitch)
                    drawIcons(0, 5, floorLevel);
                drawFriend();
                //drawCircles(0, 0);
                imageView.setImageBitmap(tempBitmap);
                flagForFriend =false;
            }
        }
        else if(i == 3){
            if (flagForFriend == false) {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing1();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ground);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                if (checkSwitch)
                    drawIcons(0, 0, floorLevel);
                imageView.setImageBitmap(tempBitmap);
            }else {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing1();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ground);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                if (checkSwitch)
                    drawIcons(0, 0, floorLevel);
                drawFriend();
                imageView.setImageBitmap(tempBitmap);
                flagForFriend = false;
            }
        }
        else if(i == 4){
            if (flagForFriend == false) {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing0();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.basement);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                Paint p = new Paint();
                if (checkSwitch)
                    drawIcons(0, 0, 4);
                imageView.setImageBitmap(tempBitmap);
            }
            else {
                cursor = getAllFloorRooms();

                shortestPath = new ShortestPath();
                paths = new ArrayList<>();
                routing0();

                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.basement);
                tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
                canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                Paint p = new Paint();
                if (checkSwitch)
                    drawIcons(0, 0, 4);
                drawFriend();
                imageView.setImageBitmap(tempBitmap);
                flagForFriend = false;
            }
        }
    }

    public void drawIcons(int scaleX,int scaleY,int floorLevel){
        if(floorLevel == 0 || floorLevel == 1 || floorLevel == 2){
            canvas.drawBitmap(getCircularIcon(elevatorIcon),(695-(elevatorIcon.getWidth()/(int)scale)/2+scaleX)*scale,(305-(elevatorIcon.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(stairsIcon),(275-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(205-(stairsIcon.getHeight()/(int)scale)/2-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(stairsIcon),(880-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(815-(stairsIcon.getHeight()/(int)scale)/2-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(stairsIcon),(760-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(335-(stairsIcon.getHeight()/(int)scale)/2-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(stairsIcon),(535-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(550-(stairsIcon.getHeight()/(int)scale)/2-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(WC),(460-(WC.getWidth()/(int)scale)/2+scaleX)*scale,(370-(WC.getHeight()/(int)scale)/2-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(WC),(720-(WC.getWidth()/(int)scale)/2+scaleX)*scale,(630-(WC.getHeight()/(int)scale)/2-scaleY)*scale,null);
        }
        else if(floorLevel == 3){
            canvas.drawBitmap(getCircularIcon(stairsIcon),(275-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(180-(stairsIcon.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(WC),(465-(WC.getWidth()/(int)scale)/2+scaleX)*scale,(350-(WC.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(WC),(735-(WC.getWidth()/(int)scale)/2+scaleX)*scale,(615-(WC.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(stairsIcon),(770-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(310-(stairsIcon.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(stairsIcon),(550-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(535-(stairsIcon.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(stairsIcon),(905-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(805-(stairsIcon.getHeight()/(int)scale)/2-scaleY)*scale,null);
            canvas.drawBitmap(ATM,(795-(ATM.getWidth()/(int)scale)/2+scaleX)*scale,(340-(ATM.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(bank,(940-(bank.getWidth()/(int)scale)/2+scaleX)*scale,(225-(bank.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(college,(820-(college.getWidth()/(int)scale)/2+scaleX)*scale,(135-(college.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
        }
        else if(floorLevel == 4){
            canvas.drawBitmap(getCircularIcon(stairsIcon),(275-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(180-(stairsIcon.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(WC),(465-(WC.getWidth()/(int)scale)/2+scaleX)*scale,(350-(WC.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(WC),(735-(WC.getWidth()/(int)scale)/2+scaleX)*scale,(615-(WC.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(stairsIcon),(770-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(310-(stairsIcon.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(getCircularIcon(stairsIcon),(550-(stairsIcon.getWidth()/(int)scale)/2+scaleX)*scale,(535-(stairsIcon.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(ATM,(795-(ATM.getWidth()/(int)scale)/2+scaleX)*scale,(340-(ATM.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(bank,(940-(bank.getWidth()/(int)scale)/2+scaleX)*scale,(225-(bank.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
            canvas.drawBitmap(college,(820-(college.getWidth()/(int)scale)/2+scaleX)*scale,(135-(college.getHeight()/(int)scale)/2-8-scaleY)*scale,null);
        }

    }

    public Bitmap getCircularIcon(Bitmap input){
        int w = input.getWidth();
        int h = input.getHeight();
        int radius = Math.min(h / 2, w / 2);
        Bitmap output = Bitmap.createBitmap(w+8 , h+8 , Bitmap.Config.ARGB_8888);
        Paint p = new Paint();
        p.setAntiAlias(true);
        Canvas c = new Canvas(output);
        c.drawARGB(0, 0, 0, 0);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle((w / 2) + 4, (h / 2) + 4, radius, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(input, 4, 4, p);
        p.setXfermode(null);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.BLACK);
        p.setStrokeWidth(10);
        c.drawCircle((w / 2) + 4, (h / 2) + 4, radius, p);
        return output;
    }
    //ICONS

    public static void insertFakeDataFloor0(SQLiteDatabase db){
        if(db == null){
            return;
        }
        //create a list of fake guests
        List<ContentValues> list = new ArrayList<ContentValues>();

        ContentValues cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 500);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 550);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 425);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 550);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Xerox");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 425);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 430);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Entrance 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 375);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 550);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "F.L college department");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 265);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 720);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Archive-Offices");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 450);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 810);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Financial management");
        list.add(cv);


        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 520);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 645);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 3");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 640);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 645);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Entrance 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 425);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 230);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 570);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 230);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Exit to MC");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 760);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 230);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 2 ");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 760);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 500);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 5");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 760);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 645);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 4");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 840);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 500);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Exit to PH");
        list.add(cv);

        try
        {
            db.beginTransaction();
            //clear the table first
            db.delete (MapContract.MapEntry.TABLE_BASEMENT,null,null);
            //go through the list and add one by one
            for(ContentValues c:list){
                db.insert(MapContract.MapEntry.TABLE_BASEMENT, null, c);
            }
            db.setTransactionSuccessful();
        }
        catch (SQLException e) {
            //too bad :(
        }
        finally
        {
            db.endTransaction();
        }

    }
    public static void insertFakeDataFloor1(SQLiteDatabase db){
        if(db == null){
            return;
        }
        //create a list of fake guests
        List<ContentValues> list = new ArrayList<ContentValues>();

        ContentValues cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 275);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 170);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 275);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 225);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Entrance 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 325);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 225);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 114B");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 420);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 225);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 114A");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 465);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 225);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 113A");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 465);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 350);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "WC 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 565);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 225);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 113B");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 600);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 225);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Registration Room");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X,850);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 225);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connect All");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 940);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 225);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Banks");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 850);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 130);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "College Student Service");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 850);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 360);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Bank Accounts");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 770);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 310);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs Entrance");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 900);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 185);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Entrance 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 465);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 470);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 3");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 550);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 535);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs Square");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 600);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 615);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 4");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 485);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 800);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 117");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 400);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 800);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 116B");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 290);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 680);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 116A");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 405);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 470);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 115");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 735);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 615);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "WC 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 860);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 615);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 102");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 860);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 755);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 101");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 860);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 805);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Entrance 3");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 905);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 805);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs 2");
        list.add(cv);

        //insert all guests in one transaction
        try
        {
            db.beginTransaction();
            //clear the table first
            db.delete (MapContract.MapEntry.TABLE_GROUND,null,null);
            //go through the list and add one by one
            for(ContentValues c:list){
                db.insert(MapContract.MapEntry.TABLE_GROUND, null, c);
            }
            db.setTransactionSuccessful();
        }
        catch (SQLException e) {
            //too bad :(
        }
        finally
        {
            db.endTransaction();
        }

    }
    public static void insertFakeDataFloor2(SQLiteDatabase db){
        if(db == null){
            return;
        }
        //create a list of fake guests
        List<ContentValues> list = new ArrayList<ContentValues>();

        ContentValues cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 280);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 195);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 280);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 320);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 217B");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 420);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 217A");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 465);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 216");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 465);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 360);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "WC 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 560);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Teacher Assistant");
        list.add(cv);


        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 660);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 212");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 745);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 211");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 805);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 285);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection Dean");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X,785);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 210");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 835);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 305);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 208");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 765);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 345);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs Dean");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 835);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 390);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 207");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 835);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 485);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 206");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 835);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 620);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 202");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 835);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 760);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 201");
        list.add(cv);



        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 835);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 805);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 885);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 805);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 725);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 620);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "WC 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 580);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 620);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 4");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 535);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 550);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs Square");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 465);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 490);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 3");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 400);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 490);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 218");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 300);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 685);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 219A");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 400);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 805);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 219B");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 485);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 805);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 220");
        list.add(cv);

        try
        {
            db.beginTransaction();
            //clear the table first
            db.delete (MapContract.MapEntry.TABLE_ONE,null,null);
            //go through the list and add one by one
            for(ContentValues c:list){
                db.insert(MapContract.MapEntry.TABLE_ONE, null, c);
            }
            db.setTransactionSuccessful();
        }
        catch (SQLException e) {
            //too bad :(
        }
        finally
        {
            db.endTransaction();
        }

    }
    public static void insertFakeDataFloor3(SQLiteDatabase db){
        if(db == null){
            return;
        }
        //create a list of fake guests
        List<ContentValues> list = new ArrayList<ContentValues>();

        ContentValues cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 285);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 190);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 285);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 420);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 317");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 470);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 316");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 470);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 365);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "WC 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 665);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 312");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 750);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 311");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 810);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 290);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection Dean");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X,790);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 245);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 310");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 838);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 300);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 308");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 770);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 345);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs Dean");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 838);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 395);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 307");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 838);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 485);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 306");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 838);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 620);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 302");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 838);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 760);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 301");
        list.add(cv);



        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 838);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 805);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 890);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 805);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 730);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 620);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "WC 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 590);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 620);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 4");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 545);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 550);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs Square");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 470);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 490);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 3");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 400);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 490);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 318A");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 305);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 595);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 318B");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 305);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 685);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 319");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 415);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 785);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 320");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 470);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 785);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 321");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 585);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 685);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 322");
        list.add(cv);

        try
        {
            db.beginTransaction();
            //clear the table first
            db.delete (MapContract.MapEntry.TABLE_TWO,null,null);
            //go through the list and add one by one
            for(ContentValues c:list){
                db.insert(MapContract.MapEntry.TABLE_TWO, null, c);
            }
            db.setTransactionSuccessful();
        }
        catch (SQLException e) {
            //too bad :(
        }
        finally
        {
            db.endTransaction();
        }

    }
    public static void insertFakeDataFloor4(SQLiteDatabase db){
        if(db == null){
            return;
        }
        //create a list of fake guests
        List<ContentValues> list = new ArrayList<ContentValues>();

        ContentValues cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 275);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 205);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 275);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 255);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 415);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 255);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 417");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 460);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 255);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 416A");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 460);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 370);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "WC 1");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 555);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 255);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 416B");
        list.add(cv);


        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 655);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 255);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 412");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 800);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 295);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection Dean");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 740);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 255);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 411");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X,780);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 255);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 410");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 830);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 315);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 408");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 760);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 355);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs Dean");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 830);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 400);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 407");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 830);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 495);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Room 406");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 830);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 630);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 402");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 830);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 770);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Lab 401");
        list.add(cv);



        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 830);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 815);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 880);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 815);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 720);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 630);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "WC 2");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 575);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 630);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 4");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 535);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 550);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Stairs Square");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 460);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 500);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Connection 3");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 395);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 510);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 418");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 305);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 605);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 419");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 305);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 695);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 420");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 395);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 785);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 421");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 480);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 785);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 422");
        list.add(cv);

        cv = new ContentValues();
        cv.put(MapContract.MapEntry.COLUMN_ROOM_X, 575);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_Y, 695);
        cv.put(MapContract.MapEntry.COLUMN_ROOM_TYPE, "Theater 423");
        list.add(cv);
        try
        {
            db.beginTransaction();
            //clear the table first
            db.delete (MapContract.MapEntry.TABLE_THREE,null,null);
            //go through the list and add one by one
            for(ContentValues c:list){
                db.insert(MapContract.MapEntry.TABLE_THREE, null, c);
            }
            db.setTransactionSuccessful();
        }
        catch (SQLException e) {
            //too bad :(
        }
        finally
        {
            db.endTransaction();
        }

    }

    private void routing0(){
        shortestPath.add_edge(0,1);
        shortestPath.add_edge(0,6);
        shortestPath.add_edge(1,3);
        shortestPath.add_edge(3,4);
        shortestPath.add_edge(4,5);
        shortestPath.add_edge(5,6);
        shortestPath.add_edge(6,0);
        shortestPath.add_edge(1,2);
        shortestPath.add_edge(2,8);
        shortestPath.add_edge(8,9);
        shortestPath.add_edge(9,10);
        shortestPath.add_edge(10,11);
        shortestPath.add_edge(11,12);
        shortestPath.add_edge(12,7);
        shortestPath.add_edge(7,6);
        shortestPath.add_edge(11,13);

    }
    private void routing1(){
        shortestPath.add_edge(0,1);
        shortestPath.add_edge(1,2);
        shortestPath.add_edge(2,3);
        shortestPath.add_edge(3,4);
        shortestPath.add_edge(4,5);
        shortestPath.add_edge(4,6);
        shortestPath.add_edge(6,7);
        shortestPath.add_edge(7,8);
        shortestPath.add_edge(8,9);
        shortestPath.add_edge(8,13);
        shortestPath.add_edge(8,10);
        shortestPath.add_edge(8,12);
        shortestPath.add_edge(8,11);
        shortestPath.add_edge(12,13);
        shortestPath.add_edge(11,9);
        shortestPath.add_edge(9,13);
        shortestPath.add_edge(10,13);
        shortestPath.add_edge(9,12);
        shortestPath.add_edge(5,14);
        shortestPath.add_edge(14,20);
        shortestPath.add_edge(14,15);
        shortestPath.add_edge(15,16);
        shortestPath.add_edge(16,21);
        shortestPath.add_edge(21,22);
        shortestPath.add_edge(22,23);
        shortestPath.add_edge(23,24);
        shortestPath.add_edge(24,25);
        shortestPath.add_edge(16,17);
        shortestPath.add_edge(17,18);
        shortestPath.add_edge(18,19);
        shortestPath.add_edge(19,20);

    }
    private void routing2(){
        shortestPath.add_edge(0,1);
        shortestPath.add_edge(1,2);
        shortestPath.add_edge(2,3);
        shortestPath.add_edge(3,4);
        shortestPath.add_edge(4,5);
        shortestPath.add_edge(4,6);
        shortestPath.add_edge(6,7);
        shortestPath.add_edge(7,8);
        shortestPath.add_edge(8,9);
        shortestPath.add_edge(8,10);
        shortestPath.add_edge(8,11);
        shortestPath.add_edge(8,12);
        shortestPath.add_edge(9,10);
        shortestPath.add_edge(9,11);
        shortestPath.add_edge(9,12);
        shortestPath.add_edge(10,11);
        shortestPath.add_edge(10,12);
        shortestPath.add_edge(11,12);
        shortestPath.add_edge(11,13);
        shortestPath.add_edge(13,14);
        shortestPath.add_edge(14,15);
        shortestPath.add_edge(15,19);
        shortestPath.add_edge(15,16);
        shortestPath.add_edge(16,17);
        shortestPath.add_edge(17,18);
        shortestPath.add_edge(19,20);
        shortestPath.add_edge(20,21);
        shortestPath.add_edge(21,22);
        shortestPath.add_edge(22,23);
        shortestPath.add_edge(23,24);
        shortestPath.add_edge(24,25);
        shortestPath.add_edge(25,26);
        shortestPath.add_edge(26,27);
        shortestPath.add_edge(27,28);
        shortestPath.add_edge(28,20);
        shortestPath.add_edge(5,22);
        shortestPath.add_edge(26,20);
    }
    private void routing3(){
        shortestPath.add_edge(0,1);
        shortestPath.add_edge(1,2);
        shortestPath.add_edge(2,3);
        shortestPath.add_edge(3,4);
        shortestPath.add_edge(3,5);
        shortestPath.add_edge(5,6);
        shortestPath.add_edge(6,7);
        shortestPath.add_edge(6,8);
        shortestPath.add_edge(6,10);
        shortestPath.add_edge(6,9);
        shortestPath.add_edge(8,7);
        shortestPath.add_edge(8,9);
        shortestPath.add_edge(8,10);
        shortestPath.add_edge(8,11);
        shortestPath.add_edge(8,10);
        shortestPath.add_edge(7,10);
        shortestPath.add_edge(7,11);
        shortestPath.add_edge(9,10);
        shortestPath.add_edge(9,11);
        shortestPath.add_edge(11,12);
        shortestPath.add_edge(11,13);
        shortestPath.add_edge(13,14);
        shortestPath.add_edge(13,17);
        shortestPath.add_edge(14,15);
        shortestPath.add_edge(15,16);
        shortestPath.add_edge(17,18);
        shortestPath.add_edge(18,19);
        shortestPath.add_edge(19,20);
        shortestPath.add_edge(20,21);
        shortestPath.add_edge(21,22);
        shortestPath.add_edge(22,23);
        shortestPath.add_edge(23,24);
        shortestPath.add_edge(24,25);
        shortestPath.add_edge(25,26);
        shortestPath.add_edge(4,20);
        shortestPath.add_edge(18,26);
    }
    private void routing4(){
        shortestPath.add_edge(0,1);
        shortestPath.add_edge(1,2);
        shortestPath.add_edge(2,3);
        shortestPath.add_edge(3,4);
        shortestPath.add_edge(3,5);
        shortestPath.add_edge(5,6);
        shortestPath.add_edge(6,7);
        shortestPath.add_edge(6,8);
        shortestPath.add_edge(6,10);
        shortestPath.add_edge(8,9);
        shortestPath.add_edge(8,7);
        shortestPath.add_edge(8,11);
        shortestPath.add_edge(9,10);
        shortestPath.add_edge(9,7);
        shortestPath.add_edge(9,11);
        shortestPath.add_edge(10,11);
        shortestPath.add_edge(10,7);
        shortestPath.add_edge(10,12);
        shortestPath.add_edge(12,13);
        shortestPath.add_edge(13,14);
        shortestPath.add_edge(14,15);
        shortestPath.add_edge(14,18);
        shortestPath.add_edge(15,16);
        shortestPath.add_edge(16,17);
        shortestPath.add_edge(18,19);
        shortestPath.add_edge(19,20);
        shortestPath.add_edge(19,27);
        shortestPath.add_edge(20,21);
        shortestPath.add_edge(21,22);
        shortestPath.add_edge(22,23);
        shortestPath.add_edge(23,24);
        shortestPath.add_edge(24,25);
        shortestPath.add_edge(25,26);
        shortestPath.add_edge(26,27);
        shortestPath.add_edge(4,21);
        shortestPath.add_edge(7,12);
    }

    //Draw Circles
    private void drawCircles(int scaleX,int scaleY){
        Paint red= new Paint(); red.setColor(Color.RED); red.setStyle(Paint.Style.FILL);red.setTextSize(50);

        int i=0;


        while (cursor.moveToPosition(i++)){

            float x = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_X)))*scale;
            float y = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_Y)))*scale;
            canvas.drawText(Integer.toString(i-1),x,y,red);
//            canvas.drawCircle(x+scaleX,y-scaleY,20,red);

        }
    }
    private void drawCircles1(){
        Paint red= new Paint(); red.setColor(Color.RED); red.setStyle(Paint.Style.FILL); red.setTextSize(50);

        int i=0;
        while (cursor.moveToPosition(i++)){
            float x = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_X)))*scale;
            float y = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_Y)))*scale;
            canvas.drawText(Integer.toString(i-1),x,y,red);

        }
    }
    //Draw Circles

    private void drawPath(int scaleX, int scaleY){
        cursor = getAllFloorRooms();
        Paint red= new Paint(); red.setColor(Color.RED); red.setStyle(Paint.Style.FILL);
        Paint black = new Paint(); black.setColor(getResources().getColor(R.color.colorAccent)); black.setStyle(Paint.Style.STROKE); black.setStrokeWidth(15f);
        cursor.moveToPosition(paths.get(0));
        float x = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_X)))*scale;
        float y = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_Y)))*scale;
        Path path1 = new Path();
        path1.moveTo(x+scaleX,y-scaleY);
        for(int i = 1;i<paths.size();i++){
            cursor.moveToPosition(paths.get(i));
            x = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_X)))*scale;
            y = Float.parseFloat(cursor.getString(cursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_Y)))*scale;
            path1.lineTo(x+scaleX,y-scaleY);
            path1.moveTo(x+scaleX,y-scaleY);

        }
        canvas.drawPath(path1,black);
    }

    private Cursor getAllFloorRooms(){
        if(floorLevel == 3)
            return mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_GROUND,null);
        else if(floorLevel == 0)
            return mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_THREE,null);
        else if(floorLevel == 1)
            return mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_TWO,null);
        else if(floorLevel == 2)
            return mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_ONE,null);
        else if(floorLevel == 4)
            return mDb.rawQuery("Select * from "+ MapContract.MapEntry.TABLE_BASEMENT,null);
        else
            return null;

    }

    public void friendOnMap(String friendLastKnownLocation,String friendName,String friendID) {
        this.friendName = friendName;
        this.friendLastKnownLocation = friendLastKnownLocation;
        this.friendID = friendID;
        String roomNumber = "";

        flagForFriend = true;
        mainActivity.showMap();

        if (friendLastKnownLocation.equals("Banks") || friendLastKnownLocation.equals("Xerox") || friendLastKnownLocation.equals("Archive-Offices")) {

        } else {
            String[] splited = friendLastKnownLocation.split("\\s+");
            roomNumber = splited[1];
        }
        if (roomNumber.startsWith("4")) {
            if (floorLevel == 0) {
                fetchMap(0);
            } else spinner.setSelection(0);

        } else if (roomNumber.startsWith("3")) {
            if (floorLevel == 1) {
                fetchMap(1);
            } else spinner.setSelection(1);
        } else if (roomNumber.startsWith("2") || friendLastKnownLocation.equals("Teacher Assistant")) {
            if (floorLevel == 2) {
                fetchMap(2);
            } else spinner.setSelection(2);
        } else if (roomNumber.startsWith("1") || friendLastKnownLocation.equals("Entrance 3")
                || friendLastKnownLocation.equals("Entrance 2") || friendLastKnownLocation.equals("College Student Service")
                || friendLastKnownLocation.equals("Banks") || friendLastKnownLocation.equals("Bank Accounts")) {
            if (floorLevel == 3) {
                fetchMap(3);
            } else spinner.setSelection(3);
        } else {
            if (floorLevel == 4) {
                fetchMap(4);
            } else spinner.setSelection(4);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    private void drawFriend() {
        Cursor newCursor=null;
       final  Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setTextSize(50f);
        if (floorLevel == 0) {
            newCursor = mDb.rawQuery("Select * from " + MapContract.MapEntry.TABLE_THREE + " Where " + MapContract.MapEntry.COLUMN_ROOM_TYPE + " = '" + friendLastKnownLocation + "'", null);
        }
        else if (floorLevel == 1) {
            newCursor = mDb.rawQuery("Select * from " + MapContract.MapEntry.TABLE_TWO + " Where " + MapContract.MapEntry.COLUMN_ROOM_TYPE + " = '" + friendLastKnownLocation + "'", null);
        }
        else if (floorLevel == 2) {
            newCursor = mDb.rawQuery("Select * from " + MapContract.MapEntry.TABLE_ONE + " Where " + MapContract.MapEntry.COLUMN_ROOM_TYPE + " = '" + friendLastKnownLocation + "'", null);
        }
        else if (floorLevel == 3) {
            newCursor = mDb.rawQuery("Select * from " + MapContract.MapEntry.TABLE_GROUND + " Where " + MapContract.MapEntry.COLUMN_ROOM_TYPE + " = '" + friendLastKnownLocation + "'", null);
        }
        else if (floorLevel == 4) {
            newCursor = mDb.rawQuery("Select * from " + MapContract.MapEntry.TABLE_BASEMENT + " Where " + MapContract.MapEntry.COLUMN_ROOM_TYPE + " = '" + friendLastKnownLocation + "'", null);
        }
        if (newCursor.getCount() != 0) {
            newCursor.moveToFirst();
            final float x = Float.parseFloat(newCursor.getString(newCursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_X))) * scale;
            final float y = Float.parseFloat(newCursor.getString(newCursor.getColumnIndex(MapContract.MapEntry.COLUMN_ROOM_Y))) * scale;
            StorageReference ref = storageReference.child("images/"+ friendID);
            canvas.drawBitmap(offlineFriend, x, y, null);
            canvas.drawText(friendName,(((x/scale))-10)*scale,(((y/scale))-10)*scale,p);
            try {
                GlideApp.with(this)
                        .asBitmap()
                        .load(ref)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                fetchMap(floorLevel);
                                friendBitmap = Bitmap.createScaledBitmap(resource,150,120,false);
                                canvas.drawBitmap(getCircularIcon(friendBitmap), x, y, null);
                                canvas.drawText(friendName,(((x/scale))-10)*scale,(((y/scale))-10)*scale,p);
                            }
                        });
            }catch (Exception e){

            }
        }
    }
    @Override
    public void onResume() {
        getActivity().registerReceiver(receiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(receiver);
        super.onPause();
    }

    private class WifiReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
         try {


            int distanceROOM212 = -1,distanceITDEAN = -1,distanceSQUARE = -1;
            Paint p = new Paint();
            p.setColor(Color.RED);
            p.setStyle(Paint.Style.FILL);
            Bitmap temp123 = BitmapFactory.decodeResource(getResources(),R.drawable.current_location);
            wifiInfo = wifiManager.getConnectionInfo();
            wifiList = wifiManager.getScanResults();
            for(int i = 0;i<wifiList.size();i++){

                if (wifiList.get(i).SSID.toString().equals("Public") && floorLevel==2) {
                    double exp = (27.55 - (20 * Math.log10(wifiList.get(i).frequency)) + Math.abs(wifiList.get(i).level)) / 20.0; double distanceMM = Math.pow(10.0, exp);
                    int distanceM = (int)distanceMM;

                    if (wifiList.get(i).BSSID.toString().equals(ROOM212)){
                        distanceROOM212 = (int)distanceM;
                    }
                    else if (wifiList.get(i).BSSID.toString().equals(ITDEAN)){
                        distanceITDEAN = (int)distanceM;
                    }
                    else if (wifiList.get(i).BSSID.toString().equals(SQUARE)){
                        distanceSQUARE = (int)distanceM;
                    }

                    if (wifiInfo != null) {
                        sourcePoint = new PointF();

                        if (wifiInfo.getBSSID().toString().equals(ROOM212)) {
                            if (distanceITDEAN!= -1 && distanceROOM212!=-1) {
                                if ((distanceROOM212 < 35 && distanceROOM212 > 5) && distanceITDEAN > 16){
                                    fetchMap(floorLevel);
                                    canvas.drawBitmap(temp123,(420-(temp123.getWidth()/(int)scale)/2)*scale,(245-(temp123.getHeight()/(int)scale)/2)*scale,null);
                                    imageView.setImageBitmap(tempBitmap);
                                    sourceID = 4;
                                    sourceSwitch = true;
                                    sourcePoint.x = 420; sourcePoint.y = 245;
                                }
                                else if ((distanceROOM212 <= 5 && distanceROOM212 >= 0) && distanceITDEAN > 16){
                                    fetchMap(floorLevel);
                                    canvas.drawBitmap(temp123,(660-(temp123.getWidth()/(int)scale)/2)*scale,(245-(temp123.getHeight()/(int)scale)/2)*scale,null);
                                    imageView.setImageBitmap(tempBitmap);
                                    sourceID = 8;
                                    sourceSwitch = true;
                                    sourcePoint.x = 660; sourcePoint.y = 245;
                                }
                                else if ((distanceROOM212 >= 0 && distanceROOM212 < 32) && distanceITDEAN < 16){
                                    fetchMap(floorLevel);
                                    canvas.drawBitmap(temp123,(805-(temp123.getWidth()/(int)scale)/2)*scale,(285-(temp123.getHeight()/(int)scale)/2)*scale,null);
                                    imageView.setImageBitmap(tempBitmap);
                                    sourceID = 10;
                                    sourceSwitch = true;
                                    sourcePoint.x =805; sourcePoint.y = 285;
                                }
                                else if ((distanceROOM212 >= 32 && distanceROOM212 < 140) && distanceITDEAN <= 32){
                                    fetchMap(floorLevel);
                                    canvas.drawBitmap(temp123,(835-(temp123.getWidth()/(int)scale)/2)*scale,(485-(temp123.getHeight()/(int)scale)/2)*scale,null);
                                    imageView.setImageBitmap(tempBitmap);
                                    sourceID = 15;
                                    sourceSwitch = true;
                                    sourcePoint.x = 835; sourcePoint.y = 485;
                                }
                                else {
                                    wifiManager.setWifiEnabled(false);
                                    wifiManager.setWifiEnabled(true);
                                }

                            }


                        } else if (wifiInfo.getBSSID().toString().equals(ITDEAN)) {
                            if (distanceITDEAN!= -1 && distanceROOM212!=-1) {
                                if ((distanceITDEAN < 70
                                        && distanceITDEAN > 8) && distanceROOM212 > 11){
                                    fetchMap(floorLevel);
                                    canvas.drawBitmap(temp123,(835-(temp123.getWidth()/(int)scale)/2)*scale,(760-(temp123.getHeight()/(int)scale)/2)*scale,null);
                                    imageView.setImageBitmap(tempBitmap);
                                    sourceID = 17;
                                    sourceSwitch = true;
                                    sourcePoint.x = 835; sourcePoint.y = 760;
                                }
                                else if ((distanceITDEAN <= 11 && distanceITDEAN >= 0) && distanceROOM212 > 8){
                                    fetchMap(floorLevel);
                                    canvas.drawBitmap(temp123,(835-(temp123.getWidth()/(int)scale)/2)*scale,(485-(temp123.getHeight()/(int)scale)/2)*scale,null);
                                    imageView.setImageBitmap(tempBitmap);
                                    sourceID = 15;
                                    sourceSwitch = true;

                                    sourcePoint.x = 835; sourcePoint.y = 485;
                                }
                                else if ((distanceITDEAN >= 0 && distanceITDEAN < 25) && distanceROOM212 < 8){
                                    fetchMap(floorLevel);
                                    canvas.drawBitmap(temp123,(805-(temp123.getWidth()/(int)scale)/2)*scale,(285-(temp123.getHeight()/(int)scale)/2)*scale,null);
                                    imageView.setImageBitmap(tempBitmap);
                                    sourceID = 10;
                                    sourceSwitch = true;
                                    sourcePoint.x = 805; sourcePoint.y = 285;
                                }
                                else {
                                    wifiManager.setWifiEnabled(false);
                                    wifiManager.setWifiEnabled(true);
                                }
                            }
                        }
                        if(destPoint != null && sourceID != -1 ){
                            paths =   shortestPath.printShortestDistance(sourceID-1,destID-1,50);
                            canvas.drawBitmap(dest,destPoint.x-(dest.getWidth()/(int)scale)/2*scale,destPoint.y-(dest.getHeight()/(int)scale)/2*scale,null);
                            imageView.setImageBitmap(tempBitmap);
                            drawPath(0,0);
                        }
                    }

                }
            }
        }catch (Exception e){
             Toast.makeText(getActivity(),"Your not not connected",Toast.LENGTH_SHORT);
         }
        }
    }

    class StartScan extends TimerTask {
        public void run() {

            if (flagWifi) {
                wifiManager.startScan();
            }
        }
    }
}
