package uk.ac.ed.inf;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents every order that is placed on a given date.
 * Once an orders object is created, it has lots of information we need about every order from a given date,
 * such as its order number and its pick up location (delivery location).
 * This means we only need to create one orders object since the orders made will not change on the given day.
 * All the information relevant to this class is on the database.
 *
 */
public final class Orders {

    /**
     * Database port number is needed to access the database that has information on every order placed on any date.
     */
    public final String dbPort;
    /**
     * We use the date variable so that we can retrieve orders from that date from the database (all at once). This
     * reduces the amount of data we pull from the database, making it so we only pull the required data. We achieve this
     * by using this data value in the SQL query when retrieving data.
     */
    public final Date fullDate;

    /**
     * List of the orders' order number of the date given.
     * These will be eight-character hexadecimal strings giving the unique order number of an order.
     */
    public static final List<String> orderNoList = new ArrayList<>();
    /**
     * List of the orders' pick up location.
     * These will be a variable-length string of at most 18 characters giving the WhatThreeWords address of
     * the delivery location.
     */
    public static final List<String> deliverToList = new ArrayList<>();
    /**
     * This hash map maps the eight-character hexadecimal string giving the unique order number of an order to the items
     * that were requested from shops in the order.
     * Since we can have one order to many items, we use a multi-valued hash map to achieve this affect.
     */
    public static final MultiValuedMap<String, String> orderItemMap = new ArrayListValuedHashMap<>();
    /**
     * This hash map maps the eight-character hexadecimal string giving the unique order number of an order to the
     * variable-length string of at most 18 characters giving the WhatThreeWords address of the delivery location.
     */
    public static final Map<String, String> orderNoDeliverToMap = new HashMap<>();

    /**
     * To limit the number of prepared statements we make, we declare them here, outside of any method that may be called more than once.
     */
    private static PreparedStatement psFlightpath;
    private static PreparedStatement psDeliveries;


    /**
     * Constructor for class Orders.
     * We must specify the port the database is running on and the date we are interested in when creating an Orders object.
     *
     * We connect to the database server and we retrieve the information we need from the two tables.
     * From one table we get the order number and where to deliver it to, and for each of these orders, we get the items
     * that was requested.
     *
     * We also drop the flightpath and deliveries table if they exist and create them.
     * We initialise our prepare statements so that we are ready to insert into any of the two tables with the methods.
     *
     * @param dbPort the port the web server is running on.
     * @param fullDate the date that the orders must be placed on.
     */
    Orders(String dbPort, Date fullDate){

        this.dbPort = dbPort;
        this.fullDate = fullDate;

        //Reading from orders table
        try{

            Connection conn = DriverManager.getConnection("jdbc:derby://localhost:" + dbPort + "/derbyDB");
            Statement statement = conn.createStatement();

            final String ordersQuery = "select * from orders where deliveryDate=(?)";
            PreparedStatement psOrderQuery = conn.prepareStatement(ordersQuery);
            psOrderQuery.setDate(1, this.fullDate);

            ResultSet rs = psOrderQuery.executeQuery();
            while (rs.next()){
                String orderNo = rs.getString(1);
                orderNoList.add(orderNo);
                deliverToList.add(rs.getString(4));
                orderNoDeliverToMap.put(orderNo, rs.getString(4));

                //Reading from orderDetails table
                try{

                    final String orderDetailsQuery = "select * from orderDetails where orderNo=(?)";
                    PreparedStatement psOrderDetailsQuery = conn.prepareStatement(orderDetailsQuery);
                    psOrderDetailsQuery.setString(1, orderNo);

                    ResultSet rs2 = psOrderDetailsQuery.executeQuery();
                    while(rs2.next()){
                        String item = rs2.getString(2);
                        orderItemMap.put(orderNo, item);
                    }

                } catch (java.sql.SQLException e){
                    e.printStackTrace();
                }
            }


            DatabaseMetaData databaseMetaData = conn.getMetaData();

            //Drop deliveries table if it exists
            ResultSet resultSet = databaseMetaData.getTables(null, null, "DELIVERIES", null);
            if (resultSet.next()){
                statement.execute("drop table deliveries");
                System.out.println("dropped deliveries table");
            }
            //Drop flightpath table if it exists
            resultSet = databaseMetaData.getTables(null, null, "FLIGHTPATH", null);
            if (resultSet.next()){
                statement.execute("drop table flightpath");
                System.out.println("dropped flightpath table");
            }

            //Create flightpath table
            statement.execute("create table flightpath(" +
                    "orderNo char(8)," +
                    "fromLongitude double," +
                    "fromLatitude double," +
                    "angle integer," +
                    "toLongitude double," +
                    "toLatitude double)");

            //Create deliveries table
            statement.execute("create table deliveries(" +
                    "orderNo char(8)," +
                    "deliveredTo varchar(19)," +
                    "costInPence int)");

            //Set up prepareStatements here so we don't create lots of them
            psFlightpath = conn.prepareStatement(
                    "insert into flightpath values (?, ?, ?, ?, ?, ?)");

            psDeliveries = conn.prepareStatement(
                    "insert into deliveries values (?, ?, ?)");


        } catch (java.sql.SQLException e){
            e.printStackTrace();
        }

    }

