package tetris.service;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ScoreServiceSpyTest {

    @Test
    void testPointsForCalled() {
        DummyScoreService realService = new DummyScoreService();

        // Spy wraps the real object
        DummyScoreService spyService = spy(realService);

        // Call method normally
        int result = spyService.pointsFor(2);

        // Real logic still runs
        assert result == 200;

        // Verify interaction
        verify(spyService).pointsFor(2);
    }
}