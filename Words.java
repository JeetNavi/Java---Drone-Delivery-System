package uk.ac.ed.inf;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Words {

    private HttpResponse<String> response = null;

    public final String webPort;

    Words(String webPort, String[] threeWords) {
            this.webPort = webPort;

            String wordOne = threeWords[0];
            String wordTwo = threeWords[1];
            String wordThree = threeWords[2];

        try {

            HttpRequest request = HttpRequest.newBuilder() //HTTP GET request.
                    .uri(URI.create("http://localhost:" + webPort + "/words/" + wordOne + "/" + wordTwo + "/" + wordThree + "/details.json"))
                    .build();
            response = Menus.client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (IllegalArgumentException e){

            System.out.println("IllegalArgumentException - URL syntactically incorrect");
            e.printStackTrace();
            System.exit(1);

        } catch (java.net.ConnectException e){

            System.out.println("Fatal error: unable to connect to localhost at port " + webPort + ".");
            e.printStackTrace();
            System.exit(1);

        } catch (Exception e){

            System.out.println("An exception has occurred");
            e.printStackTrace();

        }
    }

    public LongLat getCoords(){
        assert response != null;

        w3wDetails details = null;


        if(response.statusCode() == Menus.SUCCESSFUL_RESPONSE_CODE){
            details = new Gson().fromJson(response.body(), w3wDetails.class);
        }
        else {
            System.out.println("Status code is not 200.");
            System.exit(1);
        }

        return details.coordinates;
    }
}