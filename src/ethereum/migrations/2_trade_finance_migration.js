var TradeFinanceContract = artifacts.require("TradeFinanceContract");

module.exports = function(deployer) {
  // deployment steps
  deployer.deploy(TradeFinanceContract);
};