package com.example.martinosama.maraduermap;

import android.*;
import android.Manifest;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.martinosama.maraduermap.SQLite.MapContract;
import com.example.martinosama.maraduermap.SQLite.MapDbHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
  @BindView(R.id.tablayout_id) TabLayout tabLayout;
  @BindView(R.id.viewpager_id) ViewPager viewPager;

  MainActivity mainActivity = this;
  MapFragment mapFragment = new MapFragment(mainActivity);
  LoginFragment loginFragment = new LoginFragment(mainActivity);
  FriendsFragment friendsFragment = new FriendsFragment(mapFragment,mainActivity);

    private ViewPagerAdaptor viewPagerAdaptor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        viewPagerAdaptor = new ViewPagerAdaptor(getSupportFragmentManager());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},100);
        }
        if(sharedPreferences.getString("USERNAME",null) == null) {
            viewPagerAdaptor.addFragment(mapFragment,"");
            viewPagerAdaptor.addFragment(new LoginFragment(mainActivity),"");
            viewPager.setAdapter(viewPagerAdaptor);
            tabLayout.setupWithViewPager(viewPager, true);
            tabLayout.getTabAt(0).setIcon(R.drawable.ic_map);
            tabLayout.getTabAt(1).setIcon(R.drawable.ic_account);
        }
        else {

            viewPagerAdaptor.addFragment(mapFragment,"");
            viewPagerAdaptor.addFragment(new FriendsFragment(mapFragment,mainActivity),"");
            viewPager.setAdapter(viewPagerAdaptor);
            tabLayout.setupWithViewPager(viewPager, true);
            tabLayout.getTabAt(0).setIcon(R.drawable.ic_map);
            tabLayout.getTabAt(1).setIcon(R.drawable.ic_people_black_24dp);
        }
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    public void replaceToLoginFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().remove(new FriendsFragment(mapFragment,mainActivity)).commit();
        fragmentManager.beginTransaction().add(R.id.friendsFrameLayout,new LoginFragment(mainActivity)).commit();
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_account);
    }
    public void replaceToFriendFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().remove(new LoginFragment(mainActivity)).commit();
        fragmentManager.beginTransaction().add(R.id.loginFrameLayout,new FriendsFragment(mapFragment,mainActivity)).commit();
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_people_black_24dp);
    }
    public void showMap(){
        viewPager.setCurrentItem(0,true);
    }

}
