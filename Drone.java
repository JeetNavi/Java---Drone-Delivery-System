package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a drone that makes deliveries throughout the day.
 * The drone has 3 main attributes that we are mainly interested in: position, battery and moves.
 * We also have other attributes that will hold important information to write to the flightpath table.
 */
public final class Drone {

    /**
     *LongLat object for location of Appleton Tower.
     */
    public static final LongLat appletonTower = new LongLat(-3.186874, 55.944494);

    /**
     * Our main drone will start from Appleton Tower each day.
     */
    private LongLat position = appletonTower;
    /**
     * Our main drone will start with 1500 battery.
     */
    private int battery = 1500;
    /**
     * Our main drone will start with 0 moves made.
     */
    private int moves = 0;

    /**
     * List of LongLats that holds the list of positions a drone has moved from throughout completing an order.
     * This lists content is required to write to the flightpath table, precisely, the fromLongitude and fromLatitude column.
     */
    private List<LongLat> movesFrom = new ArrayList<>();
    /**
     * List of LongLats that holds the list of positions a drone has moved to throughout completing an order.
     * This lists content is required to write to the flightpath table, precisely, the toLongitude and toLatitude column.
     */
    private List<LongLat> movesTo = new ArrayList<>();
    /**
     * List of angles the drone has moved toward throughout completing an order.
     * This lists content is required to write to the flightpath table, precisely, the angle column.
     */
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
     * This method is called if we want our drone to travel to a list of locations in order, passed in as a list of
     * LongLats called destinations.
     * For each destination (location) in the destinations list:
     * &#064;RP: We check if there is a direct route to the destination without any NFZ's blocking the path.
     * If there is not, then we direct the drone towards the landmark which is closest to the destination, while checking for
     * unexpected visits to NFZ's because of the rounding angle problem. Once we have reached this landmark, we repeat
     * from &#064;RP until there is a direct route.
     * We then direct the drone towards the destination while checking for unexpected visits to NFZ's because of the
     * rounding angle problem, until we have reached the destination.
     * After every move (fly or hover), we add the new position to a list of points, which will be used for the GeoJson file
     * that we create.
     *
     *
     * @param landmarkPoints List of points which contains the locations of all the landmarks, which we may divert toward
     *                       if there is no direct route to the destination.
     * @param destinations List of LongLat objects which is the points we are trying to visit. These consist of
     *                     landmarks, shops and a pick up location.
     * @param buildings Buildings object which contains the required information about the NFZ's that we check for when we
     *                  move toward the destination.
     * @param path List of points which we keep passing into this method until the end of the day.
     *
     *
     */
    public final void algorithm (List<Point> landmarkPoints, List<LongLat> destinations, Buildings buildings, List<Point> path){

        int bestAngle;

        for (LongLat destination : destinations) {

            while (!buildings.checkDirectRoute(position, destination)) {
                LongLat closestLandmark = position.getClosestLandmarkToDestination(landmarkPoints, destination, buildings);
                while (position.notCloseTo(closestLandmark)) {
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(closestLandmark), closestLandmark);
                    fly(bestAngle);
                    path.add(Point.fromLngLat(position.lng, position.lat));
                }
            }

            while (position.notCloseTo(destination)){
                bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(destination), destination);
                fly(bestAngle);
                path.add(Point.fromLngLat(position.lng, position.lat));
            }


