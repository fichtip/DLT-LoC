pragma solidity ^0.5.16;

contract TradeFinanceContract {
    enum States {
        NONE,
        CREATED,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        CLOSED,
        CANCELLED
    }

    address payable internal seller;

    struct Order {
        States state;
        address payable buyer;
        uint256 orderId;
        uint256 productId;
        uint256 quantity;
        uint256 price;
        string shippingAddress;
        uint256 latestDeliveryDate;
        address payable freightCompany;
        uint256 shippingCosts;
        string trackingCode;
        bool buyerSigned;
        bool freightSigned;
    }

    uint256 orderCount;
    mapping(uint256 => Order) public orders;
    mapping(address => uint256) public balances;

    event Log(uint256 orderId, string text);

    constructor() public {
        seller = msg.sender;
    }

    modifier onlySeller() {
        require(
            msg.sender == seller,
            "Only the seller is allowed to call this function."
        );
        _;
    }

    modifier onlyBuyer(uint256 orderId) {
        require(
            msg.sender == orders[orderId].buyer,
            "Only the buyer is allowed to call this function."
        );
        _;
    }

    modifier onlySellerOrBuyer(uint256 orderId) {
        require(
            msg.sender == seller || msg.sender == orders[orderId].buyer,
            "Only the buyer and seller are allowed to call this function."
        );
        _;
    }

    modifier onlyFreightCompanyOrBuyer(uint256 _orderId) {
        require(
            msg.sender == orders[_orderId].freightCompany ||
                msg.sender == orders[_orderId].buyer,
            "Only the buyer and freight company are allowed to call this function."
        );
        _;
    }

    modifier atState(uint256 _orderId, States _state) {
        require(
            orders[_orderId].state == _state,
            "Function cannot be called at this state."
        );
        _;
    }

    modifier transitionNextState(uint256 _orderId) {
        _;
        nextState(_orderId);
    }

    function nextState(uint256 _orderId) internal {
        orders[_orderId].state = States(uint256(orders[_orderId].state) + 1);
    }

    function getOrderCount() public view returns (uint256) {
        return orderCount;
    }

    function getOrderState(uint256 _orderId) public view returns (States) {
        return orders[_orderId].state;
    }

    function addOrder(
        uint256 _orderId,
        address payable _buyer,
        uint256 _productId,
        uint256 _quantity,
        uint256 _price,
        string memory _shippingAddress,
        uint256 _latestDeliveryDate,
        uint256 _shippingCosts
    )
        public
        onlySeller
        atState(_orderId, States.NONE)
        transitionNextState(_orderId)
    {
        require(
            orders[_orderId].orderId != _orderId,
            "An order with this ID already exists."
        );
        require(
            _price >= _shippingCosts,
            "The price must be greater or equal to the shipping costs."
        );

        orders[_orderId].orderId = _orderId;
        orders[_orderId].buyer = _buyer;
        orders[_orderId].productId = _productId;
        orders[_orderId].quantity = _quantity;
        orders[_orderId].price = _price;
        orders[_orderId].shippingCosts = _shippingCosts;
        orders[_orderId].shippingAddress = _shippingAddress;
        orders[_orderId].latestDeliveryDate = _latestDeliveryDate;
        orderCount++;
    }

    function cancelOrder(uint256 _orderId) public onlySellerOrBuyer(_orderId) {
        require(
            orders[_orderId].state == States.CREATED ||
                orders[_orderId].state == States.CONFIRMED,
            "Function cannot be called at this state."
        );

        if (orders[_orderId].state == States.CONFIRMED) {
            orders[_orderId].state = States.CANCELLED;
            balances[orders[_orderId].buyer] -= orders[_orderId].price;
            orders[_orderId].buyer.transfer(orders[_orderId].price);
        } else {
            orders[_orderId].state = States.CANCELLED;
        }
        emit Log(_orderId, "Order has been cancelled");
    }

    function deliveryDatePassed(uint256 _orderId) public {
        require(
            now >= orders[_orderId].latestDeliveryDate,
            "Delivery date did not pass yet."
        );
        require(
            orders[_orderId].state < States.DELIVERED,
            "Order got already delivered."
        );
        require(
            orders[_orderId].freightSigned == false,
            "Refund not possible as the freight company already signed the arrival."
        );
		
        orders[_orderId].state = States.CANCELLED;
        balances[orders[_orderId].buyer] -= orders[_orderId].price;
        orders[_orderId].buyer.transfer(orders[_orderId].price);
        emit Log(
            _orderId,
            "Order has been cancelled due passed delivery date."
        );
    }

    function confirmOrder(uint256 _orderId)
        public
        payable
        onlyBuyer(_orderId)
        atState(_orderId, States.CREATED)
        transitionNextState(_orderId)
    {
        require(
            orders[_orderId].price <= msg.value,
            "Not enough Ether sent to cover the price of the order."
        );
        balances[orders[_orderId].buyer] += orders[_orderId].price;
        emit Log(_orderId, "Order has been confirmed and money deposited");
    }

    function shipOrder(
        uint256 _orderId,
        address payable _freightCompany,
        string memory _trackingCode
    )
        public
        onlySeller
        atState(_orderId, States.CONFIRMED)
        transitionNextState(_orderId)
    {
        orders[_orderId].freightCompany = _freightCompany;
        orders[_orderId].trackingCode = _trackingCode;
        emit Log(_orderId, "Order has been shipped");
    }

    function signArrival(uint256 _orderId)
        public
        onlyFreightCompanyOrBuyer(_orderId)
        atState(_orderId, States.SHIPPED)
    {
        if (msg.sender == orders[_orderId].buyer) {
            orders[_orderId].buyerSigned = true;
            emit Log(_orderId, "Order arrival has been signed by the buyer");
        }

        if (msg.sender == orders[_orderId].freightCompany) {
            orders[_orderId].freightSigned = true;
            emit Log(
                _orderId,
                "Order arrival has been signed by the freight company"
            );
        }

        if (orders[_orderId].buyerSigned && orders[_orderId].freightSigned) {
            nextState(_orderId);
            emit Log(
                _orderId,
                "Order arrival has been signed by the buyer and freight company"
            );
            payout(_orderId);
        }
    }

    function payout(uint256 _orderId)
        private
        atState(_orderId, States.DELIVERED)
        transitionNextState(_orderId)
    {
        balances[orders[_orderId].buyer] -= orders[_orderId].price;
        balances[seller] =
            balances[seller] +
            orders[_orderId].price -
            orders[_orderId].shippingCosts;
        balances[orders[_orderId].freightCompany] += orders[_orderId]
            .shippingCosts;

        seller.transfer(
            orders[_orderId].price - orders[_orderId].shippingCosts
        );
        orders[_orderId].freightCompany.transfer(
            orders[_orderId].shippingCosts
        );

        emit Log(_orderId, "Payout finished.");
    }
}
