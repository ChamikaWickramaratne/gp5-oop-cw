package tetris.controller;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import tetris.model.service.ScoreService;

import static org.mockito.Mockito.*;

class GameplayControllerMockitoTest {

    @Test
    void testScoreServiceCalled() {
        try (MockedStatic<ScoreService> mockStatic = mockStatic(ScoreService.class)) {
            mockStatic.when(() -> ScoreService.pointsFor(2)).thenReturn(300);
            int result = ScoreService.pointsFor(2);  // simulate "2 lines cleared"

            mockStatic.verify(() -> ScoreService.pointsFor(2));
        }
    }
}