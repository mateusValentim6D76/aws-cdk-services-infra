package com.myorg;


import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.constructs.Construct;

public class SnsCdkStack extends Stack {

    private final SnsTopic productEventsTopic;

    public SnsCdkStack(final Construct scope, final String id) {

        this(scope, id, null);
    }

    public SnsCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        productEventsTopic = SnsTopic.Builder.create(Topic.Builder.create(this, "Sns01")
                        .topicName("mv-product-events")
                        .build())
                .build();

        productEventsTopic.getTopic()
                .addSubscription(EmailSubscription.Builder.create("mvalentimcloud@gmail.com")
                        .json(true)
                        .build());
    }

    public SnsTopic getProductEventsTopic() {
        return productEventsTopic;
    }
}
