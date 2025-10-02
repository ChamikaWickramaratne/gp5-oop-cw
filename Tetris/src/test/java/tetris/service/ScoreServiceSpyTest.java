package tetris.service;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ScoreServiceSpyTest {

    @Test
    void testPointsForCalled() {
        DummyScoreService realService = new DummyScoreService();
        DummyScoreService spyService = spy(realService);
        int result = spyService.pointsFor(2);
        assert result == 200;
        verify(spyService).pointsFor(2);
    }
}