package tetris.controller;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import tetris.service.ScoreService;
import tetris.ui.Gameplay;

import static org.mockito.Mockito.*;

class GameControllerMockitoTest {

    @Test
    void testStaticScoreService() {
        try (MockedStatic<ScoreService> mockStatic = mockStatic(ScoreService.class)) {
            mockStatic.when(() -> ScoreService.pointsFor(2)).thenReturn(300);

            Gameplay controller = new Gameplay();
            controller.addPointsForClearedLines(2);

            mockStatic.verify(() -> ScoreService.pointsFor(2));
        }
    }
}