            this.hover();
            path.add(Point.fromLngLat(position.lng, position.lat));
        }
    }


    /**
     *
     * This method works the same as the algorithm method above, however instead of passing in a list of destinations,
     * we have one destination which is hardcoded as Appleton Tower.
     * This method is used at the end of the day when we have either run out of moves or we have completed delivering the orders.
     *
     * @param landmarkPoints List of points which contains the locations of all the landmarks, which we may divert toward
     *                        if there is no direct route to Appleton Tower.
     * @param buildings Buildings object which contains the required information about the NFZ's that we check for when we
     *                  move toward Appleton Tower.
     * @param path List of points which, after calling this method for our main drone, should be the final time it is updated.
     */
    public final void algorithmEnd (List<Point> landmarkPoints, Buildings buildings, List<Point> path){

        int bestAngle;

        while (position.notCloseTo(appletonTower)){

            if (!buildings.checkDirectRoute(position, appletonTower)) {
                LongLat closestLandmark = position.getClosestLandmarkToDestination(landmarkPoints, appletonTower, buildings);
                while (position.notCloseTo(closestLandmark)){
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
    }

    /**
     *
     * This method checks the number of moves a 'dummy' drone takes to complete an order plus the number of moves
     * it takes to travel back to AT.
     * It does this by normally using the algorithm and algorithmEnd methods with a dummy drone instance.
     * We need to make sure that the number of moves made by the dummy drone is less then or equal to the battery
     * of our main drone so that we are guaranteed that our main drone has enough battery to complete the order and
     * return back to AT if we are finished with the day.
     *
     * @param destinations The list of shops, landmarks and pickup location we need to visit in order, to complete the order.
     *                     This is needed to call the algorithms.
     * @param landmarkPoints List of points which contains the locations of all the landmarks, which we may divert toward
     *      *                       if there is no direct route to the destination. This is also needed for the algorithms.
     * @param buildings Buildings object which contains the required information about the NFZ's that we check for when we
     *      *                  move toward Appleton Tower. This is also needed for the algorithms.
     * @return Boolean value true if we have enough moves to go through with the order at question, and false otherwise.
     */
    public final boolean sufficientNumberOfMovesForOrder(List<LongLat> destinations, List<Point> landmarkPoints, Buildings buildings){
        Drone dummyDrone = new Drone();
        dummyDrone.position = position;
        dummyDrone.algorithm(landmarkPoints, destinations, buildings, new ArrayList<>());
        dummyDrone.algorithmEnd(landmarkPoints, buildings, new ArrayList<>());
        return dummyDrone.moves <= battery;
    }

    /**
     * Getter for drone position.
     * @return Drone position as a LongLat object.
     */
    public final LongLat getPosition(){
        return position;
    }

    /**
     * Getter for number of moves made by drone.
     * @return Number of moves the drone has made.
     */
    public final int getMoves(){
        return moves;
    }

    /**
     * Getter for the positions the drone has moved from.
     * @return List of LongLat objects representing the positions the drone has moved from.
     */
    public final List<LongLat> getMovesFrom(){
        return movesFrom;
    }

    /**
     * Getter for the positions the drone has moved to.
     * @return List of LongLat objects representing the positions the drone has moved to.
     */
    public final List<LongLat> getMovesTo(){
        return movesTo;
    }

    /**
     * Getter for the angles the drone has taken throughout its moves.
     * @return List of integers that is the angles the drone has taken in each move.
     */
    public final List<Integer> getAnglesOfMoves(){
        return anglesOfMoves;
    }

    /**
     * Setter for the drones position.
     * @param newPosition The new position that the drone should be at.
     */
    public final void setPosition(LongLat newPosition){
        this.position = newPosition;
    }

    /**
     * Setter for the drones movesFrom list.
     * @param newMovesFrom Updated list of positions that the drone has moved from (for an order, so will be empty
     *                     at the start of each order).
     */
    public final void setMovesFrom(List<LongLat> newMovesFrom){
        this.movesFrom = newMovesFrom;
    }
    /**
     * Setter for the drones movesTo list.
     * @param newMovesTo Updated list of positions that the drone has moved to (for an order, so will be empty
     *                     at the start of each order).
     */
    public final void setMovesTo(List<LongLat> newMovesTo){
        this.movesTo = newMovesTo;
    }

    /**
     * Setter for the drones angleOfMoves list.
     * @param newAnglesOfMoves Updated list of angles the drone has taken when moving.
     */
    public final void setAnglesOfMoves(List<Integer> newAnglesOfMoves){
        this.anglesOfMoves = newAnglesOfMoves;
    }

}