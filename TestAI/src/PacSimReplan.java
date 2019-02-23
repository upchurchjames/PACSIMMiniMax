
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import pacsim.BFSPath;
import pacsim.PacAction;
import pacsim.PacCell;
import pacsim.PacFace;
import pacsim.PacSim;
import pacsim.PacUtils;
import pacsim.PacmanCell;

/**
 * Simple Re-planning Search Agent
 * @author Dr. Demetrios Glinos
 */
public class PacSimReplan implements PacAction {
   
   private List<Point> path;
   private int simTime;
      
   public PacSimReplan( String fname ) {
      PacSim sim = new PacSim( fname );
      sim.init(this);
   }
   
   public static void main( String[] args ) {         
      System.out.println("\nTSP using simple replanning agent by Dr. Demetrios Glinos:");
      System.out.println("\nMaze : " + args[ 0 ] + "\n" );
      new PacSimReplan( args[ 0 ] );
   }

   @Override
   public void init() {
      simTime = 0;
      path = new ArrayList();
   }
   
   @Override
   public PacFace action( Object state ) {

      PacCell[][] grid = (PacCell[][]) state;
      PacmanCell pc = PacUtils.findPacman( grid );
      
      // make sure Pac-Man is in this game
      if( pc == null ) return null;
            
      // if current path completed (or just starting out),
      // select a the nearest food using the city-block 
      // measure and generate a path to that target
      
      if( path.isEmpty() ) {
         Point tgt = PacUtils.nearestFood( pc.getLoc(), grid);
         path = BFSPath.getPath(grid, pc.getLoc(), tgt);
         
         System.out.println("Pac-Man currently at: [ " + pc.getLoc().x
               + ", " + pc.getLoc().y + " ]");
         System.out.println("Setting new target  : [ " + tgt.x
               + ", " + tgt.y + " ]");
      }
      
      // take the next step on the current path
      
      Point next = path.remove( 0 );
      PacFace face = PacUtils.direction( pc.getLoc(), next );
      System.out.printf( "%5d : From [ %2d, %2d ] go %s%n", 
            ++simTime, pc.getLoc().x, pc.getLoc().y, face );
      return face;
   }
}
