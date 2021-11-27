package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

public class Drone {

    public LongLat appletonTower = new LongLat(-3.186874, 55.944494);

    public LongLat position = appletonTower;
    public int battery = 1500;
    public int moves = 0;

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

        while (!position.closeTo(destination)){

            if (!buildings.checkDirectRoute(position, destination)){
                //LongLat closestLandmark = position.getClosestLandmarkToDestination(landmarkPoints, destination, buildings);
                LongLat closestLandmark = new LongLat(999,999);
                double closestLandmarkDistance = 999;
                for (Point landmarkPoint : landmarkPoints){
                    LongLat landmarkLongLat = new LongLat(landmarkPoint.longitude(), landmarkPoint.latitude());
                    if (buildings.checkDirectRoute(position, landmarkLongLat)
                            && destination.distanceTo(landmarkLongLat) < closestLandmarkDistance){
                        closestLandmark = landmarkLongLat;
                        closestLandmarkDistance = destination.distanceTo(landmarkLongLat);
                    }
                }
                while (!position.closeTo(closestLandmark)){
                    bestAngle = position.bestAngle(closestLandmark);
                    bestAngle = position.angleToDodgePotentialNfz(buildings, position, bestAngle, closestLandmark);
                    path.add(Point.fromLngLat(position.lng, position.lat));
                    this.fly(bestAngle);
                }
            }
            else{
                bestAngle = position.bestAngle(destination);

                bestAngle = position.angleToDodgePotentialNfz(buildings, position, bestAngle, destination);

                path.add(Point.fromLngLat(position.lng, position.lat));
                this.fly(bestAngle);
            }
        }
        this.hover();
        path.add(Point.fromLngLat(position.lng, position.lat));
        path.add(Point.fromLngLat(position.lng, position.lat)); //cuz of how we add to path

        return path;
    }




}
