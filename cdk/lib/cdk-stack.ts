import { CfnOutput, CfnParameter, Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as cloudfront from 'aws-cdk-lib/aws-cloudfront';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as acm from 'aws-cdk-lib/aws-certificatemanager';
import * as cloudfront_origins from 'aws-cdk-lib/aws-cloudfront-origins';
import * as s3deploy from 'aws-cdk-lib/aws-s3-deployment';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as route53Targets from 'aws-cdk-lib/aws-route53-targets';
import * as events from 'aws-cdk-lib/aws-events';
import { Construct } from 'constructs';
import { ManagedPolicy, Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';
import { SecurityGroup, Vpc, Subnet } from 'aws-cdk-lib/aws-ec2';
import { AttributeType, BillingMode } from 'aws-cdk-lib/aws-dynamodb';

export class CdkStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    let secretVar = "arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-test-d-xD64OF";

    const stage = this.node.tryGetContext('stage');
    console.log(stage);

    if (stage.toUpperCase() === 'TEST-A')
      secretVar = "arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-test-a-ELkwqo";
    else if (stage.toUpperCase() === 'TEST-B')
      secretVar = "arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-test-b-Bl4Z1k";
    else if (stage.toUpperCase() === 'TEST-C')
      secretVar = "arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-test-c-n1QcV0";
    else if (stage.toUpperCase() === 'TEST-D')
      secretVar = "arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-test-d-xD64OF";
    else if (stage.toUpperCase() === 'STAGE')
      secretVar = "arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-stage-eg9dtt";
    else if (stage.toUpperCase() === 'PROD')
      secretVar = "arn:aws:secretsmanager:us-east-1:538493872512:secret:collaterate-aurora-prod-Ywlenr";

    console.log(secretVar);

    const table = new dynamodb.Table(this, "Table", {
      partitionKey: {name: "id", type: AttributeType.NUMBER},
      tableName: "collaterate_pcqueue_delta1",
      billingMode: BillingMode.PROVISIONED,
    });

    const tableScaling = table.autoScaleWriteCapacity({
      minCapacity: 1,
      maxCapacity: 10,
    });

    tableScaling.scaleOnUtilization({
      targetUtilizationPercent: 75
    });

    const lambdaRole = new Role(this, `collaterate_pcqueue_role-${stage}`,
      { assumedBy: new ServicePrincipal("lambda.amazonaws.com") });

    lambdaRole.addManagedPolicy(ManagedPolicy.fromManagedPolicyArn(this, "AWSLBER", "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"));
    lambdaRole.addManagedPolicy(ManagedPolicy.fromManagedPolicyArn(this, "AWSLVPCER", "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"));
    lambdaRole.addManagedPolicy(ManagedPolicy.fromManagedPolicyArn(this, "ADDBFA", "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"));
    lambdaRole.addManagedPolicy(ManagedPolicy.fromManagedPolicyArn(this, "AAPIGIFA", "arn:aws:iam::aws:policy/AmazonAPIGatewayInvokeFullAccess"));
    lambdaRole.addManagedPolicy(ManagedPolicy.fromManagedPolicyArn(this, "SMRW", "arn:aws:iam::aws:policy/SecretsManagerReadWrite"));

    const subnetSelection = {
      subnets: [
        Subnet.fromSubnetId(this, "1", "subnet-2a9efa70"),
        Subnet.fromSubnetId(this, "2", "subnet-7d849041")
      ]
    };

    const vpc = new Vpc(this, `collaterate_pcqueue_vpc-${stage}`, {
      maxAzs: 2
    });

    const pcQueueLambdaFunction = new lambda.Function(this, "collaterate-pcqueue", {
      runtime: lambda.Runtime.JAVA_11,
      handler: "com.collaterate.pcqueue.CollateratePcQueueDataRetrieveLambdaHandler::handleRequest",
      memorySize: 1024,
      timeout: Duration.seconds(20),
      functionName: `CollateratePcQueueDataRetrieveLambdaHandler-${stage}`,
      code: lambda.Code.fromAsset('../assets/function.jar'),
      environment: {
        "SECRET_ARN_READER": secretVar,
        "REGION": "us-east-1"
      },
      vpcSubnets: subnetSelection,
      securityGroups: [SecurityGroup.fromSecurityGroupId(this, "securityGroup", "sg-39a6b648")],
      vpc: vpc,
      role: lambdaRole
    });

    const pcQueueLambdaFunctionTarget = new eventsTargets.LambdaFunction(pcQueueLambdaFunction);

    const schedulerRule = new events.Rule(this, "lambda-scheduler", {
      ruleName: `collaterate_pcqueue_lambda_rule-${stage}`,
      schedule: events.Schedule.rate(Duration.minutes(10)),
      targets: [pcQueueLambdaFunctionTarget]
    });

    const zone = route53.HostedZone.fromLookup(this, 'Zone', { domainName: 'test-d-collaterate.net' });
    console.log(zone.hostedZoneId);
    const siteDomain = 'pcqueue.' + 'test-d-collaterate.net';
    const cloudfrontOAI = new cloudfront.OriginAccessIdentity(this, 'cloudfront-OAI', {
      comment: `OAI for PC Queue`
    });

    new CfnOutput(this, 'Site', { value: 'https://' + siteDomain });

    // Content bucket
    const siteBucket = new s3.Bucket(this, 'SiteBucket', {
      bucketName: `${siteDomain}`,
      publicReadAccess: false,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,

      /**
       * The default removal policy is RETAIN, which means that cdk destroy will not attempt to delete
       * the new bucket, and it will remain in your account until manually deleted. By setting the policy to
       * DESTROY, cdk destroy will attempt to delete the bucket, but will error if the bucket is not empty.
       */
      removalPolicy: RemovalPolicy.DESTROY, // NOT recommended for production code

      /**
       * For sample purposes only, if you create an S3 bucket then populate it, stack destruction fails.  This
       * setting will enable full cleanup of the demo.
       */
      autoDeleteObjects: true, // NOT recommended for production code
    });

    siteBucket.addToResourcePolicy(new iam.PolicyStatement({
      actions: ['s3:GetObject'],
      resources: [siteBucket.arnForObjects('*')],
      principals: [new iam.CanonicalUserPrincipal(cloudfrontOAI.cloudFrontOriginAccessIdentityS3CanonicalUserId)]
    }));
    new CfnOutput(this, 'Bucket', { value: siteBucket.bucketName });

    const certificate = acm.Certificate.fromCertificateArn(this, 'SiteCertificate', "arn:aws:acm:us-east-1:538493872512:certificate/30213864-5f3c-401f-87d5-d86a9e133fc1");
    new CfnOutput(this, 'Certificate', { value: certificate.certificateArn });

    const distribution = new cloudfront.Distribution(this, 'SiteDistribution', {
      certificate: certificate,
      defaultRootObject: "index.html",
      domainNames: [siteDomain],
      minimumProtocolVersion: cloudfront.SecurityPolicyProtocol.TLS_V1_2_2021,
      errorResponses: [
        {
          httpStatus: 403,
          responseHttpStatus: 403,
          responsePagePath: '/error.html',
          ttl: Duration.minutes(30),
        }
      ],
      defaultBehavior: {
        origin: new cloudfront_origins.S3Origin(siteBucket, { originAccessIdentity: cloudfrontOAI }),
        compress: true,
        allowedMethods: cloudfront.AllowedMethods.ALLOW_GET_HEAD_OPTIONS,
        viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
      }
    })

    new CfnOutput(this, 'DistributionId', { value: distribution.distributionId });

    // Route53 alias record for the CloudFront distribution
    new route53.ARecord(this, 'SiteAliasRecord', {
      recordName: siteDomain,
      target: route53.RecordTarget.fromAlias(new route53Targets.CloudFrontTarget(distribution)),
      zone
    });

    // Deploy site contents to S3 bucket
    new s3deploy.BucketDeployment(this, 'DeployWithInvalidation', {
      sources: [s3deploy.Source.asset('./site-contents')],
      destinationBucket: siteBucket,
      distribution,
      distributionPaths: ['/*'],
    });

  }
}
