# Tic-Tac-Toe

This code implements a multiplayer multithread tic-tac-toe game. A server is started where a large number of clients can connect and play. 

## Features

Real-time chat

Auto-move for players that take too long

Handling for more than two players

A thread-per-connection model (A new thread for each connected player)

A Gui featuring A quit button, a countdown timer and a turn label indicating which players turn it is

![Gui Example](/guiexample.png/)

Use "java TicTacToeServer [ip] [port]" and "java TicTacToeClient [username] [ip] [port]"

