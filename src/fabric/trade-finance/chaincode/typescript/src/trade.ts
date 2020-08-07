/*
 * SPDX-License-Identifier: Apache-2.0
 */

import { Context, Contract } from "fabric-contract-api";
import { Order, State } from "./order";

export class TradeFinance extends Contract {

    public async queryOrder(ctx: Context, _orderId: string): Promise<string> {
        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (!orderAsBytes || orderAsBytes.length === 0) {
            throw new Error("${_orderId} does not exist");
        }
        console.log(orderAsBytes.toString());
        return orderAsBytes.toString();
    }

    public async queryAllOrders(ctx: Context): Promise<string> {
        const startKey = "";
        const endKey = "";
        const allResults = [];
        for await (const { key, value } of ctx.stub.getStateByRange(startKey, endKey)) {
            const strValue = Buffer.from(value).toString("utf8");
            let record;
            try {
                record = JSON.parse(strValue);
            } catch (err) {
                console.log(err);
                record = strValue;
            }
            allResults.push({ Key: key, Record: record });
        }
        console.info(allResults);
        return JSON.stringify(allResults);
    }

    public async createOrder(ctx: Context,
        _orderId: string,
        _productId: number,
        _quantity: number,
        _price: number,
        _shippingAddress: string,
        _latestDeliveryDate: string,
        _shippingCosts: number) {
        console.info("============= START : Create Order ===========");

        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (orderAsBytes || orderAsBytes.length > 0) {
            throw new Error("An order with ID ${_orderId} does already exist");
        }

        if (_price < _shippingCosts) {
            throw new Error("The price must be greater or equal to the shipping costs.");
        }

        var splittedDate = _latestDeliveryDate.split("-"); // date given in yyyy-mm-dd format
        var parsedDate = new Date(parseInt(splittedDate[0]), parseInt(splittedDate[1]) - 1, parseInt(splittedDate[2]));

        const order: Order = {
            docType: "order",
            state: State.CREATED,
            orderId: _orderId,
            productId: _productId,
            quantity: _quantity,
            price: _price,
            shippingAddress: _shippingAddress,
            latestDeliveryDate: parsedDate,
            shippingCosts: _shippingCosts,
            trackingCode: undefined,
            buyerSigned: undefined,
            freightSigned: undefined
        };

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("============= END : Create Order ===========");
    }

    public async cancelOrder(ctx: Context, _orderId: string) {
        console.info("============= START : cancelOrder ===========");

        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (!orderAsBytes || orderAsBytes.length === 0) {
            throw new Error("An order with ID ${_orderId} does already exist");
        }
        const order: Order = JSON.parse(orderAsBytes.toString());

        if (order.state == State.DELIVERED || order.state == State.SHIPPED || order.state == State.CANCELLED || order.state == State.PASSED) {
            throw new Error("The state of order ${_orderId} does not allow this action");
        }

        order.state = State.CANCELLED;

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("============= END : cancelOrder ===========");
    }

    public async deliveryDatePassed(ctx: Context, _orderId: string) {
        console.info("============= START : deliveryDatePassed ===========");

        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (!orderAsBytes || orderAsBytes.length === 0) {
            throw new Error("An order with ID ${_orderId} does already exist");
        }
        const order: Order = JSON.parse(orderAsBytes.toString());

        if (order.state >= State.DELIVERED) {
            throw new Error("The state of order ${_orderId} does not allow this action");
        }

        var currentDate = new Date();
        if (currentDate > order.latestDeliveryDate) {
            order.state = State.PASSED;
        } else {
            throw new Error("Delivery date did not pass yet.");
        }

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("============= END : deliveryDatePassed ===========");
    }

    public async confirmOrder(ctx: Context, _orderId: string) {
        console.info("============= START : confirmOrder ===========");

        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (!orderAsBytes || orderAsBytes.length === 0) {
            throw new Error("An order with ID ${_orderId} does already exist");
        }
        const order: Order = JSON.parse(orderAsBytes.toString());

        if (order.state != State.CREATED) {
            throw new Error("The state of order ${_orderId} does not allow this action");
        }

        order.state = State.CONFIRMED;

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("============= END : confirmOrder ===========");
    }

    public async shipOrder(ctx: Context, _orderId: string, _trackingCode: string) {
        console.info("============= START : shipOrder ===========");

        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (!orderAsBytes || orderAsBytes.length === 0) {
            throw new Error("An order with ID ${_orderId} does already exist");
        }
        const order: Order = JSON.parse(orderAsBytes.toString());

        if (order.state != State.CONFIRMED) {
            throw new Error("The state of order ${_orderId} does not allow this action");
        }

        order.state = State.CONFIRMED;
        order.trackingCode = _trackingCode;

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("============= END : shipOrder ===========");
    }

    public async signArrivalBuyer(ctx: Context, _orderId: string) {
        console.info("============= START : signArrivalBuyer ===========");

        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (!orderAsBytes || orderAsBytes.length === 0) {
            throw new Error("An order with ID ${_orderId} does already exist");
        }
        const order: Order = JSON.parse(orderAsBytes.toString());

        if (order.state != State.SHIPPED) {
            throw new Error("The state of order ${_orderId} does not allow this action");
        }

        order.buyerSigned = true;
        if (order.buyerSigned && order.freightSigned) {
            order.state = State.DELIVERED;
        }

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("============= END : signArrivalBuyer ===========");
    }

    public async signArrivalFreight(ctx: Context, _orderId: string) {
        console.info("============= START : signArrivalFreight ===========");

        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (!orderAsBytes || orderAsBytes.length === 0) {
            throw new Error("An order with ID ${_orderId} does already exist");
        }
        const order: Order = JSON.parse(orderAsBytes.toString());

        if (order.state != State.SHIPPED) {
            throw new Error("The state of order ${_orderId} does not allow this action");
        }

        order.freightSigned = true;
        if (order.buyerSigned && order.freightSigned) {
            order.state = State.DELIVERED;
        }

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("============= END : signArrivalFreight ===========");
    }
}
