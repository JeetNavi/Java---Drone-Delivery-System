package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

public class LongLat {

    //This is the distance a drone moves when flying and is the distance used to check whether two locations are close
    public static final double DEFAULT_DISTANCE = 0.00015;

    public static final int NORTH = 90;
    public static final int EAST = 0;
    public static final int SOUTH = 270;
    public static final int WEST = 180;
    public static final int HOVER = -999;

    public final double lng;
    public final double lat;

    /**
     * Constructor for class LongLat.
     *
     * @param longitude We use longitude and latitude for locations.
     * @param latitude  We use longitude and latitude for locations.
     */
    LongLat(double longitude, double latitude) {
        this.lng = longitude;
        this.lat = latitude;
    }

    /**
     * Method that calculates the distance between the current location of the drone and another given
     * location.
     *
     * @param point instance of LongLat which is the other location we use to calculate the distance.
     * @return distance between the two points as double.
     */
    public double distanceTo(LongLat point) {
        double x = lng - point.lng;
        double y = lat - point.lat;
        return Math.sqrt((x * x) + (y * y));
    }

    /**
     * Method that calculates whether the current location is close to a given location AKA if they are within
     * 0.00015 from each other.
     *
     * @param point instance of LongLat which is the other location we use to see the current location is close.
     * @return true if the current location of the drone is close to the location given.
     */
    public boolean closeTo(LongLat point) {
        return distanceTo(point) < DEFAULT_DISTANCE;
    }

    /**
     * Method that calculates the coordinates of the drones position after either flying or hovering.
     *
     * @param angle the angle the drone is moving towards where we follow the convention on the document.
     * @return LongLat object which is the new coordinates of the drone.
     */
    public LongLat nextPosition(int angle) {
        //Switch statement for "regular" angles for: east, north , west and south. Also includes angle for hovering.
        switch (angle) {
            //drone is hovering i.e. no changes to latitude/longitude.
            case HOVER:
                return this;
            //drone will fly northward i.e. latitude increases.
            case NORTH:
                return new LongLat(this.lng, this.lat + DEFAULT_DISTANCE);
            //drone will fly westward i.e. longitude decreases.
            case WEST:
                return new LongLat(this.lng - DEFAULT_DISTANCE, this.lat);
            //drone will fly southward i.e. latitude decreases.
            case SOUTH:
                return new LongLat(this.lng, this.lat - DEFAULT_DISTANCE);
            //drone will fly eastward i.e. longitude increases.
            case EAST:
                return new LongLat(this.lng + DEFAULT_DISTANCE, this.lat);
        }

        //Use of trig for formula for calculating the distance travelled in both latitude and longitude.
        //double latDist = Math.sin(Math.toRadians(angle % 90)) * DEFAULT_DISTANCE;
        //double longDist = Math.sqrt((DEFAULT_DISTANCE * DEFAULT_DISTANCE) - (latDist * latDist));

        //angles between 0 and 90 would be increasing longitude and increasing latitude.
        if (angle < 90) {
            double latDist = Math.sin(Math.toRadians(angle % 90)) * DEFAULT_DISTANCE;
            double longDist = Math.sqrt((DEFAULT_DISTANCE * DEFAULT_DISTANCE) - (latDist * latDist));
            return new LongLat(this.lng + longDist, this.lat + latDist);
        }

        //angles between 90 and 180 would be increasing latitude and decreasing longitude.
        else if (angle < 180) {
            double latDist = Math.sin(Math.toRadians(180 - angle)) * DEFAULT_DISTANCE;
            double longDist = Math.sqrt((DEFAULT_DISTANCE * DEFAULT_DISTANCE) - (latDist * latDist));
            return new LongLat(this.lng - longDist, this.lat + latDist);
        }

        //angles between 180 and 270 would be decreasing longitude and decreasing latitude.
        else if (angle < 270) {
            double latDist = Math.sin(Math.toRadians(angle % 90)) * DEFAULT_DISTANCE;
            double longDist = Math.sqrt((DEFAULT_DISTANCE * DEFAULT_DISTANCE) - (latDist * latDist));
            return new LongLat(this.lng - longDist, this.lat - latDist);
        }

        //angles between 270 and 360 would be decreasing latitude and increasing longitude.
        else{
            double latDist = Math.sin(Math.toRadians(360 - angle)) * DEFAULT_DISTANCE;
            double longDist = Math.sqrt((DEFAULT_DISTANCE * DEFAULT_DISTANCE) - (latDist * latDist));
            return new LongLat(this.lng + longDist, this.lat - latDist);
        }
    }


    public int bestAngle(LongLat destination){

        int bestAngle = 0;

        double lngStart = lng;
        double latStart = lat;
        double lngEnd = destination.lng;
        double latEnd = destination.lat;

        double lngDist = Math.abs(lngStart - lngEnd);
        double latDist = Math.abs(latStart - latEnd);


        int angle = ((int)(Math.round(((int)(Math.round(Math.toDegrees(Math.atan(latDist/lngDist)))))/10.0)*10));


        if (lngStart <= lngEnd && latStart <= latEnd) {

            return angle;
        }

        if(lngStart >= lngEnd && latStart <= latEnd){

            return 180 - angle;
        }

        if(lngStart >= lngEnd && latStart >= latEnd){

            return angle + 180;
        }

        if(lngStart <= lngEnd && latStart >= latEnd){

            return (360 - angle) % 360;
        }

        return bestAngle;

    }

    public LongLat getClosestLandmarkToDestination(List<Point> landmarkPoints, LongLat destination, Buildings buildings){
        List <LongLat> landmarkLongLats = new ArrayList<>();
        for (Point p : landmarkPoints){
            landmarkLongLats.add(new LongLat(p.longitude(), p.latitude()));
        }

        LongLat closest = new LongLat(-999,-999);

        for (LongLat landmark : landmarkLongLats){
            if (destination.distanceTo(landmark) < destination.distanceTo(closest) && buildings.checkDirectRoute(this, landmark)) {
                closest = landmark;
            }

        }
        return closest; //maybe move this method to buildings?
    }

    public int angleToDodgePotentialNfz (Buildings buildings, int bestAngle , LongLat destination){

        int potentialAdjustedAngle1 = (bestAngle + 10) % 360;
        int potentialAdjustedAngle2 = (bestAngle - 10) % 360;

        LongLat potentialNextPosition = (nextPosition(bestAngle));
        LongLat potentialNextAdjustedPosition1 = (nextPosition(potentialAdjustedAngle1));
        LongLat potentialNextAdjustedPosition2 = (nextPosition(potentialAdjustedAngle2));

        if (!buildings.checkDirectRoute(this, potentialNextPosition)) {
            if (buildings.checkDirectRoute(this, potentialNextAdjustedPosition1)) {
                bestAngle = potentialAdjustedAngle1; //say about rounding to 10 causing unexpected journey through nfz so we check for + and - 10
            } else if (buildings.checkDirectRoute(this, potentialNextAdjustedPosition2)){
                bestAngle = potentialAdjustedAngle2;
            }else{
                double distanceOne = nextPosition(potentialAdjustedAngle1).distanceTo(destination);
                double distanceTwo = nextPosition(potentialAdjustedAngle2).distanceTo(destination);
                if (distanceOne < distanceTwo){
                    return angleToDodgePotentialNfz(buildings, potentialAdjustedAngle1, destination);
                } else return angleToDodgePotentialNfz(buildings, potentialAdjustedAngle2, destination);
            }
        }
        return bestAngle;
    }

}
