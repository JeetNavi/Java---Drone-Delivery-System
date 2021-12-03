package uk.ac.ed.inf;

import java.util.List;

/**
 * This class represents the JSON record for a shop.
 */
public final class ShopDetails {

    /**
     * The name of the shop
     */
    public String name;
    /**
     * The location of the shop as a WhatThreeWords address.
     */
    public String location;
    /**
     * The items on sale in the shop, together with their price in pence.
     * We use an ItemDetails object to represent this.
     */
    public List<ItemDetails> menu;

}
