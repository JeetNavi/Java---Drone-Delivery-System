package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

public class LongLat {

    public static Drone dummyDrone = new Drone();
    public static final double DEFAULT_DISTANCE = 0.00015;

    public static final int NORTH = 90;
    public static final int EAST = 0;
    public static final int SOUTH = 270;
    public static final int WEST = 180;
    public static final int JUNK_VALUE = -999;

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
     * Method that calculates the distance between the current Longlat object and a given Longlat object passed in
     * as a parameter.
     * Uses the concept of the Pythagorean theorem.
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
     * Method that calculates whether the location of the current Longlat object is close to the location of a
     * given Longlat object AKA if they are within a distance of 0.00015 from each other.
     * This makes use of the distanceTo method above.
     *
     * @param point instance of LongLat which is the other location we use to see if the current location is close.
     * @return true if the current location of the Longlat object is within a distance of 0.00015 (default distance)
     * from the given Longlat object.
     */
    public boolean closeTo(LongLat point) {
        return distanceTo(point) < DEFAULT_DISTANCE;
    }

    /**
     * Method that calculates the new position (coordinates) of a LongLat object after either flying (angles between 0
     * and 350 inclusive) or hovering (angle of junk value -999).
     *
     * @param angle The angle the drone is moving towards where we follow the convention on the document.
     * @return LongLat object which is the new coordinates of the location after moving towards the given angle.
     */
    public LongLat nextPosition(int angle) {
        //Switch statement for "regular" angles for: east, north , west and south. Also includes angle for hovering.
        switch (angle) {
            //drone is hovering i.e. no changes to latitude/longitude.
            case JUNK_VALUE:
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

        double v = Math.sin(Math.toRadians(angle % 90)) * DEFAULT_DISTANCE;

        //angles between 0 and 90 would be increasing longitude and increasing latitude.
        if (angle < 90) {
            double latDist = v;
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
            double latDist = v;
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

    /**
     *
     * Method that calculates the best angle (rounded to the nearest 10) to take to travel from the current
     * Longlat object to the given Longlat object passed in.
     * Uses mathematical formula Tan x = Opposite / Adjacent in calculating best angle.
     *
     * @param destination Longlat object in which we calculate the best angle to travel toward it.
     * @return Integer value of the best angle to take to travel to destination.
     */
    public int bestAngle(LongLat destination){

        int bestAngle = 0;

        double lngStart = lng;
        double latStart = lat;
        double lngEnd = destination.lng;
        double latEnd = destination.lat;

        double lngDist = Math.abs(lngStart - lngEnd);
        double latDist = Math.abs(latStart - latEnd);


        //Calculates the best angle (rounded to nearest 10) relative to its quadrant using Tan x = Opposite / Adjacent.
        int angle = ((int)(Math.round(((int)(Math.round(Math.toDegrees(Math.atan(latDist/lngDist)))))/10.0)*10));


        //Quadrant 1 - North East
        if (lngStart <= lngEnd && latStart <= latEnd) {
            return angle;
        }

        //Quadrant 2 - North West
        if(lngStart >= lngEnd && latStart <= latEnd){
            return 180 - angle;
        }

        //Quadrant 3 - South West
        if(lngStart >= lngEnd && latStart >= latEnd){
            return angle + 180;
        }

        //Quadrant 4 - South East
        if(lngStart <= lngEnd && latStart >= latEnd){
            return (360 - angle) % 360;
        }

        return bestAngle;
    }

    /**
     *
     * Method that gets the closest landmark (as a Longlat object) from some other location of another Longlat object
     * that we call destination.
     * As well as checking if the landmark is closest, we must also check that there is a direct path toward it
     * and that there are no no-fly-zones preventing a direct route from the position of the current longlat object.
     *
     * @param landmarkPoints A list of all the landmarks as Point objects - which is very similar to a longlat object.
     * @param destination Longlat object from which we are getting the closest landmark to.
     * @param buildings Buildings object which contains all the information about the no-fly-zones and enables the use
     *                  of the checkDirectRoute method which checks for no-fly-zones in the direct route to landmark.
     * @return The closest Landmark to the provided destination as a Longlat object that can be directly travelled to
     * from the position of the current longlat object.
     */
    public LongLat getClosestLandmarkToDestination(List<Point> landmarkPoints, LongLat destination, Buildings buildings){
        List <LongLat> landmarkLongLats = new ArrayList<>();
        //Convert every landmark Point object to Longlat object.
        for (Point p : landmarkPoints){
            landmarkLongLats.add(new LongLat(p.longitude(), p.latitude()));
        }

        //Create closest Longlat object with junk value positions to start with.
        LongLat closestLandmark = new LongLat(JUNK_VALUE,JUNK_VALUE);

        for (LongLat landmark : landmarkLongLats){
            if (destination.distanceTo(landmark) < destination.distanceTo(closestLandmark) && buildings.checkDirectRoute(this, landmark)) {
                closestLandmark = landmark;
            }
        }
        return closestLandmark;
    }

    /**
     *
     * Since the best angle had to be rounded to the nearest 10, the rounded angle could cause the unexpected visit to
     * a NFZ.
     * This method provides a solution if such a situation was to arise, and calculates an adjusted angle that
     * will dodge the NFZ if taken.
     *
     * @param buildings Buildings object which contains all the information about the no-fly-zones and enables the use
     *      *           of the checkDirectRoute method which checks for no-fly-zones in the next move of the adjusted angle.
     * @param bestAngle The integer value of the first best angle calculated where rounding it to the nearest 10 could've
     *                  caused the unexpected visit to NFZ.
     * @param destination Longlat object of the final destination that we are aiming to go to. This is relevant here to
     *                    help decide whether we should adjust the angle up 10 or down 10 degrees. We pick the one that
     *                    gets us closer to the destination.
     * @return
     */
    public int angleToDodgePotentialNfz (Buildings buildings, int bestAngle , LongLat destination){

        //Angle rounding to 10 causing unexpected journey through nfz so we check for + and - 10
        int potentialAdjustedAngle1 = (bestAngle + 10) % 360;
        int potentialAdjustedAngle2 = (bestAngle - 10) % 360;

        LongLat potentialNextPosition = (nextPosition(bestAngle));
        LongLat potentialNextAdjustedPosition1 = (nextPosition(potentialAdjustedAngle1));
        LongLat potentialNextAdjustedPosition2 = (nextPosition(potentialAdjustedAngle2));

        if (!buildings.checkDirectRoute(this, potentialNextPosition)) {

            if (buildings.checkDirectRoute(this, potentialNextAdjustedPosition1)) {
                bestAngle = potentialAdjustedAngle1;
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
