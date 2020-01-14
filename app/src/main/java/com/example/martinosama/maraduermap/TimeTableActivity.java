package com.example.martinosama.maraduermap;

import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

public class TimeTableActivity extends AppCompatActivity {


    private ViewPager mViewPager;
    private ViewPagerAdaptor viewPagerAdaptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_table);

        viewPagerAdaptor = new ViewPagerAdaptor(getSupportFragmentManager());
        viewPagerAdaptor.addFragment(new AddTimeTable(),"Add Courses");
        viewPagerAdaptor.addFragment(new ViewTimeTable(),"Your Courses");
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(viewPagerAdaptor);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs1);

        tabLayout.setupWithViewPager(mViewPager);

    }
}
