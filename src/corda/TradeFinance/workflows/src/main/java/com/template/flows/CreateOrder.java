package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.TradeFinanceContract;
import com.template.states.OrderState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

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
public class CreateOrder extends FlowLogic<String> {
    private final ProgressTracker progressTracker = tracker();

    private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating a CreateOrder transaction");
    private static final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
    private static final ProgressTracker.Step COLLECTING_SIGNATURES = new ProgressTracker.Step("Collecting the signatures of the other parties.");
    private static final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Recording transaction") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    private static ProgressTracker tracker() {
        return new ProgressTracker(
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                COLLECTING_SIGNATURES,
                FINALISING_TRANSACTION
        );
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    //private variables
    private Party seller;
    private String buyer;
    private String orderId;
    private int productId;
    private double quantity;
    private Amount<Currency> price;
    private Amount<Currency> shippingCosts;
    private String shippingAddress;
    private Instant latestDeliveryDate;

    //public constructor
    public CreateOrder(String buyer, String orderId, int productId, double quantity, String price, String shippingCosts, String shippingAddress, String latestDeliveryDate) {
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

        // Step 0. Check if an order with this ID already exists
        QueryCriteria.LinearStateQueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria().withExternalId(Collections.singletonList(this.orderId));
        List<StateAndRef<OrderState>> results = getServiceHub().getVaultService().queryBy(OrderState.class, queryCriteria).getStates();
        if (results.size() != 0) {
            throw new IllegalArgumentException("An order with ID " + this.orderId + " already exists.");
        }

        // Step 1. Get a reference to the notary service on our network and our key pair.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Step 2. Compose the State that carries the order data
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        Party buyerParty = getServiceHub().getIdentityService().partiesFromName(this.buyer, true).stream().findFirst().get();
        final OrderState output = new OrderState(this.seller, buyerParty, this.orderId, this.productId, this.quantity, this.price, this.shippingCosts, this.shippingAddress, this.latestDeliveryDate);

        // Step 3. Create a new TransactionBuilder object.
        final TransactionBuilder builder = new TransactionBuilder(notary);

        // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
        builder.addOutputState(output, TradeFinanceContract.ID);
        builder.addCommand(new TradeFinanceContract.Commands.Create(getOurIdentity()), output.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        // Step 5. Verify and sign it with our KeyPair.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        builder.verify(getServiceHub());
        final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        progressTracker.setCurrentStep(COLLECTING_SIGNATURES);
        List<Party> otherParties = output.getParticipants().stream().map(el -> (Party) el).collect(Collectors.toList());
        otherParties.remove(getOurIdentity());
        List<FlowSession> sessions = otherParties.stream().map(this::initiateFlow).collect(Collectors.toList());

        SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

        // Step 7. Assuming no exceptions, we can now finalise the transaction
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        subFlow(new FinalityFlow(stx, sessions));

        return "Order with ID '" + this.orderId + "' of buyer '" + buyerParty.getName() + "' added.";
    }
}
