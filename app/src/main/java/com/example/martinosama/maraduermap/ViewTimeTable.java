package com.example.martinosama.maraduermap;

import android.os.Bundle;
import android.support.annotation.Nullable;
import  android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.example.martinosama.maraduermap.FriendsFragment.currentStudentID;


public class ViewTimeTable extends Fragment {
    View v;
    TextView textView;
    RecyclerView timeTableRecycleView;
    String id;
    DatabaseReference friendRequestsDatabase,studentsDatabase;
    private LinkedHashMap<String,String> currentStudentFriendRequestsListNamesWithIDs = new LinkedHashMap<String ,String >();

    private ArrayList<String> studentCourseIDs =new ArrayList<String>();
    private ArrayList<String> courseIDs = new ArrayList<String >();


    public ViewTimeTable() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_time_table,container,false);

//        timeTableRecycleView = (RecyclerView)v.findViewById(R.id.courseRecycle);
//        timeTableRecycleView.setLayoutManager(new LinearLayoutManager(getActivity()));
//
//        friendRequestsDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("coursesCodes");
//        studentsDatabase = FirebaseDatabase.getInstance().getReference("Summer Course");
//        studentsDatabase.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                for (DataSnapshot data:dataSnapshot.getChildren()) {
//
//                    String studentID = data.getKey();
//                    courseIDs.add(studentID);
//                }
//                friendRequestsDatabase.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        currentStudentFriendRequestsListNamesWithIDs.clear();
//                        studentCourseIDs = (ArrayList<String>) dataSnapshot.getValue();
//
//                        for (int i = 0; i < studentCourseIDs.size(); i++) {
//                            id = String.valueOf(studentCourseIDs.get(i));
//                            for (int j = 0; j < courseIDs.size(); j++) {
//                                if (studentCourseIDs.get(i).toString().equals(courseIDs.get(j))) {
//                                    currentStudentFriendRequestsListNamesWithIDs.put(id, courseIDs.get(j));
//                                    break;
//                                }
//                            }
//                        }
//                        timeTableRecycleView.setAdapter(new FriendListAdapter(getActivity(), currentStudentFriendRequestsListNamesWithIDs, 1,currentStudentID, null));
//
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//
//                    }
//                });
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });

        return v;
    }
}

