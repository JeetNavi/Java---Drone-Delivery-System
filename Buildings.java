package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.awt.geom.Line2D;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * This class represents the buildings (excluding shops) which is relevant to us.
 * These buildings include the no-fly-zones (NFZ's) and the landmarks.
 * Once a buildings object is called, it should contain all the information needed about the NFZ's and the landmarks.
 * This means that only one buildings object is ever needed if we are working with orders for one date, since it is
 * expected that information on buildings will not change throughout the day.
 *
 */
public final class Buildings {

    /**
     * We declare the list of NFZ edges here so we can use it in the checkDirectRoute method below.
     * This list contains the edges of every NFZ polygon as a line2D object.
     */
    public static final List<Line2D> nfzEdges = new ArrayList<>();
    /**
     * We declare landmarkPoints here so we can make use of it in the main method.
     * This list contains all the landmarks that we may divert toward at some stage during our deliveries due to NFZ's.
     */
    public static final List<Point> landmarkPoints = new ArrayList<>();

    /**
     * Web port is needed to retrieve the information about the buildings from the web server.
     */
    public final String webPort;

    /**
     * Constructor for class Buildings.
     * Whenever we create a buildings object (only once per day of orders), we:
     * Get the information from the web server about the NFZ's.
     * We get the polygons that are the NFZ's and add them to an arrat as well as the
     * coordinates (corner points) of these NFZ's and create an array to store these in.
     * We also create an array that stores the edges of the NFZ's with respect to their polygons, to make checking
     * for line intersection with each of these edges possible.
     *
     * We also get the information about the landmarks from the web server.
     * We store this information as a list of point object from which we can get its longitude and latitude coordinates.
     *
     * @param webPort The port the server is running on.
     */
    Buildings(String webPort) {

        this.webPort = webPort;

        HttpResponse<String> response = null;
        final List<Polygon> nfzPolygons = new ArrayList<>();
        final List<List<List<Point>>> nfzCornerPoints = new ArrayList<>();

        //Accessing information about the NFZ's from the web server.
        try {

            HttpRequest request = HttpRequest.newBuilder() //HTTP GET request.
                    .uri(URI.create("http://localhost:" + webPort + "/buildings/no-fly-zones.geojson"))
                    .build();
            //We use the same client as the one created in the Menus class to avoid creating many clients.
            response = Menus.client.send(request, HttpResponse.BodyHandlers.ofString());

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

        String NfzGeoJson = null;


        assert response != null;
        if (response.statusCode() == Menus.SUCCESSFUL_RESPONSE_CODE) {
            NfzGeoJson = response.body();
        } else {
            System.err.println("Status code is not 200.");
            System.exit(1);
        }

        FeatureCollection fc = FeatureCollection.fromJson(NfzGeoJson);
        List<Feature> featureObjects = fc.features();

        assert featureObjects != null;
        for (Feature f : featureObjects) {
            //no fly zones will only consist of polygons
            nfzPolygons.add((Polygon) f.geometry());
        }

        for (Polygon p : nfzPolygons) {
            nfzCornerPoints.add(p.coordinates());
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


        //Accessing information about the landmarks from the web server.
        try {

            HttpRequest request = HttpRequest.newBuilder() //HTTP GET request.
                    .uri(URI.create("http://localhost:" + webPort + "/buildings/landmarks.geojson"))
                    .build();
            response = Menus.client.send(request, HttpResponse.BodyHandlers.ofString());

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

        String LandmarksGeoJson = null;

        if (response.statusCode() == Menus.SUCCESSFUL_RESPONSE_CODE) {
            LandmarksGeoJson = response.body();
        } else {
            System.err.println("Status code is not 200.");
            System.exit(1);
        }

        FeatureCollection fc2 = FeatureCollection.fromJson(LandmarksGeoJson);
        List<Feature> featureObjects2 = fc2.features();

        assert featureObjects2 != null;
        for (Feature f : featureObjects2) {
            //Landmarks will consist only of Points.
            landmarkPoints.add((Point) f.geometry());
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
    public final boolean checkDirectRoute(LongLat start, LongLat destination){

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
