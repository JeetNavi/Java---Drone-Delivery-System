package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.io.File;
import java.io.FileWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class App 
{
    public static void main( String[] args )
    {

        final double APPLETON_LONGITUDE = -3.186874;
        final double APPLETON_LATITUDE = 55.944494;
        Point appleton = Point.fromLngLat(APPLETON_LONGITUDE, APPLETON_LATITUDE);

        String date = args[0];
        String month = args[1];
        String year = args[2];
        String webPort = args[3];
        String dbPort = args[4];
        Date fullDate = Date.valueOf(args[2] + "-" + args[1] + "-" + args[0]);


        Drone drone = new Drone();
        Orders orders = new Orders(dbPort, fullDate);
        Menus menu = new Menus(webPort);
        Buildings buildings = new Buildings(webPort);

        Map<String, LongLat> shopsToLongLat = menu.getShopsToLongLat();
        Map<String, Integer> ordersSortedByValue = Orders.getOrderedValuableOrdersToCostMap(menu);
        Map<String, LongLat> OrderNoToDeliverToLongLat = Orders.getOrderNoToDeliverToLongLat(webPort);

        List<Point> landmarkPoints = Buildings.landmarkPoints;
        landmarkPoints.add(appleton);

        //We add Appleton Tower to our path since we always start from there.
        List<Point> path = new ArrayList<>();
        path.add(Point.fromLngLat(appleton.longitude(), appleton.latitude()));

        //Calculating the total monetary value of orders placed on the given date.
        double totalMonetaryValuePlaced = 0;
        double monetaryValue = 0;
        for (double orderCost : ordersSortedByValue.values()){
            totalMonetaryValuePlaced += orderCost;
        }

        String currentOrderNo = null;

        int counter2 = 0;

        topLoop:
        for (String orderNo : ordersSortedByValue.keySet()){
            counter2+=1;
            System.out.println(counter2);
            if (counter2 == 20){
                counter2 = counter2;
            }
            Collection<String> itemNames = Orders.getItemNamesFromOrder(orderNo);
            String[] shopsToVisit = menu.shopsArrayFromItems(itemNames);
            LongLat deliverToLongLat = OrderNoToDeliverToLongLat.get(orderNo);
            String[] tspShopsToVisit = menu.getTspShopsToVisitList(drone.getPosition(), shopsToVisit, deliverToLongLat, landmarkPoints, buildings, orders, orderNo);
            for (String shop : tspShopsToVisit){
                LongLat destination = shopsToLongLat.get(shop);
                path = drone.algorithm(landmarkPoints, destination, buildings, path);
                if (drone.getOutOfMoves()){
                    break topLoop;
                }
            }
            path = drone.algorithm(landmarkPoints, deliverToLongLat, buildings, path);
            int costInPenceOfOrder = ordersSortedByValue.get(orderNo);
            //We keep updating this variable to find the total monetary value of orders delivered.
            monetaryValue += costInPenceOfOrder;
            Orders.insertIntoDeliveries(orderNo, costInPenceOfOrder);

            //Beginning to write to the flightpath table.
            int counter = 0;
            for (LongLat moveFrom : drone.getMovesFrom()){
                Orders.insertIntoFlightpath(orderNo, moveFrom.lng, moveFrom.lat,
                        drone.getAnglesOfMoves().get(counter), drone.getMovesTo().get(counter).lng, drone.getMovesTo().get(counter).lat);
                counter+=1;
            }

            //Since we are finished with the current order, and we have written everything relevant to the flightpath table
            //about the order, we can clear the lists to begin with our next order.
            drone.setMovesFrom(new ArrayList<LongLat>());
            drone.setMovesTo(new ArrayList<LongLat>());
            drone.setAnglesOfMoves(new ArrayList<Integer>());

            currentOrderNo = orderNo;
        }


        //this is for when we realise we have no moves left, we break out of the topLoop but never insert into table flightpath.
        //This is why we had to keep a variable currentOrderNo to remember the orderNo in such a situation.
        //Beginning to write to the flightpath table.
        int counter = 0;
        for (LongLat moveFrom : drone.getMovesFrom()){
            Orders.insertIntoFlightpath(currentOrderNo, moveFrom.lng, moveFrom.lat,
                    drone.getAnglesOfMoves().get(counter), drone.getMovesTo().get(counter).lng, drone.getMovesTo().get(counter).lat);
            counter+=1;
        }

        //Since we are done with all the orders, and have written all the relevant information to the flightpath table about
        //the orders, we can clear these lists so that they can be updated with the information on the flightpath back to
        //appleton tower.
        drone.setMovesFrom(new ArrayList<LongLat>());
        drone.setMovesTo(new ArrayList<LongLat>());
        drone.setAnglesOfMoves(new ArrayList<Integer>());

        //Path finding back to Appleton Tower.
        path = drone.algorithmEnd(landmarkPoints, buildings, path);

        //Beginning to write to the flightpath table.
        counter = 0;
        for (LongLat moveFrom : drone.getMovesFrom()){
            Orders.insertIntoFlightpath(null, moveFrom.lng, moveFrom.lat,
                    drone.getAnglesOfMoves().get(counter), drone.getMovesTo().get(counter).lng, drone.getMovesTo().get(counter).lat);
            counter+=1;
        }

        //Calculating the percentage monetary value delivered on the date in question.
        double percentageMonetaryValue = (monetaryValue / totalMonetaryValuePlaced) * 100;
        System.out.println("Percentage monetary value is " + percentageMonetaryValue + "%");
        System.out.println(drone.getMoves() + " Moves made");



        LineString x = LineString.fromLngLats(path);
        Geometry g = (Geometry)x;
        Feature f = Feature.fromGeometry(g);
        FeatureCollection fc = FeatureCollection.fromFeature(f);

        //Creating the geoJson file that will hold information about the drones flightpath throughout the day.
        try {
            File myObj = new File("drone-" + date + "-" + month + "-" + year +".geojson");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());

            } else {
                System.out.println("GeoJson file " + myObj.getName() + " already exists.");
            }
        } catch (java.io.IOException e) {
            System.err.println("An error occurred.");
            e.printStackTrace();
        }

        //Writing to the geoJson file
        try {
            FileWriter myWriter = new FileWriter("drone-" + date + "-" + month + "-" + year +".geojson");
            myWriter.write(fc.toJson());
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (java.io.IOException e) {
            System.err.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
