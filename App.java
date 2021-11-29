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


        Orders orders = new Orders(dbPort, fullDate);

        Menus menu = new Menus(webPort);
        Map<String, LongLat> shopsToLongLat = menu.getShopsToLongLat();
        //YOU HAVE SHOPS TO LONGLAT YOU HAVE A ARRAY OF SHOPS FOR A ORDER YOU HAVE MAP (IN ORDERS CLASS) THAT MAPS ORDER NO TO LONGLAT OF DELIVERYLOCATION
        //CREATE MAP THAT MAPS ORDERNO TO LIST OF SHOPS
        //

        Map<String, Integer> ordersSortedByValue = orders.getOrderedValuableOrdersToCostMap(menu);


        Buildings buildings = new Buildings(webPort);
        List<Point> landmarkPoints = buildings.getLandmarksPoints();

        Point appleton = Point.fromLngLat(LongLat.APPLETON_LONGITUDE, LongLat.APPLETON_LATITUDE);

        //LongLat drone = new LongLat(topMeadows.lng, topMeadows.lat);

        Drone drone = new Drone();
        //drone.position = new LongLat(LongLat.APPLETON_LONGITUDE, LongLat.APPLETON_LATITUDE);



        //int bestAngle;

        List<Point> path = new ArrayList<>();
        path.add(Point.fromLngLat(appleton.longitude(), appleton.latitude()));

        //List<Point> landmarkPoints = new ArrayList<>();
        landmarkPoints.add(appleton);





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

        String currentOrderNo = null;

        double totalMonetaryValuePlaced = 0;
        double monetaryValue = 0;
        for (double orderCost : ordersSortedByValue.values()){
            totalMonetaryValuePlaced += orderCost;
        }

        topLoop:
        for (String orderNo : ordersSortedByValue.keySet()){
            Collection<String> itemNames = orders.getItemNamesFromOrder(orderNo);
            String[] shopsToVisit = menu.shopsArrayFromItems(itemNames);
            Map<String, LongLat> OrderNoToDeliverToLongLat = orders.getOrderNoToDeliverToLongLat(webPort);
            LongLat deliverToLongLat = OrderNoToDeliverToLongLat.get(orderNo);
            String[] tspShopsToVisit = menu.getTspShopsToVisitList(drone.position, shopsToVisit, shopsToLongLat, deliverToLongLat, landmarkPoints, buildings, orders, orderNo);
            for (String shop : tspShopsToVisit){
                LongLat destination = shopsToLongLat.get(shop);
                path = drone.algorithm(landmarkPoints, destination, buildings, path, orders, orderNo);
                if (drone.outOfMoves){
                    break topLoop;
                }
            }
            path = drone.algorithm(landmarkPoints, deliverToLongLat, buildings, path, orders, orderNo);
            int costInPenceOfOrder = ordersSortedByValue.get(orderNo);
            monetaryValue += costInPenceOfOrder;
            orders.insertIntoDeliveries(orderNo, costInPenceOfOrder);

            int counter = 0;
            for (LongLat moveFrom : drone.movesFrom){
                orders.insertIntoFlightpath(orderNo, moveFrom.lng, moveFrom.lat,
                        drone.anglesOfMoves.get(counter), drone.movesTo.get(counter).lng, drone.movesTo.get(counter).lat);
                counter+=1;
            }

            drone.movesFrom.clear();
            drone.movesTo.clear();
            drone.anglesOfMoves.clear();

            currentOrderNo = orderNo;

        }


        //this is for when you realise you have no moves left, you break out of the toploop but never insert into table
        int counter = 0;
        for (LongLat moveFrom : drone.movesFrom){
            orders.insertIntoFlightpath(currentOrderNo, moveFrom.lng, moveFrom.lat,
                    drone.anglesOfMoves.get(counter), drone.movesTo.get(counter).lng, drone.movesTo.get(counter).lat);
            counter+=1;
        }

        drone.movesFrom.clear();
        drone.movesTo.clear();
        drone.anglesOfMoves.clear();


        path = drone.algorithmEnd(landmarkPoints, buildings, path, orders, null);

        counter = 0;
        for (LongLat moveFrom : drone.movesFrom){
            orders.insertIntoFlightpath(null, moveFrom.lng, moveFrom.lat,
                    drone.anglesOfMoves.get(counter), drone.movesTo.get(counter).lng, drone.movesTo.get(counter).lat);
            counter+=1;
        }

        double percentageMonetaryValue = (monetaryValue / totalMonetaryValuePlaced) * 100;
        System.out.println(percentageMonetaryValue);
        System.out.println(drone.moves);


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