    /**
     *
     * Gets the item names from a given order by searching in a multivalued HashMap called orderItemMap.
     * This means that the key may appear more than once in the HashMap. This is to allow more than one item being related
     * to an order.
     *
     * @param orderNo String orderNo of the order from which we want to get the items from.
     * @return Collection of strings which is the names of the items from the order.
     */
    public final Collection<String> getItemNamesFromOrder(String orderNo){
        return (orderItemMap.get(orderNo));
    }

    /**
     *
     * Method that creates a hashmap that maps orders' order number to the pick up location as a LongLat.
     * This iterates over each W3W pick up location and
     * Uses the getCoordinates method from Words class to convert W3W address into LongLat.
     *
     * @param webPort webPort of web server is needed to convert from w3w into longlat.
     * @return Hashmap mapping orderNo to pick up location as LongLat.
     */
    public final Map<String, LongLat> getOrderNoToDeliverToLongLat(String webPort){

        Map<String, LongLat> orderNoToDeliverToLongLat = new HashMap<>();

        for (String orderNo : orderNoDeliverToMap.keySet()){
            Words words = new Words(webPort, orderNoDeliverToMap.get(orderNo).split("\\."));
            orderNoToDeliverToLongLat.put(orderNo, words.getCoordinates());
        }
        return orderNoToDeliverToLongLat;
    }


    /**
     *
     * Method that creates a hashmap that maps order numbers to their monetary value.
     * This hashmap is sorted so that the value is decreasing.
     * So the orders with the highest value is at the start of the hashmap.
     *
     * @param menus Menus object is needed so that we can use the getDeliveryCost method for orders.
     * @return sorted Hashmap that maps order numbers to its value in decreasing order of value.
     */
    public final Map<String, Integer> getOrderedValuableOrdersToCostMap (Menus menus){

        Map<String, Integer> orderedValuableOrdersToCostMap = new HashMap<>();

        for (String orderNo : orderNoList){
            int cost = menus.getDeliveryCost(orderItemMap.get(orderNo));
            orderedValuableOrdersToCostMap.put(orderNo, cost);
        }

        List<Map.Entry<String, Integer>> list = new LinkedList<>(orderedValuableOrdersToCostMap.entrySet());

        // Sorting the list based on values in decreasing order
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    /**
     *
     * Method that writes to the deliveries table.
     * The prepared statement created in the constructor is getting updated and executed with values passed in.
     *
     * @param orderNo  the eight-character hexadecimal string assigned to this order in the orders table.
     * @param costInPence the total cost of the order, including the standard 50p delivery charge.
     */
    public final void insertIntoDeliveries (String orderNo, int costInPence){

        try{
            psDeliveries.setString(1, orderNo);
            psDeliveries.setString(2, orderNoDeliverToMap.get(orderNo));
            psDeliveries.setInt(3, costInPence);
            psDeliveries.execute();
        } catch (java.sql.SQLException e){
            e.printStackTrace();

        }
    }

    /**
     *
     * Method that writes to the flightpath table.
     * The prepared statement created in the constructor is getting updated and executed with values passed in.
     *
     * @param orderNo the eight-character order number for the lunch order which the drone is currently
     * collecting or delivering.
     * @param fromLongitude the longitude of the drone at the start of this move.
     * @param fromLatitude the latitude of the drone at the start of this move.
     * @param angle the angle of travel of the drone in this move.
     * @param toLongitude the longitude of the drone at the end of this move.
     * @param toLatitude the latitude of the drone at the end of this move.
     */
    public final void insertIntoFlightpath (String orderNo, double fromLongitude, double fromLatitude, int angle, double toLongitude, double toLatitude){

        try{
            psFlightpath.setString(1, orderNo);
            psFlightpath.setDouble(2, fromLongitude);
            psFlightpath.setDouble(3, fromLatitude);
            psFlightpath.setInt(4, angle);
            psFlightpath.setDouble(5, toLongitude);
            psFlightpath.setDouble(6, toLatitude);
            psFlightpath.execute();
        } catch (java.sql.SQLException e){
            e.printStackTrace();
        }
    }


}
