package uk.ac.ed.inf;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * This class represents the coordinates of W3W addresses.
 * Once a words object is called with the web port and the three words that make up a what three words address, we will have
 * the coordinates of its address.
 */
public final class Words {

    /**
     * the web port where the web server is being run is needed to retrieve the information about the W3W address.
     */
    public final String webPort;
    /**
     * The LongLat object is created to associate the W3W address to coordinates.
     */
    private static LongLat coordinates;

    /**
     * Constructor for Words class.
     * Whenever we create a words object, we must pass in the port the server is running on and a string array consisting
     * of 3 words which combines to make a what 3 words address.
     *
     * From the web server, with our 3 word address the only thing that we are interested in getting is the coordinates of it.
     * @param webPort the port the web server is running on.
     * @param threeWords String array of the words that make up the W3W address, that we would like to convert to coordinates.
     */
    Words(String webPort, String[] threeWords) {
        this.webPort = webPort;

        String wordOne = threeWords[0];
        String wordTwo = threeWords[1];
        String wordThree = threeWords[2];

        try {

            HttpRequest request = HttpRequest.newBuilder() //HTTP GET request.
                    .uri(URI.create("http://localhost:" + webPort + "/words/" + wordOne + "/" + wordTwo + "/" + wordThree + "/details.json"))
                    .build();
            HttpResponse<String> response = Menus.client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == Menus.SUCCESSFUL_RESPONSE_CODE){
                coordinates = new Gson().fromJson(response.body(), W3wDetails.class).coordinates;
            }
            else {
                System.err.println("Status code is not 200.");
                System.exit(1);
            }

        } catch (IllegalArgumentException e){

            System.err.println("IllegalArgumentException - URL syntactically incorrect");
            e.printStackTrace();
            System.exit(1);

        } catch (java.net.ConnectException e){

            System.err.println("Fatal error: unable to connect to localhost at port " + webPort + ".");
            e.printStackTrace();
            System.exit(1);

        } catch (Exception e){

            System.err.println("An exception has occurred");
            e.printStackTrace();
        }
    }

    /**
     * Getter for coordinates.
     * @return LongLat objects which is the coordinates of the W3W address.
     */
    public LongLat getCoordinates(){
        return coordinates;
    }
}