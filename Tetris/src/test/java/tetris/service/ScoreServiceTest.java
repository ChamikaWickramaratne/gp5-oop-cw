package tetris.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tetris.model.service.ScoreService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreServiceTest {

    @ParameterizedTest
    @CsvSource({
            "1, 100",
            "2, 300",
            "3, 600",
            "4, 1000"
    })
    void testPointsForClearedLines(int lines, int expected) {
        assertEquals(expected, ScoreService.pointsFor(lines));
    }
}
