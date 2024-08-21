package com.myorg;


import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

public class DynamoDbStack extends Stack {

    private final Table productEventsDynamoDB;

    public DynamoDbStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DynamoDbStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        productEventsDynamoDB = Table.Builder.create(this, "productEventsDynamoDb")
                .tableName("product-events")
                .billingMode(BillingMode.PROVISIONED)
                .readCapacity(1)
                .writeCapacity(1)
                .partitionKey(Attribute.builder()
                        .name("pk")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute("ttl")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        productEventsDynamoDB.autoScaleReadCapacity(EnableScalingProps.builder()
                .minCapacity(1)
                .maxCapacity(4)
                .build())
                    .scaleOnUtilization(UtilizationScalingProps.builder()
                            .targetUtilizationPercent(70)
                            .scaleInCooldown(Duration.seconds(30))
                            .scaleOutCooldown(Duration.seconds(30))
                            .build());

        productEventsDynamoDB.autoScaleWriteCapacity(EnableScalingProps.builder()
                        .minCapacity(1)
                        .maxCapacity(4)
                        .build())
                .scaleOnUtilization(UtilizationScalingProps.builder()
                        .targetUtilizationPercent(70)
                        .scaleInCooldown(Duration.seconds(30))
                        .scaleOutCooldown(Duration.seconds(30))
                        .build());
    }

    public Table getProductEventsDynamoDB() {
        return productEventsDynamoDB;
    }
}
