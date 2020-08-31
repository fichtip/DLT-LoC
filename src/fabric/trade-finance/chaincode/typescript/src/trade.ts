/*
 * SPDX-License-Identifier: Apache-2.0
 */

import { Context, Contract } from "fabric-contract-api";
import { Order, State } from "./order";

export class TradeFinance extends Contract {

    private restrictedCall(ctx: Context, allowedAffiliation: string) {
        if (!ctx.clientIdentity.assertAttributeValue("hf.Affiliation", allowedAffiliation)) {
            throw new Error("Only users with affiliation " + allowedAffiliation + " are allowed to call this function");
        }
    }

    private restrictedCall2(ctx: Context, allowedAffiliation1: string, allowedAffiliation2: string) {
        if (!ctx.clientIdentity.assertAttributeValue("hf.Affiliation", allowedAffiliation1) && !ctx.clientIdentity.assertAttributeValue("hf.Affiliation", allowedAffiliation2)) {
            throw new Error("Only users with affiliation " + allowedAffiliation1 + " or " + allowedAffiliation2 + " are allowed to call this function.");
        }
    }

    private async getOrder(ctx: Context, _orderId: string): Promise<Order> {
        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (orderAsBytes.length === 0) {
            throw new Error("An order with ID " + _orderId + " does not exist");
        }
        const order: Order = JSON.parse(orderAsBytes.toString());
        return order;
    }

    public async queryOrder(ctx: Context, _orderId: string): Promise<string> {
        const order = await this.getOrder(ctx, _orderId);
        //console.log(order.toString());
        return JSON.stringify(order);
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
        //console.info(allResults);
        return JSON.stringify(allResults);
    }

    public async createOrder(ctx: Context,
        _orderId: string,
        _productId: number,
        _quantity: number,
        _price: number,
        _shippingCosts: number,
        _shippingAddress: string,
        _latestDeliveryDate: string) {
        console.info("============= START : Create Order ===========");

        this.restrictedCall(ctx, "seller");
        const orderAsBytes = await ctx.stub.getState(_orderId);
        if (orderAsBytes.length > 0) {
            throw new Error("An order with ID " + _orderId + " does already exist");
        }

        _productId = Number(_productId);
        _quantity = Number(_quantity);
        _price = Number(_price);
        _shippingCosts = Number(_shippingCosts);

        if (_price < _shippingCosts) {
            throw new Error("The price must be greater or equal to the shipping costs.");
        }

        var splittedDate = _latestDeliveryDate.split("-"); // date given in yyyy-mm-dd format
        var parsedDate = new Date(parseInt(splittedDate[0]), parseInt(splittedDate[1]) - 1, parseInt(splittedDate[2]));
        //console.info("parsedDate:" + parsedDate.toLocaleString());

        const order: Order = {
            docType: "order",
            state: State.CREATED,
            orderId: _orderId,
            productId: _productId,
            quantity: _quantity,
            price: _price,
            shippingCosts: _shippingCosts,
            shippingAddress: _shippingAddress,
            latestDeliveryDate: parsedDate,
            trackingCode: undefined,
            buyerSigned: undefined,
            freightSigned: undefined
        };

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("============= END : Create Order ===========");
    }

    public async cancelOrder(ctx: Context, _orderId: string) {
        console.info("============= START : cancelOrder ===========");

        this.restrictedCall2(ctx, "seller", "buyer");
        const order = await this.getOrder(ctx, _orderId);

        if (order.state == State.DELIVERED || order.state == State.SHIPPED || order.state == State.CANCELLED || order.state == State.PASSED) {
            throw new Error("The state of order " + _orderId + "  does not allow this action");
        }

        order.state = State.CANCELLED;

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("Order " + _orderId + " has been cancelled.");
        console.info("============= END : cancelOrder ===========");
    }

    public async deliveryDatePassed(ctx: Context, _orderId: string): Promise<boolean> {
        console.info("============= START : deliveryDatePassed ===========");
        var passed = false;

        const order = await this.getOrder(ctx, _orderId);

        if (order.state >= State.DELIVERED) {
            throw new Error("The state of order " + _orderId + "  does not allow this action");
        }

        var currentDate = new Date();
        if (currentDate > new Date(order.latestDeliveryDate)) {
            order.state = State.PASSED;
            await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
            passed = true;
            console.info("Order " + _orderId + " has been cancelled due passed delivery date.");
        }

        console.info("============= END : deliveryDatePassed ===========");
        return passed;
    }

    public async confirmOrder(ctx: Context, _orderId: string) {
        console.info("============= START : confirmOrder ===========");

        this.restrictedCall(ctx, "buyer");
        const order = await this.getOrder(ctx, _orderId);

        if (order.state != State.CREATED) {
            throw new Error("The state of order " + _orderId + "  does not allow this action");
        }

        order.state = State.CONFIRMED;

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("Order " + _orderId + " has been confirmed.");
        console.info("============= END : confirmOrder ===========");
    }

    public async shipOrder(ctx: Context, _orderId: string, _trackingCode: string) {
        console.info("============= START : shipOrder ===========");

        this.restrictedCall(ctx, "seller");
        const order = await this.getOrder(ctx, _orderId);

        if (order.state != State.CONFIRMED) {
            throw new Error("The state of order " + _orderId + " does not allow this action");
        }

        order.state = State.SHIPPED;
        order.trackingCode = _trackingCode;

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("Order " + _orderId + " has been shipped.");
        console.info("============= END : shipOrder ===========");
    }

    public async signArrival(ctx: Context, _orderId: string) {
        console.info("============= START : signArrival ===========");

        this.restrictedCall2(ctx, "freight", "buyer");
        const order = await this.getOrder(ctx, _orderId);

        if (order.state != State.SHIPPED) {
            throw new Error("The state of order " + _orderId + " does not allow this action");
        }

        if (ctx.clientIdentity.assertAttributeValue("hf.Affiliation", "buyer")) {
            order.buyerSigned = true;
            console.info("Order " + _orderId + " arrival has been signed by the buyer.");
        }

        if (ctx.clientIdentity.assertAttributeValue("hf.Affiliation", "freight")) {
            order.freightSigned = true;
            console.info("Order " + _orderId + " arrival has been signed by the freight company.");
        }

        if (order.buyerSigned && order.freightSigned) {
            order.state = State.DELIVERED;
            console.info("Order " + _orderId + " has been delivered.");
        }

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info("============= END : signArrival ===========");
    }
}
