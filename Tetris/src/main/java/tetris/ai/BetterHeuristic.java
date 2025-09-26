// tetris/ai/BetterHeuristic.java
package tetris.ai;

public class BetterHeuristic implements Heuristic {
    // Start weights (tune!):
    private final double wLines =  +12.0;  // reward lines more
    private final double wHoles =  -9.0;   // punish holes more
    private final double wHeight = -0.45;  // aggregate height
    private final double wBump  =  -0.35;  // bumpiness
    private final double wWells = -0.30;   // total well depth
    private final double wRowTrans = -0.25;
    private final double wColTrans = -0.35;

    @Override
    public double evaluate(int[][] b, int linesCleared) {
        int H = b.length, W = b[0].length;

        // Column heights
        int[] heights = new int[W];
        for (int x=0; x<W; x++) {
            int y=0; while (y<H && b[y][x]==0) y++;
            heights[x] = H - y;
        }

        // Holes
        int holes = 0;
        for (int x=0; x<W; x++) {
            boolean seen = false;
            for (int y=0; y<H; y++) {
                if (b[y][x] != 0) seen = true;
                else if (seen) holes++;
            }
        }

        // Aggregate height & bumpiness
        int agg = 0, bump = 0;
        for (int x=0; x<W; x++) agg += heights[x];
        for (int x=0; x<W-1; x++) bump += Math.abs(heights[x]-heights[x+1]);

        // Wells
        int wells = 0;
        for (int x=0; x<W; x++) {
            int left = (x==0) ? Integer.MAX_VALUE : heights[x-1];
            int right= (x==W-1)? Integer.MAX_VALUE : heights[x+1];
            int neighborMin = Math.min(left, right);
            if (heights[x] < neighborMin) wells += (neighborMin - heights[x]);
        }

        // Row transitions
        int rowTrans = 0;
        for (int y=0; y<H; y++) {
            int prev = 1; // treat outside as filled
            for (int x=0; x<W; x++) {
                int cur = (b[y][x] != 0) ? 1 : 0;
                if (cur != prev) rowTrans++;
                prev = cur;
            }
            if (prev == 0) rowTrans++; // right border
        }

        // Column transitions
        int colTrans = 0;
        for (int x=0; x<W; x++) {
            int prev = 1; // top border filled
            for (int y=0; y<H; y++) {
                int cur = (b[y][x] != 0) ? 1 : 0;
                if (cur != prev) colTrans++;
                prev = cur;
            }
            if (prev == 0) colTrans++; // bottom border
        }

        return wLines*linesCleared
                + wHeight*agg
                + wHoles*holes
                + wBump*bump
                + wWells*wells
                + wRowTrans*rowTrans
                + wColTrans*colTrans;
    }
}
