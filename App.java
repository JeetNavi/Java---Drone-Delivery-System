package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.mapbox.geojson.*;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.awt.geom.Line2D;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {

        //final HttpClient client = HttpClient.newHttpClient(); //Only 1 client is created for all requests.
        String date = args[0];
        String month = args[1];
        String year = args[2];
        String webPort = args[3];
        String dbPort = args[4];
        Date fullDate = Date.valueOf(args[2] + "-" + args[1] + "-" + args[0]);


        Orders order = new Orders(dbPort, fullDate);

        Menus menu = new Menus(webPort);
        Map<String, LongLat> shopsToLongLat = menu.getShopsToLongLat();
        //YOU HAVE SHOPS TO LONGLAT YOU HAVE A ARRAY OF SHOPS FOR A ORDER YOU HAVE MAP (IN ORDERS CLASS) THAT MAPS ORDER NO TO LONGLAT OF DELIVERYLOCATION
        //CREATE MAP THAT MAPS ORDERNO TO LIST OF SHOPS
        //

        System.out.println(order.getOrderNoList());
        System.out.println(order.getDeliverToList());
        List<String> orderNoList = order.getOrderNoList();


        Buildings buildings = new Buildings(webPort);
        List<Point> landmarkPoints = buildings.getLandmarksPoints();
        List<Polygon> nfzPolygons = buildings.getNfzPolygons();
        List<List<List<Point>>> nfzCornerPoints = buildings.getNfzCornerPoints();


        LongLat kfc = new LongLat(LongLat.LONGITUDE_BOUNDARY_EAST, LongLat.LATITUDE_BOUNDARY_NORTH);
        LongLat topMeadows = new LongLat(LongLat.LONGITUDE_BOUNDARY_WEST, LongLat.LATITUDE_BOUNDARY_SOUTH);
        //LongLat appleton = new LongLat(LongLat.APPLETON_LONGITUDE, LongLat.APPLETON_LATITUDE);
        Point appleton = Point.fromLngLat(LongLat.APPLETON_LONGITUDE, LongLat.APPLETON_LATITUDE);

        //LongLat drone = new LongLat(topMeadows.lng, topMeadows.lat);

        Drone drone = new Drone();
        //drone.position = new LongLat(LongLat.APPLETON_LONGITUDE, LongLat.APPLETON_LATITUDE);



        //int bestAngle;

        List<Point> path = new ArrayList<>();

        //List<Point> landmarkPoints = new ArrayList<>();
        landmarkPoints.add(appleton);



        LongLat dest = kfc;

        /**
        while (!drone.position.closeTo(dest)){

            LongLat currentPosition = drone.position;

            if (!buildings.checkDirectRoute(currentPosition, dest)){
                LongLat closestLandmark = new LongLat(999,999);
                double closestLandmarkDistance = 999;
                for (Point landmarkPoint : landmarkPoints){
                    LongLat landmarkLongLat = new LongLat(landmarkPoint.longitude(), landmarkPoint.latitude());
                    if (buildings.checkDirectRoute(currentPosition, landmarkLongLat)
                    && dest.distanceTo(landmarkLongLat) < closestLandmarkDistance){
                        closestLandmark = landmarkLongLat;
                        closestLandmarkDistance = dest.distanceTo(landmarkLongLat);
                    }
                }
                while (!drone.position.closeTo(closestLandmark)){
                    currentPosition = drone.position;
                    bestAngle = currentPosition.bestAngle(closestLandmark);
                    pl.add(Point.fromLngLat(drone.position.lng, drone.position.lat));
                    drone.fly(bestAngle);
                    drone.battery -= 1;
                }
            }
            else{
                bestAngle = currentPosition.bestAngle(kfc);
                pl.add(Point.fromLngLat(drone.position.lng, drone.position.lat));
                drone.fly(bestAngle);
                drone.battery -= 1;
            }
        }

         **/

        //path = drone.algorithm(landmarkPoints, dest, buildings, path);
        //Map<String, LongLat> shopsToLongLat = menu.getShopsToLongLat();

        for (String orderNo : orderNoList){
            Collection<String> itemNames = order.getItemNamesFromOrder(orderNo);
            String[] shopsToVisit = menu.shopsArrayFromItems(itemNames);
            Map<String, LongLat> OrderNoToDeliverToLongLat = order.getOrderNoToDeliverToLongLat(webPort);
            LongLat deliverToLongLat = OrderNoToDeliverToLongLat.get(orderNo);
            String[] tspShopsToVisit = menu.getTspShopsToVisitList(drone.position, shopsToVisit, shopsToLongLat, deliverToLongLat, landmarkPoints, buildings);
            for (String shop : tspShopsToVisit){
                LongLat destination = shopsToLongLat.get(shop);
                path = drone.algorithm(landmarkPoints, destination, buildings, path);
            }
            path = drone.algorithm(landmarkPoints, deliverToLongLat, buildings, path);
        }
        dest = new LongLat(LongLat.APPLETON_LONGITUDE, LongLat.APPLETON_LATITUDE);
        path = drone.algorithm(landmarkPoints, dest, buildings, path);


        System.out.println(drone.battery);



        //CREATING THE GEOJSON FILE
        LineString x = LineString.fromLngLats(path);
        Geometry g = (Geometry)x;
        Feature f = Feature.fromGeometry(g);
        FeatureCollection fc = FeatureCollection.fromFeature(f);

        try {
            File myObj = new File("drone-" + date + "-" + month + "-" + year +".geojson");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());

            } else {
                System.out.println("File already exists.");
            }
        } catch (java.io.IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter("drone-" + date + "-" + month + "-" + year +".geojson");
            myWriter.write(fc.toJson());
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (java.io.IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}

