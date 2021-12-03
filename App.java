package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.io.File;
import java.io.FileWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class is where we run our main method from.
 * This is where we create instances of every class and use methods from other classes to get our drone working.
 */
public final class App
{
    /**
     * This method contains the general functionality of how the program processes and completes the deliveries, as well
     * as creates/writes to the necessary files.
     *
     * We first store the program arguments with meaningful variable names.
     * We create every object we need: The main Drone object, Orders object, Menus object and Buildings object.
     *
     * We also create a hash map called ordersSortedByValue which is just a hash map that maps order numbers to its value.
     * This hash map however is sorted so that orders of higher value appear first in the map. (Descending in value).
     * We also create a hash map called orderNoToDeliverToLongLat which simply maps the order number to its delivery
     * location as a LongLat object.
     *
     * We create a list of point objects called landmark points which contains all the landmarks' location as a point object.
     * We include Appleton Tower as a landmark (way point).
     *
     * We create our path list which will contain the list of points that the main drone has travelled to (every move).
     * At the end of the day, this will be converted to a single line string which we write to a created geoJson file.
     *
     * We enter the for loop where our drone will attempt to collect and deliver every order placed starting with the
     * most valuable ones first, this is a greedy approach.
     * For each order, we pre-compute the most efficient route using the method getTspShopsToVisitLongLatList.
     * We then check that we have a sufficient number of moves to make this delivery as well as return back to AT.
     *      If it does, then we complete the order and delivery, updating the two tables and the path of the drone.
     *      If it does not, then we repeat this process for the next most valuable order in the list (or hash map in our case).
     * Once we get to the point where we have delivered every order or we have no battery left to deliver any more, we
     * return back to AT.
     * While delivering the orders, we also update the monetary value of orders delivered.
     *
     *
     *
     * @param args The program arguments of the form [date, month, year, webPort, dbPort].
     *             The calendar arguments is the day from which we would like to complete those days orders.
     *             The webPort is the port the web server is running on.
     *             The dbPort is the port the database is running on.
     */
    public static void main( String[] args )
    {

        Point appleton = Point.fromLngLat(Drone.appletonTower.lng, Drone.appletonTower.lat);

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

        Map<String, Integer> ordersSortedByValue = orders.getOrderedValuableOrdersToCostMap(menu);
        Map<String, LongLat> OrderNoToDeliverToLongLat = orders.getOrderNoToDeliverToLongLat(webPort);

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

        int counter;

        for (String orderNo : ordersSortedByValue.keySet()){

            Collection<String> itemNames = orders.getItemNamesFromOrder(orderNo);
            String[] shopsToVisit = menu.shopsArrayFromItems(itemNames);
            LongLat deliverToLongLat = OrderNoToDeliverToLongLat.get(orderNo);
            List<LongLat> tspShopsToVisitLongLats = menu.getTspShopsToVisitLongLatList(drone.getPosition(), shopsToVisit, deliverToLongLat, landmarkPoints, buildings);

            //Check if the drone has enough moves to complete the order as well as return back to AT.
            //If it does have enough moves for this, then we execute the algorithm, update the monetary value and write into the deliveries table.
            if (drone.sufficientNumberOfMovesForOrder(tspShopsToVisitLongLats, landmarkPoints, buildings)){
                drone.algorithm(landmarkPoints, tspShopsToVisitLongLats, buildings, path);
                int costInPenceOfOrder = ordersSortedByValue.get(orderNo);
                monetaryValue += costInPenceOfOrder;
                orders.insertIntoDeliveries(orderNo, costInPenceOfOrder);
                counter = 0;
                for (LongLat moveFrom : drone.getMovesFrom()){
                    orders.insertIntoFlightpath(orderNo, moveFrom.lng, moveFrom.lat,
                            drone.getAnglesOfMoves().get(counter), drone.getMovesTo().get(counter).lng, drone.getMovesTo().get(counter).lat);
                    counter+=1;
                }
                //Since we have completed writing the orders' data into the tables, we can clear them and make them ready
                //for the next order.
                drone.setMovesFrom(new ArrayList<>());
                drone.setMovesTo(new ArrayList<>());
                drone.setAnglesOfMoves(new ArrayList<>());
            }
        }

        //After we have either completed delivering every order, or we have no battery left in the drone to deliver any
        //more orders, we call this method that returns the drone back to AT.
        drone.algorithmEnd(landmarkPoints, buildings, path);

        //Write to the flightpath table.
        counter = 0;
        for (LongLat moveFrom : drone.getMovesFrom()){
            orders.insertIntoFlightpath(null, moveFrom.lng, moveFrom.lat,
                    drone.getAnglesOfMoves().get(counter), drone.getMovesTo().get(counter).lng, drone.getMovesTo().get(counter).lat);
            counter+=1;
        }

        //Calculating the percentage monetary value delivered on the date in question.
        double percentageMonetaryValue = (monetaryValue / totalMonetaryValuePlaced) * 100;
        System.out.println("Total monetary value delivered is " + monetaryValue);
        System.out.println("Total monetary value placed is " + totalMonetaryValuePlaced);
        System.out.println("Percentage monetary value is " + percentageMonetaryValue + "%");
        System.out.println(drone.getMoves() + " Moves made");

        LineString x = LineString.fromLngLats(path);
        Feature f = Feature.fromGeometry(x);
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
