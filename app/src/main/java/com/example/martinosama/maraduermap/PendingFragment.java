package com.example.martinosama.maraduermap;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
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

public class PendingFragment extends Fragment {
    View v;
    RecyclerView pendingRequestsRecyclerView;
    String id;
    DatabaseReference pendingFriendsDatabase,studentsDatabase;
    private LinkedHashMap<String,String> currentStudentPendingFriendListNamesWithIDs = new LinkedHashMap<String ,String >();

    private ArrayList<Long> currentStudentPendingFriendListIDs=new ArrayList<Long>();
    private LinkedHashMap<String,String> studentsIDs = new LinkedHashMap<String, String>();


    public PendingFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_pending,container,false);


        pendingRequestsRecyclerView = (RecyclerView)v.findViewById(R.id.allPendingFriendsRecyclerView);
        pendingRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        pendingFriendsDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("pendingfriends");
        studentsDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo");
        studentsDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data:dataSnapshot.getChildren()) {

                    String studentName = (String)data.child("name").getValue();
                    String studentID = data.getKey();
                    studentsIDs.put(studentID,studentName);
                }
                pendingFriendsDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        currentStudentPendingFriendListNamesWithIDs.clear();
                        currentStudentPendingFriendListIDs = (ArrayList<Long>) dataSnapshot.getValue();

                        for (int i = 0; i < currentStudentPendingFriendListIDs.size(); i++) {
                            id = String.valueOf(currentStudentPendingFriendListIDs.get(i));
                            if (!Objects.equals(id, "0"))
                            currentStudentPendingFriendListNamesWithIDs.put(id, studentsIDs.get(id));

                        }
                        pendingRequestsRecyclerView.setAdapter(new FriendListAdapter(getActivity(), currentStudentPendingFriendListNamesWithIDs, 2,currentStudentID, null));

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
