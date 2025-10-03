src/main/java/tetris
│
├── config
│   ├── ConfigService.java              # Loads/saves TetrisConfig from JSON
│   ├── PlayerType.java                 # Enum for selecting player types (Human, AI, External)
│   └── TetrisConfig.java              # Singleton holding game configuration (width, height, level, sound, etc.)
│
├── controller
│   ├── AIPlayer.java                  # AI player logic that interacts with the game
│   ├── BaseState.java                # Abstract base for game states (common behavior)
│   ├── ExternalPlayer.java           # Wrapper for moves coming from an external server
│   ├── ExternalPlayerClient.java     # Networking client for external Tetris server
│   ├── GameOverState.java            # State implementation for game-over
│   ├── GamePane.java                 # Main game controller (manages board, states, AI, external players)
│   ├── GameplayController.java       # Mediator handling input and coordinating model updates
│   ├── GameState.java                # Interface for defining different game state behaviors
│   ├── HumanPlayer.java              # Implementation of a human-controlled player
│   ├── INetwork.java                 # Interface for network communication with external player
│   ├── NetworkListener.java          # Observer for network events/updates
│   ├── PausedState.java              # State implementation representing paused gameplay
│   ├── Player.java                   # Abstract player class (base for Human, AI, External)
│   ├── PlayerFactory.java            # Factory to create/configure players (Human/AI/External)
│   └── RunningState.java             # State implementation representing active gameplay
│
├── model
│   ├── ai
│   │   ├── BetterHeuristic.java      # Provides AI heuristics for piece placement
│   │   └── Heuristic.java            # Interface or utility class for AI evaluation logic
│   │
│   ├── dto
│   │   ├── OpMove.java              # Represents a move operation from AI/external player
│   │   └── PureGame.java            # Snapshot of game state for AI/external communication
│   │
│   ├── piece
│   │   ├── ActivePiece.java         # Represents a tetromino piece currently in play
│   │   └── Piece.java               # Abstract base for tetromino pieces
│   │
│   ├── rules
│   │   ├── RotationStrategy.java    # Interface for rotation systems
│   │   └── SrsRotation.java         # Super Rotation System (SRS) implementation
│   │
│   ├── service
│   │   ├── HighScoreManager.java    # Manages saving/loading scores from JSON
│   │   ├── Score.java              # Entity representing player score details
│   │   ├── ScoreObserver.java      # Observer interface for score changes (Observer Pattern)
│   │   └── ScoreService.java       # Utility for calculating score points
│   │
│   ├── types
│   │   └── PlayerType.java         # Player type reference for gameplay
│   │
│   ├── Board.java                  # Represents the Tetris board/grid with placed pieces
│   ├── TetrominoType.java          # Enum for tetromino shapes (I, O, T, S, Z, J, L)
│   └── Vec.java                    # 2D vector utility for board coordinates
│
├── view
│   ├── ConfigScreen.java           # JavaFX screen for editing configuration
│   ├── GameSettings.java           # UI for adjusting gameplay settings
│   ├── HighScore.java              # UI screen for displaying high scores
│   ├── MainMenu.java               # Main navigation menu
│   ├── SinglePlayerView.java       # UI for single-player gameplay
│   ├── SplashScreen.java           # Startup splash/loading screen
│   ├── TwoPlayerBoard.java         # UI for multiplayer game view
│   └── UIConfiguration.java        # Reusable UI configuration logic
│
├── ExternalBotServer.java           # Entry point for running external bot player
├── TetrisServer.jar                 # External networking server JAR for multiplayer/AI integration
│
└── resources
│   ├── sounds/                     # Sound effects used in gameplay
│   │   ├── erase-line.wav
│   │   ├── game-finish.wav
│   │   └── move-turn.wav
│   ├── pic.png                     # Splash screen image
│   ├── styles.css                  # JavaFX CSS styling for UI
│   └── scores.json                 # Persistent JSON storing high scores
│
└── TetrisConfig.json               # JSON file storing saved configuration
