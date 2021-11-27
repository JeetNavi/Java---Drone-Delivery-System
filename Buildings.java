package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.awt.geom.Line2D;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class Buildings {

    private HttpResponse<String> response = null;

    public final String webPort;

    private List <Point> landmarkPoints = new ArrayList<>();
    private List <Polygon> nfzPolygons = new ArrayList<>();
    private List<List<List<Point>>> nfzCornerPoints = new ArrayList<>();

    Buildings(String webPort){

        this.webPort = webPort;

        try {

            System.out.println(webPort); //i checked and this does output 9898

            HttpRequest request = HttpRequest.newBuilder() //HTTP GET request.
                    .uri(URI.create("http://localhost:" + webPort + "/buildings/no-fly-zones.geojson"))
                    .build();
            response = Menus.client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("hi");

        } catch (IllegalArgumentException e){

            System.out.println("IllegalArgumentException - URL syntactically incorrect");
            e.printStackTrace();
            System.exit(1);

        } catch (java.net.ConnectException e){

            System.out.println("Fatal error: unable to connect to localhost at port " + webPort + ".");
            e.printStackTrace();
            System.exit(1);

        }
        catch (Exception e){

            System.out.println("An exception has occurred");
            e.printStackTrace();

        }



        String NfzGeoJson = null;

        assert response != null;
        if(response.statusCode() == Menus.SUCCESSFUL_RESPONSE_CODE){
            NfzGeoJson = response.body();
        }
        else {
            System.out.println("Status code is not 200.");
            System.exit(1);
        }

        FeatureCollection fc = FeatureCollection.fromJson(NfzGeoJson);
        List<Feature> featureObjects = fc.features();
        List <Geometry> geometryObjects = new ArrayList<>();


        assert featureObjects != null;
        for (Feature f : featureObjects) {
            //no fly zones will only consist of polygons
            nfzPolygons.add((Polygon)f.geometry());
        }

        for (Polygon p : nfzPolygons){
            nfzCornerPoints.add(p.coordinates());
        }

        /**
         for (List<List<Point>> p : pointObjects){
         for (List<Point> p2 : p){
         for (Point p3 : p2){
         System.out.println(p3.latitude());
         System.out.println(p3.longitude());
         }
         }
         }
         **/







        try {

            HttpRequest request = HttpRequest.newBuilder() //HTTP GET request.
                    .uri(URI.create("http://localhost:" + webPort + "/buildings/landmarks.geojson"))
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

        String LandmarksGeoJson = null;

        if(response.statusCode() == Menus.SUCCESSFUL_RESPONSE_CODE){
            LandmarksGeoJson = response.body();
        }
        else {
            System.out.println("Status code is not 200.");
            System.exit(1);
        }

        FeatureCollection fc2 = FeatureCollection.fromJson(LandmarksGeoJson);
        List <Feature> featureObjects2 = fc2.features();
        //List <Geometry> geometryObjects = new ArrayList<>();
        //List <List<List<Point>>> pointObjects = new ArrayList<>();


        assert featureObjects2 != null;
        for (Feature f : featureObjects2) {
            //no fly zones will only consist of polygons
            landmarkPoints.add((Point)f.geometry());
            //pointObjects.add(((Polygon)f.geometry()).coordinates());
        }
    }

    public List<Point> getLandmarksPoints(){
        return landmarkPoints;
    }

    public List<Polygon> getNfzPolygons(){
        return nfzPolygons;
    }

    public List<List<List<Point>>> getNfzCornerPoints(){
        return nfzCornerPoints;
    }

    public boolean checkDirectRoute(LongLat start, LongLat destination){
        Line2D lineToDest = new Line2D.Double(start.lng, start.lat, destination.lng, destination.lat);

        boolean directRoute = true;

        for (List<List<Point>> PolygonsCorners : nfzCornerPoints) {
            for (List<Point> PolygonCorners : PolygonsCorners) {
                for (int i = 0; i < PolygonCorners.size() - 1; i++) {
                    Line2D line2 = new Line2D.Double(PolygonCorners.get(i).longitude(), PolygonCorners.get(i).latitude(),
                            PolygonCorners.get(i + 1).longitude(), PolygonCorners.get(i + 1).latitude());
                    if (line2.intersectsLine(lineToDest)) {
                        directRoute = false;
                    }
                }
            }
        }

        return directRoute;

    }


}


