package com.template.contracts;

import com.template.states.OrderState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

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
        final Commands commandData = command.getValue();

        //if (command.getValue() instanceof Commands.Add) {
        if (commandData.equals(new Commands.Add())) {
            //Retrieve the output state of the transaction
            OrderState output = tx.outputsOfType(OrderState.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("No inputs should be consumed when adding a new order.", tx.getInputStates().size() == 0);
                require.using("Only the seller is allowed to call this function", command.getSigners().contains(output.getSeller().getOwningKey()));
                require.using("The price must be greater or equal to the shipping costs.", output.getPrice().compareTo(output.getShippingCosts()) >= 0);
                //require.using("The message must be Hello-World", output.getMsg().equals("Hello-World"));
                return null;
            });
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Add implements Commands {
        }

        class CheckDate implements Commands {
        }

        class Cancel implements Commands {
        }

        class Confirm implements Commands {
        }

        class Sign implements Commands {
        }
    }
}