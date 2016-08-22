package ua.naiksoftware.rxgoogle;

import rx.SingleSubscriber;
import rx.Subscriber;

/**
 * Wrapper for Subscriber or SingleSubscriber
 *
 * Created by naik on 22.08.16.
 */

class SubscriberWrapper {

    private final Subscriber subscriber;
    private final SingleSubscriber singleSubscriber;

    SubscriberWrapper(Subscriber subscriber) {
        this.subscriber = subscriber;
        singleSubscriber = null;
    }

    SubscriberWrapper(SingleSubscriber singleSubscriber) {
        this.singleSubscriber = singleSubscriber;
        subscriber = null;
    }

    boolean isUnsubscribed() {
        return subscriber != null ? subscriber.isUnsubscribed() : singleSubscriber.isUnsubscribed();
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public SingleSubscriber getSingleSubscriber() {
        return singleSubscriber;
    }
}
