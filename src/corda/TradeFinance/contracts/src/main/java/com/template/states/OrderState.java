package com.template.states;

import com.template.contracts.TradeFinanceContract;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// *********
// * State *
// *********
@BelongsToContract(TradeFinanceContract.class)
public class OrderState implements ContractState {

    @CordaSerializable
    public enum State {
        CREATED,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        PASSED
    }

    //private variables
    private Party seller;
    private State orderState;
    private Party buyer;
    private int orderId;
    private int productId;
    private double quantity;
    private Amount<Currency> price;
    private Amount<Currency> shippingCosts;
    private String shippingAddress;
    private Instant latestDeliveryDate;
    private Party freightCompany;
    private String trackingCode;
    private boolean buyerSigned;
    private boolean freightSigned;

    /* Constructor of your Corda state */
    @ConstructorForDeserialization
    public OrderState(Party seller, State orderState, Party buyer, int orderId, int productId, double quantity, Amount<Currency> price, Amount<Currency> shippingCosts, String shippingAddress, Instant latestDeliveryDate, Party freightCompany, String trackingCode, boolean buyerSigned, boolean freightSigned) {
        this.seller = seller;
        this.orderState = orderState;
        this.buyer = buyer;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.shippingCosts = shippingCosts;
        this.shippingAddress = shippingAddress;
        this.latestDeliveryDate = latestDeliveryDate;
        this.freightCompany = freightCompany;
        this.trackingCode = trackingCode;
        this.buyerSigned = buyerSigned;
        this.freightSigned = freightSigned;
    }

    public OrderState(Party seller, State orderState, Party buyer, int orderId, int productId, double quantity, Amount<Currency> price, Amount<Currency> shippingCosts, String shippingAddress, Instant latestDeliveryDate) {
        this.buyer = buyer;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.shippingCosts = shippingCosts;
        this.shippingAddress = shippingAddress;
        this.latestDeliveryDate = latestDeliveryDate;
    }

    //getters
    public Party getSeller() {
        return seller;
    }

    public State getOrderState() {
        return orderState;
    }

    public void setOrderState(State orderState) {
        this.orderState = orderState;
    }

    public Party getBuyer() {
        return buyer;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getProductId() {
        return productId;
    }

    public double getQuantity() {
        return quantity;
    }

    public Amount<Currency> getPrice() {
        return price;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public Instant getLatestDeliveryDate() {
        return latestDeliveryDate;
    }

    public Party getFreightCompany() {
        return freightCompany;
    }

    public void setFreightCompany(Party freightCompany) {
        this.freightCompany = freightCompany;
    }

    public Amount<Currency> getShippingCosts() {
        return shippingCosts;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public boolean isBuyerSigned() {
        return buyerSigned;
    }

    public void setBuyerSigned(boolean buyerSigned) {
        this.buyerSigned = buyerSigned;
    }

    public boolean isFreightSigned() {
        return freightSigned;
    }

    public void setFreightSigned(boolean freightSigned) {
        this.freightSigned = freightSigned;
    }

    public OrderState copy() {
        return new OrderState(this.seller, this.orderState, this.buyer, this.orderId, this.productId, this.quantity, this.price, this.shippingCosts, this.shippingAddress, this.latestDeliveryDate, this.freightCompany, this.trackingCode, this.buyerSigned, this.freightSigned);
    }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    public List<AbstractParty> getParticipants() {
        if (this.orderState == State.CREATED || this.orderState == State.CONFIRMED) {
            return Stream.of(seller, buyer).filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            return Stream.of(seller, buyer, freightCompany).filter(Objects::nonNull).collect(Collectors.toList());
        }
    }
}