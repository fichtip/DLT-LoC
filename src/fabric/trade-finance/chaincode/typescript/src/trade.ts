/*
 * SPDX-License-Identifier: Apache-2.0
 */

import { Context, Contract } from 'fabric-contract-api';
import { Order, State } from './order';

export class TradeFinance extends Contract {

    public async queryOrder(ctx: Context, _orderId: string): Promise<string> {
        const orderAsBytes = await ctx.stub.getState(_orderId); // get the order from chaincode state
        if (!orderAsBytes || orderAsBytes.length === 0) {
            throw new Error(`${_orderId} does not exist`);
        }
        console.log(orderAsBytes.toString());
        return orderAsBytes.toString();
    }

    public async createOrder(ctx: Context,
        _orderId: string,
        _productId: number,
        _quantity: number,
        _price: number,
        _shippingAddress: string,
        _latestDeliveryDate: string,
        _shippingCosts: number) {
        console.info('============= START : Create Order ===========');

        const order: Order = {
            docType: 'order',
            state: State.CREATED,
            orderId: _orderId,
            productId: _productId,
            quantity: _quantity,
            price: _price,
            shippingAddress: _shippingAddress,
            latestDeliveryDate: _latestDeliveryDate,
            shippingCosts: _shippingCosts,
            trackingCode: undefined,
            buyerSigned: undefined,
            freightSigned: undefined
        };

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info('============= END : Create Order ===========');
    }

    public async queryAllOrders(ctx: Context): Promise<string> {
        const startKey = '';
        const endKey = '';
        const allResults = [];
        for await (const { key, value } of ctx.stub.getStateByRange(startKey, endKey)) {
            const strValue = Buffer.from(value).toString('utf8');
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

    public async cancelOrder(ctx: Context, _orderId: string) {
        console.info('============= START : cancelOrder ===========');

        const orderAsBytes = await ctx.stub.getState(_orderId); // get the order from chaincode state
        if (!orderAsBytes || orderAsBytes.length === 0) {
            throw new Error(`${_orderId} does not exist`);
        }
        const order: Order = JSON.parse(orderAsBytes.toString());

        if (order.state == State.CLOSED || order.state == State.DELIVERED || order.state == State.SHIPPED || order.state == State.CANCELLED) {
            throw new Error(`The state of order ${_orderId} does not allow this action`);
        }

        order.state = State.CANCELLED;

        await ctx.stub.putState(_orderId, Buffer.from(JSON.stringify(order)));
        console.info('============= END : cancelOrder ===========');
    }

}
