package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Point;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class Menus {

    //Delivery cost of 50 pence applies to all orders.
    public static final int DELIVERY_COST = 50;
    public static final int SUCCESSFUL_RESPONSE_CODE = 200;
    public static final int MAX_SHOPS_TO_VISIT = 2;

    //Only 1 client is created for all requests across every class.
    public static final HttpClient client = HttpClient.newHttpClient();
    public static final Map<String, Integer> prices = new HashMap<String, Integer>();
    public static final Map<String, String> itemToShop = new HashMap<>();
    public static final Map<String, String[]> shopToWords = new HashMap<>();

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

            System.err.println("IllegalArgumentException - URL syntactically incorrect");
            e.printStackTrace();
            System.exit(1);

        } catch (java.net.ConnectException e) {

            System.err.println("Fatal error: unable to connect to localhost at port " + webPort + ".");
            e.printStackTrace();
            System.exit(1);

        } catch (Exception e) {

            System.err.println("An exception has occurred");
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
            System.err.println("Status code is not 200.");
            System.exit(1);
        }
    }

        /**
         *
         * Method which calculates the total price of the given items as well as a 50 pence delivery fee.
         * This makes use of the HashMap 'prices' to retrieve the price of an item.
         *
         * @param items Collection of Strings which are the names of the items that we would like the prices for.
         * @return Integer value of the total price of all items in pence, including the 50 pence delivery charge.
         */
        public int getDeliveryCost(Collection<String> items) {

            int price = DELIVERY_COST;

            for (String item : items) {
                price += prices.get(item);
            }

            return price;
        }

    /**
     *
     * Method that creates a string array of shop names that are home to the items passed in in the collection of item names.
     * Since 2 items can be from 1 shop, to avoid the duplication of shops in the final array, we make use of a Set which
     * is later casted to an array.
     *
     * @param items Collection of Strings which are the names of items sold at shops.
     * @return String array of shop names that are home to the items in the collection.
     */
    public String[] shopsArrayFromItems(Collection<String> items) {

            Set<String> shops = new HashSet<>();

            for (String item : items){
                shops.add(itemToShop.get(item));
            }

            return shops.toArray(String[]::new);

        }


    /**
     *
     * Method that creates a useful HashMap that maps each shop to its location as a LongLat object.
     * This makes use of the Words class which gets the location of a shops what3words address from the web server.
     *
     * @return HashMap that maps shop names to their locations as LongLats.
     */
    public Map<String, LongLat> getShopsToLongLat (){

            Map<String, LongLat> shopsToLongLat = new HashMap<>();

            for (String shop : shopToWords.keySet()){
                Words words = new Words(webPort, shopToWords.get(shop));
                shopsToLongLat.put(shop, words.getCoordinates());
            }

            return shopsToLongLat;
        }

    /**
     * Method that sorts an array of shops to visit for a given order (orderNo) such that travelling to the shops
     * in order of how they appear in the array is the best (minimal moves) route.
     * Since an order can only have items from at most 2 shops, there is only two combinations of routes to test:
     * Route 1: Current Position -> Shop 1 -> Shop 2 -> Pickup Location,
     * Route 2: Current Position -> Shop 2 -> Shop 1 -> Pickup location.
     * Note: if an order only has items from one shop, it does not enter the first if statement and returns the shopsToVisit
     * array directly without any sorting (since it is of 1 element).
     * This is a brute force approach to a TSP problem.
     * The algorithm method is needed here to calculate the cost (number of moves) of each route.
     *
     * @param currentPosition LongLat object where we are starting the order from.
     * @param shopsToVisit Array of Strings which can be of length 1 or 2 which holds the name of the shops to visit for the order.
     * @param deliverToLongLat Pickup Location which gets considered when calculating the cost of each route.
     * @param landmarkPoints List of landmark Point objects which is needed as a parameter to run the algorithm method.
     * @param buildings Buildings object which contains information about the NFZ's that is crucial when calculating the cost of each route.
     * @return
     */
        public String[] getTspShopsToVisitList (LongLat currentPosition, String[] shopsToVisit,
                LongLat deliverToLongLat, List<Point> landmarkPoints, Buildings buildings){

            //Max number of shops that can be visited per order is 2.
            if(shopsToVisit.length == MAX_SHOPS_TO_VISIT) {

                Map<String, LongLat> shopsToLonglat = getShopsToLongLat();

                Drone testDrone1 = new Drone();
                testDrone1.setPosition(currentPosition);

                Drone testDrone2 = new Drone();
                testDrone2.setPosition(currentPosition);

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

                if (testDrone1.getMoves() > testDrone2.getMoves()){
                    return shopsToVisit2;
                }

                return shopsToVisit;

            }

            return shopsToVisit;
        }

}

