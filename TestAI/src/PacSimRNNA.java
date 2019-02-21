import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import pacsim.BFSPath;
import pacsim.PacAction;
import pacsim.PacCell;
import pacsim.PacFace;
import pacsim.PacSim;
import pacsim.PacUtils;
import pacsim.PacmanCell;


public class PacSimRNNA implements PacAction {
    private List<Point> path;
    private int simTime;

    public PacSimRNNA ( String name ) {
        PacSim sim = new PacSim(name);
        sim.init(this);
    }

    public static void main ( String[] args ) {
        System.out.println("\nTSP using Repetitive Nearest Neighbor Algorithm by James Upchurch:");
        System.out.println("\nMaze: " + args[0] + "\n");
        new PacSimRNNA( args[0] );
    }

    public void init() {
        simTime = 0;
        path = new ArrayList();
    }

    @Override
    public PacFace action ( Object state ) {
        PacCell[][] grid = (PacCell[][]) state;
        PacmanCell pc = PacUtils.findPacman(grid);
        PacmanCell temp = pc;

        // Verify PacMan is on grid
        if (pc == null) return null;

        // Implement RNNA
        if (path.isEmpty()) {
            List<List<Point>> paths = new ArrayList<>();
            List<Point> food = PacUtils.findFood(grid);
            List<Point> unvisitedFood = PacUtils.findFood(grid);
            getCostTable(grid, pc, food);

            // NNA
            RNNA(food, unvisitedFood, grid, temp);

            for (int j = 0; j < path.size(); j++) {
                System.out.println(path.get(j));
            }
        }

        Point next = path.remove(0);
        System.out.println("Pacman moves to (" + next.x + ", " + next.y + ")");
        return PacUtils.direction(pc.getLoc(), next);
    }

    void RNNA (List<Point> food, List<Point> unvisitedFood, PacCell[][] grid, PacmanCell pc) {
        PacmanCell temp = pc;
        for (int i = 0; i < food.size(); i++) {
            Point nearFood = findNearestFood(grid, unvisitedFood, temp);

            path.addAll(BFSPath.getPath(grid, temp.getLoc(), nearFood));
            unvisitedFood.remove(nearFood);

            temp = new PacmanCell(nearFood.x, nearFood.y);
        }

    }

    Point findNearestFood(PacCell[][] grid, List<Point> unvisitedFood, PacmanCell pc) {
        int min = Integer.MAX_VALUE;
        Point nearFood = null;

        for (int i = 0; i < unvisitedFood.size(); i++) {
            int dist = BFSPath.getPath(grid, unvisitedFood.get(i), pc.getLoc()).size();
            if (dist <= min) {
                min = dist;
                nearFood = unvisitedFood.get(i);
            }

        }

        return nearFood;
    }

// Prints and returns 2D adjacency matrix where Pacman is first row/column and
// all other entries are food pellets.
    private int[][] getCostTable(PacCell[][] grid, PacmanCell pc, List<Point> food){

        // Cost table size is n+1 by n+1
        int tableSize = food.size() + 1;
        int[][] costTable = new int[tableSize][tableSize];

        for(Point pt : food)
        {
            int cost = BFSPath.getPath(grid, pc.getLoc(), pt).size();
            int id = food.indexOf(pt) + 1;
            costTable[id][0] = costTable[0][id] = cost;
        }

        for(int x = 0; x < tableSize - 1; x++) {
            for(int y = 0; y < tableSize - 1; y++) {
                costTable[x + 1][y + 1] = BFSPath.getPath(grid, food.get(x), food.get(y)).size();
            }
        }

        // Print cost table
        System.out.println("Cost table:\n");
        for(int x=0; x<tableSize; x++){
            for(int y=0; y<tableSize; y++) {
                System.out.printf("%-3d ", costTable[x][y]);
            }
            System.out.println();
        }

        System.out.println("\nFood Array:\n\n");

        for(Point pt : food) {
            System.out.println(food.indexOf(pt) + " : (" + pt.x + "," + pt.y + ")");
        }

        System.out.println();

        return costTable;
    }
}