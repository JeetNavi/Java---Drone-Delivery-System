package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

public class Drone {

    public LongLat appletonTower = new LongLat(-3.186874, 55.944494);

    public LongLat position = appletonTower;
    public int battery = 1500;
    public int moves = 0;
    public boolean outOfMovess = false;


    public void fly(int angle){
        position = position.nextPosition(angle);
        battery -= 1;
        moves += 1;
    }

    public void hover()
    {battery -= 1;
        moves += 1;}

    public List<Point> algorithm (List<Point> landmarkPoints, LongLat destination, Buildings buildings, List<Point> path){

        int bestAngle;

        topLoop:
        while (!position.closeTo(destination)){


            if (!buildings.checkDirectRoute(position, destination)) {
                LongLat closestLandmark = position.getClosestLandmarkToDestination(landmarkPoints, destination, buildings);
                while (!position.closeTo(closestLandmark)){
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position, position.bestAngle(closestLandmark), closestLandmark);
                    fly(bestAngle);
                    path.add(Point.fromLngLat(position.lng, position.lat));
                    if (battery == 100){
                        int i = 5;
                    }
                    outOfMovess = outOfMoves(landmarkPoints, buildings);
                    if (outOfMovess){
                        break topLoop;
                    }
                }
            }
            else{
                bestAngle = position.angleToDodgePotentialNfz(buildings, position, position.bestAngle(destination), destination);
                fly(bestAngle);
                path.add(Point.fromLngLat(position.lng, position.lat));
                outOfMovess = outOfMoves(landmarkPoints, buildings);
                if (outOfMovess){
                    break topLoop;
                }
                if (battery == 100){
                    int i = 5;
                }
            }
        }
        if (!outOfMovess) {
            this.hover();
            path.add(Point.fromLngLat(position.lng, position.lat));
            outOfMovess = outOfMoves(landmarkPoints, buildings);
            if (battery == 100){
                int i = 5;
            }
        }
        return path;
    }

    public List<Point> algorithmEnd (List<Point> landmarkPoints, Buildings buildings, List<Point> path){

        int bestAngle;

        while (!position.closeTo(appletonTower)){

            if (!buildings.checkDirectRoute(position, appletonTower)) {
                LongLat closestLandmark = position.getClosestLandmarkToDestination(landmarkPoints, appletonTower, buildings);
                while (!position.closeTo(closestLandmark)){
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position, position.bestAngle(closestLandmark), closestLandmark);
                    fly(bestAngle);
                    path.add(Point.fromLngLat(position.lng, position.lat));
                }
            }
            else{
                bestAngle = position.angleToDodgePotentialNfz(buildings, position, position.bestAngle(appletonTower), appletonTower);
                fly(bestAngle);
                path.add(Point.fromLngLat(position.lng, position.lat));
            }
        }

        return path;
    }

    public boolean outOfMoves(List<Point> landmarkPoints, Buildings buildings){

        Drone dummyDrone = new Drone();
        dummyDrone.position = position;
        dummyDrone.moves = 0;

        List<Point> dummyList = new ArrayList<>();

        dummyDrone.algorithmEnd(landmarkPoints, buildings, dummyList);

        return (battery - dummyDrone.moves < 5);

    }

}
