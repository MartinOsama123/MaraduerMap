package com.example.martinosama.maraduermap;

import android.os.Bundle;
import android.support.annotation.Nullable;
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
import java.util.Objects;

import static com.example.martinosama.maraduermap.FriendsFragment.currentStudentID;

/**
 * Created by Mosaad on 6/3/2018.
 */

public class FriendRequestFragment extends android.support.v4.app.Fragment {
    View v;
    TextView textView;
    RecyclerView friendRequestRecyclerView;
    String id;
    DatabaseReference friendRequestsDatabase,studentsDatabase;
    private LinkedHashMap<String,String> currentStudentFriendRequestsListNamesWithIDs = new LinkedHashMap<String ,String >();

    private ArrayList<Long> currentStudentFriendRequestsListIDs=new ArrayList<Long>();
    private LinkedHashMap<String,String> studentsIDs = new LinkedHashMap<String, String>();


    public FriendRequestFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_friend_requests,container,false);

        friendRequestRecyclerView = (RecyclerView)v.findViewById(R.id.allFriendRequestsRecyclerView);
        friendRequestRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        friendRequestsDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("friendrequests");
        studentsDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo");
        studentsDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data:dataSnapshot.getChildren()) {

                    String studentName = (String)data.child("name").getValue();
                    String studentID = data.getKey();
                    studentsIDs.put(studentID,studentName);
                }
                friendRequestsDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        currentStudentFriendRequestsListNamesWithIDs.clear();
                        currentStudentFriendRequestsListIDs = (ArrayList<Long>) dataSnapshot.getValue();

                        for (int i = 0; i < currentStudentFriendRequestsListIDs.size(); i++) {
                            id = String.valueOf(currentStudentFriendRequestsListIDs.get(i));
                            if (!Objects.equals(id, "0"))
                            currentStudentFriendRequestsListNamesWithIDs.put(id, studentsIDs.get(id));

                        }
                        friendRequestRecyclerView.setAdapter(new FriendListAdapter(getActivity(), currentStudentFriendRequestsListNamesWithIDs, 1,currentStudentID, null));

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return v;
    }
}

