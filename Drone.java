package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

public class Drone {

    public LongLat appletonTower = new LongLat(-3.186874, 55.944494);

    public LongLat position = appletonTower;
    public int battery = 1500;
    public int moves = 0;
    public boolean outOfMoves = false;
    public List<LongLat> movesFrom = new ArrayList<>();
    public List<LongLat> movesTo = new ArrayList<>();
    public List<Integer> anglesOfMoves = new ArrayList<>();


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
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position, position.bestAngle(closestLandmark), closestLandmark);

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
                bestAngle = position.angleToDodgePotentialNfz(buildings, position, position.bestAngle(destination), destination);

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
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position, position.bestAngle(closestLandmark), closestLandmark);

                    //LongLat previousPosition = new LongLat(position.lng, position.lat);

                    fly(bestAngle);
                    path.add(Point.fromLngLat(position.lng, position.lat));

                    //orders.insertIntoFlightpath(orderNo, previousPosition.lng, previousPosition.lat, bestAngle, position.lng, position.lat);
                }
            }
            else{
                bestAngle = position.angleToDodgePotentialNfz(buildings, position, position.bestAngle(appletonTower), appletonTower);

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

}
