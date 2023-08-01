package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealerTest {

    Dealer dealer;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Logger logger;
    @Mock
    Env env;

    private Player[] players;

    void assertInvariants() {
        assertTrue(players.length >= 0);
        assertTrue(env.config.deckSize >= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        env = new Env(logger, new Config(logger, (String) null), ui, util);
        players = new Player[2];
        for(int i=0;i<players.length; i++){
            players[i] = new Player(env, dealer, table, i, false);
        }
        
        dealer = new Dealer(env,table,players);
        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    @Test
    void insideRemoveAllCardsIsFalseAtStartup() {


        // call the method we are testing
        assertEquals(dealer.insideRemoveAllCards,false);

        
    }

    @Test
    void assertPlayerThreadWontStartBeforeDealerRun() {


        // call the method we are testing
        assertEquals(dealer.playerThreadstarted,false);

        
    }
    
    

   


   

}