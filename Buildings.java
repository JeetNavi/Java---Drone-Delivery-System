package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.awt.geom.Line2D;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class Buildings {

    public static HttpResponse<String> response = null;
    public static final List<Point> landmarkPoints = new ArrayList<>();
    public static final List <Polygon> nfzPolygons = new ArrayList<>();
    public static final List<List<List<Point>>> nfzCornerPoints = new ArrayList<>();
    public static final List<Line2D> nfzEdges = new ArrayList<>();

    public final String webPort;


    Buildings(String webPort){

        this.webPort = webPort;

        //Accessing information about the NFZ's from the web server.
        try {

            HttpRequest request = HttpRequest.newBuilder() //HTTP GET request.
                    .uri(URI.create("http://localhost:" + webPort + "/buildings/no-fly-zones.geojson"))
                    .build();
            //We use the same client as the one created in the Menus class to avoid creating many clients.
            response = Menus.client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (IllegalArgumentException e){

            System.err.println("IllegalArgumentException - URL syntactically incorrect");
            e.printStackTrace();
            System.exit(1);

        } catch (java.net.ConnectException e){

            System.err.println("Fatal error: unable to connect to localhost at port " + webPort + ".");
            e.printStackTrace();
            System.exit(1);

        }
        catch (Exception e){

            System.err.println("An exception has occurred");
            e.printStackTrace();

        }

        String NfzGeoJson = null;


        assert response != null;
        if(response.statusCode() == Menus.SUCCESSFUL_RESPONSE_CODE){
            NfzGeoJson = response.body();
        }
        else {
            System.err.println("Status code is not 200.");
            System.exit(1);
        }

        FeatureCollection fc = FeatureCollection.fromJson(NfzGeoJson);
        List<Feature> featureObjects = fc.features();

        assert featureObjects != null;
        for (Feature f : featureObjects) {
            //no fly zones will only consist of polygons
            nfzPolygons.add((Polygon)f.geometry());
        }

        for (Polygon p : nfzPolygons){
            nfzCornerPoints.add(p.coordinates());
        }



        //Accessing information about the landmarks from the web server.
        try {

            HttpRequest request = HttpRequest.newBuilder() //HTTP GET request.
                    .uri(URI.create("http://localhost:" + webPort + "/buildings/landmarks.geojson"))
                    .build();
            response = Menus.client.send(request, HttpResponse.BodyHandlers.ofString());

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

        String LandmarksGeoJson = null;

        if(response.statusCode() == Menus.SUCCESSFUL_RESPONSE_CODE){
            LandmarksGeoJson = response.body();
        }
        else {
            System.err.println("Status code is not 200.");
            System.exit(1);
        }

        FeatureCollection fc2 = FeatureCollection.fromJson(LandmarksGeoJson);
        List <Feature> featureObjects2 = fc2.features();

        assert featureObjects2 != null;
        for (Feature f : featureObjects2) {
            //Landmarks will consist only of Points.
            landmarkPoints.add((Point)f.geometry());
        }

        //Populating the list of NFZ edges which helps for checking line intersection.
        for (List<List<Point>> PolygonsCorners : nfzCornerPoints) {
            for (List<Point> PolygonCorners : PolygonsCorners) {
                for (int i = 0; i < PolygonCorners.size() - 1; i++) {
                    nfzEdges.add(new Line2D.Double(PolygonCorners.get(i).longitude(), PolygonCorners.get(i).latitude(),
                            PolygonCorners.get(i + 1).longitude(), PolygonCorners.get(i + 1).latitude()));
                }
            }
        }
    }


    /**
     *
     * Method that checks whether a direct route (straight line) between the positions of two Longlat objects intersect a NFZ.
     * It does the required checks by iterating through every edge (with respect to its polygon) of each polygon
     * of the NFZ's and checking whether the direct route intersects at any point with an edge in NFZ.
     *
     * @param start Starting LongLat object from which we would like to travel from.
     * @param destination Ending LongLat object to which we would like to travel to.
     * @return Boolean value true if there exists a route such that it is a straight line from start to destination
     * that does not intersect with any NFZ.
     * Note, the direct route may have an angle of travel that is not a multiple of 10, and thus when calculating this
     * angle and rounding to 10, we may make an unexpected visit through a NFZ; this is handled in the LongLat class
     * with the angleToDodgePotentialNfz method.
     */
    public boolean checkDirectRoute(LongLat start, LongLat destination){

        Line2D lineToDest = new Line2D.Double(start.lng, start.lat, destination.lng, destination.lat);
        boolean directRoute = true;

        for (Line2D edge : nfzEdges){
            if (edge.intersectsLine(lineToDest)){
                directRoute = false;
                break;
            }
        }
        return directRoute;
    }

}
