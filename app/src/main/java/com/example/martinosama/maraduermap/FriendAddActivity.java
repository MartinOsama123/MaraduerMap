package com.example.martinosama.maraduermap;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class FriendAddActivity extends AppCompatActivity implements FriendListAdapter.RecyclerViewAdapterOnClickHandler{

    private EditText studentSearchEditText;
    private RecyclerView studentListRecyclerView;

    private DatabaseReference studentsDatabase;
    private DatabaseReference friendsDatabase;
    private DatabaseReference addFriendDatabase;

    private LinkedHashMap<String,String> studentListNamesWithIDForSearch;

    private String currentStudentID;
    private LinkedHashMap<String,String> studentsIDs;
    private ArrayList<Long> currentStudentFriendsListIDs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_add);

        currentStudentID = getIntent().getStringExtra("ID");
        currentStudentFriendsListIDs = (ArrayList<Long>) getIntent().getSerializableExtra("Friends");
        studentsIDs = new LinkedHashMap<String, String>();
        studentListNamesWithIDForSearch = new LinkedHashMap<>();

        ActionBar actionBar = this.getSupportActionBar();
        studentSearchEditText = (EditText)findViewById(R.id.studentNameOrIdEditText);
        studentListRecyclerView = (RecyclerView) findViewById(R.id.studentsRecyclerView);

        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        studentListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        studentsDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo");

        //DISPLAY STUDENTS WITH NULL LIST
        studentListRecyclerView.setAdapter(new FriendListAdapter(FriendAddActivity.this, studentListNamesWithIDForSearch,0,"", FriendAddActivity.this));
        //DISPLAY STUDENTS WITH NULL LIST

        studentsDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data:dataSnapshot.getChildren()) {
                    String studentName = (String)data.child("name").getValue();
                    String studentID = data.getKey();
                    studentsIDs.put(studentID,studentName);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //DISPLAY STUDENTS THAT STARTS WITH USER'S INPUT
        studentSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                String userInput = charSequence.toString().toLowerCase();

                studentListNamesWithIDForSearch.clear();
                if (userInput.isEmpty()) {
                    studentListRecyclerView.setAdapter(new FriendListAdapter(FriendAddActivity.this, studentListNamesWithIDForSearch, 0, "", FriendAddActivity.this));
                    return;
                }
                if (studentsIDs.size() != 0 && currentStudentFriendsListIDs.size()!=0) {
                    for (Map.Entry<String, String> entry : studentsIDs.entrySet()) {
                        String studentID = entry.getKey();
                        String studentName = entry.getValue();
                        String studentNameToLower = studentName.toLowerCase();

                        if (studentID.startsWith(userInput)
                                || studentNameToLower.startsWith(userInput)) {

                            if (studentID.equals(currentStudentID)) {
                                continue;
                            }

                            studentListNamesWithIDForSearch.put(studentID, studentName);
                        }
                    }
                    for (int j = 0; j < currentStudentFriendsListIDs.size(); j++) {
                        String friendId = currentStudentFriendsListIDs.get(j).toString();
                        studentListNamesWithIDForSearch.remove(friendId);
                    }
                    studentListRecyclerView.setAdapter(new FriendListAdapter(FriendAddActivity.this, studentListNamesWithIDForSearch, 0, "", FriendAddActivity.this));
                }else {
                    Toast.makeText(FriendAddActivity.this, "You are not Connected to the internet.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        //DISPLAY STUDENTS THAT STARTS WITH USER'S INPUT
    }

    private void addFriend(final String addedFriendID) {

        friendsDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("pendingfriends");

        friendsDatabase.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                ArrayList<Long> currentValue = (ArrayList<Long>) mutableData.getValue();
                if (currentValue.get(0) == 0) {
                    currentValue.clear();
                    currentValue.add(Long.valueOf(addedFriendID));
                } else {
                    currentValue.add(Long.valueOf(addedFriendID));
                }
                mutableData.setValue(currentValue);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

        addFriendDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(addedFriendID).child("friendrequests");

        addFriendDatabase.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                ArrayList<Long> currentValue = (ArrayList<Long>) mutableData.getValue();
                if (currentValue.get(0) == 0) {
                    currentValue.clear();
                    currentValue.add(Long.valueOf(currentStudentID));
                } else {
                    currentValue.add(Long.valueOf(currentStudentID));
                }
                mutableData.setValue(currentValue);
                return Transaction.success(mutableData);
            }
            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            }
        });
    }

    @Override
    public void onClickListener(final String studentID,String studentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(FriendAddActivity.this);

        builder.setTitle("Add "+studentName+" as a friend?");

        builder.setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String studentName="";
                        addFriend(studentID);
                        currentStudentFriendsListIDs.add(Long.valueOf(studentID));
                        studentName = studentsIDs.get(studentID);
                        Toast.makeText(FriendAddActivity.this, "A request was sent to "+studentName+".", Toast.LENGTH_SHORT).show();
                        dialog.cancel();

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }
}
