const TradeFinanceContract = artifacts.require("TradeFinanceContract");

contract("TradeFinanceContract", accounts => {

    it("getOrderCount init 0", () =>
        TradeFinanceContract.deployed()
            .then(instance => instance.getOrderCount.call())
            .then(orderCount => {
                assert.equal(
                    orderCount.valueOf(),
                    0,
                    "the order count after init was not 0"
                );
            }));

});