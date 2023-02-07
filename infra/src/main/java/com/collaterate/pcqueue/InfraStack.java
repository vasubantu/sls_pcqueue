/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/
package com.collaterate.pcqueue;

import software.amazon.awscdk.*;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.certificatemanager.*;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.cognito.CognitoDomainOptions;
import software.amazon.awscdk.services.cognito.CustomDomainOptions;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolDomainOptions;
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
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.s3.*;
import software.constructs.Construct;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.CfnBucket;

import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Map;

public class InfraStack extends Stack {
    public InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        String secretVar ="";

        String env = (String)this.getNode().tryGetContext("env");

        if(env.toUpperCase().equals("TEST-A"))
            secretVar="arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-test-a-ELkwqo";
        else if(env.toUpperCase().equals("TEST-B"))
            secretVar="arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-test-b-Bl4Z1k";
        else if(env.toUpperCase().equals("TEST-C"))
            secretVar="arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-test-c-n1QcV0";
        else if(env.toUpperCase().equals("TEST-D"))
            secretVar="arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-test-d-xD64OF";
        else if(env.toUpperCase().equals("STAGE"))
            secretVar="arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-stage-eg9dtt";
        else if(env.toUpperCase().equals("PROD"))
            secretVar="arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-prod-Ywlenr";

//        Table globalTable = Table.Builder.create(this, "Table")
//                .partitionKey(Attribute.builder().name("id").type(AttributeType.NUMBER).build())
//                .tableName("collaterate_pcqueue_delta1")
//                .replicationRegions(List.of("us-east-1"))
//                .billingMode(BillingMode.PROVISIONED)
//                .build();
//
//        globalTable.autoScaleWriteCapacity(EnableScalingProps.builder()
//                .minCapacity(1)
//                .maxCapacity(10)
//                .build()).scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(75).build());

        SubnetSelection subnetSelection = SubnetSelection.builder()
                .subnets(List.of(Subnet.fromSubnetId(this, "1", "subnet-2a9efa70"),
                        Subnet.fromSubnetId(this, "2", "subnet-7d849041")))
                .build();

        Vpc vpc = Vpc.Builder.create(this, "collaterate_pcqueue_vpc-"+env)
                .maxAzs(2)
                .build();

        Role myRole = Role.Builder.create(this, "collaterate_pcqueue_role-"+env)
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
                .functionName("CollateratePcQueueDataRetrieveLambdaHandler-"+env)
                .code(Code.fromAsset("../assets/function.jar"))
                .environment(Map.of("SECRET_ARN_READER",secretVar,
                        "REGION","us-east-1"))
                .vpcSubnets(subnetSelection)
                .securityGroups(List.of(SecurityGroup.fromSecurityGroupId(this,"securityGroupId", "sg-39a6b648")))
                .vpc(vpc)
                .role(myRole)
                .build();

        //Passing EventBridge Rule
        Rule rule = Rule.Builder.create(this, "lambda-scheduler")
                .ruleName("collaterate_pcqueue_lamdba_rule-"+env)
                .schedule(Schedule.rate(Duration.minutes(10)))
                .build();

        //adding rule to Lambda
        // rule.addTarget(new LambdaFunction(function));
        rule.addTarget(new LambdaFunction(function));

        String certificateArn = "arn:aws:acm:us-east-1:538493872512:certificate/30213864-5f3c-401f-87d5-d86a9e133fc1";
        ICertificate domainCert = Certificate.fromCertificateArn(this, "domainCert", certificateArn);


     // Route53 hosted zone created out-of-band
        IHostedZone zone = HostedZone.fromLookup(this, "HostedZone", HostedZoneProviderProps.builder().domainName("test-d-collaterate.net").build());

//        DnsValidatedCertificate websiteCertificate = DnsValidatedCertificate.Builder.create(this, "WebsiteCertificate")
//                .hostedZone(zone)
//                .region("us-east-1")
//                .domainName("test-d-collaterate.net")
//                .subjectAlternativeNames(List.of(String.format("www.%s", "test-d-collaterate.net")))
//                .build();

        CnameRecord.Builder.create(this, "CnameApiRecord")
                .recordName("pcqueue.test-d-collaterate.net")
                .zone(zone)
                .domainName("test-d-collaterate.net")
                .build();

        Bucket websiteBucket = Bucket.Builder.create(this, "SecretWitBucket")
                .bucketName(String.format("collateratepcqueue.website"))
                .encryption(BucketEncryption.UNENCRYPTED)
                .websiteIndexDocument("index.html")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

//        OriginAccessIdentity webOai = OriginAccessIdentity.Builder.create(this, "WebOai")
//                .comment(String.format("OriginAccessIdentity for %s", "pcqueue.test-d-collaterate.net"))
//                .build();
//
//
//        websiteBucket.grantRead(webOai);

        CloudFrontWebDistribution distribution = new CloudFrontWebDistribution(this, "CloudFrontWebDistribution",
                new CloudFrontWebDistributionProps.Builder()
                        .originConfigs(List.of(new SourceConfiguration.Builder()
                                .s3OriginSource(new S3OriginConfig.Builder()
                                        .s3BucketSource(websiteBucket)
                                        .build())
                                .behaviors(List.of(new Behavior.Builder()
                                        .isDefaultBehavior(true)
                                        .cachedMethods(CloudFrontAllowedCachedMethods.GET_HEAD)
                                        .allowedMethods(CloudFrontAllowedMethods.ALL)
                                        .build()))
                                .build()))
                        .defaultRootObject("index.html")
                        .errorConfigurations(List.of(CfnDistribution.CustomErrorResponseProperty.builder()
                                .errorCode(403)
                                .responsePagePath("/index.html")
                                .responseCode(200)
                                .build()))
                        .enableIpV6(true)
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .viewerCertificate(ViewerCertificate.fromAcmCertificate(domainCert, ViewerCertificateOptions.builder()
                                .aliases(List.of("pcqueue.test-d-collaterate.net", "*.pcqueue.test-d-collaterate.net"))
                                .securityPolicy(SecurityPolicyProtocol.TLS_V1)// default
                                .sslMethod(SSLMethod.SNI).build())
                        		)
                        .build());

    }
}
