package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.TradeFinanceContract;
import com.template.states.OrderState;
import com.template.utils.DataUtils;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;
import java.util.stream.Collectors;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class CancelOrder extends FlowLogic<String> {
    private final ProgressTracker progressTracker = tracker();

    private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating a CancelOrder transaction");
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
    private final String orderId;

    //public constructor
    public CancelOrder(String orderId) {
        this.orderId = orderId;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        // Step 1. Get the order data from the vault
        StateAndRef<OrderState> inputOrderStateAndRef = DataUtils.getOrder(getServiceHub(), this.orderId);
        OrderState inputOrderState = inputOrderStateAndRef.getState().getData();

        // Generate State for transfer
        // Step 2. Get a reference to the notary service on our network and our key pair.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Step 3. Compose the State that carries the order data
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        OrderState outputOrderState = inputOrderState.copy();
        outputOrderState.setOrderState(OrderState.State.CANCELLED);

        // Step 4. Create a new TransactionBuilder object.
        final TransactionBuilder builder = new TransactionBuilder(notary);

        // Step 5. Add the order as an output state, as well as a command to the transaction builder.
        builder.addInputState(inputOrderStateAndRef);
        builder.addOutputState(outputOrderState);
        builder.addCommand(new TradeFinanceContract.Commands.Cancel(getOurIdentity()), outputOrderState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        // Step 6. Verify and sign it with our KeyPair.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        builder.verify(getServiceHub());
        final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

        // Step 7. Collect the other party's signature using the SignTransactionFlow.
        progressTracker.setCurrentStep(COLLECTING_SIGNATURES);
        List<Party> otherParties = outputOrderState.getParticipants().stream().map(el -> (Party) el).collect(Collectors.toList());
        otherParties.remove(getOurIdentity());
        List<FlowSession> sessions = otherParties.stream().map(this::initiateFlow).collect(Collectors.toList());

        SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

        // Step 8. Assuming no exceptions, we can now finalise the transaction
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        subFlow(new FinalityFlow(stx, sessions));

        return "Cancel flow for order with ID '" + this.orderId + "' of buyer '" + outputOrderState.getBuyer().getName() + "' executed.";
    }
}
