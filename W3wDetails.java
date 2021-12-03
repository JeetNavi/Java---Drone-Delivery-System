package uk.ac.ed.inf;

/**
 * This class represents the JSON record for every what3Words information corresponding to a What3Words address.
 * We are only interested in the coordinates field of the JSON records.
 */
public final class W3wDetails {

    /**
     * This associates a What3Words address with a specific longitude ("lng") and longitude ("lat") also known as a LongLat object.
     */
    public LongLat coordinates;

}
