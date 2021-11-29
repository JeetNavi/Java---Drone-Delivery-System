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


    public void fly(int angle){
        movesFrom.add(position);
        position = position.nextPosition(angle);
        battery -= 1;
        moves += 1;
        movesTo.add(position);
        anglesOfMoves.add(angle);
    }

    public void hover()
    {
        movesFrom.add(position);
        battery -= 1;
        moves += 1;
        movesTo.add(position);
        anglesOfMoves.add(LongLat.HOVER);
    }

    public List<Point> algorithm (List<Point> landmarkPoints, LongLat destination, Buildings buildings, List<Point> path, Orders orders, String orderNo){

        int bestAngle;

        topLoop:
        while (!position.closeTo(destination)){


            if (!buildings.checkDirectRoute(position, destination)) {
                LongLat closestLandmark = position.getClosestLandmarkToDestination(landmarkPoints, destination, buildings);
                while (!position.closeTo(closestLandmark)){
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(closestLandmark), closestLandmark);

                    //LongLat previousPosition = new LongLat(position.lng, position.lat);

                    fly(bestAngle);
                    path.add(Point.fromLngLat(position.lng, position.lat));

                    //orders.insertIntoFlightpath(orderNo, previousPosition.lng, previousPosition.lat, bestAngle, position.lng, position.lat);

                    outOfMoves = outOfMoves(landmarkPoints, buildings, orders, orderNo);
                    if (outOfMoves){
                        break topLoop;
                    }
                }
            }
            else{
                bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(destination), destination);

                //LongLat previousPosition = new LongLat(position.lng, position.lat);

                fly(bestAngle);
                path.add(Point.fromLngLat(position.lng, position.lat));

                //orders.insertIntoFlightpath(orderNo, previousPosition.lng, previousPosition.lat, bestAngle, position.lng, position.lat);

                outOfMoves = outOfMoves(landmarkPoints, buildings, orders, orderNo);
                if (outOfMoves){
                    break topLoop;
                }
            }
        }
        if (!outOfMoves) {
            this.hover();
            path.add(Point.fromLngLat(position.lng, position.lat));
            //orders.insertIntoFlightpath(orderNo, position.lng, position.lat, LongLat.HOVER, position.lng, position.lat);
            outOfMoves = outOfMoves(landmarkPoints, buildings, orders, orderNo);
        }
        return path;
    }

    public List<Point> algorithmEnd (List<Point> landmarkPoints, Buildings buildings, List<Point> path, Orders orders, String orderNo){

        int bestAngle;

        while (!position.closeTo(appletonTower)){

            if (!buildings.checkDirectRoute(position, appletonTower)) {
                LongLat closestLandmark = position.getClosestLandmarkToDestination(landmarkPoints, appletonTower, buildings);
                while (!position.closeTo(closestLandmark)){
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(closestLandmark), closestLandmark);

                    //LongLat previousPosition = new LongLat(position.lng, position.lat);

                    fly(bestAngle);
                    path.add(Point.fromLngLat(position.lng, position.lat));

                    //orders.insertIntoFlightpath(orderNo, previousPosition.lng, previousPosition.lat, bestAngle, position.lng, position.lat);
                }
            }
            else{
                bestAngle = position.angleToDodgePotentialNfz(buildings, position.bestAngle(appletonTower), appletonTower);

                //LongLat previousPosition = new LongLat(position.lng, position.lat);

                fly(bestAngle);
                path.add(Point.fromLngLat(position.lng, position.lat));

                //orders.insertIntoFlightpath(orderNo, previousPosition.lng, previousPosition.lat, bestAngle, position.lng, position.lat);
            }
        }

        return path;
    }

    public boolean outOfMoves(List<Point> landmarkPoints, Buildings buildings, Orders orders, String orderNo){

        Drone dummyDrone = new Drone();
        dummyDrone.position = position;
        dummyDrone.moves = 0;

        List<Point> dummyList = new ArrayList<>();

        dummyDrone.algorithmEnd(landmarkPoints, buildings, dummyList, orders, orderNo);

        return (battery - dummyDrone.moves < 5);

    }

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
