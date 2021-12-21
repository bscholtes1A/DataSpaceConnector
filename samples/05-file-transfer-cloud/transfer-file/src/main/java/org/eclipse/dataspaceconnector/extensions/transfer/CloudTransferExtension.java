package org.eclipse.dataspaceconnector.extensions.transfer;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.common.azure.BlobStoreApi;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.reader.azure.blob.BlobStoreReader;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.transfer.inline.core.InlineDataFlowController;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.writer.s3.S3BucketWriter;

import java.time.temporal.ChronoUnit;

import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;

public class CloudTransferExtension implements ServiceExtension {
    public static final String USE_EU_POLICY = "use-eu";

    @Override
    public String name() {
        return "Cloud-Based Transfer";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerFlowController(context);
        registerDataEntries(context);
        savePolicies(context);
    }

    private void registerFlowController(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var dataFlowMgr = context.getService(DataFlowManager.class);
        var dataAddressResolver = context.getService(DataAddressResolver.class);
        var blobStoreApi = context.getService(BlobStoreApi.class);

        var dataOperatorRegistry = context.getService(DataOperatorRegistry.class);
        dataOperatorRegistry.registerReader(new BlobStoreReader(blobStoreApi));

        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .withBackoff(500, 5000, ChronoUnit.MILLIS)
                .withMaxRetries(3);
        dataOperatorRegistry.registerWriter(new S3BucketWriter(context.getMonitor(), context.getTypeManager(), retryPolicy));

        dataFlowMgr.register(new InlineDataFlowController(vault, context.getMonitor(), dataOperatorRegistry, dataAddressResolver));
    }

    private void registerDataEntries(ServiceExtensionContext context) {
        AssetLoader assetIndex = context.getService(AssetLoader.class);

        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("container", "src-container")
                .property("blobname", "test-document.txt")
                .build();

        String assetId = "test-document";
        Asset asset = Asset.Builder.newInstance().id(assetId).policyId(USE_EU_POLICY).build();

        assetIndex.accept(asset, dataAddress);
    }

    private void savePolicies(ServiceExtensionContext context) {
        PolicyRegistry policyRegistry = context.getService(PolicyRegistry.class);

        LiteralExpression spatialExpression = new LiteralExpression("ids:absoluteSpatialPosition");
        var euConstraint = AtomicConstraint.Builder.newInstance().leftExpression(spatialExpression).operator(IN).rightExpression(new LiteralExpression("eu")).build();
        var euUsePermission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("idsc:USE").build()).constraint(euConstraint).build();
        var euPolicy = Policy.Builder.newInstance().id(USE_EU_POLICY).permission(euUsePermission).build();
        policyRegistry.registerPolicy(euPolicy);
    }

}
