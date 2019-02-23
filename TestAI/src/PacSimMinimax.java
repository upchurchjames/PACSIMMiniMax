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
        PacFace newFace = null;
        Point temp = PacUtils.findPacman(grid).getLoc();
        this.tree = null;

        float maxUtil = Integer.MIN_VALUE;
        int maxUtilID = -1;
        move = 0;

        generate(grid);
        GameTreeNode root = tree.root;
        // tree.printTree(root);

        for(GameTreeNode node : root.possibleMoves)
        {
            node.value = GetValue(node);
            if (node.value > maxUtil)
            {
                maxUtil = node.value;
                maxUtilID = root.possibleMoves.indexOf(node);
            }
        }

        Point oldPC = PacUtils.findPacman(grid).getLoc();
        Point newPC = PacUtils.findPacman(root.possibleMoves.get(maxUtilID).grid).getLoc();

        newFace = PacUtils.direction(oldPC, newPC);
        System.out.println(newPC);

        return newFace;
    }

    // Generate tree of possible states
    public void generate(PacCell[][] grid)
    {
        // Create tree with root
        tree = new GameTree(grid, player);
        GameTreeNode root = tree.root;

        // Build remaining tree with possible states from root
        generate_Moves(root);

    }

    // Generate tree based on possible moves at specified depth
    public void generate_Moves(GameTreeNode root)
    {
        if (move >= depth)
            return;

        boolean include = true;

        // Find all ghost positions
        List<Point> ghosts = PacUtils.findGhosts(root.grid);

        // Generate moves in every direction
        // (ie not added to list of possible moves
        for (int j = 0; j < 4; j++)
        {
            // Copy grid and determine direction of next move
            PacCell[][] tempGrid = root.grid.clone();
            PacFace dir;

            // Pick the next direction
            switch(j)
            {
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
                PacmanCell pacman = PacUtils.findPacman(tempGrid);
                if (pacman == null)
                    continue;

                Point pc = pacman.getLoc();
                Point neighbor = PacUtils.neighbor(dir, pacman, tempGrid).getLoc();
                System.out.println(PacUtils.neighbor(dir, pacman, tempGrid).getClass());
                if (PacUtils.neighbor(dir, pacman, tempGrid).getClass().equals(WallCell.class))
                {
                    include = false;
                }

                //System.out.println(PacUtils.neighbor(dir, pacman, tempGrid).getClass());
                tempGrid = PacUtils.movePacman(pc, neighbor, tempGrid);


            } else
            {
                Point ghost = ghosts.get(this.player - 1);
                Point neighbor = PacUtils.neighbor(dir, ghost, tempGrid).getLoc();

                if (PacUtils.neighbor(dir, ghost, tempGrid).getClass().equals(WallCell.class))
                {
                    include = false;
                }

                tempGrid = PacUtils.moveGhost(ghost, neighbor, tempGrid);
                //System.out.println(PacUtils.neighbor(dir, ghost, tempGrid).getClass());

                PacmanCell temp = PacUtils.findPacman(tempGrid);

                System.out.println();
            }

            if (include)
            {
                //System.out.print(this.player + " ");
                GameTreeNode newMove = new GameTreeNode(tempGrid, this.player);

                // If this is a leaf node, calculate the value of the resulting state
                if (move == depth && this.player == 2)
                {
                    float nearestPowerDist;

                    PacmanCell pc = PacUtils.findPacman(newMove.grid);
                    List<Point> newGhosts = PacUtils.findGhosts(newMove.grid);

                    if (pc == null)
                    {
                        continue;
                    }

                    if (newGhosts.size() != 0)
                    {
                        GhostCell nearestGhost = PacUtils.nearestGhost(pc.getLoc(), newMove.grid);
                        newMove.value -= (float) 4 / (float) BFSPath.getPath(newMove.grid, pc.getLoc(), nearestGhost.getLoc()).size();
                    }

                    if (PacUtils.numPower(newMove.grid) > 0)
                    {
                        nearestPowerDist = (float) 1 / (float) BFSPath.getPath(newMove.grid, pc.getLoc(), PacUtils.nearestPower(pc.getLoc(), newMove.grid)).size();
                        newMove.value += nearestPowerDist;
                    }
                }

                root.possibleMoves.add(newMove);
            }

            include = true;
        }

        this.player++;

        if (this.player > 2)
        {
            this.player = 0;
            move += 1;
        }

        int currentPlayer = this.player;
        int currentMove = move;

        // Generate subtrees for possible states from each generated possible move
        for(GameTreeNode node : root.possibleMoves)
        {
            generate_Moves(node);
            this.player = currentPlayer;
            move = currentMove;
        }
    }

    public float GetValue(GameTreeNode root)
    {
        float utility = 0;
        int player = root.player;
        // System.out.println("CURRENT PLAYER: " + player);

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
            if (player == 0)
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

            // System.out.print("UTILITY: " + node.value + " ");
        }

        // System.out.println();

        return utility;
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class GameTree
{
    GameTreeNode root;

    public GameTree(PacCell[][] state, int player)
    {
        root = new GameTreeNode(state, player);
    }

    void printTree(GameTreeNode root)
    {
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

class GameTreeNode
{
    int player;
    float value;
    PacCell[][] grid;
    List<GameTreeNode> possibleMoves;

    public GameTreeNode(PacCell[][] state, int player)
    {
        this.player = player;
        value = 0;
        grid = PacUtils.cloneGrid(state);
        possibleMoves = new ArrayList<>();
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////