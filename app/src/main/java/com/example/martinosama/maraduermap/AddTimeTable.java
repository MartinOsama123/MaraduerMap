package com.example.martinosama.maraduermap;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.martinosama.maraduermap.FriendsFragment.currentStudentID;

/**
 * Created by Mosaad on 7/7/2018.
 */

public class AddTimeTable extends Fragment {
    View v;
    @BindView(R.id.lectureDaySpinner)
    Spinner lectureDaySpinner;
    @BindView(R.id.sectionDaySpinner)
    Spinner sectionDaySpinner;
    @BindView(R.id.courseCodeSpinner)
    Spinner courseCodeSpinner;
    @BindView(R.id.lectureDay)
    LinearLayout lectureLayout;
    @BindView(R.id.sectionDay)
    LinearLayout sectionLayout;
    @BindView(R.id.addCourseBtn)
    Button button;
    @BindView(R.id.lectureSep)
    View lectureSep;
    @BindView(R.id.secionSep)
    View sectionSep;
    @BindView(R.id.lectureText)TextView lectureText;
    @BindView(R.id.sectionTextView)TextView sectionText;
    @BindView(R.id.lectureTime)LinearLayout lectureTime;
    @BindView(R.id.sectionTime)LinearLayout sectionTime;
    @BindView(R.id.lectureData)TextView lectureData;
    @BindView(R.id.sectionData) TextView sectionData;

    private ArrayList<String> coursesCode = new ArrayList<String>();
    private ArrayList<String> lectureDay = new ArrayList<String>();
    private ArrayList<String> lectureRoom = new ArrayList<String>();
    private ArrayList<String> lectureTimes = new ArrayList<String>();
    private ArrayList<String> sectionDay = new ArrayList<String>();
    private ArrayList<String> sectionRoom = new ArrayList<String>();
    private ArrayList<String> sectionTimes = new ArrayList<String>();

