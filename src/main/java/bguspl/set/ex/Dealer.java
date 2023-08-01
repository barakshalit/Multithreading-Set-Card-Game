package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime;  
    public ConcurrentLinkedDeque<int []> setsToTest;
    Thread dealerThread;
    public boolean needToCheck;// when a set is sent to the dealer
    public boolean needToCheckwhile;
    public boolean correctSet;// when a set is correct
    public boolean timeout; // when the timer is 0
    public boolean playerThreadstarted;
    public boolean insideRemoveAllCards;
    public boolean valid;


    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        Collections.shuffle(deck);
        setsToTest = new ConcurrentLinkedDeque<int[]>();
        needToCheck = false;
        needToCheckwhile = false;
        timeout = false;
        correctSet = false;
        reshuffleTime  = System.currentTimeMillis() + env.config.turnTimeoutMillis; //setting the next time the timer needs to reset from current time, old value was - Long.MAX_VALUE;
        playerThreadstarted = false;
        insideRemoveAllCards = false;
        valid = true;
         
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        dealerThread = Thread.currentThread();
       
        while (!shouldFinish()) {
            
            placeCardsOnTable();
            if(!playerThreadstarted){
                
                for (int i =0; i<players.length; i++){
                    if(i < players.length){
                        players[i].setAndRunPlayerThread();
        
                    }
                }
                playerThreadstarted = true;

            }
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable(); 
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
       
        updateTimerDisplay(true);
        while (!terminate && !timeout && !shouldFinish()) {
            
            sleepUntilWokenOrTimeout(); //waiting for 1 sec some action to trigger the dealer or timer to timeout
            
            if (setsToTest.size() > 0){
                removeCardsFromTable();
                placeCardsOnTable();
            }
            if(correctSet) {
                updateTimerDisplay(true);
                correctSet = false;
            } 
            
        }
    
        timeout=false;
    }
 
    /**
     * Called when the game should be terminated due to an external event.
     */

     //needs to wait for the dealer thread to finish its job gracefuly.
    public void terminate() {
    
        terminate = true;
        
        //waiting for the dealer thread to finish its job gracefully:
        try {
            for(int i=0;i<players.length; i++){
                players[i].terminate();
            }
            Thread.currentThread().join();
            } catch(Exception ignoredException) {};
        
        
    }
    
    

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || (env.util.findSets(deck, 1).size() == 0 && table.setsOnTable() == 0)  ;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        insideRemoveAllCards = true;
        synchronized(table) {   
            int [] temp = setsToTest.removeFirst();// toCheck wiil hold at the first index the palyer Id.
            
            int [] toCheckSlots = Arrays.copyOfRange(temp, 1, temp.length);
         
            int [] toCheckCards= new int[3];
            
            
            valid = true;
            for (int i=0;i<toCheckSlots.length;i++){
                if(table.slotToCard[toCheckSlots[i]] != null){
                    toCheckCards[i]= table.slotToCard[toCheckSlots[i]];
                }
                else{
                    valid = false;
                }
                
               
                
            }
            
            if(valid){
                 //set is correct!
                if (env.util.testSet(toCheckCards)){ 
                    correctSet = true;
                    for(int i=0; i<3; i++){
                        
                        //env.ui.removeToken(temp[0], table.cardToSlot[toCheckCards[i]]);
                        table.removeToken(temp[0], table.cardToSlot[toCheckCards[i]]);
                        env.ui.removeCard(table.cardToSlot[toCheckCards[i]]);
                        table.removeCard(table.cardToSlot[toCheckCards[i]]);
                        
                        
                                                            
                    }

                    
                    //removing players tokens from the slots of the correct set:
                    for(int i = 0; i<players.length; i++){
                        players[i].removeTokens(toCheckSlots);
                    }
                
                    //awarding the player with a point and sending the player to sleep
                    players[temp[0]].correct = true;
                    players[temp[0]].needToCheckPlayer = true;
                    needToCheck = false;
                    needToCheckwhile = false;
                    
                
                
                }
                //set is not correct!
                else{ 
                    //penalizing the player for picking non-set
                    players[temp[0]].correct = false;
                    players[temp[0]].needToCheckPlayer = true;
                    needToCheck = false;
                    needToCheckwhile = false;
                    players[temp[0]].gotPenalty = true;
                    

                
                
                
                

                }
            

            }
           
   
        
        }
}

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        synchronized(table){
            for (int i =0;i<12 && (12- table.countCards() >0) && deck.size() !=0;i++ ) {
                if (table.slotToCard[i] == null){
                    
                    int toAdd = deck.remove(0);
                    table.placeCard(toAdd, i);
                    env.ui.placeCard(table.slotToCard[i],i);
                }
        
           }

        }
        
       insideRemoveAllCards = false;
       
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        while (!timeout && !needToCheckwhile){
            
            try {this.dealerThread.sleep(env.config.tableDelayMillis); 

                } catch(Exception interruptedException) {}          
                
            if (System.currentTimeMillis() >= reshuffleTime){ //timer reached zero
                updateTimerDisplay(true);
                timeout = true;
        
            }
            if (needToCheck){
                updateTimerDisplay(false);
                needToCheckwhile = true;
            }

            updateTimerDisplay(false); //timer didnt reached zero
            
        }

    }
        

        


        
    

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    //updating the GRAPHICAL timer on the window
    private void updateTimerDisplay(boolean reset) {
        
        if (reset){
            synchronized (table){
            env.ui.setCountdown(env.config.turnTimeoutMillis,false);
            reshuffleTime  = System.currentTimeMillis() + env.config.turnTimeoutMillis;
            needToCheckwhile = false;
            

            }   
        }
        else{
            if(reshuffleTime-System.currentTimeMillis() >= env.config.turnTimeoutWarningMillis){
                env.ui.setCountdown((reshuffleTime-System.currentTimeMillis()),false);
            }
            else{
                env.ui.setCountdown((reshuffleTime-System.currentTimeMillis()),true);

            }
        }
        

    }

    

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
       // if(deck.size() + table.countCards() >=12){
            insideRemoveAllCards = true;
       
        synchronized(table){
           
            needToCheck = false;
           
            // removing all the tokens
            env.ui.removeTokens();

            //removing all cards and returning them to the deck
            for (int i = 0; i<table.slotToCard.length; i++){
                if(table.slotToCard[i] != null){
                    deck.add(table.slotToCard[i]);
                    table.removeCard(i);
                    env.ui.removeCard(i);
                }
                
    
            }
            
            // empty the players action


            for(int i = 0; i<players.length; i++){
                players[i].removeAllTokens();
                try {
                    players[i].playersActions.clear();
                    
                    
                } catch (Exception ignored) {
                }

               
                
            }
            
            insideRemoveAllCards = false;
            

        }

      
        

}



    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        removeAllCardsFromTable(); 
        
        int size = 0;
        int highscore = 0;
        for (int i=0;i<players.length;i++){
            if (players[i].getScore() > highscore){
                highscore = players[i].getScore();
            }
        }

        for (int i=0;i<players.length;i++){
            if (players[i].getScore() == highscore){
                size++;
            }
        }
        int[] winners = new int[size];
        int j =0;
        for (int i=0;i<players.length;i++){
            if (players[i].getScore() == highscore){
                winners[j] = players[i].id;
                j++;
            }
        } 

        env.ui.announceWinner(winners);    

    }




    
}
