package tetris.model.ai;

public class BetterHeuristic implements Heuristic {
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

        int[] heights = new int[W];
        for (int x=0; x<W; x++) {
            int y=0; while (y<H && b[y][x]==0) y++;
            heights[x] = H - y;
        }

        int holes = 0;
        for (int x=0; x<W; x++) {
            boolean seen = false;
            for (int y=0; y<H; y++) {
                if (b[y][x] != 0) seen = true;
                else if (seen) holes++;
            }
        }

        int agg = 0, bump = 0;
        for (int x=0; x<W; x++) agg += heights[x];
        for (int x=0; x<W-1; x++) bump += Math.abs(heights[x]-heights[x+1]);

        int wells = 0;
        for (int x=0; x<W; x++) {
            int left = (x==0) ? Integer.MAX_VALUE : heights[x-1];
            int right= (x==W-1)? Integer.MAX_VALUE : heights[x+1];
            int neighborMin = Math.min(left, right);
            if (heights[x] < neighborMin) wells += (neighborMin - heights[x]);
        }

        int rowTrans = 0;
        for (int y=0; y<H; y++) {
            int prev = 1;
            for (int x=0; x<W; x++) {
                int cur = (b[y][x] != 0) ? 1 : 0;
                if (cur != prev) rowTrans++;
                prev = cur;
            }
            if (prev == 0) rowTrans++;
        }

        int colTrans = 0;
        for (int x=0; x<W; x++) {
            int prev = 1;
            for (int y=0; y<H; y++) {
                int cur = (b[y][x] != 0) ? 1 : 0;
                if (cur != prev) colTrans++;
                prev = cur;
            }
            if (prev == 0) colTrans++;
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
