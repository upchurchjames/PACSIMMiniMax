import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import pacsim.*;

public class PacSimMinimax implements PacAction {
    int depth;
    GameTree tree;
    PacFace[] pacFace = { PacFace.N, PacFace.E, PacFace.S, PacFace.W };

    public PacSimMinimax(int depth, String fname, int te, int gran, int max) {
        PacSim sim = new PacSim(fname, te, gran, max);
        this.depth = depth;
        sim.init(this);
    }

    public static void main(String[] args) {
        String fname = args[0];
        int depth = Integer.parseInt(args[1]);

        int te = 0;
        int gr = 0;
        int ml = 0;

        if (args.length == 5) {
            te = Integer.parseInt(args[2]);
            gr = Integer.parseInt(args[3]);
            ml = Integer.parseInt(args[4]);
        }

        new PacSimMinimax(depth, fname, te, gr, ml);

        System.out.println("\nAdversarial Search using Minimax by Daniel Simoes and James Upchurch:");
        System.out.println("\n  Game Board  : " + fname);
        System.out.println("    Search Depth : " + depth + "\n");

        if (te > 0) {
            System.out.println("    Preliminary runs : " + te + "\n   Granularity : " + gr + "\n   Max move limit  : "
                    + ml + "\n\nPreliminary run results :\n");
        }
    }

    @Override
    public void init() {
    }

    // Heuristic: Distance to nearest ghost
    @Override
    public PacFace action(Object state) {
        PacCell[][] grid = (PacCell[][]) state;
        PacFace newFace = null;
        this.tree = null;

        float maxUtil = Integer.MIN_VALUE;
        int maxUtilID = -1;

        generate(grid);
        GameTreeNode root = tree.root;
        // tree.printTree(root);

        for (GameTreeNode node : root.possibleMoves) {
            node.value = GetValue(node);
            if (node.value > maxUtil) {
                maxUtil = node.value;
                maxUtilID = root.possibleMoves.indexOf(node);
            }
        }

        if (maxUtilID == -1) {
            return null;
        }

        Point oldPC = PacUtils.findPacman(grid).getLoc();
        Point newPC = PacUtils.findPacman(root.possibleMoves.get(maxUtilID).grid).getLoc();

        if (newPC == null)
            return null;

        newFace = PacUtils.direction(oldPC, newPC);
        // System.out.println(newPC);

        return newFace;
    }

    // Generate tree of possible states
    public void generate(PacCell[][] grid) {
        // Create tree with root
        tree = new GameTree(grid);
        GameTreeNode root = tree.root;

        // Build remaining tree with possible states from root
        generate_Moves(root, 0);

    }

    // Generate tree based on possible moves at specified depth
    public void generate_Moves(GameTreeNode root, int move) {
        if (move >= depth) {
            return;
        }

        // Find all ghost positions
        List<Point> ghosts = PacUtils.findGhosts(root.grid);
        PacCell[][] tempGrid = root.grid.clone();
        PacmanCell pacman;
        Point pc;
        PacCell neighbor;
        Boolean onFood = false;
        for (int i = 0; i < 4; i++) {
            tempGrid = root.grid.clone();
            pacman = PacUtils.findPacman(tempGrid);
            if (pacman == null)
                continue;

            pc = pacman.getLoc();
            neighbor = PacUtils.neighbor(pacFace[i], pacman, tempGrid);
            // System.out.println(PacUtils.neighbor(dir, pacman, tempGrid).getClass());
            if (neighbor instanceof WallCell || neighbor instanceof HouseCell || neighbor instanceof GhostCell)
                continue;
            onFood = neighbor instanceof FoodCell;
            tempGrid = PacUtils.movePacman(pc, neighbor.getLoc(), tempGrid);

            for (int j = 0; j < 4; j++) {
                // Copy grid and determine direction of next move

                neighbor = PacUtils.neighbor(pacFace[j], ghosts.get(0), tempGrid);
                // System.out.println(PacUtils.neighbor(dir, pacman, tempGrid).getClass());
                if (neighbor instanceof WallCell)
                    continue;
                tempGrid = PacUtils.moveGhost(ghosts.get(0), neighbor.getLoc(), tempGrid);

                for (int k = 0; k < 4; k++) {

                    neighbor = PacUtils.neighbor(pacFace[k], ghosts.get(1), tempGrid);
                    // System.out.println(PacUtils.neighbor(dir, pacman, tempGrid).getClass());
                    if (neighbor instanceof WallCell)
                        continue;
                    tempGrid = PacUtils.moveGhost(ghosts.get(1), neighbor.getLoc(), tempGrid);

                    GameTreeNode newMove = new GameTreeNode(tempGrid);

                    // If this is a leaf node, calculate the value of the resulting state
                    if (move == depth - 1) {
                        PacmanCell newPc = PacUtils.findPacman(newMove.grid);
                        List<Point> newGhosts = PacUtils.findGhosts(newMove.grid);
                        // System.out.println(newGhosts);
                        if (newPc == null) {
                            continue;
                        }

                        if (newGhosts.size() != 0) {
                            GhostCell nearestGhost = PacUtils.nearestGhost(newPc.getLoc(), newMove.grid);
                            newMove.value += (float) BFSPath
                                    .getPath(newMove.grid, newPc.getLoc(), nearestGhost.getLoc()).size();
                        }

                        if (PacUtils.foodRemains(newMove.grid)) {
                            newMove.value -= 2 * (float) BFSPath.getPath(newMove.grid, newPc.getLoc(),
                                    PacUtils.nearestFood(newPc.getLoc(), newMove.grid)).size();
                        }

                        if (onFood) {
                            newMove.value += 20;
                        }
                    }

                    root.possibleMoves.add(newMove);
                    // tempGrid = root.grid.clone();
                }
            }
        }

        for (GameTreeNode node : root.possibleMoves) {
            generate_Moves(node, ++move);
        }
    }

    public float GetValue(GameTreeNode root) {
        float utility = 0;
        // System.out.println("CURRENT PLAYER: " + player);

        // If there are no more possible moves, this is a leaf node,
        // therefore return the distance to the nearest Ghost
        if (root.possibleMoves.isEmpty()) {
            return root.value;
        }

        for (GameTreeNode node : root.possibleMoves) {
            // If utility is 0 and GetValue(node) happens to return a negative value,
            // We still need to update the value of the node to the returned utility
            node.value = utility = ((utility == 0) ? GetValue(node) : Math.max(utility, GetValue(node)));

            // System.out.print("UTILITY: " + node.value + " ");
        }

        // System.out.println();

        return utility;
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class GameTree {
    GameTreeNode root;

    public GameTree(PacCell[][] state) {
        root = new GameTreeNode(state);
    }

    void printTree(GameTreeNode root) {
        System.out.println("\n\n" + root);
        System.out.print("Children: ");
        for (GameTreeNode node : root.possibleMoves) {
            System.out.print(node + "  ");
        }

        for (GameTreeNode node : root.possibleMoves) {
            printTree(node);
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class GameTreeNode {
    float value;
    PacCell[][] grid;
    List<GameTreeNode> possibleMoves;

    public GameTreeNode(PacCell[][] state) {
        value = 0;
        grid = PacUtils.cloneGrid(state);
        possibleMoves = new ArrayList<>();
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////