/**
 * University of Central Florida
 * CAP4630 - Speing2019
 * Author(s): Daniel Simoes and James Upchurch
 */
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
 * Simple RNNA Agent
 * @author Daniel Simoes and James Upchurch
 */
public class PacSimRNNA implements PacAction {
   
   private List<Point> path;
   private List<Point> foodList;
   private List<Point> tgtList;
   private int simTime;
   private int[][] distance;
   private int step10PopNum;
      
   public PacSimRNNA( String fname ) {
      PacSim sim = new PacSim( fname );
      sim.init(this);
   }
   
   public static void main( String[] args ) {         
      System.out.println("\nTSP using simple RNNA agent by Daniel Simoes and James Upchurch:");
      System.out.println("\nMaze : " + args[ 0 ] + "\n" );
      new PacSimRNNA( args[ 0 ] );
   }

   @Override
   public void init() {
      simTime = 0;
      path = new ArrayList<Point>();
      tgtList = null;
      step10PopNum = 0;
   }
   
   @Override
   public PacFace action( Object state ) {

      PacCell[][] grid = (PacCell[][]) state;
      PacmanCell pc = PacUtils.findPacman( grid );
      Long preTime;
      
      // make sure Pac-Man is in this game
      if( pc == null ) return null;

      if(tgtList == null){
         foodList = PacUtils.findFood(grid);
         distance = new int[foodList.size()][foodList.size()];

         System.out.println("Cost table:\n");
         for(int i = -1; i < foodList.size(); i++) {
            if (i != -1){
            System.out.printf("%2d ",  BFSPath.getPath(grid, pc.getLoc(), foodList.get(i)).size());}
            for(int j = 0; j < foodList.size(); j++){
               if (i ==-1){
                  if(j ==0) {System.out.printf( "%2d ",  j);}
                  System.out.printf( "%2d ",  BFSPath.getPath(grid, pc.getLoc(), foodList.get(j)).size());}
               else{
               distance[i][j] = BFSPath.getPath(grid, foodList.get(i), foodList.get(j)).size();
               System.out.printf( "%2d ",  distance[i][j]);}
            }
            System.out.println();
         }

         System.out.println("\nFood Array:\n");
         for (int i = 0; i < foodList.size(); i++) {
            System.out.println(i + " : ("+foodList.get(i).x+","+foodList.get(i).y+")");
         }
         
         preTime = System.currentTimeMillis();
         tgtList = RNNA(grid, pc.getLoc());
         System.out.println("\nTime to generate plan: "+(System.currentTimeMillis()-preTime)+" msec");
         System.out.println("\nSolution moves:\n");
      }

      if( path.isEmpty() ) {
         Point tgt = tgtList.remove(0);
         path = BFSPath.getPath(grid, pc.getLoc(), tgt);
      }
      
      // take the next step on the current path
      Point next = path.remove( 0 );
      PacFace face = PacUtils.direction( pc.getLoc(), next );
      System.out.printf( "%5d : From [ %2d, %2d ] go %s%n", 
            ++simTime, pc.getLoc().x, pc.getLoc().y, face );
      return face;
   }

   List<Point> RNNA(PacCell[][] grid, Point pacLoc){
      ArrayList<Integer> unused= new ArrayList<Integer>();
      PointCost tempPointCost;
      for(int i = 0; i < foodList.size(); i++) {
         unused.add(Integer.valueOf(i));
      } 

      System.out.println("\nPopulation at step 1 :");
      System.out.printf("%d : cost=%d : [(%d,%d),%d]\n",0,BFSPath.getPath(grid, pacLoc, foodList.get(0)).size(),foodList.get(0).x,foodList.get(0).y,BFSPath.getPath(grid, pacLoc, foodList.get(0)).size());
      for (int i = 1; i < foodList.size(); i++){
         System.out.printf("%d : cost=%d : [(%d,%d),%d]\n",i,BFSPath.getPath(grid, pacLoc, foodList.get(i)).size(),foodList.get(i).x,foodList.get(i).y,BFSPath.getPath(grid, pacLoc, foodList.get(i)).size());
      }

      System.out.println("\nPopulation at step 10 :");
      PointCost bestPointCost = NNA(new PointCost(Integer.valueOf(0), BFSPath.getPath(grid, pacLoc, foodList.get(0)).size()), new ArrayList<Integer>(unused), grid, pacLoc);
      for (int i = 1; i < foodList.size(); i++){
         tempPointCost =  NNA(new PointCost(Integer.valueOf(i), BFSPath.getPath(grid, pacLoc, foodList.get(i)).size()), new ArrayList<Integer>(unused), grid, pacLoc);
         if (tempPointCost.cost < bestPointCost.cost) bestPointCost = tempPointCost;
      }

      List<Point> tgts = new ArrayList<Point>();
      for(int i = 0; i<foodList.size(); i++){
         tgts.add(foodList.get(bestPointCost.points.get(i)));
      }
      return tgts;
   }
   
   PointCost NNA(PointCost path, ArrayList<Integer> unused, PacCell[][] grid, Point pacLoc){
      PointCost bestPointCost;
      PointCost tempPointCost;

      unused.remove(path.points.get(path.points.size()-1));
      if (unused.size() == 0) {
         System.out.printf("%d : cost=%d : [(%d,%d),%d]",step10PopNum++,path.cost,foodList.get(path.points.get(0)).x,foodList.get(path.points.get(0)).y,BFSPath.getPath(grid, pacLoc, foodList.get(path.points.get(0))).size());
         for (int i = 1; i<foodList.size(); i++){
            System.out.printf(" [(%d,%d),%d]",foodList.get(path.points.get(i)).x,foodList.get(path.points.get(i)).y,distance[path.points.get(i-1)][path.points.get(i)]);
         }
         System.out.println();
         return path;
      }
      
      unused.sort((a, b) -> distance[path.points.get(path.points.size()-1)][a] < distance[path.points.get(path.points.size()-1)][b] ? -1 
                          : distance[path.points.get(path.points.size()-1)][a] == distance[path.points.get(path.points.size()-1)][b] ? 0 
                          : 1);
      
      
      bestPointCost = NNA(new PointCost(path.AddPoint(unused.get(0), distance[path.points.get(path.points.size()-1)][unused.get(0)])), new ArrayList<Integer>(unused), grid, pacLoc);
      int min = distance[path.points.get(path.points.size()-1)][unused.get(0)];
      
      //Branch out if there are multiple pellets of the same distance
      for (int i = 1; i < unused.size(); i++){
         if (distance[path.points.get(path.points.size()-1)][unused.get(i)] == min){           
            tempPointCost = NNA(new PointCost(path.AddPoint(unused.get(i), distance[path.points.get(path.points.size()-1)][unused.get(i)])), new ArrayList<Integer>(unused), grid, pacLoc);
            if(tempPointCost.cost < bestPointCost.cost){
               bestPointCost = tempPointCost;
            }
         }
      }
      return bestPointCost;
   }
}

class PointCost {
   public ArrayList<Integer> points = new ArrayList<Integer>();
   public int cost;
   PointCost(Integer point, int cost){
      this.points.add(point);
      this.cost = cost;
   }

   PointCost(PointCost copy) {
      this.points.addAll(copy.points);
      this.cost = copy.cost;
   }

   PointCost AddPoint(int point, int distance){
      PointCost temp = new PointCost(this);
      temp.points.add(Integer.valueOf(point));
      temp.cost += distance;
      return temp;
   }
}