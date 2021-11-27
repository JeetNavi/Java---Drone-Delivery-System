package uk.ac.ed.inf;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class Orders {

    public final String dbPort;
    public final Date fullDate;

    public List<String> orderNoList = new ArrayList<String>();
    public List<String> customerList = new ArrayList<>();
    public List<String> deliverToList = new ArrayList<>();

    MultiValuedMap<String, String> orderItemMap = new ArrayListValuedHashMap<>();
    Map<String, String> orderNoDeliverToMap = new HashMap<>();

    Orders(String dbPort, Date fullDate){

        this.dbPort = dbPort;
        this.fullDate = fullDate;

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

}
