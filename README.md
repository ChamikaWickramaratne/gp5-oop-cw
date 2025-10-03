src/main/java/tetris
│
├── config
│   ├── ConfigService.java     # Handles loading/saving TetrisConfig from JSON
│   ├── PlayerType.java        # Enum for selecting player types (Human, AI, External)
│   └── TetrisConfig.java      # Singleton holding game configuration (width, height, level, sound, etc.)
│
├── controller
│   ├── AIPlayer.java               # AI player logic that interacts with the game
│   ├── BaseState.java              # Abstract base for game states (common state behavior)
│   ├── ExternalPlayer.java         # Wrapper for moves coming from an external server
│   ├── ExternalPlayerClient.java   # Networking client for external Tetris server
│   ├── GameOverState.java          # State implementation representing game-over
│   ├── GamePane.java               # Main game controller (manages board, states, AI, external players)
│   ├── GameplayController.java     # Mediator handling input and coordinating model updates
│   ├── GameState.java              # Interface for defining behavior of different states
│   ├── HumanPlayer.java            # Implementation of a human-controlled player
│   ├── INetwork.java               # Interface for network communication with external player
│   ├── NetworkListener.java        # Observer for network events/updates
│   ├── PausedState.java            # State implementation representing paused gameplay
│   ├── Player.java                 # Abstract player class (common structure for Human, AI, External)
│   └── PlayerFactory.java          # Factory pattern to create/configure players (Human/AI/External)
│   └── RunningState.java           # State implementation representing active gameplay (normal running mode)
│
├── model
│   ├── ai
│   │   ├── BetterHeuristic.java  # Provides AI heuristics for piece placement
│   │   └── heuristic.java        # Supporting AI logic (utility class)
│   │
│   ├── dto
│   │   ├── OpMove.java           # Represents a move operation from AI/external player
│   │   └── PureGame.java         # Snapshot of game state for AI/external communication
│   │
│   ├── piece
│   │   ├── ActivePiece.java      # Represents a tetromino piece currently in play
│   │   └── Piece.java            # Abstract base for tetromino pieces
│   │
│   ├── rules
│   │   ├── RotationStrategy.java # Interface for rotation systems
│   │   └── SrsRotation.java      # Super Rotation System implementation
│   │
│   ├── service
│   │   ├── HighScoreManager.java # Manages saving/loading scores from JSON (File I/O)
│   │   ├── Score.java            # Entity representing player score details
│   │   ├── ScoreObserver.java    # Observer interface for score changes (Observer Pattern)
│   │   └── ScoreService.java     # Utility for calculating score points
│   │
│   ├── types
│   │   └── PlayerType.java       # Player type reference for gameplay
│   │
│   ├── Board.java                # Represents the Tetris board/grid with placed pieces
│   ├── TetrominoType.java        # Enum representing tetromino shapes (I, O, T, S, Z, J, L)
│   └── Vec.java                  # 2D vector utility for board coordinates
│
├── view
│   ├── ConfigScreen.java         # JavaFX screen for editing configuration
│   ├── GameSettings.java         # UI for adjusting gameplay settings
│   ├── HighScore.java            # UI screen for displaying high scores
│   ├── MainMenu.java             # Main navigation menu
│   ├── SinglePlayerView.java     # UI for single-player gameplay
│   ├── SplashScreen.java         # Startup splash/loading screen
│   ├── TwoPlayerBoard.java       # UI for multiplayer game view
│   └── UIConfiguration.java      # Handles reusable UI configuration logic
│
├── ExternalBotServer.java        # Entry point for running external bot player
├── TetrisServer.jar              # External networking server JAR for multiplayer/AI integration
│
└── resources
│   ├── sounds/                   # Sound effects used in gameplay
│   │   ├── erase-line.wav
│   │   ├── game-finish.wav
│   │   └── move-turn.wav
│   ├── pic.png                   # Splash Screen Image
│   ├── styles.css                # JavaFX CSS styling for UI
│   └── scores.json               # Persistent JSON file storing high scores
│
└── TetrisConfig.json             # JSON file storing saved configuration

src/test/java/tetris
│
├── controller
│   └── GameControllerMockitoTest.java   # Unit test using Mockito to mock static methods
│
├── service
│   ├── DummyScoreService.java           # Stub service used for testing score logic
│   ├── FakeHighScoreManager.java        # Fake implementation of HighScoreManager for tests
│   ├── HighScoreManagerTest.java        # Tests for loading/saving high scores
│   ├── ScoreServiceSpyTest.java         # Spy-based test verifying ScoreService interactions
│   └── ScoreServiceTest.java            # Unit tests for ScoreService scoring rules
│
└── GameTests.java                       # Main JUnit test class covering integration scenarios

