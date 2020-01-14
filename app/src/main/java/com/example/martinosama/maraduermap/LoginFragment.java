package com.example.martinosama.maraduermap;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressLint("ValidFragment")
public class LoginFragment extends Fragment {
    View view;
    public static String LOGGED_IN=null;
    MainActivity mainActivity;
    public LoginFragment(MainActivity mainActivity){
        this.mainActivity =mainActivity;
    }
    private SharedPreferences sharedPreferences;
    DatabaseReference mDatabase;
    @BindView(R.id.password_field)
    EditText passwordText;
    @BindView(R.id.idField)
    EditText idText;
    @BindView(R.id.loginButton)
    Button loginButton;
    @BindView(R.id.login_layout)
    ConstraintLayout constraintLayout;
    String ID,pass;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.log_in, container, false);
        ButterKnife.bind(this,view);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        idText.setText(sharedPreferences.getString("USERNAME",null));
        passwordText.setText(sharedPreferences.getString("PASSWORD",null));
        mDatabase = FirebaseDatabase.getInstance().getReference().child("StudentInfo");
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            ID = idText.getText().toString();
                            pass = passwordText.getText().toString();
                            if (dataSnapshot.child(ID) != null) {
                                if (dataSnapshot.child(ID).child("password").getValue().toString().equals(pass)) {
                                   SharedPreferences.Editor editor = sharedPreferences.edit();
                                   editor.putString("USERNAME",idText.getText().toString());
                                   editor.putString("PASSWORD",passwordText.getText().toString());
                                   editor.putBoolean("AVAILABILITY",(Boolean)dataSnapshot.child(ID).child("available").getValue());
                                   editor.apply();
                                    Log.i("Tender","JKS");
                                    mainActivity.replaceToFriendFragment();
                                } else Log.i("Error", "WRONG PASSWORD");
                            }
                        } catch (NullPointerException ex) {
                            Log.i("Error", "WRONG ID");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("USERNAME",sharedPreferences.getString("USERNAME",null));
        outState.putString("PASS",sharedPreferences.getString("PASSWORD",null));
        super.onSaveInstanceState(outState);
    }
}