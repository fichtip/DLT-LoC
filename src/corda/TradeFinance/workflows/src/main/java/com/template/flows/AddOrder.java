package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.TradeFinanceContract;
import com.template.states.OrderState;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class AddOrder extends FlowLogic<String> {

    //private variables
    private Party seller;
    private String buyer;
    private int orderId;
    private int productId;
    private double quantity;
    private Amount<Currency> price;
    private Amount<Currency> shippingCosts;
    private String shippingAddress;
    private Instant latestDeliveryDate;

    //public constructor
    public AddOrder(String buyer, int orderId, int productId, double quantity, String price, String shippingCosts, String shippingAddress, String latestDeliveryDate) {
        this.buyer = buyer;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = Amount.parseCurrency(price);
        this.shippingCosts = Amount.parseCurrency(shippingCosts);
        this.shippingAddress = shippingAddress;
        this.latestDeliveryDate = LocalDate.parse(latestDeliveryDate).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        this.seller = getOurIdentity();

        // Step 1. Get a reference to the notary service on our network and our key pair.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Step 2. Compose the State that carries the order data
        Party buyerParty = getServiceHub().getIdentityService().partiesFromName(this.buyer, true).stream().findFirst().get();
        final OrderState output = new OrderState(this.seller, OrderState.State.CREATED, buyerParty, this.orderId, this.productId, this.quantity, this.price, this.shippingCosts, this.shippingAddress, this.latestDeliveryDate);

        // Step 3. Create a new TransactionBuilder object.
        final TransactionBuilder builder = new TransactionBuilder(notary);

        // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
        builder.addOutputState(output);
        builder.addCommand(new TradeFinanceContract.Commands.Add(), Arrays.asList(this.seller.getOwningKey(), buyerParty.getOwningKey()));

        // Step 5. Verify and sign it with our KeyPair.
        builder.verify(getServiceHub());
        final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        List<Party> otherParties = output.getParticipants().stream().map(el -> (Party) el).collect(Collectors.toList());
        otherParties.remove(getOurIdentity());
        List<FlowSession> sessions = otherParties.stream().map(this::initiateFlow).collect(Collectors.toList());

        SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

        // Step 7. Assuming no exceptions, we can now finalise the transaction
        subFlow(new FinalityFlow(stx, sessions));

        return "Order with ID '" + this.orderId + "' of user '" + buyerParty.getName() + "' added.";
    }
}
