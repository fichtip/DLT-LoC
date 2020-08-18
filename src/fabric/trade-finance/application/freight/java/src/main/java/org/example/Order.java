package org.example;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.bouncycastle.util.Strings;

public class Order implements java.io.Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -1774134125317583092L;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

    public enum State {
        @SerializedName("0")
        CREATED,

        @SerializedName("1")
        CONFIRMED,

        @SerializedName("2")
        SHIPPED,

        @SerializedName("3")
        DELIVERED,

        @SerializedName("4")
        CANCELLED,

        @SerializedName("5")
        PASSED
    }

    private State state;
    private String orderId;
    private int productId;
    private double quantity;
    private double price;
    private double shippingCosts;
    private String shippingAddress;
    private Date latestDeliveryDate;
    private String trackingCode;
    private boolean buyerSigned;
    private boolean freightSigned;

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return this.productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public double getQuantity() {
        return this.quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getShippingCosts() {
        return this.shippingCosts;
    }

    public void setShippingCosts(double shippingCosts) {
        this.shippingCosts = shippingCosts;
    }

    public String getShippingAddress() {
        return this.shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Date getLatestDeliveryDate() {
        return this.latestDeliveryDate;
    }

    public void setLatestDeliveryDate(Date latestDeliveryDate) {
        this.latestDeliveryDate = latestDeliveryDate;
    }

    public String getTrackingCode() {
        return this.trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public boolean isBuyerSigned() {
        return this.buyerSigned;
    }

    public boolean getBuyerSigned() {
        return this.buyerSigned;
    }

    public void setBuyerSigned(boolean buyerSigned) {
        this.buyerSigned = buyerSigned;
    }

    public boolean isFreightSigned() {
        return this.freightSigned;
    }

    public boolean getFreightSigned() {
        return this.freightSigned;
    }

    public void setFreightSigned(boolean freightSigned) {
        this.freightSigned = freightSigned;
    }

    /**
     * Deserialize a string to order object
     *
     * @param data data to form back into the object
     */
    public static Order deserialize(String data) {
        return Order.gson.fromJson(data, Order.class);
    }

    public static Order deserialize(byte[] data) {
        return Order.gson.fromJson(Strings.fromByteArray(data), Order.class);
    }

    /**
     * Serialize an order object to string
     *
     * @param order data to form back into the object
     */
    public static String serialize(Order order) {
        return Order.gson.toJson(order);
    }

    @Override
    public String toString() {
        return "{" + " state='" + getState() + "'" + ", orderId='" + getOrderId() + "'" + ", productId='"
                + getProductId() + "'" + ", quantity='" + getQuantity() + "'" + ", price='" + getPrice() + "'"
                + ", shippingCosts='" + getShippingCosts() + "'" + ", shippingAddress='" + getShippingAddress() + "'"
                + ", latestDeliveryDate='" + sdf.format(getLatestDeliveryDate()) + "'" + ", trackingCode='"
                + getTrackingCode() + "'" + ", buyerSigned='" + isBuyerSigned() + "'" + ", freightSigned='"
                + isFreightSigned() + "'" + "}";
    }

}
