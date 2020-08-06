/*
 * SPDX-License-Identifier: Apache-2.0
 */

export enum State {
    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CLOSED,
    CANCELLED
}

export class Order {
    public docType?: string;
    public state: State;
    public orderId: string;
    public productId: number;
    public quantity: number;
    public price: number;
    public shippingAddress: string;
    public latestDeliveryDate: string;
    public shippingCosts: number;
    public trackingCode: string;
    public buyerSigned: boolean;
    public freightSigned: boolean;
}
