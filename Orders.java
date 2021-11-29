package uk.ac.ed.inf;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

public class Orders {

    public final String dbPort;
    public final Date fullDate;

    public List<String> orderNoList = new ArrayList<String>();
    public List<String> customerList = new ArrayList<>();
    public List<String> deliverToList = new ArrayList<>();

    MultiValuedMap<String, String> orderItemMap = new ArrayListValuedHashMap<>();
    Map<String, String> orderNoDeliverToMap = new HashMap<>();
    Connection conn;

    PreparedStatement psFlightpath;
    PreparedStatement psDeliveries;

    Orders(String dbPort, Date fullDate){

        this.dbPort = dbPort;
        this.fullDate = fullDate;

        try{

            conn = DriverManager.getConnection("jdbc:derby://localhost:" + dbPort + "/derbyDB");
            //conn.setAutoCommit(false);
            Statement statement = conn.createStatement();

            final String ordersQuery = "select * from orders where deliveryDate=(?)";
            PreparedStatement psOrderQuery = conn.prepareStatement(ordersQuery);
            psOrderQuery.setDate(1, this.fullDate);

            ResultSet rs = psOrderQuery.executeQuery();
            while (rs.next()){
                String orderNo = rs.getString(1);
                orderNoList.add(orderNo);
                customerList.add(rs.getString(3));
                deliverToList.add(rs.getString(4));
                orderNoDeliverToMap.put(orderNo, rs.getString(4));


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
            ResultSet resultSet = databaseMetaData.getTables(null, null, "DELIVERIES", null);
            if (resultSet.next()){
                statement.execute("drop table deliveries");
                System.out.println("dropped deliveries table");
            }
            resultSet = databaseMetaData.getTables(null, null, "FLIGHTPATH", null);
            if (resultSet.next()){
                statement.execute("drop table flightpath");
                System.out.println("dropped flightpath table");
            }

            statement.execute("create table flightpath(" +
                    "orderNo char(8)," +
                    "fromLongitude double," +
                    "fromLatitude double," +
                    "angle integer," +
                    "toLongitude double," +
                    "toLatitude double)");

            statement.execute("create table deliveries(" +
                    "orderNo char(8)," +
                    "deliveredTo varchar(19)," +
                    "costInPence int)");


            psFlightpath = conn.prepareStatement(
                    "insert into flightpath values (?, ?, ?, ?, ?, ?)");

            psDeliveries = conn.prepareStatement(
                    "insert into deliveries values (?, ?, ?)");


        } catch (java.sql.SQLException e){
            e.printStackTrace();
        }



    }

    public Collection<String> getItemNamesFromOrder(String orderNo){
        return (orderItemMap.get(orderNo));
    }

    public List<String> getOrderNoList(){
        return orderNoList;
    }

    public List<String> getCustomerList(){
        return customerList;
    }

    public List<String> getDeliverToList(){
        return deliverToList;
    }

    public Map<String, String> getOrderNoDeliverToMap() {return orderNoDeliverToMap;}

    public String getDeliverToFromOrder(String orderNo) {return orderNoDeliverToMap.get(orderNo);}

    public Map<String, LongLat> getOrderNoToDeliverToLongLat(String webPort){

        Map<String, LongLat> orderNoToDeliverToLongLat = new HashMap<>();

        for (String orderNo : orderNoDeliverToMap.keySet()){
            Words words = new Words(webPort, orderNoDeliverToMap.get(orderNo).split("\\."));
            orderNoToDeliverToLongLat.put(orderNo, words.getCoords());
        }

        return orderNoToDeliverToLongLat;
    }

    public Map<String, Integer> getOrderedValuableOrdersToCostMap (Menus menus){

        Map<String, Integer> orderedValuableOrdersToCostMap = new HashMap<>();

        for (String orderNo : orderNoList){
            int cost = menus.getDeliveryCost(getItemNamesFromOrder(orderNo));
            orderedValuableOrdersToCostMap.put(orderNo, cost);
        }

        List<Map.Entry<String, Integer>> list = new LinkedList<>(orderedValuableOrdersToCostMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()) == 0
        ? o2.getKey().compareTo(o1.getKey())
        : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    public void insertIntoDeliveries (String orderNo, int costInPence){



        try{
            psDeliveries.setString(1, orderNo);
            psDeliveries.setString(2, orderNoDeliverToMap.get(orderNo));
            psDeliveries.setInt(3, costInPence);
            psDeliveries.execute();
        } catch (java.sql.SQLException e){
            e.printStackTrace();

        }




    }

    public void insertIntoFlightpath (String orderNo, double fromLongitude, double fromLatitude, int angle, double toLongitude, double toLatitude){



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
