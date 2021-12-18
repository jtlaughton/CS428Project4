package com.example.project4;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.List;

// Fragment for player number and guesses
public class PlayerFragment extends Fragment {
    private static final String TAG = "PlayerFragment";
    private ListView moveList = null;
    private TextView number = null;
    private ArrayAdapter<String> adapter;

    // inflate our fragment with player_contents layout file
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player_contents, container, false);
    }

    // when view is created get the moveList and number from our the view that was created
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        moveList = (ListView) getView().findViewById(R.id.moves);
        number = (TextView) getView().findViewById(R.id.numberView);
        super.onViewCreated(view, savedInstanceState);
    }

    // make it so moveList focuses toward the bottom and that there is no choice option
    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        // Set the list choice mode to allow only one selection at a time
        moveList.setChoiceMode(ListView.CHOICE_MODE_NONE);
        moveList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
    }

    // sets the text for the players number
    public void setNumber(String num){
        number.setText("Number: " + num);
    }

    // set adapter based on type of player
    public void setOurAdapter(String type){
        switch(type){
            // if player one type adapter should be for playerOneGuesses
            case "PLAYER1":
                adapter = new ArrayAdapter<String>(
                        getActivity(),
                        R.layout.move_item,
                        MainActivity.playerOneGuesses
                );
                moveList.setAdapter(adapter);
                return;
            // otherwise it should be for playerTwoGuesses
            default:
                adapter = new ArrayAdapter<String>(
                        getActivity(),
                        R.layout.move_item,
                        MainActivity.playerTwoGuesses
                );
                moveList.setAdapter(adapter);
                return;
        }
    }

    // refresh the list view when new data is added
    public void updateAdapter(){
        adapter.notifyDataSetChanged();
    }
}

