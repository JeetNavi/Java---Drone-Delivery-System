package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

public class Drone {


    public static final LongLat appletonTower = new LongLat(-3.186874, 55.944494);

    private LongLat position = appletonTower;
    private int battery = 1500;
    private int moves = 0;
    private boolean outOfMoves = false;

    private List<LongLat> movesFrom = new ArrayList<>();
    private List<LongLat> movesTo = new ArrayList<>();
    private List<Integer> anglesOfMoves = new ArrayList<>();


    /**
     *
     * Method that makes the drone fly toward a given angle.
     * It will move 0.00015 towards the angle.
     * The position of the drone is updated using the nexPosition method from LongLat class.
     * Battery goes down 1 after one move.
     * Our moves counter will go up 1.
     * We also add the angle of travel in a list of angles that is added to the flightpath tables.
     * We also add the position of the drone to a list before we flew to the next position, which is also for the flightpath table.
     * We also add the position of the drone to a list after we fly to the next position, which is also for the flightpath table.
     * These lists gets reset after each order is complete and added to the tables in the db.
     *
     * @param angle Integer angle in degrees is the direction which we would like to fly toward where we follow the convention that
     *              0 degrees is east and 90 is north, etc...
     */
    public void fly(int angle){
        movesFrom.add(position);
        position = position.nextPosition(angle);
        battery -= 1;
        moves += 1;
        movesTo.add(position);
        anglesOfMoves.add(angle);
    }

    /**
     *
     * Method that makes the drone hover in a position.
     * The position after a hover move will be unchanged.
     * The battery will go down by 1.
     * Our moves counter will go up by 1.
     * We add the same position to both movesFrom list and movesTo list.
     * Since we are hovering, the angle is the junk value of -999, which is added to the list angleOfMoves.
     *
     */
    public void hover()
    {
        movesFrom.add(position);
        battery -= 1;
        moves += 1;
        movesTo.add(position);
        anglesOfMoves.add(LongLat.JUNK_VALUE);
    }

