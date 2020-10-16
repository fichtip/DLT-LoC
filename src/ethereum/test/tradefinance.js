const TradeFinanceContract = artifacts.require("TradeFinanceContract");

contract("TradeFinanceContract", accounts => {
    let seller = accounts[0];
    let buyer = accounts[1];
    let freightCompany = accounts[2];

    it("check test environment", () => {
        TradeFinanceContract.deployed()
            .then(instance => instance.getOrderCount())
            .then(orderCount => {
                assert.equal(
                    orderCount.toNumber(),
                    0,
                    "the order count after adding an order was not 0"
                );
            });
    });

    it("create order test", () => {
        let instance;

        return TradeFinanceContract.deployed()
            .then(inst => {
                instance = inst;
                return instance.addOrder(1, buyer, 100, 2, web3.utils.toWei("10", "ether"), "Karlsplatz 13, 1040 Wien", 1594771200, web3.utils.toWei("2", "ether"), { from: seller });
            })
            .then(() => instance.getOrderCount())
            .then(orderCount => {
                assert.equal(
                    orderCount.toNumber(),
                    1,
                    "the order count after adding an order was not 1"
                );
            })
            .then(() => instance.getOrderState(1))
            .then(orderState => {
                assert.equal(
                    orderState.toNumber(),
                    1,
                    "the order state after adding was not CREATED (1)."
                );
            })
    });

    it("confirm order test", () => {
        let instance;

        return TradeFinanceContract.deployed()
            .then(inst => {
                instance = inst;
                return instance.addOrder(2, buyer, 100, 2, web3.utils.toWei("10", "ether"), "Karlsplatz 13, 1040 Wien", 1594771200, web3.utils.toWei("2", "ether"), { from: seller });
            })
            .then(() => instance.getOrderCount())
            .then(orderCount => {
                assert.equal(
                    orderCount.toNumber(),
                    2,
                    "the order count after adding an order was not 1"
                );
            })
            .then(() => instance.confirmOrder(2, { from: buyer, value: web3.utils.toWei("10", "ether") }))
            .then(() => instance.getOrderState(2))
            .then(orderState => {
                assert.equal(
                    orderState.toNumber(),
                    2,
                    "the order state after confirming was not CONFIRMED (2)."
                );
            })
    });

    it("sign arrival test", () => {
        let instance;

        return TradeFinanceContract.deployed()
            .then(inst => {
                instance = inst;
                return instance.addOrder(3, buyer, 123587, 5.0, web3.utils.toWei("15", "ether"), "Ballhausplatz 2, 1010 Wien", 1594771200, web3.utils.toWei("3", "ether"), { from: seller });
            })
            .then(() => instance.confirmOrder(3, { from: buyer, value: web3.utils.toWei("15", "ether") }))
            .then(() => instance.shipOrder(3, freightCompany, "1AXCAW311", { from: seller }))
            .then(() => instance.signArrival(3, { from: buyer }))
            .then(() => instance.signArrival(3, { from: freightCompany }))
            .then(() => instance.getOrderState(3))
            .then(orderState => {
                assert.equal(
                    orderState.toNumber(),
                    5,
                    "the order state after confirming was not CLOSED (5)."
                );
            })
    });

    it("delivery date passed test", () => {
        let instance;

        return TradeFinanceContract.deployed()
            .then(inst => {
                instance = inst;
                return instance.addOrder(4, buyer, 123587, 5.0, web3.utils.toWei("15", "ether"), "Ballhausplatz 2, 1010 Wien", 1594771200, web3.utils.toWei("3", "ether"), { from: seller });
            })
            .then(() => instance.confirmOrder(4, { from: buyer, value: web3.utils.toWei("15", "ether") }))
            .then(() => instance.shipOrder(4, freightCompany, "1AXCAW311", { from: seller }))
            .then(() => instance.deliveryDatePassed(4, { from: buyer }))
            .then(() => instance.getOrderState(4))
            .then(orderState => {
                assert.equal(
                    orderState.toNumber(),
                    7,
                    "the order state after confirming was not PASSED (7)."
                );
            })
    });

});