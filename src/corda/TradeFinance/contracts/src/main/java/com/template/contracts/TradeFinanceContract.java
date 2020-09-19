package com.template.contracts;

import com.template.states.OrderState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class TradeFinanceContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.template.contracts.TradeFinanceContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {

        /* We can use the requireSingleCommand function to extract command data from a transaction.
         * However, it is possible to have multiple commands in a single transaction.*/
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);

        //Retrieve the input and output states of the transaction
        OrderState input = tx.getInputs().size() != 0 ? tx.inputsOfType(OrderState.class).get(0) : null;
        OrderState output = tx.outputsOfType(OrderState.class).get(0);
        if (command.getValue() instanceof Commands.Create) {
            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("No inputs should be consumed when adding a new order.", tx.getInputStates().size() == 0);
                require.using("Only the seller is allowed to call this function.", command.getValue().getInitiator().getOwningKey().equals(output.getSeller().getOwningKey()));
                require.using("The price must be greater or equal to the shipping costs.", output.getPrice().compareTo(output.getShippingCosts()) >= 0);
                return null;
            });
        } else if (command.getValue() instanceof Commands.Cancel) {
            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Exactly one input should be consumed when cancelling an order.", tx.getInputStates().size() == 1);
                require.using("Function cannot be called at this state: " + input.getOrderState(), Stream.of(input.getOrderState()).anyMatch(Arrays.asList(OrderState.State.CREATED, OrderState.State.CONFIRMED)::contains));
                require.using("Either the seller or the buyer must sign this transaction.", Arrays.asList(output.getSeller().getOwningKey(), output.getBuyer().getOwningKey()).contains(command.getValue().getInitiator().getOwningKey()));
                return null;
            });
        } else if (command.getValue() instanceof Commands.CheckDate) {
            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Exactly one input should be consumed when checking the order delivery date.", tx.getInputStates().size() == 1);
                require.using("Function cannot be called at this state: " + input.getOrderState(), input.getOrderState() != OrderState.State.DELIVERED);
                require.using("Delivery date did not pass yet.", Instant.now().isAfter(input.getLatestDeliveryDate()));
                require.using("Refund not possible as the freight company already signed the arrival.", !input.isFreightSigned());
                require.using("Either the seller, the buyer or the freight company must sign this transaction.", output.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()).contains(command.getValue().getInitiator().getOwningKey()));
                return null;
            });
        } else if (command.getValue() instanceof Commands.Confirm) {
            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Exactly one input should be consumed when confirming an order.", tx.getInputStates().size() == 1);
                require.using("Function cannot be called at this state: " + input.getOrderState(), input.getOrderState() == OrderState.State.CREATED);
                require.using("Only the buyer is allowed to call this function.", command.getValue().getInitiator().getOwningKey().equals(output.getBuyer().getOwningKey()));
                return null;
            });
        } else if (command.getValue() instanceof Commands.Ship) {
            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Exactly one input should be consumed when shipping an order.", tx.getInputStates().size() == 1);
                require.using("Function cannot be called at this state: " + input.getOrderState(), input.getOrderState() == OrderState.State.CONFIRMED);
                require.using("Only the seller is allowed to sign this transaction.", command.getValue().getInitiator().getOwningKey().equals(output.getSeller().getOwningKey()));
                return null;
            });
        } else if (command.getValue() instanceof Commands.Sign) {
            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("Exactly one input should be consumed when signing an order.", tx.getInputStates().size() == 1);
                require.using("Function cannot be called at this state: " + input.getOrderState(), input.getOrderState() == OrderState.State.SHIPPED);
                require.using("Only the buyer and freight company are allowed to sign this transaction.", Arrays.asList(output.getBuyer().getOwningKey(), output.getFreightCompany().getOwningKey()).contains(command.getValue().getInitiator().getOwningKey()));
                return null;
            });
        }
    }

    // Used to indicate the transaction's intent.
    public abstract static class Commands implements CommandData {
        private Party initiator;

        public Commands(Party initiator) {
            this.initiator = initiator;
        }

        public Party getInitiator() {
            return initiator;
        }

        public static class Create extends Commands {
            public Create(Party initiator) {
                super(initiator);
            }
        }

        public static class CheckDate extends Commands {
            public CheckDate(Party initiator) {
                super(initiator);
            }
        }

        public static class Cancel extends Commands {
            public Cancel(Party initiator) {
                super(initiator);
            }
        }

        public static class Confirm extends Commands {
            public Confirm(Party initiator) {
                super(initiator);
            }
        }

        public static class Ship extends Commands {
            public Ship(Party initiator) {
                super(initiator);
            }
        }

        public static class Sign extends Commands {
            public Sign(Party initiator) {
                super(initiator);
            }
        }
    }
}