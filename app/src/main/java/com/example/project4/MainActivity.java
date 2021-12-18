package com.example.project4;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public static ArrayList<String> playerOneGuesses = new ArrayList<>();
    public static ArrayList<String> playerTwoGuesses = new ArrayList<>();

    private int threadCount = 0;

    private int totalPlayers = 0;
    private int totalLooping = 0;
    private String playerOneNumber;
    private String playerTwoNumber;
    private int currentTurn = 0;

    private PlayerFragment playerFragOne;
    private PlayerFragment playerFragTwo;

    private TextView winnerText;
    private Button startButton;

    private boolean playerOneRunning = false;
    private boolean playerTwoRunning = false;

    // handler for the UI thread. Determines what to update based data and player
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
      public void handleMessage(Message msg){
          int what = msg.what;
          switch(what){
              // if we get a looping message increment total amount looping
              // if we get 2 looping messages that means both threads are ready to start
              case LOOPING_PLAYER_ONE:
              case LOOPING_PLAYER_TWO:
                  totalLooping++;
                  if(totalLooping == 2){
                      playerOne.playerOneHandler.sendEmptyMessage(GAME_START);
                      playerTwo.playerTwoHandler.sendEmptyMessage(GAME_START);
                  }
                  return;
              // if we finished generating player 2 increment total players, log the number for player 2,
              // and set player twos associated fragment number to the number from playerTwo
              // if we finished generating both player player one will be ready to compute the first move
              case FINISHED_GENERATING_PLAYER_TWO:
                  totalPlayers++;
                  if(totalPlayers == 2){
                      playerOne.playerOneHandler.sendEmptyMessage(FIRST_MOVE);
                  }
                  playerTwoNumber = (String)(msg.obj);
                  playerFragTwo.setNumber(playerTwoNumber);
                  return;
              // same as for player 2 but with player one instead
              case FINISHED_GENERATING_PLAYER_ONE:
                  totalPlayers++;
                  if(totalPlayers == 2){
                      playerOne.playerOneHandler.sendEmptyMessage(FIRST_MOVE);
                  }
                  playerOneNumber = (String)(msg.obj);
                  playerFragOne.setNumber(playerOneNumber);
                  return;
              // update associated views with new information from turn
              case UPDATE_PLAYER_ONE:
                  // don't do anything if we get a message from a thread that isn't paired with us
                  // this can happen if thread was in the middle of computing a turn when a new game started
                  if(msg.getData().getInt("ID") != playerTwo.ID)
                      return;
                  // if the response indicates a win for player 2 or turn count reached its time for a game over
                  // only need to check on player 1 for turn count because of the turn based nature
                  if((msg.arg1 == 4) || (currentTurn == 20)){
                      // if a win for player 2 let user know
                      // otherwise let them know that the turn count was reached
                      if(msg.arg1 == 4)
                        winnerText.setText("Player Two Wins");
                      else
                          winnerText.setText("Max Turns Reached");

                      // send game overs to bother players
                      playerOne.playerOneHandler.sendEmptyMessage(GAME_OVER);
                      playerTwo.playerTwoHandler.sendEmptyMessage(GAME_OVER);
                      return;
                  }

                  // build a string with all the turn information from player ones turn
                  String temp = "Turn: " + (currentTurn + 1) + "\n" + "Player 1 Guess: ";
                  temp += (String)(msg.obj) + "\n";
                  temp += "Player 2 Total Correct In Place: " + msg.arg1 + "\n";
                  temp += "Player 2 Total Correct Not In Place: " + msg.arg2 + "\n";
                  temp += "Player 2 Missed Digit: " + msg.getData().getInt("DIGIT");

                  // add turn information to our list of guesses and update the list
                  playerOneGuesses.add(temp);
                  playerFragOne.updateAdapter();
                  // start turn for player 2
                  playerTwo.playerTwoHandler.sendEmptyMessage(COMPUTE_TURN);
                  return;
              // same as player 1 except with roles reversed and no need for keeping track of current turn
              case UPDATE_PLAYER_TWO:
                  if(msg.getData().getInt("ID") != playerOne.ID)
                      return;
                  if(msg.arg1 == 4){
                      winnerText.setText("Player One Wins");
                      playerOne.playerOneHandler.sendEmptyMessage(GAME_OVER);
                      playerTwo.playerTwoHandler.sendEmptyMessage(GAME_OVER);
                      return;
                  }
                  String temp2 = "Turn: " + (currentTurn + 1) + "\n" + "Player 2 Guess: ";
                  temp2 += (String)(msg.obj) + "\n";
                  temp2 += "Player 1 Total Correct In Place: " + msg.arg1 + "\n";
                  temp2 += "Player 1 Total Correct Not In Place: " + msg.arg2 + "\n";
                  temp2 += "Player 1 Missed Digit: " + msg.getData().getInt("DIGIT");

                  playerTwoGuesses.add(temp2);
                  playerFragTwo.updateAdapter();

                  playerOne.playerOneHandler.sendEmptyMessage(COMPUTE_TURN);
                  // we update current turn at the end of player 2's turn
                  currentTurn++;
                  return;
          }
      }
    };

    // global runnable instances so that they can be accessed in each internal class
    private PlayerOneRunnable playerOne;
    private PlayerTwoRunnable playerTwo;

    // for seeing what type of message
    public static final int COMPUTE_TURN = 0;
    public static final int GAME_OVER = 1;
    public static final int UPDATE_PLAYER_ONE = 2;
    public static final int UPDATE_PLAYER_TWO = 3;
    public static final int GAME_START = 4;
    public static final int FIRST_MOVE = 5;
    public static final int FINISHED_GENERATING_PLAYER_ONE = 6;
    public static final int FINISHED_GENERATING_PLAYER_TWO = 7;
    public static final int LOOPING_PLAYER_ONE = 8;
    public static final int LOOPING_PLAYER_TWO = 9;

    private Thread t1;
    private Thread t2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the references for the two player fragments
        playerFragOne = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.playerOne);
        playerFragTwo = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.playerTwo);

        // set the array adapters for those fragments
        playerFragOne.setOurAdapter("PLAYER1");
        playerFragTwo.setOurAdapter("PLAYER2");

        startButton = (Button)findViewById(R.id.startGame);
        winnerText = (TextView)findViewById(R.id.winnerText);

        // start a new game on click
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // if a player one is running end it
                if(playerOneRunning){
                    // send a game over message at the start of the message queue, so it quits the looper
                    Message msg = playerOne.playerOneHandler.obtainMessage(GAME_OVER);
                    playerOne.playerOneHandler.sendMessageAtFrontOfQueue(msg);
                }
                // otherwise a player one will now be running
                else
                    playerOneRunning = true;

                // if player two is running end it
                if(playerTwoRunning){
                    // send a game over message at the start of the message queue so it quits the looper
                    Message msg = playerTwo.playerTwoHandler.obtainMessage(GAME_OVER);
                    playerTwo.playerTwoHandler.sendMessageAtFrontOfQueue(msg);
                }
                // otherwise a player two will now be running
                else
                    playerTwoRunning = true;

                // create new runnable instances
                playerOne = new PlayerOneRunnable(threadCount);
                playerTwo = new PlayerTwoRunnable(threadCount);

                // clear old data from list views
                playerOneGuesses.clear();
                playerTwoGuesses.clear();

                // update the list views
                playerFragOne.updateAdapter();
                playerFragTwo.updateAdapter();

                // reset game state tracking
                totalPlayers = 0;
                totalLooping = 0;
                currentTurn = 0;
                winnerText.setText("");

                // increment to create unique thread pair ids
                threadCount++;

                // start the runnables
                t1 = new Thread(playerOne);
                t2 = new Thread(playerTwo);

                t1.start();
                t2.start();
            }
        });
    }

    public class PlayerOneRunnable implements Runnable {
        public Handler playerOneHandler = null;
        // ensures paired threads only process messages from their opponent
        private int ID;

        // player 2 data for runnable
        private int twoNumInPosition;
        private int twoNumNotInPosition;
        private int twoMissed;

        // Data from player two
        public ArrayList<Integer> playerTwoGuess;
        public int numberCorrectInPos;
        public int numberCorrectNotPos;
        public int missedDigit;

        // Internal Data For Strategy
        private ArrayList<Integer> myNumber = new ArrayList<>();
        private ArrayList<Integer> wrongGuesses = new ArrayList<>();
        private ArrayList<Integer> currentGuess = new ArrayList<>();

        private int currentPerm = 0;
        private ArrayList<ArrayList<Integer>> permutations = new ArrayList<>();
        private boolean calculatePerms = false;

        // id is the id of the pair of threads
        PlayerOneRunnable(int id){
            ID = id;
        }

        public void run(){
            Looper.prepare();

            // create handler for different stages of the game
            playerOneHandler = new Handler(Looper.myLooper()){
              public void handleMessage(Message msg){
                int what = msg.what;
                // based on message type do different things
                switch (what){
                    case GAME_START:
                        Random rand = new Random();

                        // generate number with unique values
                        for(int i = 0; i < 4; i++){
                            int possibleData = rand.nextInt(10);

                            while(myNumber.contains(possibleData)){
                                possibleData = rand.nextInt(10);
                            }

                            myNumber.add(possibleData);
                        }

                        // send message to ui thread with number for player 1
                        // Also lets UI thread know that this thread has been fully generated
                        Message theMsg = mHandler.obtainMessage(FINISHED_GENERATING_PLAYER_ONE);
                        theMsg.obj = myNumber.toString();

                        mHandler.sendMessage(theMsg);
                        return;
                    case FIRST_MOVE:
                        // first move is a little different since there is no previous data to go off of
                        if(ID != playerTwo.ID)
                            return;

                        // make a unique random guess
                        Random rand1 = new Random();
                        ArrayList<Integer> firstGuess = new ArrayList<>();

                        // gets 4 different random numbers
                        for(int i = 0; i < 4; i++){
                            int possibleData = rand1.nextInt(10);

                            while(firstGuess.contains(possibleData)){
                                possibleData = rand1.nextInt(10);
                            }

                            firstGuess.add(possibleData);
                        }

                        // set current guess to that random guess
                        currentGuess = new ArrayList<>(firstGuess);

                        // send player two a runnable to set data fields with values from this guess
                        playerTwo.playerTwoHandler.post(() -> {
                            // we don't want to set any data if the receiving player is in a different group
                            // this can potentially happen when starting a new game
                            if(ID != playerTwo.ID)
                                return;

                            // set the data fields
                            playerTwo.playerOneGuess = new ArrayList<>(currentGuess);
                            playerTwo.numberCorrectInPos = -1;  // use -1 as a flag to indicate to player 2 it needs to make its first guess
                            playerTwo.numberCorrectNotPos = -1;
                            playerTwo.missedDigit = -1;
                        });

                        // let UI thread know player ones guess and its response to player 2
                        // arg1 is always the number of correct numbers in the correct position
                        // arg2 is always the number of correct numbers in wrong positions
                        Message msgToUI1 = mHandler.obtainMessage(UPDATE_PLAYER_ONE);
                        msgToUI1.obj = currentGuess.toString();
                        msgToUI1.arg1 = 0;
                        msgToUI1.arg2 = 0;
                        // need to send some more data so send a bundle
                        Bundle newData = new Bundle();
                        // digit is the number they got wrong
                        // since there is nothing to get wrong at this stage
                        // set to -1
                        newData.putInt("DIGIT", -1);
                        // ID is the thread pair id
                        // makes sure mHandler doesn't dispatch messages with old data
                        newData.putInt("ID", ID);
                        msgToUI1.setData(newData);
                        mHandler.sendMessage(msgToUI1);
                        return;
                    case GAME_OVER:
                        Looper.myLooper().quit();
                        playerOneRunning = false;
                        return;
                    case COMPUTE_TURN:

                        // needed to determine what to do
                        int totalCorrect = numberCorrectInPos + numberCorrectNotPos;

                        // compute the next guess

                        // this only happens if we are still off in some way because UI thread determines if there is a winner
                        if(totalCorrect == 4){
                            // if we haven't yet calculated all possible permutations of our guess with 4 correct numbers
                            // do it now
                            if(!calculatePerms){
                                permutations = getPermutations( new ArrayList<>(currentGuess));
                                calculatePerms = true;
                            }
                            // otherwise compute as normal
                            else
                                computeFour();
                        }
                        // if we are bellow 4 correct eliminate incorrect guesses one by one
                        else{
                            computeBelowFour();
                        }

                        // Gather info for player 2
                        int numInPosition = 0;
                        int numNotInPosition = 0;

                        ArrayList<Integer> missedNumbers = new ArrayList<>();

                        // get number in position, number correct but not in position, and the missed digits
                        for(int i = 0; i < 4; i++){
                            Integer number = playerTwoGuess.get(i);

                            if(myNumber.get(i).equals(number))
                                numInPosition++;
                            else if(myNumber.contains(number))
                                numNotInPosition++;
                            else
                                missedNumbers.add(number);
                        }

                        // generator to pick a random digit
                        Random random = new Random();
                        int upperBound = missedNumbers.size();

                        // pick a random missed digit if number correct < 4
                        // otherwise set it to -1 so it wont be used
                        if(((numInPosition + numNotInPosition) < 4) && (missedNumbers.size() > 0))
                            twoMissed = missedNumbers.get(random.nextInt(upperBound));
                        else
                            twoMissed = -1;

                        // sleep for human reading
                        try { Thread.sleep(2000); }
                        catch (InterruptedException e) { System.out.println("Thread interrupted!") ; }

                        // set the instance fields so they're effectively final
                        twoNumInPosition = numInPosition;
                        twoNumNotInPosition = numNotInPosition;

                        // send to playerTwoHandler so that it will set the corresponding instance fields with a runnable
                        playerTwo.playerTwoHandler.post(() -> {
                            // make sure not to do anything if IDs dont match
                            if(ID != playerTwo.ID)
                                return;
                            //otherwise send the information computed this turn to playerTwo
                            playerTwo.playerOneGuess = new ArrayList<>(currentGuess);
                            playerTwo.numberCorrectInPos = twoNumInPosition;
                            playerTwo.numberCorrectNotPos = twoNumNotInPosition;
                            playerTwo.missedDigit = twoMissed;
                        });

                        // give UI thread information on the game
                        // again obj is always the current guess, arg1 is always the number correct in the proper position, and arg2 is always number correct in the wrong position
                        // The bundle saves the randomly chosen missed digit, and the ID of this thread
                        Message msgToUI = mHandler.obtainMessage(UPDATE_PLAYER_ONE);
                        msgToUI.obj = currentGuess.toString();
                        msgToUI.arg1 = twoNumInPosition;
                        msgToUI.arg2 = twoNumNotInPosition;
                        Bundle newData1 = new Bundle();
                        newData1.putInt("DIGIT", twoMissed);
                        newData1.putInt("ID", ID);
                        msgToUI.setData(newData1);
                        mHandler.sendMessage(msgToUI);

                        return;
                }
              }
            };

            mHandler.sendEmptyMessage(LOOPING_PLAYER_ONE);
            Looper.loop();
        }

        // STRATEGY EXPLANATION: This strategy works by eliminating all wrong numbers, then generating all possible permutations from the correct numbers, and iterating through them on each guess

        // only gets called if we have less than 4 correct numbers (in correct or incorrect position)
        // eventually eliminates incorrect numbers
        private void computeBelowFour(){
            // on this turn we know we have a missed digit because we have less than 4 correct total
            // also this is guaranteed to be unique because we always change the value at this position to a
            // new unique value

            wrongGuesses.add(missedDigit);

            int positionOfMissed = -1;

            // find position of missed digit in the current guess
            for(int i = 0; i < 4; i++){
                if(currentGuess.get(i).equals(missedDigit)){
                    positionOfMissed = i;
                    break;
                }
            }

            // prime loop for finding a new unique number
            Integer newGuess = missedDigit;

            // random number generator
            Random random = new Random();
            int upperBound = 10;

            // generate a new guess until its not in the current guess or in past wrong guesses
            while(currentGuess.contains(newGuess) || wrongGuesses.contains(newGuess)){
                newGuess = random.nextInt(upperBound);
            }

            // guaranteed to be unique outside the while loop
            currentGuess.set(positionOfMissed, newGuess);
        }

        // Computes the next guess when we have four correct numbers
        // Does this by permuting all the possible orderings of 4 numbers
        private void computeFour(){
            // nothing left to compute since we checked all permutations
            // this should never happen because one of the permutations is guaranteed to be correct
            // but its here just in case
            if(currentPerm == permutations.size()){
                return;
            }

            // set current guess to the next permutation
            currentGuess = new ArrayList<>(permutations.get(currentPerm));
            currentPerm++;
        }

        // Calculates all permutations based on the iterative version of heap's algorithm
        // Algorithm used can be found here on wikipedia: https://en.wikipedia.org/wiki/Heap%27s_algorithm
        private ArrayList<ArrayList<Integer>> getPermutations(ArrayList<Integer> theVals){
            int[] positions = new int[4];

            for (int i = 0; i < 4; i++){
                positions[i] = 0;
            }

            ArrayList<ArrayList<Integer>> perms = new ArrayList<>();    // holds all the permutations that we generate
            Integer[] theValues = new Integer[theVals.size()];          // create an array to manipulate instead of theVals otherwise it doesn't generate how we want
            theValues = theVals.toArray(theValues);

            int i = 0;
            while(i < 4){
                if(positions[i] < i){
                    // swap based on parity
                    swap(theValues, i % 2 == 0 ? 0: positions[i], i);
                    // store the new permutation in perms
                    ArrayList<Integer> temp = new ArrayList<>();
                    Collections.addAll(temp, theValues);
                    perms.add(temp);

                    positions[i]++;
                    i = 0;
                }
                else{
                    positions[i] = 0;
                    i++;
                }
            }

            // return the permutations
            return perms;
        }

        // swap function to swap array elements
        private void swap(Integer[] input, int i, int j){
            Integer temp = input[i];
            input[i] = input[j];
            input[j] = temp;
        }
    }

    public class PlayerTwoRunnable implements Runnable {
        public Handler playerTwoHandler = null;
        private int ID;

        // Data from player two
        public ArrayList<Integer> playerOneGuess;
        public int numberCorrectInPos;
        public int numberCorrectNotPos;
        public int missedDigit;

        // player 1 data for runnable
        private int oneNumInPosition;
        private int oneNumNotInPosition;
        private int oneMissed;


        // Internal Data For Strategy
        private ArrayList<Integer> myNumber = new ArrayList<>();
        private ArrayList<Integer> wrongGuesses = new ArrayList<>();
        private ArrayList<Integer> currentGuess = new ArrayList<>();

        private int currentI = 0;
        private int currentJ = 1;
        private ArrayList<Integer> bestGuess = new ArrayList<>();
        private int bestInPos = -1;

        // set ID for similar reasons as PlayerOneRunnable
        PlayerTwoRunnable(int id){
            ID = id;
        }

        public void run(){
            Looper.prepare();

            playerTwoHandler = new Handler(Looper.myLooper()){
                public void handleMessage(Message msg){
                    int what = msg.what;
                    switch (what){
                        case GAME_START:
                            // generate random sequence
                            Random rand = new Random();

                            for(int i = 0; i < 4; i++){
                                int possibleData = rand.nextInt(10);

                                while(myNumber.contains(possibleData)){
                                    possibleData = rand.nextInt(10);
                                }

                                myNumber.add(possibleData);
                            }

                            // let ui know that player two has finished generating
                            Message theMsg = mHandler.obtainMessage(FINISHED_GENERATING_PLAYER_TWO);
                            theMsg.obj = myNumber.toString();

                            mHandler.sendMessage(theMsg);
                            return;
                        case GAME_OVER:
                            Looper.myLooper().quit();
                            playerTwoRunning = false;
                            return;
                        case COMPUTE_TURN:

                            int totalCorrect = numberCorrectInPos + numberCorrectNotPos;

                            // compute the next guess

                            // this means player one made its first guess so no prior info. Player two will always guess 1 2 3 4 at first
                            if(totalCorrect < 0){
                                currentGuess = new ArrayList<>();
                                currentGuess.add(1);
                                currentGuess.add(2);
                                currentGuess.add(3);
                                currentGuess.add(4);
                            }
                            // this only happens if we are still off in some way because UI thread determines if there is a winner
                            else if(totalCorrect == 4){
                                computeFour();
                            }
                            else{
                                computeBelowFour();
                            }

                            // Gather info for player 1
                            int numInPosition = 0;
                            int numNotInPosition = 0;

                            ArrayList<Integer> missedNumbers = new ArrayList<>();

                            // get number in position, number correct but not in position, and the missed digits
                            for(int i = 0; i < 4; i++){
                                Integer number = playerOneGuess.get(i);

                                if(myNumber.get(i).equals(number))
                                    numInPosition++;
                                else if(myNumber.contains(number))
                                    numNotInPosition++;
                                else
                                    missedNumbers.add(number);
                            }

                            // generator to pick a random digit
                            Random random = new Random();
                            int upperBound = missedNumbers.size();

                            // pick a random missed digit if number correct < 4
                            // otherwise set it to -1 so it wont be used
                            if(((numInPosition + numNotInPosition) < 4) && (missedNumbers.size() > 0))
                                oneMissed = missedNumbers.get(random.nextInt(upperBound));
                            else
                                oneMissed = -1;

                            // sleep for human reading
                            try { Thread.sleep(2000); }
                            catch (InterruptedException e) { System.out.println("Thread interrupted!") ; }

                            // set the instance fields so they're effectively final
                            oneNumInPosition = numInPosition;
                            oneNumNotInPosition = numNotInPosition;

                            // send to playerOneHandler so that it will set the corresponding instance fields with a runnable
                            playerOne.playerOneHandler.post(() -> {
                                // make sure to only send to player one if the IDs are the same
                                if(ID != playerOne.ID)
                                    return;

                                // send data to player one
                                playerOne.playerTwoGuess = new ArrayList<>(currentGuess);
                                playerOne.numberCorrectInPos = oneNumInPosition;
                                playerOne.numberCorrectNotPos = oneNumNotInPosition;
                                playerOne.missedDigit = oneMissed;
                            });

                            // give UI thread information on the game
                            // again obj is always the current guess, arg1 is always the number correct in the proper position, and arg2 is always number correct in the wrong position
                            // The bundle saves the randomly chosen missed digit, and the ID of this thread
                            Message msgToUI = mHandler.obtainMessage(UPDATE_PLAYER_TWO);
                            msgToUI.obj = currentGuess.toString();
                            msgToUI.arg1 = oneNumInPosition;
                            msgToUI.arg2 = oneNumNotInPosition;
                            Bundle newData1 = new Bundle();
                            newData1.putInt("DIGIT", oneMissed);
                            newData1.putInt("ID", ID);
                            msgToUI.setData(newData1);
                            mHandler.sendMessage(msgToUI);
                    }
                }
            };

            mHandler.sendEmptyMessage(LOOPING_PLAYER_TWO);
            Looper.loop();
        }

        // STRATEGY EXPLAINATION: this strategy start off the same way as player one by eliminating the wrong numbers, but it differs in what it does after that.
        // After getting rid of all incorrect numbers, it then builds a best guess based on if it's number correct in position improves over the previous best.
        // It swaps values in order to make the next potential best guess

        // only gets called if we have less than 4 correct numbers (in correct or incorrect position)
        // eventually eliminates incorrect numbers
        private void computeBelowFour(){
            // on this turn we know we have a missed digit because we have less than 4 correct total
            // also this is guaranteed to be unique because we always change the value at this position to a
            // new unique value

            wrongGuesses.add(missedDigit);

            int positionOfMissed = -1;

            // find position of missed digit in the current guess
            for(int i = 0; i < 4; i++){
                if(currentGuess.get(i).equals(missedDigit)){
                    positionOfMissed = i;
                    break;
                }
            }

            // prime loop for finding a new unique number
            Integer newGuess = missedDigit;

            // random number generator
            Random random = new Random();
            int upperBound = 10;

            // generate a new guess until its not in the current guess or in past wrong guesses
            while(currentGuess.contains(newGuess) || wrongGuesses.contains(newGuess)){
                newGuess = random.nextInt(upperBound);
            }

            // guaranteed to be unique outside the while loop
            currentGuess.set(positionOfMissed, newGuess);
        }

        // Computes the next guess when we have four correct numbers
        // Does this by keeping track of our current best guess and its associated best numberCorrectInPos
        // If we get a better guess update these values
        // otherwise it resets back to best guess and tries swapping another value
        private void computeFour(){
            // this acts sort of like a loop
            // makes sure on the current best guess we never swap values that were already swapped

            // if we are out of bounds with j update i to progress 1 and set j to 1 past i
            if(currentJ == 4){
                currentI++;
                currentJ = currentI + 1;
            }
            // if I is at the final position there is nothing left to swap so dont do anything
            if(currentI == 3)
                return;

            // if we got a better guess do this
            if(numberCorrectInPos > bestInPos){
                // update best guess and associated best number of correct in position
                bestInPos = numberCorrectInPos;
                bestGuess = new ArrayList<>(currentGuess);
                // reset to initial "loop" state so that we don't miss potential values
                currentI = 0;
                currentJ = 1;
            }

            // make a guess based off of best guess, then swap based on i and j
            currentGuess = new ArrayList<>(bestGuess);
            swap(currentGuess, currentI, currentJ);
            currentJ++;
        }

        // swap function to swap array list values
        private void swap(ArrayList<Integer> input, int i, int j){
            Integer temp = input.get(i);
            input.set(i, input.get(j));
            input.set(j, temp);
        }
    }

}