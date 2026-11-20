package com.compare.playwright.data;

/** Catalog items used by the tests (ids and prices match the demo app). */
public record Product(int id, String name, int priceCents) {

    public static final Product BACKPACK = new Product(1, "Trail Backpack", 2999);
    public static final Product BIKE_LIGHT = new Product(2, "Clip Bike Light", 999);
    public static final Product T_SHIRT = new Product(3, "Cotton T-Shirt", 1599);
    public static final Product JACKET = new Product(4, "Fleece Jacket", 4999);
    public static final Product CAMP_MUG = new Product(8, "Camp Mug", 699);

    public static final int CATALOG_SIZE = 12;

    public String price() {
        return String.format("$%d.%02d", priceCents / 100, priceCents % 100);
    }
}