    /**
     *
     * This method is the main algorithm which controls the flightpath of the drone.
     * This method is called if we want our drone to travel from point A to point B (destination).
     * We first check if there is a direct route to the destination without any NFZ's blocking the path.
     * If there is, then we simply take this route, while checking for unexpected NFZ's because of rounding the angle.
     * If there is not, then we direct the drone towards the landmark which is closest to the destination, then after
     * that it is sent toward the destination as before, if there is a direct route (which there should be).
     * We repeat this until either we are close to the destination or our drone has enough battery to only return back
     * to Appleton Tower, in which we break out of the method completely.
     * After every move (fly or hover), we add the new position to a list of points, which will be used for the GeoJson file
     * that we create.
     *
     *
     * @param landmarkPoints List of points which contains the locations of all the landmarks, which we may divert toward
     *                       if there is no direct route to the destination.
     * @param destination LongLat object which is the point we are trying to move towards. This can be a landmark, shop or pick-up location.
     * @param buildings Buildings object which contains the required information about the NFZ's that we check for when we
     *                  move toward the destination.
     * @param path List of points which we keep passing into this method until the end of the day.
     *
     * @return List of points which contains the drones positions move by move.
     * By the end of the day, this list should contain every position the drone moved to in order.
     */
    public List<Point> algorithm (List<Point> landmarkPoints, LongLat destination, Buildings buildings, List<Point> path){

        int bestAngle;

        topLoop:
        while (!position.closeTo(destination)){


            if (!buildings.checkDirectRoute(position, destination)) {
                LongLat closestLandmark = position.getClosestLandmarkToDestination(landmarkPoints, destination, buildings);
                while (!position.closeTo(closestLandmark)){
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(closestLandmark), closestLandmark);
                    fly(bestAngle);
                    path.add(Point.fromLngLat(position.lng, position.lat));

                    outOfMoves = outOfMoves(landmarkPoints, buildings);
                    if (outOfMoves){
                        break topLoop;
                    }
                }
            }
            else{
                bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(destination), destination);
                fly(bestAngle);
                path.add(Point.fromLngLat(position.lng, position.lat));

                outOfMoves = outOfMoves(landmarkPoints, buildings);
                if (outOfMoves){
                    break topLoop;
                }
            }
        }
        if (!outOfMoves) {
            this.hover();
            path.add(Point.fromLngLat(position.lng, position.lat));

            outOfMoves = outOfMoves(landmarkPoints, buildings);
        }
        return path;
    }


    /**
     *
     * This method works the same as the algorithm method above, however the destination is hardcoded as Appleton Tower.
     * This method is used at the end of the day when we have either run out of moves or we have completed delivering the orders.
     *
     * @param landmarkPoints List of points which contains the locations of all the landmarks, which we may divert toward
     *                        if there is no direct route to Appleton Tower.
     * @param buildings Buildings object which contains the required information about the NFZ's that we check for when we
     *                  move toward Appleton Tower.
     * @param path List of points which, after calling this method for our main drone, should be the final time it is updated.
     * @return List of points which contains the drones positions move by move throughout the day.
     */
    public List<Point> algorithmEnd (List<Point> landmarkPoints, Buildings buildings, List<Point> path){

        int bestAngle;

        while (!position.closeTo(appletonTower)){

            if (!buildings.checkDirectRoute(position, appletonTower)) {
                LongLat closestLandmark = position.getClosestLandmarkToDestination(landmarkPoints, appletonTower, buildings);
                while (!position.closeTo(closestLandmark)){
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(closestLandmark), closestLandmark);
                    fly(bestAngle);
                    path.add(Point.fromLngLat(position.lng, position.lat));
                }
            }
            else{
                bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(appletonTower), appletonTower);
                fly(bestAngle);
                path.add(Point.fromLngLat(position.lng, position.lat));
            }
        }

        return path;
    }

    /**
     *
     * Method which runs the algorithmEnd on a dummy drone (which is of the same position of the main drone)
     * and makes sure that the number of moves made by that dummy drone to get to Appleton Tower is at least 5 lower
     * than the battery left in the main drone.
     * We use the value 5 here as a safety net in case we need to make some unexpected adjustments to angles, causing
     * us to make more moves than calculated.
     *
     *
     * @param landmarkPoints List of points which contains the locations of all the landmarks, which is needed as a parameter
     *                       for the algorithmEnd method.
     * @param buildings Buildings object which contains the required information about the NFZ's that we need to pass in
     *                  to the algorithmEnd method.
     * @return Boolean value true if we need to stop with our deliveries because we don't have sufficient battery left,
     *         or false if we can carry on with enough battery.
     */
    public boolean outOfMoves(List<Point> landmarkPoints, Buildings buildings){

        int SAFETY_BUFFER = 5;

        Drone dummyDrone = new Drone();
        dummyDrone.position = position;
        dummyDrone.moves = 0;

        List<Point> dummyList = new ArrayList<>();

        dummyDrone.algorithmEnd(landmarkPoints, buildings, dummyList);

        return (battery - dummyDrone.moves < SAFETY_BUFFER);

    }

    //Getters
    public LongLat getPosition(){
        return position;
    }

    public int getMoves(){
        return moves;
    }

    public boolean getOutOfMoves(){
        return outOfMoves;
    }

    public List<LongLat> getMovesFrom(){
        return movesFrom;
    }

    public List<LongLat> getMovesTo(){
        return movesTo;
    }

    public List<Integer> getAnglesOfMoves(){
        return anglesOfMoves;
    }

    //Setters
    public void setPosition(LongLat newPosition){
        this.position = newPosition;
    }

    public void setMovesFrom(List<LongLat> newMovesFrom){
        this.movesFrom = newMovesFrom;
    }

    public void setMovesTo(List<LongLat> newMovesTo){
        this.movesTo = newMovesTo;
    }

    public void setAnglesOfMoves(List<Integer> newAnglesOfMoves){
        this.anglesOfMoves = newAnglesOfMoves;
    }

}