package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Point;
import org.apache.derby.shared.common.util.ArrayUtil;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class Menus {

    private static final int DELIVERY_COST = 50; //Delivery cost of 50 pence applies to all deliveries.
    public static final int SUCCESSFUL_RESPONSE_CODE = 200;

    public static final HttpClient client = HttpClient.newHttpClient(); //Only 1 client is created for all requests.
    private static final Map<String, Integer> prices = new HashMap<String, Integer>();
    private static final Map<String, String> itemToShop = new HashMap<>();
    private static final Map<String, String[]> shopToWords = new HashMap<>();

    public final String webPort;

    /**
     * Constructor for class Menus.
     *
     * @param webPort     The port where the web server is running.
     */
    Menus(String webPort) {
        this.webPort = webPort;

        HttpResponse<String> response = null;
        try {

            HttpRequest request = HttpRequest.newBuilder() //HTTP GET request.
                    .uri(URI.create("http://localhost:" + webPort + "/menus/menus.json"))
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (IllegalArgumentException e) {

            System.out.println("IllegalArgumentException - URL syntactically incorrect");
            e.printStackTrace();
            System.exit(1);

        } catch (java.net.ConnectException e) {

            System.out.println("Fatal error: unable to connect to localhost at port " + webPort + ".");
            e.printStackTrace();
            System.exit(1);

        } catch (Exception e) {

            System.out.println("An exception has occurred");
            e.printStackTrace();

        }

        assert response != null;
        if (response.statusCode() == SUCCESSFUL_RESPONSE_CODE) {
            Type listType = new TypeToken<List<ShopDetails>>() {
            }.getType();
            List<ShopDetails> shopDetailsList = new Gson().fromJson(response.body(), listType);

            for (ShopDetails shop : shopDetailsList) {
                shopToWords.put(shop.name, shop.location.split("\\."));
                for (ItemDetails itemDetail : shop.menu) {
                    prices.put(itemDetail.item, itemDetail.pence);
                    itemToShop.put(itemDetail.item, shop.name);
                }
            }

        } else {
            System.out.println("Status code is not 200.");
            System.exit(1);
        }



    }

    /**
     * Method which calculates the price of items given as the parameter.
     *
     * @param items variable number of strings which are items from shops.
     * @return price of all items given in pence.
     */
    public int getDeliveryCost(Collection<String> items) {
        int price = DELIVERY_COST;

        for (String item : items) {
            price += prices.get(item);
        }

        return price;
    }

    public String[] shopsArrayFromItems(Collection<String> items) {

        Set <String> shops = new HashSet<>();

        for (String item : items){
            shops.add(itemToShop.get(item));
        }

        return shops.toArray(String[]::new);

    }


    public Map<String, LongLat> getShopsToLongLat (){

        Map<String, LongLat> shopsToLongLat = new HashMap<>();

        for (String shop : shopToWords.keySet()){
            Words words = new Words(webPort, shopToWords.get(shop));
            shopsToLongLat.put(shop, words.getCoords());
        }

        return shopsToLongLat;
    }

    public String[] getTspShopsToVisitList (LongLat currentPosition, String[] shopsToVisit, Map<String, LongLat> shopsToLonglat,
                                            LongLat deliverToLongLat, List<Point> landmarkPoints, Buildings buildings){


        if(shopsToVisit.length == 2) {

            Drone testDrone1 = new Drone();
            testDrone1.position = currentPosition;

            Drone testDrone2 = new Drone();
            testDrone2.position = currentPosition;

            String[] shopsToVisit2 = {shopsToVisit[1], shopsToVisit[0]};

            for (String shop : shopsToVisit){
                LongLat destination = shopsToLonglat.get(shop);
                testDrone1.algorithm(landmarkPoints, destination, buildings, new ArrayList<>());
            }
            testDrone1.algorithm(landmarkPoints, deliverToLongLat, buildings, new ArrayList<>());

            for (String shop: shopsToVisit2){
                LongLat destination = shopsToLonglat.get(shop);
                testDrone2.algorithm(landmarkPoints, destination, buildings, new ArrayList<>());
            }
            testDrone2.algorithm(landmarkPoints, deliverToLongLat, buildings, new ArrayList<>());

            if (testDrone1.moves > testDrone2.moves){
                return shopsToVisit2;
            }

            return shopsToVisit;



        }


        //if (shopsToVisit.length == 2){
        //    if (currentPosition.distanceTo(shopsToLonglat.get(shopsToVisit[1])) < currentPosition.distanceTo(shopsToLonglat.get(shopsToVisit[0]))){
        //        String closeShop = shopsToVisit[1];
        //        String furtherShop = shopsToVisit[0];
        //        shopsToVisit[1] = furtherShop;
        //        shopsToVisit[0] = closeShop;
        //    }
        //}
        return shopsToVisit;
    }


}

