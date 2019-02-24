import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import pacsim.*;


public class PacSimMinimax implements PacAction
{
    int depth, player = 0, move = 0;
    GameTree tree;

    public PacSimMinimax( int depth, String fname, int te, int gran, int max )
    {
        PacSim sim = new PacSim ( fname, te, gran, max );
        this.depth = depth;
        sim.init(this);
    }

    public static void main ( String[] args )
    {
        String fname = args[0];
        int depth = Integer.parseInt(args[1]);

        int te = 0;
        int gr = 0;
        int ml = 0;

        if ( args.length == 5 )
        {
            te = Integer.parseInt(args[2]);
            gr = Integer.parseInt(args[3]);
            ml = Integer.parseInt(args[4]);
        }

        new PacSimMinimax( depth, fname, te, gr, ml );

        System.out.println("\nAdversarial Search using Minimax by Daniel Simoes and James Upchurch:");
        System.out.println("\n  Game Board  : " + fname);
        System.out.println("    Search Depth : " + depth + "\n");

        if ( te > 0 )
        {
            System.out.println("    Preliminary runs : " + te
                    + "\n   Granularity : " + gr
                    + "\n   Max move limit  : " + ml
                    + "\n\nPreliminary run results :\n");
        }
    }

    @Override
    public void init() {}

    // Heuristic: Distance to nearest ghost
    @Override
    public PacFace action( Object state ){
        PacCell[][] grid = (PacCell[][]) state;
        PacFace newFace;

        this.tree = null;

        float maxUtil = Integer.MIN_VALUE;
        int maxUtilID = -1;
        move = 0;
        player = 0;

        generate(grid);
        GameTreeNode root = tree.root;

        for(GameTreeNode node : root.possibleMoves)
        {
            node.value = GetValue(node);
            //System.out.println(node.value + " ");
            if (node.value >= maxUtil) {
                maxUtil = node.value;
                maxUtilID = root.possibleMoves.indexOf(node);
            }
        }

        if (maxUtilID == -1)
        {
            return null;
        }

        Point oldPC = PacUtils.findPacman(grid).getLoc();
        Point newPC = root.possibleMoves.get(maxUtilID).pcLoc;

        if (newPC == null)
            return null;

        newFace = PacUtils.direction(oldPC, newPC);
        //System.out.println(newPC);

        return newFace;
    }

    // Generate tree of possible states
    public void generate(PacCell[][] grid)
    {
        // Create tree with root
        tree = new GameTree(grid, player);
        GameTreeNode root = tree.root;

        // Build remaining tree with possible states from root
        generate_Moves(root, grid);
        //tree.printTree(root);

    }

    // Generate tree based on possible moves at specified depth
    public void generate_Moves(GameTreeNode root, PacCell[][] grid) {
        if (move >= depth)
            return;

        List<PacCell[][]> grids = new ArrayList();
        int foodRem = PacUtils.numFood(grid);

        // Generate moves in every direction
        // (ie not added to list of possible moves
        for (int j = 0; j < 4; j++) {
            PacFace dir;
            Point ghost = null;
            PacCell[][] gridClone = PacUtils.cloneGrid(grid);

            // Pick the next direction
            switch (j) {
                case 0:
                    dir = PacFace.N;
                    break;
                case 1:
                    dir = PacFace.E;
                    break;
                case 2:
                    dir = PacFace.S;
                    break;
                case 3:
                    dir = PacFace.W;
                    break;
                default:
                    dir = null;
            }

            // Make move in direction, dir, and add new possible state to list of possible moves
            // if Pacman's turn, move pacman, else move ghost
            if (this.player == 0) {
                PacCell neighbor = PacUtils.neighbor(dir, root.pcLoc, gridClone);

                if (neighbor instanceof WallCell
                        || neighbor instanceof GhostCell
                        || neighbor instanceof HouseCell)
                {
                    continue;
                }

                gridClone = PacUtils.movePacman(root.pcLoc, neighbor.getLoc(), gridClone);
            } else {
                if (this.player == 1) ghost = new Point(root.blinkyLoc);
                else ghost = new Point(root.inkyLoc);

                PacCell neighbor = PacUtils.neighbor(dir, ghost, gridClone);

                if (neighbor instanceof WallCell)
                {
                    continue;
                }

                gridClone = PacUtils.moveGhost(ghost, neighbor.getLoc(), gridClone);

                ghost.move(neighbor.getX(), neighbor.getY());
            };

            GameTreeNode newMove;
            PacCell pacman = PacUtils.findPacman(gridClone);
            Point pc = null;
            if (pacman != null) {
                pc = pacman.getLoc();
            }

            // INCLUDE MOVE IN LIST OF POSSIBLE MOVES
            if (pc != null)
            {
                if (this.player == 0)
                    newMove = new GameTreeNode(this.player, pc, root.blinkyLoc, root.inkyLoc);
                else if (this.player == 1)
                    newMove = new GameTreeNode(this.player, pc, ghost, root.inkyLoc);
                else
                    newMove = new GameTreeNode(this.player, pc, root.blinkyLoc, ghost);


                // If this is a leaf node, calculate the value of the resulting state
                // EVALUATION FUNCTION
                if (move == depth - 1 && this.player == 2) {
                    List<Point> Ghosts = PacUtils.findGhosts(gridClone);

                    if (Ghosts.size() != 0) {
                        GhostCell nearestGhost = PacUtils.nearestGhost(pc, gridClone);
                        newMove.value += 4 * (BFSPath.getPath(gridClone, nearestGhost.getLoc(), pc).size());
                    }

                    if (PacUtils.foodRemains(gridClone))
                    {
                        newMove.value -= 1.5 * (BFSPath.getPath(gridClone, PacUtils.nearestFood(pc, gridClone), pc).size());
                        if (foodRem > PacUtils.numFood(gridClone))
                        {
                            newMove.value += .75 * foodRem;
                        }
                    }

                    if (PacUtils.numPower(gridClone) > 0)
                    {
                        newMove.value -= 2.5 * (BFSPath.getPath(gridClone, PacUtils.nearestPower(pc, gridClone), pc).size());
                    }

                    //System.out.println("Utility: " + newMove.value + " Blinky: " + newMove.blinkyLoc + " Inky: " +  newMove.inkyLoc+ " Pacman: " + newMove.pcLoc);
                }

                root.possibleMoves.add(newMove);
                grids.add(gridClone);
            }
        }

        this.player++;

        if (this.player > 2) {
            this.player = 0;
            this.move++;
        }

        int currentPlayer = this.player;
        int currentMove = this.move;

        // Generate subtrees for possible states from each generated possible move
        for (GameTreeNode node : root.possibleMoves) {
            generate_Moves(node, grids.get(root.possibleMoves.indexOf(node)));
            this.player = currentPlayer;
            this.move = currentMove;
        }

    }