    private boolean flag=false;
    private  int lecDayPosition,secDayPosition;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.add_time_fragmet, container, false);
        ButterKnife.bind(this,v);
        coursesCode.add("Choose course Code");
        final DatabaseReference courseCodeDatabase = FirebaseDatabase.getInstance().getReference().child("Summer Course");
        courseCodeDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    String d = data.getKey();
                    coursesCode.add(d);
                }
                ArrayAdapter<String> courseCodeSpinnerAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, coursesCode);
                courseCodeSpinner.setAdapter(courseCodeSpinnerAdapter);
                courseCodeSpinner.setSelection(0);
                courseCodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(final AdapterView<?> parent, View view, final int position, long id) {
                        lectureDay.clear();lectureRoom.clear();lectureTimes.clear();
                        sectionDay.clear();sectionRoom.clear();sectionTimes.clear();

                        if(position != 0){
                            for (int i=0;i<coursesCode.size();i++){
                                if (i==position){
                                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                                        String d = data.getKey();
                                        if (d.equals(parent.getItemAtPosition(i).toString())){
                                            for (DataSnapshot data1 : data.getChildren()){
                                             if (data1.getKey().toString().contains("Lecture")){
                                                 HashMap<String,String> lectureData = (HashMap<String, String>) data1.getValue();
                                                 lectureDay.add(lectureData.get("Day"));
                                                 lectureRoom.add(lectureData.get("Room"));
                                                 lectureTimes.add(lectureData.get("Time"));
                                             }
                                             else {
                                                 HashMap<String,String> sectionData = (HashMap<String, String>) data1.getValue();
                                                 sectionDay.add(sectionData.get("Day"));
                                                 sectionRoom.add(sectionData.get("Room"));
                                                 sectionTimes.add(sectionData.get("Time"));
                                             }
                                            }
                                        }
                                    }
                                }
                            }
                            ArrayAdapter<String> lectureDaySpinnerAdapter = new ArrayAdapter<String>(getActivity(),
                                    android.R.layout.simple_spinner_item, lectureDay);
                            ArrayAdapter<String> sectionDaySpinnerAdapter = new ArrayAdapter<String>(getActivity(),
                                    android.R.layout.simple_spinner_item, sectionDay);
                            lectureDaySpinner.setAdapter(lectureDaySpinnerAdapter);
                            sectionDaySpinner.setAdapter(sectionDaySpinnerAdapter);
                            lectureDaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    lectureData.setText("Time: "+lectureTimes.get(position)+" Loc: "+lectureRoom.get(position));
                                    lecDayPosition = position;
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {

                                }
                            });
                            sectionDaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    sectionData.setText("Time: "+sectionTimes.get(position)+" Loc: "+sectionRoom.get(position));
                                    secDayPosition = position;
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {

                                }
                            });

                            lectureDaySpinner.setVisibility(View.VISIBLE);
                            sectionDaySpinner.setVisibility(View.VISIBLE);
                            lectureLayout.setVisibility(View.VISIBLE);
                            sectionLayout.setVisibility(View.VISIBLE);
                            lectureSep.setVisibility(View.VISIBLE);
                            sectionSep.setVisibility(View.VISIBLE);
                            lectureText.setVisibility(View.VISIBLE);
                            sectionText.setVisibility(View.VISIBLE);
                            lectureTime.setVisibility(View.VISIBLE);
                            sectionTime.setVisibility(View.VISIBLE);
                            button.setVisibility(View.VISIBLE);
                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    DatabaseReference addCourseCodeDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("coursesCodes");

                                    final HashMap<String,HashMap<String,String>> addedCourseData = new HashMap<String,HashMap<String, String>>();
                                    HashMap<String,String> courseData = new HashMap<String, String>();
                                    courseData.put("Day",lectureDay.get(lecDayPosition));
                                    courseData.put("Room",lectureRoom.get(lecDayPosition));
                                    courseData.put("Time",lectureTimes.get(lecDayPosition));
                                    addedCourseData.put("Lecture",courseData);
                                    courseData = new HashMap<String, String>();
                                    courseData.put("Day",sectionDay.get(secDayPosition));
                                    courseData.put("Room",sectionRoom.get(secDayPosition));
                                    courseData.put("Time",sectionTimes.get(secDayPosition));
                                    addedCourseData.put("Section",courseData);
                                    addCourseCodeDatabase.runTransaction(new Transaction.Handler() {
                                        @Override
                                        public Transaction.Result doTransaction(MutableData mutableData) {
                                            HashMap<String,HashMap<String,HashMap<String,String>>> currentValue = (HashMap<String,HashMap<String,HashMap<String,String>>>) mutableData.getValue();
                                            Map.Entry<String, HashMap<String, HashMap<String, String>>> toGetKey =  currentValue.entrySet().iterator().next();
                                            String code = toGetKey.getKey();
                                            if (code.equals("courseCode")) {
                                                currentValue.clear();
                                                currentValue.put(parent.getItemAtPosition(position).toString(),addedCourseData);
                                            } else {
                                                if (code.equals(parent.getItemAtPosition(position).toString())) {
                                                    flag=true;
                                                }
                                                else {
                                                    currentValue.put(parent.getItemAtPosition(position).toString(),addedCourseData);
                                                    flag=false;
                                                }
                                            }
                                            mutableData.setValue(currentValue);
                                            return Transaction.success(mutableData);
                                        }

                                        @Override
                                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                                        }
                                    });
                                    if (flag==false){
                                        Toast.makeText(getContext(), "You already added this course.", Toast.LENGTH_SHORT).show();
                                    }else
                                        Toast.makeText(getContext(), "Course Added.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            if (sectionDay.size()==0){
                                sectionSep.setVisibility(View.GONE);
                                sectionLayout.setVisibility(View.GONE);
                                sectionData.setVisibility(View.GONE);
                                sectionText.setVisibility(View.GONE);
                            }
                        }
                        else {
                            lectureDaySpinner.setVisibility(View.INVISIBLE);
                            sectionDaySpinner.setVisibility(View.INVISIBLE);
                            lectureLayout.setVisibility(View.INVISIBLE);
                            sectionLayout.setVisibility(View.INVISIBLE);
                            lectureSep.setVisibility(View.INVISIBLE);
                            sectionSep.setVisibility(View.INVISIBLE);
                            lectureText.setVisibility(View.INVISIBLE);
                            sectionText.setVisibility(View.INVISIBLE);
                            lectureTime.setVisibility(View.INVISIBLE);
                            sectionTime.setVisibility(View.INVISIBLE);
                            button.setVisibility(View.INVISIBLE);
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return v;
    }
}
