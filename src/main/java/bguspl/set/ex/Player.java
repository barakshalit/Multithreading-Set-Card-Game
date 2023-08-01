package bguspl.set.ex;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.concurrent.BlockingQueue;

import javax.management.monitor.MonitorSettingException;

import java.util.concurrent.ThreadLocalRandom;


import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    //private int tokenInUse;
    public boolean[] tokens;
    public Queue<Integer> playersActions;
    public Dealer dealer;
    public boolean correct;
    public boolean gotPenalty;
    public boolean needToCheckPlayer;
   

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.tokens = new boolean [12];
        playersActions = new LinkedList<Integer>();
        this.dealer = dealer;
        correct = false;
        gotPenalty = false;
        needToCheckPlayer = false;
        

    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            synchronized(this){
                if(playersActions.size() > 0){
                        if(!dealer.needToCheck && !dealer.insideRemoveAllCards){
                            Integer slot = playersActions.poll();
                            if(slot!=null){
                                placeorRemoveToken(slot);
                            }
                            else{
    
                            }
                   
                        }

                }
            
            }
                penaltyPointOrNon();  
           
        }


        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {        
                int toPress = Math.abs(ThreadLocalRandom.current().nextInt()) % 12;
                
                 if(playersActions.size()<3){           
                    keyPressed(toPress);
          
                 }  
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
        try {
            aiThread.join();
            } catch(Exception ignoredException) {};

        try {
            playerThread.join();
        } catch(Exception ignoredException) {};
        
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public synchronized void keyPressed(int slot) {

            if(playersActions.size()<3 && !dealer.insideRemoveAllCards){

                this.playersActions.add(slot);
            }
    }
          
    

    
    public void placeorRemoveToken(int slot){

        synchronized(table){
            if(table.slotToCard[slot] != null){//the slot has valid card
                if((!this.tokens[slot])){ //the slot is empty
                    // want to palce token
                    if(realtokens()<3){
                        table.placeToken(this.id, slot);
                        tokens[slot] = true;
    
                        //sending the dealer set to check
                        if (realtokens() == 3){
                            sendToDealer();
                           
    
                        }
    
                    }
                    
                }
                //removing tokens
                else{
                    table.removeToken(this.id, slot);
                    tokens[slot] = false;
                }

            }
            
        }      
}


    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        //sending the player thread to sleep for 1 second
        env.ui.setFreeze(this.id,env.config.pointFreezeMillis);
        try {
            getAiThread().sleep(env.config.pointFreezeMillis);
            getPlayerThread().sleep(env.config.pointFreezeMillis);
        
        } catch(Exception interupptedException) {
        };
        env.ui.setFreeze(this.id,0);
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score); //updating the actual and ui score
       
        removeAllTokens();
    

        
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        env.ui.setFreeze(this.id,env.config.penaltyFreezeMillis);
        try { 
        playerThread.sleep(env.config.penaltyFreezeMillis);
        
        } catch(Exception ignoredException) {
        };
        env.ui.setFreeze(this.id,0);
        
}
        

    public int getScore() {
        return score;
    }

    public void setAndRunPlayerThread(){
        this.playerThread = new Thread(this, "palyer" + id);
        playerThread.start();
    }

    public Thread getPlayerThread(){
        return this.playerThread;
    }
    public Thread getAiThread(){
        return this.aiThread;
    }

    public boolean isHuman(){
        return human;
    }


    public synchronized void sendToDealer(){
            int[] setToTest = new int[4] ;
            setToTest[0] = id;
            int j= 1;
            
            for (int i=0; i<tokens.length;i++) {
                if (tokens[i]){
                    setToTest[j] = i;
                    j++;
                }
            }
          
            this.dealer.setsToTest.add(setToTest);
            dealer.needToCheck = true;   

    }



    public int realtokens(){
        int count = 0;
            for (int i=0; i<tokens.length;i++) {
                if (tokens[i]){
                    count++;
                }
            }

            return count;
    }
    public void removeAllTokens(){
            for (int i=0; i<tokens.length;i++) {
                if (tokens[i]){
                    tokens[i] = false;
                    
                }
            }

    }

    public void penaltyPointOrNon(){
        if (correct && realtokens() == 3 && needToCheckPlayer){
            point();
            needToCheckPlayer = false;
        }
        else{ 
            if(realtokens() == 3 && gotPenalty && needToCheckPlayer){
                penalty();
                gotPenalty = false;
                needToCheckPlayer = false;
        }
           
        }
    }

    public void removeTokens(int[] slots){
        for(int i=0;i<slots.length;i++){
            if(tokens[slots[i]]){
                tokens[slots[i]] = false;
                table.removeToken(id, slots[i]);
            }
        }

    }
    

}