    public float GetValue(GameTreeNode root)
    {
        float utility = 0;

        // If there are no more possible moves, this is a leaf node,
        // therefore return the distance to the nearest Ghost
        if (root.possibleMoves.isEmpty())
        {
            return root.value;
        }

        for(GameTreeNode node : root.possibleMoves)
        {
            // If this is Pacman's move, return the max value
            // Else, return the min value
            if (root.player == 0)
            {
                // If utility is 0 and GetValue(node) happens to return a negative value,
                // We still need to update the value of the node to the returned utility
                node.value = utility = ((utility == 0) ? GetValue(node) : Math.max(utility, GetValue(node)));
            } else
            {
                // If utility is 0 and GetValue(node) happens to return a positive value,
                // We still need to update the value of the node to the returned utility
                node.value = utility = ((utility == 0) ? GetValue(node) : Math.min(utility, GetValue(node)));
            }
        }

        return utility;
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class GameTree
{
    GameTreeNode root;

    public GameTree(PacCell[][] state, int player)
    {
        Point pcLoc = PacUtils.findPacman(state).getLoc();
        Point blinkyLoc, inkyLoc;
        PacCell nearestGhost = PacUtils.nearestGhost(pcLoc, state);

        List<Point> Ghosts = PacUtils.findGhosts(state);

        if (nearestGhost instanceof BlinkyCell)
        {
            blinkyLoc = nearestGhost.getLoc();
            Ghosts.remove(nearestGhost.getLoc());
            inkyLoc = Ghosts.get(0);
        } else {
            inkyLoc = nearestGhost.getLoc();
            Ghosts.remove(nearestGhost.getLoc());
            blinkyLoc = Ghosts.get(0);
        }

        root = new GameTreeNode(player, pcLoc, blinkyLoc, inkyLoc);
    }

    void printTree(GameTreeNode root)
    {
        System.out.println("\n\n" + root);
        System.out.print("Children: ");
        for (GameTreeNode node : root.possibleMoves) {
            System.out.print(node.value);

        }

        for (GameTreeNode node : root.possibleMoves) {
            printTree(node);
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class GameTreeNode
{
    int player;
    float value;
    List<GameTreeNode> possibleMoves;
    Point pcLoc;
    Point blinkyLoc;
    Point inkyLoc;

    public GameTreeNode(int player, Point pcLoc, Point blinkyLoc, Point inkyLoc)
    {
        this.player = player;
        value = Float.MIN_VALUE;
        possibleMoves = new ArrayList<>();
        this.pcLoc = pcLoc;
        this.blinkyLoc = blinkyLoc;
        this.inkyLoc = inkyLoc;

    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////