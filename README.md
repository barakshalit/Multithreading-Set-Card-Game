# Multithreading-Set-Card-Game
A java multithreading project implementing the "Set' card game.
This project supports 2 players mode and Player VS PC aswell.

The goal of the assignment is to practice concurrent programming on Java 8 environment, and get basic experience with unit testing.

![ezgif com-video-to-gif (1)](https://github.com/barakshalit/Multithreading-Set-Card-Game/assets/76451972/c123a301-5899-4c9a-ad2f-5bb7dd13a428)




## Introduction
In the following assignment we were required to implement a simple version of the game “Set”.
A description of the original game can be found here: [Set Card Game](https://en.wikipedia.org/wiki/Set_(card_game)). For reference only, there
is also the next video for a more intuitive understanding of the game: [How to play Set](https://www.youtube.com/watch?v=NzXDfSFQ1c0).
(keep in mind though, that we use slightly different rules in this implementation).

In this assignemnt i only implemented the logic part (board, card and keyboard handeling was already implemented).

## This version of the game
The game contains a deck of 81 cards. Each card contains a drawing with four features (color,
number, shape, shading).

For the sake of simplicity, in this document we describe the standard version of the game (having 4 types
of features, 3 options per feature, etc.). In the configuration file there are many values which can be
configured (including the number of features, number of options per feature and more). 

The game starts with 12 drawn cards from the deck that are placed on a 3x4 grid on the table.
The goal of each player is to find a combination of three cards from the cards on the table that
are said to make up a “legal set”.

A “legal set” is defined as a set of 3 cards, that for each one of the four features — color,
number, shape, and shading — the three cards must display that feature as either: (a) all the
same, or: (b) all different (in other words, for each feature the three cards must avoid having
two cards showing one version of the feature and the remaining card showing a different
version).

The possible values of the features are:

▪ The color: red, green or purple.

▪ The number of shapes: 1, 2 or 3.

▪ The geometry of the shapes: squiggle, diamond or oval.

▪ The shading of the shapes: solid, partial or empty.

For example:

![image](https://github.com/barakshalit/Multithreading-Set-Card-Game/assets/76451972/a81fd385-f9d4-4124-b081-37efc8476f58)


Example1: these 3 cards do form a set, because the shadings of the three cards are all the same,
while the numbers, the colors, and the shapes are all different.

![image](https://github.com/barakshalit/Multithreading-Set-Card-Game/assets/76451972/07aff4e4-c7bf-4140-8a30-604b556cf5d0)


Example 2: these 3 cards do not form a set (although the numbers of the three cards are all the same,
and the colors, and shapes are all different, only two cards have the same shading).

The game's active (i.e., initiate events) components contain the dealer and the players.
The players play together simultaneously on the table, trying to find a legal set of 3 cards. They
do so by placing tokens on the cards, and once they place the third token, they should ask the
dealer to check if the set is legal.

If the set is not legal, the player gets a penalty, freezing his ability of removing or placing his
tokens for a specified time period.

If the set is a legal set, the dealer will discard the cards that form the set from the table, replace
them with 3 new cards from the deck and give the successful player one point. In this case the
player also gets frozen although for a shorter time period.

To keep the game more interesting and dynamic, and in case no legal sets are currently available
on the table, once every minute the dealer collects all the cards from the table, reshuffles the
deck and draws them anew.

The game will continue as long as there is a legal set to be found in the remaining cards (that are
either on table or in the deck). When there is no legal set left, the game will end and the player
with the most points will be declared as the winner!

Each player controls 12 unique keys on the keyboard as follows. The default keys are:
![image](https://github.com/barakshalit/Multithreading-Set-Card-Game/assets/76451972/ed4c5261-155a-43fc-b8cc-99f923ef4748)



The keys layout is the same as the table's cards slots (3x4), and each key press dispatches the
respective player’s action, which is either to place/remove a token from the card in that slot - if
a card is not present/present there.

The game supports 2 player types: human and non-human.

The input from the human players is taken from the physical keyboard as an input.

The non-human players are simulated by threads that continually produce random key presses.

## The game design

#### The Cards & Features
Cards are represented in the game by int values from 0 to 80.
Each card has 4 features with size 3. Therefore, the features of a card can be represented as an
array of integers of length 4. Each cell in the array represents a feature. The value in the cell
represents one of the 3 possible values of the feature. In that way each card can get a unique id
based on the features that described it and vice versa. The id of each card is calculated as
follows:
3^0 ∗ 𝑓1 + 3^1 ∗ 𝑓2 + 3^2 ∗ 𝑓3 + 3^3 ∗ 𝑓4 where 𝑓𝑛 is the value of feature n.

#### The Table
The table is the data structure for the game. It is a passive object that is used by the dealer and
the players to share data (more on the dealer and players later).

The table holds the placed cards on a grid of 3x4, and keeps track of which token was placed by
whom.

#### The Players
For each player, a separate thread is created to represent the player’s entity.

The player object manages the player data (such as the player id and type - human/non-human).

For non-human players, **another thread** is created to simulate key presses.

In the implementation i maintained a queue of incoming actions (which are dispatched by key presses – either
from the keyboard or from the simulator). The queue size is equal to the number of cards
that form a legal set (3).

The player thread consumes the actions from the queue, placing or removing a token in the
corresponding slot in the grid on the table.

Once the player places his third token on the table, he must notify the dealer and wait until the
dealer checks if it is a legal set or not. The dealer then gives him either a point or a penalty
accordingly.

The penalty for marking an illegal set is getting frozen for a few seconds (i.e., not being able to
perform any actions). However, even when marking a legal set, the player gets frozen for a
second.

#### The Dealer
The dealer is represented by a single thread, which is the main thread in charge of the game
flow. It handles the following tasks:

▪ Creates and runs the player threads.

▪ Dealing the cards to the table.

▪ Shuffling the cards.

▪ Collecting the cards back from the table when needed.

▪ Checking if the tokens that were placed by the player form a legal set.

▪ Keeping track of the countdown timer.

▪ Awarding the player with points and/or penalizing them.

▪ Checking if any legal sets can be formed from the remaining cards in the deck.

▪ Announcing the winner(s).

Notes:
1. When dealing cards to the table and collecting cards from the table the dealer thread should
sleep for a short period (as is already written for you in Table::placeCard and
Table::removeCard methods provided in the skeleton files).
2. Checking user sets should be done “fairly” – if 2 players try to claim a set at roughly the
same time, they should be serviced by the dealer in “first come first served” (FIFO) order.

Any kind of synchronization mechanism used for this specific part of the program was take
this into consideration.

#### The Graphic User Interface & Keyboard Input

###### The User Interface

The UI class opens a new window on screen with the following components:

![image](https://github.com/barakshalit/Multithreading-Set-Card-Game/assets/76451972/518504d5-6b71-49b6-aaf0-84c382653c4b)



The configuration settings of the game reside in the file config.properties and is loaded into the
Config class.


###### The Input Manager

The handling of the input from the keyboard has been implemented before in the InputManager
class. It will automatically call Player::keyPressed method and pass as an argument the slot
number of the card that corresponds to the key that was pressed.
The slot number is: 𝒄𝒐𝒍𝒖𝒎𝒏 + 𝒕𝒐𝒕𝒂𝒍 𝒄𝒐𝒍𝒖𝒎𝒏𝒔 ∗ 𝒓𝒐𝒘

###### The Window Manager
When the user clicks the close window button, the class WindowManager that we provided you
with, automatically calls Dealer::terminate method of the dealer thread, and Player::terminate
method for each opened player thread.

###### Class Main
Loads all the required components, initializes the logger, and creates and runs the dealer thread.

###### Class Env
Helper class for sharing the Logger, Config, UserInterface and Util classes.

###### Class Config
Loads and parses the configuration file.
The configuration object is passed to any class that may
need it. 

The Config class tries to load config.properties from the current working directory. If it cannot
find it, it will look for it in the resources directory and if it can’t find it there too, it will use the
predefined properties in the class.

###### Class Util
This class contains several utility methods.

Not all the fields of the Config class are identical to what appears in config.properties (e.g., some fields in
the file are in seconds and in the class are in milliseconds, and there are some additional fields in the class
for convenience).

#### Program execution / flow
Following is a diagram of the general flow of the program3
Note that this diagram only describes the general flow. It does not address how the synchronization was handled, graphic display updates and such.

![image](https://github.com/barakshalit/Multithreading-Set-Card-Game/assets/76451972/2793ebe6-b5c4-4a34-aa0e-a0c632b938d1)


#### Building with Maven
In this assignment i use Maven as my build tool.

#### Unit testing
In this assignment, we use JUnit for testing.

###### Uses of JUnit tests and Mockito
As appear in the files.
