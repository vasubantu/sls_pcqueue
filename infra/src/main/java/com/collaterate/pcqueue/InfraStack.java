/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/
package com.collaterate.pcqueue;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Subnet;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.CfnBucket;
import software.constructs.Construct;
import java.util.List;
import java.util.Map;

public class InfraStack extends Stack {
    public InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        CfnParameter secretVar = CfnParameter.Builder.create(this, "secretVar")
                .description("Some variable that can be passed in at deploy-time.")
                .type("String")
                .build();

        Table globalTable = Table.Builder.create(this, "Table")
                .partitionKey(Attribute.builder().name("id").type(AttributeType.NUMBER).build())
                .tableName("collaterate_pcqueue_delta")
                .replicationRegions(List.of("us-east-1"))
                .billingMode(BillingMode.PROVISIONED)
                .build();

        globalTable.autoScaleWriteCapacity(EnableScalingProps.builder()
                .minCapacity(1)
                .maxCapacity(10)
                .build()).scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(75).build());

        SubnetSelection subnetSelection = SubnetSelection.builder()
                .subnets(List.of(Subnet.fromSubnetId(this, "1", "subnet-2a9efa70"),
                        Subnet.fromSubnetId(this, "2", "subnet-7d849041")))
                .build();

        Vpc vpc = Vpc.Builder.create(this, "collaterate_pcqueue_vpc")
                .maxAzs(3)
                .build();

        Role myRole = Role.Builder.create(this, "collaterate_pcqueue_role")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromManagedPolicyArn(this,"AWSLBER","arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"),
                        ManagedPolicy.fromManagedPolicyArn(this,"AWSLVPCER","arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"),
                        ManagedPolicy.fromManagedPolicyArn(this,"ADDBFA","arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"),
                        ManagedPolicy.fromManagedPolicyArn(this,"AAPIGIFA","arn:aws:iam::aws:policy/AmazonAPIGatewayInvokeFullAccess"),
                        ManagedPolicy.fromManagedPolicyArn(this,"SMRW","arn:aws:iam::aws:policy/SecretsManagerReadWrite")))
                .build();

        Function function = Function.Builder.create(this, "collaterate-pcqueue")
        .runtime(Runtime.JAVA_11)
        .handler("com.collaterate.pcqueue.CollateratePcQueueDataRetrieveLambdaHandler::handleRequest")
        .memorySize(1024)
        .timeout(Duration.seconds(20))
        .functionName("CollateratePcQueueDataRetrieveLambdaHandler")
        .code(Code.fromAsset("../assets/function.jar"))
        .environment(Map.of("SECRET_ARN_READER",secretVar.getValueAsString(),
        		"REGION","us-east-1"))
        .vpcSubnets(subnetSelection)
        .securityGroups(List.of(SecurityGroup.fromSecurityGroupId(this,"securityGroupId", "sg-39a6b648")))
        .vpc(vpc)
        .role(myRole)
        .build();

        //Passing EventBridge Rule
        Rule rule = Rule.Builder.create(this, "lambda-scheduler")
                .ruleName("collaterate_pcqueue_lamdba_rule")
                .schedule(Schedule.rate(Duration.minutes(10)))
                .build();

        //adding rule to Lambda
       // rule.addTarget(new LambdaFunction(function));
        rule.addTarget(new LambdaFunction(function));

    }
}
