package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

// ******************
// * Responder flow *
// ******************
@InitiatedBy(SignArrival.class)
public class SignArrivalResponder extends FlowLogic<Void> {

    //private variable
    private FlowSession counterpartySession;

    //Constructor
    public SignArrivalResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
            @Suspendable
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                /*
                 * SignTransactionFlow will automatically verify the transaction and its signatures before signing it.
                 * However, just because a transaction is contractually valid doesn’t mean we necessarily want to sign.
                 * What if we don’t want to deal with the counterparty in question, or the value is too high,
                 * or we’re not happy with the transaction’s structure? checkTransaction
                 * allows us to define these additional checks. If any of these conditions are not met,
                 * we will not sign the transaction - even if the transaction and its signatures are contractually valid.
                 * ----------
                 * For this cordapp, we will not implement any additional checks.
                 * */
            }
        });
        //Stored the transaction into data base.
        subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
        return null;
    }
}
